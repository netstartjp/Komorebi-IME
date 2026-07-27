package me.zssu.ime.ime

import android.content.Context
import me.zssu.ime.keyboard.InputStyle
import me.zssu.ime.mozc.MozcEngine
import org.mozc.android.inputmethod.japanese.protobuf.ProtoConfig.Config
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.CompositionMode
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.DecoderExperimentParams
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.Input
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.KeyEvent
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.Output
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.Request
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.SessionCommand
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCandidateWindow.Category
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCandidateWindow.CandidateAttribute
import me.zssu.ime.settings.ImeSettings

/**
 * Task-level view of a mozc session: sends key events and session commands, and flattens the
 * [Output] proto into the three things the IME actually renders.
 *
 * The [Request] configured here is what tells mozc it is driving a mobile 12-key surface. Getting
 * it wrong is the classic reason a from-scratch mozc client produces desktop-style behaviour on a
 * phone (no mobile prediction, wrong segment sizing), so it is set once at session creation and
 * refreshed whenever the layout's [InputStyle] changes.
 */
class MozcSession(context: Context) {

    private val engine: MozcEngine? = MozcEngine.get(context)

    val isAvailable: Boolean get() = engine != null

    data class State(
        /** Text mozc has finalised; append it to the editor. */
        val committedText: String = "",
        /** Current composition, to be shown underlined. */
        val preedit: String = "",
        /** Caret position within [preedit], in Java chars. */
        val preeditCursor: Int = 0,
        val candidates: List<Candidate> = emptyList(),
        val focusedCandidateIndex: Int = -1,
        /** Mozc's visible preedit/conversion segments, retained so the client can draw boundaries. */
        val segments: List<Segment> = emptyList(),
        /** True after conversion starts; suggestions and predictions do not count as conversion. */
        val isConverting: Boolean = false,
        /**
         * Whether mozc handled the key at all.
         *
         * False means it declined and expects the client to act — which it does for any key it has
         * no use for in the current state, notably space on an empty composition. Treating that as
         * "nothing to do" silently swallows the keystroke.
         */
        val consumed: Boolean = false,
    ) {
        val hasComposition: Boolean get() = preedit.isNotEmpty()
    }

    data class Candidate(
        val id: Int,
        val text: String,
        /** True when Mozc marks the candidate with its DELETABLE history attribute. */
        val deletable: Boolean = false,
        /** Used to keep supplementary user dictionaries from overwhelming live suggestions. */
        internal val fromUserDictionary: Boolean = false,
    )
    data class Segment(val text: String, val highlighted: Boolean)

    private var requestedStyle: InputStyle? = null
    private var currentStyle: InputStyle? = null
    private var configApplied = false
    private var incognito = false
    private var phoneToggleEnabled = false
    private var deletableCandidateIds: Set<Int> = emptySet()

    fun applyInputStyle(style: InputStyle) {
        requestedStyle = style
        val effectiveStyle = if (phoneToggleEnabled && style == InputStyle.FLICK_HIRAGANA) {
            InputStyle.TOGGLE_FLICK_HIRAGANA
        } else {
            style
        }
        if (effectiveStyle == currentStyle) return
        val engine = engine ?: return
        val request = Request.newBuilder()
            .setSpecialRomanjiTable(
                Request.SpecialRomanjiTable.forNumber(effectiveStyle.mozcTableNumber)
                    ?: Request.SpecialRomanjiTable.DEFAULT_TABLE
            )
            .setZeroQuerySuggestion(true)
            .setMixedConversion(true)
            .setUpdateInputModeFromSurroundingText(false)
            .setAutoPartialSuggestion(true)
            .setSpaceOnAlphanumeric(Request.SpaceOnAlphanumeric.SPACE_OR_CONVERT_COMMITTING_COMPOSITION)
            // Keep an unfinished reading intact at either edge. The keyboard's arrows move within
            // the composition one character at a time; crossing an edge must not silently commit
            // the whole reading and hand the movement to the editor.
            .setCrossingEdgeBehavior(Request.CrossingEdgeBehavior.DO_NOTHING)
            // Forgives a missing or stray dakuten/small kana: "かつこう" still finds 学校. Costs
            // nothing extra — it widens an existing dictionary lookup rather than adding one.
            .setKanaModifierInsensitiveConversion(true)
            // Let corrected readings consult learned history as well as the static dictionary.
            // One query retains the strongest learned correction without multiplying the work
            // performed after every tap.
            .setDecoderExperimentParams(
                DecoderExperimentParams.newBuilder()
                    .setTypingCorrectionApplyUserHistorySize(1)
            )
            .build()
        engine.eval(
            Input.newBuilder()
                .setType(Input.CommandType.SET_REQUEST)
                .setRequest(request)
        )
        applyConfig(engine)

        // The table alone is not enough. It controls transliteration; the composition mode controls
        // whether mozc runs conversion over the result, so the latin and numeric planes have to be
        // put into HALF_ASCII or they come back with kanji candidates for "abc".
        engine.sendCommand(
            SessionCommand.newBuilder()
                .setType(SessionCommand.CommandType.SWITCH_COMPOSITION_MODE)
                .setCompositionMode(
                    CompositionMode.forNumber(effectiveStyle.mozcCompositionMode)
                        ?: CompositionMode.HIRAGANA
                )
                .build()
        )
        currentStyle = effectiveStyle
    }

    /**
     * Sends one key.
     *
     * [text] is a *romanji-table key*, not the character the user sees. mozc's FLICK_TO_HIRAGANA
     * table is keyed on ASCII — `1` produces あ, `_` produces い, `*` cycles dakuten/small forms —
     * and the table is what implements dakuten cycling and small-kana rules. Sending the kana
     * directly would bypass all of it, so a single ASCII char goes out as `key_code` and lets mozc
     * do the composing.
     *
     * Anything outside ASCII is text we resolved ourselves and mozc has no table entry for (emoji,
     * a symbol run), so it goes out as `key_string` for direct insertion.
     */
    fun sendText(text: String): State? {
        // In flick-only mode, finish the *previous* ordinary key immediately before starting the
        // next one. Finishing after every key breaks the table's suffix modifiers: `か` must still
        // be open when `*`, `[`, `]`, or backtick arrives so it can become が/ぱ/small kana.
        // Doing it here still makes repeated centre taps produce separate characters.
        if (!phoneToggleEnabled && !FlickTableInput.isModifierKey(currentStyle, text)) {
            stopKeyToggling()
        }

        val key = KeyEvent.newBuilder()
        val singleChar = text.length == 1 && text[0].code in 0x20..0x7E
        if (singleChar) {
            key.keyCode = text[0].code
        } else {
            key.keyString = text
        }
        return engine?.sendKey(key.build())?.toState()
    }

    fun sendSpecialKey(
        specialKey: KeyEvent.SpecialKey,
        shift: Boolean = false,
    ): State? {
        val key = KeyEvent.newBuilder().setSpecialKey(specialKey)
        if (shift) key.addModifierKeys(KeyEvent.ModifierKey.SHIFT)
        return engine?.sendKey(key.build())?.toState()
    }

    /**
     * Opens the conversion candidate list with one user action.
     *
     * A freshly entered 12-key character is intentionally left open for ゛小゜ modification.
     * Sending Space directly can therefore spend the first press only closing that table chunk.
     * Close it explicitly, then send Space so the same press reaches Mozc's Convert command.
     */
    fun convertOrSpace(): State? {
        stopKeyToggling()
        return sendSpecialKey(KeyEvent.SpecialKey.SPACE)
    }

    /**
     * Turns on the two correction features. Both are AND-ed with the Request flags above and both
     * default to off.
     *
     * `use_typing_correction` is what reaches our TypingCorrectionModel: upstream leaves the
     * predictor's typing-correction path in place but ships a stub supplemental model, so on a
     * stock OSS build this flag changes nothing. See
     * patches/0001-rule-based-typing-correction.patch.
     */
    private fun applyConfig(engine: MozcEngine) {
        if (configApplied) return
        val output = engine.eval(Input.newBuilder().setType(Input.CommandType.GET_CONFIG))
        val builder =
            if (output != null && output.hasConfig()) output.config.toBuilder()
            else Config.newBuilder()
        engine.eval(
            Input.newBuilder()
                .setType(Input.CommandType.SET_CONFIG)
                .setConfig(
                    builder
                        .setUseTypingCorrection(true)
                        .setUseKanaModifierInsensitiveConversion(true)
                        .setUseHistorySuggest(true)
                        .setSuggestionsSize(9)
                        .setComposingTimeoutThresholdMsec(
                            if (phoneToggleEnabled) ImeSettings.TOGGLE_TIMEOUT_MILLIS else 0
                        )
                        .setIncognitoMode(incognito)
                )
        )
        configApplied = true
    }

    /**
     * Prevents both learning from and consulting mutable history in sensitive editors.
     *
     * This is Mozc's own incognito switch, not merely a hidden candidate view: conversion requests
     * therefore cannot accidentally update history while a password field is active.
     */
    fun setIncognitoMode(enabled: Boolean) {
        if (incognito == enabled && configApplied) return
        incognito = enabled
        configApplied = false
        engine?.let(::applyConfig)
    }

    fun setPhoneToggleEnabled(enabled: Boolean) {
        if (phoneToggleEnabled == enabled && configApplied) return
        phoneToggleEnabled = enabled
        configApplied = false
        currentStyle = null
        engine?.let(::applyConfig)
        requestedStyle?.let(::applyInputStyle)
    }

    fun submit(): State? = sendSessionCommand(SessionCommand.CommandType.SUBMIT)

    fun revert(): State? = sendSessionCommand(SessionCommand.CommandType.REVERT)

    fun resetContext(): State? = sendSessionCommand(SessionCommand.CommandType.RESET_CONTEXT)

    /** Pulls the last commit back into a composition so it can be re-converted. */
    fun undo(): State? = sendSessionCommand(SessionCommand.CommandType.UNDO)

    /** Backspace on an empty composition is the editor's problem, not mozc's. */
    fun undoOrRewind(): State? = sendSessionCommand(SessionCommand.CommandType.UNDO_OR_REWIND)

    fun selectCandidate(candidateId: Int): State? = engine?.sendCommand(
        SessionCommand.newBuilder()
            .setType(SessionCommand.CommandType.SUBMIT_CANDIDATE)
            .setId(candidateId)
            .build()
    )?.toState()

    fun highlightCandidate(candidateId: Int): State? = engine?.sendCommand(
        SessionCommand.newBuilder()
            .setType(SessionCommand.CommandType.HIGHLIGHT_CANDIDATE)
            .setId(candidateId)
            .build()
    )?.toState()

    fun deleteCandidateFromHistory(candidateId: Int): State? = engine?.sendCommand(
        SessionCommand.newBuilder()
            .setType(SessionCommand.CommandType.DELETE_CANDIDATE_FROM_HISTORY)
            .setId(candidateId)
            .build()
    )?.toState()

    /**
     * Ends any toggle-in-progress. Flick input still needs this: a same-key tap sequence is what
     * mozc uses to cycle characters, and moving the caret must not be absorbed by that cycle.
     */
    fun stopKeyToggling(): State? = sendSessionCommand(SessionCommand.CommandType.STOP_KEY_TOGGLING)

    fun close() {
        engine?.deleteSession()
        requestedStyle = null
        currentStyle = null
    }

    private fun sendSessionCommand(type: SessionCommand.CommandType): State? =
        engine?.sendCommand(SessionCommand.newBuilder().setType(type).build())?.toState()

    /**
     * Whether candidate [id] is for a reading other than the one being typed.
     *
     * See [exactReadingFirst] for what is done with the answer.
     *
     * mozc's mobile prediction mixes two kinds of candidate into one list: conversions of exactly
     * what was typed, and predictions that assume more typing to come. It ranks them together by
     * cost, so a prediction routinely outranks the plain conversion — typing でんわ offers 電話番号
     * above 電話, and あり offers ありだと above あり. That is useful when it guesses right and a
     * nuisance when the word was already finished, which is the common case.
     *
     * `CandidateWord.key` is set exactly when the candidate's reading differs from the composition
     * (it covers both longer readings like でんわばんごう and shorter partial ones like こんにち),
     * so it is the signal for pushing those below the exact matches.
     */
    private fun idsExtendingReading(reading: String): Set<Int> {
        val metadata = if (hasAllCandidateWords()) {
            allCandidateWords.candidatesList.take(CandidateRanking.MAX_LIVE_POOL_SIZE)
        } else {
            return emptySet()
        }
        var ids: MutableSet<Int>? = null
        for (word in metadata) {
            if (word.hasKey() && word.key != reading) {
                (ids ?: HashSet<Int>().also { ids = it }).add(word.id)
            }
        }
        return ids ?: emptySet()
    }

    private fun Output.toState(): State {
        val preeditText = StringBuilder()
        var cursor = 0
        if (hasPreedit()) {
            preedit.segmentList.forEach { preeditText.append(it.value) }
            cursor = preedit.cursor
        }

        val candidateMetadata = if (hasAllCandidateWords()) {
            allCandidateWords.candidatesList.take(CandidateRanking.MAX_LIVE_POOL_SIZE)
        } else {
            emptyList()
        }
        if (hasAllCandidateWords()) {
            // The toolbar can display only a handful of candidates. Scanning and allocating
            // metadata for Mozc's entire result on every keystroke adds latency without changing
            // anything the user can act on.
            deletableCandidateIds = candidateMetadata
                .filter { CandidateAttribute.DELETABLE in it.attributesList }
                .map { it.id }
                .toSet()
        } else if (!hasCandidateWindow()) {
            deletableCandidateIds = emptySet()
        }
        val userDictionaryCandidateIds = if (candidateMetadata.isNotEmpty()) {
            candidateMetadata.asSequence()
                .filter { CandidateAttribute.USER_DICTIONARY in it.attributesList }
                .map { it.id }
                .toHashSet()
        } else {
            emptySet()
        }
        val candidates = if (hasCandidateWindow()) {
            val visible = candidateWindow.candidateList.map {
                Candidate(
                    id = it.id,
                    text = it.value,
                    deletable = it.id in deletableCandidateIds,
                    fromUserDictionary = it.id in userDictionaryCandidateIds,
                )
            }

            // User dictionaries intentionally receive a strong native ranking boost. That is right
            // for someone's own entries, but it made the bundled 24k-entry kana→English dictionary
            // occupy nearly the whole toolbar. During an unfocused Japanese suggestion only, draw
            // additional candidates from Mozc's complete result and apply a small stable penalty to
            // Latin user-dictionary values. Explicit conversion, focused prediction, and the ASCII
            // keyboard retain Mozc's original order.
            val isLiveJapaneseSuggestion =
                candidateWindow.category == Category.SUGGESTION &&
                    !candidateWindow.hasFocusedIndex() &&
                    (currentStyle == InputStyle.FLICK_HIRAGANA ||
                        currentStyle == InputStyle.TOGGLE_FLICK_HIRAGANA)
            if (isLiveJapaneseSuggestion) {
                val complete = if (hasAllCandidateWords()) {
                    allCandidateWords.candidatesList.asSequence()
                        .take(CandidateRanking.MAX_LIVE_POOL_SIZE)
                        .map {
                            Candidate(
                                id = it.id,
                                text = it.value,
                                deletable = CandidateAttribute.DELETABLE in it.attributesList,
                                fromUserDictionary = it.id in userDictionaryCandidateIds,
                            )
                        }
                        .toList()
                } else {
                    emptyList()
                }
                CandidateRanking.rankLiveJapaneseSuggestions(
                    candidates = (visible + complete).distinctBy { it.id },
                    limit = visible.size,
                )
            } else {
                visible
            }
        } else {
            emptyList()
        }
        val segments = if (hasPreedit()) {
            preedit.segmentList.map {
                Segment(
                    text = it.value,
                    highlighted = it.annotation ==
                        org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.Preedit
                            .Segment.Annotation.HIGHLIGHT,
                )
            }
        } else {
            emptyList()
        }

        val reading = preeditText.toString()
        val extending = idsExtendingReading(reading)
        val focusedId = if (hasCandidateWindow() && candidateWindow.hasFocusedIndex()) {
            candidates.getOrNull(candidateWindow.focusedIndex)?.id
        } else {
            null
        }
        val (reordered, reorderedFocused) = exactReadingFirst(
            candidates, focusedId, extending::contains,
        )

        return State(
            committedText = if (hasResult()) result.value else "",
            preedit = preeditText.toString(),
            preeditCursor = cursor.coerceIn(0, preeditText.length),
            candidates = reordered,
            focusedCandidateIndex = reorderedFocused,
            segments = segments,
            isConverting = hasCandidateWindow() &&
                candidateWindow.category == Category.CONVERSION &&
                candidateWindow.hasFocusedIndex(),
            consumed = consumed,
        )
    }
}

/** Mozc table keys that transform the preceding chunk rather than starting a new character. */
internal object FlickTableInput {
    fun isModifierKey(style: InputStyle?, text: String): Boolean = when (style) {
        InputStyle.FLICK_HIRAGANA,
        InputStyle.TOGGLE_FLICK_HIRAGANA,
        -> text == "*" || text == "[" || text == "]" || text == "`"

        InputStyle.FLICK_HALFWIDTH_ASCII,
        InputStyle.TOGGLE_FLICK_HALFWIDTH_ASCII,
        -> text == "*"

        else -> false
    }
}

/**
 * Conservative client-side adjustment for the unfocused toolbar.
 *
 * Native order is retained as a score and English user-dictionary entries receive only a bounded
 * delay. They are still available, but cannot displace every normal Japanese candidate merely
 * because supplemental dictionaries use Mozc's high-priority user-dictionary path.
 */
internal object CandidateRanking {
    const val MAX_LIVE_POOL_SIZE = 32
    private const val LATIN_USER_DICTIONARY_PENALTY = 6

    fun rankLiveJapaneseSuggestions(
        candidates: List<MozcSession.Candidate>,
        limit: Int,
    ): List<MozcSession.Candidate> {
        if (limit <= 0 || candidates.isEmpty()) return emptyList()
        if (candidates.none { containsJapaneseScript(it.text) }) return candidates.take(limit)

        return candidates.withIndex()
            .sortedBy { indexed ->
                indexed.index + if (isLatinUserDictionaryEntry(indexed.value)) {
                    LATIN_USER_DICTIONARY_PENALTY
                } else {
                    0
                }
            }
            .map { it.value }
            .take(limit)
    }

    private fun isLatinUserDictionaryEntry(candidate: MozcSession.Candidate): Boolean =
        candidate.fromUserDictionary &&
            candidate.text.any { it in 'A'..'Z' || it in 'a'..'z' } &&
            !containsJapaneseScript(candidate.text)

    private fun containsJapaneseScript(text: String): Boolean = text.any { ch ->
        ch in '\u3040'..'\u30ff' || // Hiragana, Katakana
            ch in '\u3400'..'\u4dbf' || // CJK Extension A
            ch in '\u4e00'..'\u9fff' || // CJK Unified Ideographs
            ch == '\u3005' || ch == '\u3006' || ch == '\u303b'
    }
}

/**
 * Moves the candidates that convert exactly what was typed ahead of the predictions.
 *
 * A stable partition, not a re-ranking: mozc's order within each group is left alone, because it
 * is the product of a language model this has no business second-guessing. All this decides is
 * that a finished word beats a guess about an unfinished one.
 *
 * Split out from the proto handling so the focus arithmetic can be tested — the focused index
 * points at a slot, and once the list is reordered that slot holds something else.
 *
 * @param focusedId the id of the focused candidate, or null when nothing is focused
 * @return the reordered list and the index the focus moved to, or -1 for none
 */
internal fun exactReadingFirst(
    candidates: List<MozcSession.Candidate>,
    focusedId: Int?,
    extendsReading: (Int) -> Boolean,
): Pair<List<MozcSession.Candidate>, Int> {
    val ordered =
        if (candidates.none { extendsReading(it.id) }) candidates
        else candidates.sortedBy { if (extendsReading(it.id)) 1 else 0 }
    val focused = focusedId?.let { id -> ordered.indexOfFirst { it.id == id } } ?: -1
    return ordered to focused
}
