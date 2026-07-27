package me.zssu.ime.ime

import android.content.Context
import android.content.ClipboardManager
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.view.Gravity
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnAttach
import androidx.core.view.updatePadding
import me.zssu.ime.keyboard.CandidateStripView
import me.zssu.ime.keyboard.EmojiRepository
import me.zssu.ime.keyboard.FlickDirection
import me.zssu.ime.keyboard.FlickKeyboardView
import me.zssu.ime.keyboard.KeyAction
import me.zssu.ime.keyboard.KeyOutput
import me.zssu.ime.keyboard.KeySpec
import me.zssu.ime.keyboard.KeyboardLayout
import me.zssu.ime.keyboard.KeyboardPanelView
import me.zssu.ime.keyboard.LayoutRepository
import me.zssu.ime.keyboard.MeaningPopupView
import me.zssu.ime.settings.ImeSettings
import me.zssu.ime.settings.AppProfileStore
import me.zssu.ime.settings.SettingsActivity
import me.zssu.ime.dictionary.MeaningDictionaryRepository
import me.zssu.ime.theme.KeyboardTheme
import me.zssu.ime.theme.MaterialYouTheme
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.KeyEvent

/**
 * The input method itself.
 *
 * Responsibilities are kept narrow on purpose: translate [KeyOutput]s into mozc calls, and push
 * mozc's resulting [MozcSession.State] into the editor and the candidate strip. Flick geometry
 * lives in [FlickKeyboardView], conversion lives in mozc, and layout/theme data lives in
 * [LayoutRepository].
 */
class ZinnaImeService : InputMethodService() {

    private lateinit var repository: LayoutRepository
    private lateinit var settings: ImeSettings
    private lateinit var appProfiles: AppProfileStore
    private lateinit var emojiRepository: EmojiRepository
    private lateinit var meaningDictionaries: MeaningDictionaryRepository
    private lateinit var priorityCandidates: PriorityCandidateRepository
    private lateinit var clipboardHistory: ClipboardHistory
    private lateinit var session: MozcSession

    private var keyboardView: FlickKeyboardView? = null
    private var candidateView: CandidateStripView? = null
    private var meaningPopup: MeaningPopupView? = null
    private var layout: KeyboardLayout? = null
    private var theme: KeyboardTheme = KeyboardTheme.Default
    private var fieldPolicy = InputFieldPolicy.DEFAULT
    private var activeProfile: AppProfileStore.Profile? = null
    private var lastCandidates: List<MozcSession.Candidate> = emptyList()
    private var lastFocusedCandidateIndex: Int = -1

    /**
     * Whether mozc currently holds a composition. Mirrors the last rendered state so Enter can
     * decide between 確定 and the editor's action *before* it submits and destroys the evidence.
     */
    private var isComposing = false
    private var isConverting = false
    private var renderedPreeditCursor = 0

    /**
     * Set while a state we rendered is still settling in the editor.
     *
     * commitText + setComposingText inside one batch edit make the editor post a selection
     * callback whose composing-span coordinates can lag the text we just wrote. Comparing that
     * stale span against the new caret made the callback look like an external edit, so the
     * continuation candidates a conversion just produced were cleared and the toolbar flashed
     * back over them. A collapsed-caret update that arrives while this is set is our own echo and
     * is ignored; a range selection is still the user grabbing the text, so it is honoured.
     */
    private var ownSelectionUpdatePending = false
    private var editorStateUpdateInProgress = false
    private var editorSelectionUpdateObserved = false

    /** [ImeSettings.revision] the current input view was built from. */
    private var builtFromRevision = Int.MIN_VALUE

    /**
     * The system-bar insets last dispatched to us.
     *
     * Kept because a view built mid-session may never be handed them again: the window's insets
     * have not changed, so nothing re-dispatches, and a fresh panel would sit at zero padding with
     * its bottom row under the navigation bar. Seeding from these makes the rebuilt view correct on
     * its first frame instead of waiting for a dispatch that may not come.
     */
    private var systemInsetLeft = 0
    private var systemInsetRight = 0
    private var systemInsetBottom = 0

    override fun onCreate() {
        super.onCreate()
        repository = LayoutRepository(this)
        settings = ImeSettings(this)
        appProfiles = AppProfileStore(this)
        emojiRepository = EmojiRepository(this)
        meaningDictionaries = MeaningDictionaryRepository(this)
        priorityCandidates = PriorityCandidateRepository(this)
        clipboardHistory = ClipboardHistory()
        session = MozcSession(this, priorityCandidates::match)
        if (!session.isAvailable) {
            // Without the native engine there is nothing useful to do; the keyboard still renders
            // so the user can switch away rather than being stuck with a dead input field.
            Log.e(TAG, "mozc engine unavailable — conversion disabled")
        }
    }

    override fun onCreateInputView(): View {
        // onStartInputView normally makes this available first. Keep an explicitly selected policy
        // if an OEM calls view creation with no EditorInfo instead of falling back to normal text.
        currentInputEditorInfo?.let {
            activeProfile = appProfiles.profileFor(it.packageName)
            fieldPolicy = effectivePolicy(InputFieldPolicy.from(it), activeProfile)
        }
        val loaded = repository.loadLayout(initialLayoutId(fieldPolicy))
            ?: repository.loadLayout(LayoutRepository.DEFAULT_LAYOUT_ID)
        if (loaded == null) Log.e(TAG, "default layout missing from assets")
        layout = loaded
        theme = resolveTheme()

        val panel = KeyboardPanelView(this).apply {
            // The IME window runs edge-to-edge from targetSdk 35, so it extends underneath the
            // navigation bar and the bottom key row ends up beneath the gesture pill. Padding the
            // panel lifts the keys clear while its own background keeps covering the strip behind
            // the bar, so the keyboard still reaches the bottom of the screen.
            setBackgroundColor(this@ZinnaImeService.theme.backgroundColor)
            setBackgroundImage(settings.backgroundImage, settings.backgroundOpacity)
            // Start from what we already know, so a rebuild triggered by a settings change is
            // padded before it draws rather than after the next dispatch — which, since the
            // window's insets did not change, may never arrive.
            updatePadding(
                left = systemInsetLeft,
                right = systemInsetRight,
                bottom = systemInsetBottom,
            )
            ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
                val bars = insets.getInsets(
                    WindowInsetsCompat.Type.navigationBars() or
                        WindowInsetsCompat.Type.displayCutout()
                )
                systemInsetLeft = bars.left
                systemInsetRight = bars.right
                systemInsetBottom = bars.bottom
                view.updatePadding(left = bars.left, right = bars.right, bottom = bars.bottom)
                WindowInsetsCompat.CONSUMED
            }
            // And ask for a fresh dispatch once attached, so a stale seed (after a rotation, say)
            // is corrected rather than persisting for the rest of the session.
            doOnAttach { ViewCompat.requestApplyInsets(it) }
        }

        val root = FrameLayout(this)
        root.addView(panel, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        ))

        val popupView = MeaningPopupView(this).apply {
            applyTheme(this@ZinnaImeService.theme)
            onDismiss = {
                candidateView?.setCandidates(lastCandidates, lastFocusedCandidateIndex)
            }
        }

        val candidates = CandidateStripView(this).apply {
            theme = this@ZinnaImeService.theme
            listener = CandidateStripView.OnCandidateSelectedListener { candidate ->
                render(session.selectCandidate(candidate.id))
            }
            onCandidateLongPressed = { candidate ->
                val entries = meaningDictionaries.lookup(candidate.text)
                val definitions = entries.flatMap { entry ->
                    entry.meanings.map { meaning ->
                        buildString {
                            if (entry.tags.isNotEmpty()) append("[${entry.tags.joinToString("・")}] ")
                            append(meaning)
                            entry.source?.let { append(" — $it") }
                        }
                    }
                }
                val exactPriority = candidate.priorityMatch == PriorityMatch.EXACT
                popupView.show(
                    term = candidate.text,
                    reading = entries.firstOrNull()?.reading ?: candidate.inputReading,
                    definitions = definitions,
                    canDelete = candidate.deletable,
                    onDelete = {
                        render(session.deleteCandidateFromHistory(candidate.id))
                        Toast.makeText(
                            this@ZinnaImeService,
                            "「${candidate.text}」を学習候補から削除しました",
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
                    canPrioritize = candidate.inputReading.isNotEmpty(),
                    isPrioritized = exactPriority,
                    onTogglePriority = {
                        val saved = if (exactPriority) {
                            priorityCandidates.unpin(candidate.inputReading, candidate.text)
                        } else {
                            priorityCandidates.pin(candidate.inputReading, candidate.text)
                        }
                        if (saved) {
                            refreshCandidatePriorities()
                            Toast.makeText(
                                this@ZinnaImeService,
                                if (exactPriority) {
                                    "「${candidate.text}」の最優先を解除しました"
                                } else {
                                    "「${candidate.text}」を最優先にしました"
                                },
                                Toast.LENGTH_SHORT,
                            ).show()
                        } else {
                            Toast.makeText(
                                this@ZinnaImeService,
                                "最優先設定を保存できませんでした",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                )
            }
            onCandidateDeleteRequested = { candidate ->
                render(session.deleteCandidateFromHistory(candidate.id))
                Toast.makeText(
                    this@ZinnaImeService,
                    "「${candidate.text}」を学習候補から削除しました",
                    Toast.LENGTH_SHORT,
                ).show()
            }
            onCandidateDetailsClosed = {
                setCandidates(lastCandidates, lastFocusedCandidateIndex)
            }
            onToolAction = ::handleToolAction
            onClipboardItemSelected = { text ->
                currentInputConnection?.commitText(text, 1)
                showTools()
            }
            onClipboardHistoryCleared = {
                clipboardHistory.clear()
                showClipboardHistory(emptyList())
                Toast.makeText(
                    this@ZinnaImeService,
                    "クリップボード履歴を消去しました",
                    Toast.LENGTH_SHORT,
                ).show()
            }
            onEmojiSelected = { emoji ->
                currentInputConnection?.commitText(emoji, 1)
                emojiRepository.recordUsed(emoji)
                showEmoji(emojiRepository.recents(), emojiRepository.favorites())
            }
            onEmojiFavoriteToggled = { emoji ->
                val added = emojiRepository.toggleFavorite(emoji)
                Toast.makeText(
                    this@ZinnaImeService,
                    if (added) "お気に入りに追加しました" else "お気に入りから外しました",
                    Toast.LENGTH_SHORT,
                ).show()
                showEmoji(emojiRepository.recents(), emojiRepository.favorites())
            }
            oneHandMode = when (effectiveOneHandMode()) {
                ImeSettings.OneHandMode.OFF -> CandidateStripView.OneHandDisplayMode.FULL
                ImeSettings.OneHandMode.LEFT -> CandidateStripView.OneHandDisplayMode.LEFT
                ImeSettings.OneHandMode.RIGHT -> CandidateStripView.OneHandDisplayMode.RIGHT
            }
        }
        val stripHeight = if (fieldPolicy.showCandidates) {
            (theme.keyHeightDp * 0.8f * resources.displayMetrics.density).toInt()
        } else {
            0
        }

        val keyboard = FlickKeyboardView(this).apply {
            theme = this@ZinnaImeService.theme
            layout = loaded?.let(::adaptLayoutForStyle)
            guideOverflowTop = stripHeight.toFloat()
            guideStyle = settings.flickGuideStyle
            listener = FlickKeyboardView.OnKeyOutputListener(::onKeyOutput)
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
            gravity = when (effectiveOneHandMode()) {
                ImeSettings.OneHandMode.LEFT -> Gravity.START
                ImeSettings.OneHandMode.RIGHT -> Gravity.END
                ImeSettings.OneHandMode.OFF -> Gravity.CENTER_HORIZONTAL
            }
        }
        panel.gravity = when (effectiveOneHandMode()) {
            ImeSettings.OneHandMode.LEFT -> Gravity.START
            ImeSettings.OneHandMode.RIGHT -> Gravity.END
            ImeSettings.OneHandMode.OFF -> Gravity.CENTER_HORIZONTAL
        }
        val oneHandWidth =
            (resources.displayMetrics.widthPixels * ONE_HAND_WIDTH).toInt()
        val contentWidth = if (effectiveOneHandMode() == ImeSettings.OneHandMode.OFF) {
            LinearLayout.LayoutParams.MATCH_PARENT
        } else {
            oneHandWidth
        }
        content.addView(
            candidates,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, stripHeight),
        )
        content.addView(
            keyboard,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
        panel.addView(
            content,
            LinearLayout.LayoutParams(contentWidth, LinearLayout.LayoutParams.WRAP_CONTENT),
        )

        root.addView(
            popupView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )

        candidateView = candidates
        keyboardView = keyboard
        meaningPopup = popupView
        builtFromRevision = combinedRevision()
        session.setPhoneToggleEnabled(
            settings.flickInputMode == ImeSettings.FlickInputMode.FLICK_AND_TOGGLE
        )
        session.setIncognitoMode(fieldPolicy.incognito)
        loaded?.let { session.applyInputStyle(it.inputStyle) }
        candidates.showTools()
        return root
    }

    private fun handleToolAction(action: CandidateStripView.ToolAction) {
        when (action) {
            CandidateStripView.ToolAction.CLIPBOARD -> {
                clipboardHistory.record(
                    this,
                    getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager,
                )
                candidateView?.showClipboardHistory(clipboardHistory.items())
            }
            CandidateStripView.ToolAction.EMOJI -> candidateView?.showEmoji(
                emojiRepository.recents(),
                emojiRepository.favorites(),
                CandidateStripView.EmojiPage.RECENT,
            )
            CandidateStripView.ToolAction.ONE_HAND_CYCLE -> cycleOneHandMode()
            CandidateStripView.ToolAction.SETTINGS -> startActivity(
                Intent(this, SettingsActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    /**
     * One toolbar button walks 全幅 → 左 → 右 → 全幅. Cycling (rather than two directional
     * buttons) means the way back is the same tap that got you here, so nobody is stranded in a
     * narrow keyboard with no obvious exit.
     */
    private fun cycleOneHandMode() {
        val next = when (effectiveOneHandMode()) {
            ImeSettings.OneHandMode.OFF -> ImeSettings.OneHandMode.LEFT
            ImeSettings.OneHandMode.LEFT -> ImeSettings.OneHandMode.RIGHT
            ImeSettings.OneHandMode.RIGHT -> ImeSettings.OneHandMode.OFF
        }
        val profile = activeProfile
        if (profile != null) {
            activeProfile = profile.copy(oneHandMode = next.name).also(appProfiles::save)
        } else {
            settings.oneHandMode = next
        }
        setInputView(onCreateInputView())
    }

    /** Keeps the bottom-left key as the language switch on the mixed QWERTY alphabet plane. */
    private fun adaptLayoutForStyle(source: KeyboardLayout): KeyboardLayout {
        return effectiveKeyboardStyle().adapt(source)
    }

    /**
     * Material You unless the user has dropped a theme file in.
     *
     * A user override wins because they asked for it explicitly; otherwise we follow the system
     * palette, which also means the keyboard tracks light/dark without a setting.
     */
    private fun resolveTheme(): KeyboardTheme {
        val selected = settings.activeThemeId
        val base = if (selected == MaterialYouTheme.ID) {
            MaterialYouTheme.create(this, forceDark = settings.pureBlack)
        } else {
            repository.loadTheme(selected)
                ?: MaterialYouTheme.create(this, forceDark = settings.pureBlack)
        }
        val sized = base.copy(keyHeightDp = base.keyHeightDp * effectiveKeyHeightScale())
        return if (settings.pureBlack) sized.asPureBlack() else sized
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        meaningPopup?.hide(notify = false)
        appProfiles.recordSeenPackage(info?.packageName)
        val nextProfile = appProfiles.profileFor(info?.packageName)
        val nextPolicy = effectivePolicy(InputFieldPolicy.from(info), nextProfile)
        // Settings live in another process's Activity, so this is the first moment we can notice
        // they changed. Rebuilding only on a revision bump keeps the common case free.
        if (combinedRevision() != builtFromRevision ||
            nextPolicy != fieldPolicy ||
            nextProfile != activeProfile
        ) {
            activeProfile = nextProfile
            fieldPolicy = nextPolicy
            setInputView(onCreateInputView())
        }
        session.setIncognitoMode(nextPolicy.incognito)
        if (!restarting) session.resetContext()
        isComposing = false
        isConverting = false
        renderedPreeditCursor = 0
        keyboardView?.isConversionAvailable = false
        ownSelectionUpdatePending = false
        if (!nextPolicy.incognito) {
            clipboardHistory.record(
                this,
                getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager,
            )
        }
        candidateView?.clear()
    }

    /**
     * Rebuilds the keyboard so a light/dark switch or a wallpaper recolour is picked up. The
     * dynamic palette is read at view-creation time, so without this the keyboard keeps the colours
     * it was born with until the IME process restarts.
     */
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        setInputView(onCreateInputView())
    }

    override fun onFinishInput() {
        super.onFinishInput()
        meaningPopup?.hide(notify = false)
        // Anything half-composed at this point can no longer be committed anywhere sensible.
        session.resetContext()
        isComposing = false
        isConverting = false
        renderedPreeditCursor = 0
        keyboardView?.isConversionAvailable = false
        ownSelectionUpdatePending = false
        currentInputConnection?.finishComposingText()
        candidateView?.clear()
    }

    /**
     * Drops mozc's composition when the editor moves or replaces its composing span behind us.
     *
     * Editors are allowed to mutate their text without sending a key event. Chrome's address-bar
     * clear button is a common example: without this callback, the underline disappears from the
     * editor but the old reading remains inside mozc and is prepended to the next word.
     *
     * Updates produced by our own [render] leave the selection collapsed at Mozc's cursor inside
     * the composing span, so they are ignored. Any other selection means the editor is now
     * authoritative and retaining the native composition would make the two sides disagree.
     */
    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int,
    ) {
        super.onUpdateSelection(
            oldSelStart,
            oldSelEnd,
            newSelStart,
            newSelEnd,
            candidatesStart,
            candidatesEnd,
        )
        // setComposingText has to place the caret at the end before setSelection can move it into
        // the middle. Some editors report that temporary position synchronously, even inside a
        // batch edit; it is an implementation detail of our render, not an external edit.
        if (editorStateUpdateInProgress) {
            editorSelectionUpdateObserved = true
            return
        }
        if (ownSelectionUpdatePending) {
            ownSelectionUpdatePending = false
            // Our render leaves the caret collapsed; a range selection means the user intervened.
            if (newSelStart == newSelEnd) return
        }
        if (!SelectionUpdate.invalidatesComposition(
                isComposing = isComposing,
                newSelectionStart = newSelStart,
                newSelectionEnd = newSelEnd,
                composingStart = candidatesStart,
                composingEnd = candidatesEnd,
                preeditCursor = renderedPreeditCursor,
            )
        ) {
            return
        }

        // Clear the guard first: finishComposingText can synchronously cause another selection
        // callback in some editors.
        isComposing = false
        isConverting = false
        renderedPreeditCursor = 0
        keyboardView?.isConversionAvailable = false
        session.resetContext()
        currentInputConnection?.finishComposingText()
        candidateView?.clear()
    }

    override fun onDestroy() {
        session.close()
        super.onDestroy()
    }

    private fun onKeyOutput(output: KeyOutput, key: KeySpec, direction: FlickDirection) {
        when (val action = output.action) {
            is KeyAction.Input -> render(session.sendText(action.text))

            // The dakuten/small-kana cycle is a table entry, not a session command: mozc's flick
            // table maps '*' onto "advance the preceding kana one step" (あ→ぁ→あ, は→ば→ぱ→は).
            is KeyAction.ModifyChar -> render(session.sendText(CYCLE_MODIFIER_KEY))

            is KeyAction.InsertSymbol -> handleSymbol(action.text)

            is KeyAction.Undo -> render(session.undo())

            is KeyAction.Backspace -> handleBackspace()

            is KeyAction.Space -> handleSpace()

            // The state decides the visible operation: convert while composing and insert the
            // plane's normal space while idle. Enter has its own key.
            is KeyAction.Convert -> handleSpace()

            is KeyAction.Enter -> handleEnter()

            is KeyAction.MoveCursor -> handleCursorMove(
                delta = action.delta,
                adjustSegment = isConverting &&
                    direction != FlickDirection.CENTER &&
                    (key.center.action is KeyAction.Space ||
                        key.center.action is KeyAction.Convert),
            )

            is KeyAction.SwitchLayout -> switchLayout(action.layoutId)

            // Consumed by the keyboard view, which owns the shift state because it has to draw it.
            // Reaching here would mean the view stopped intercepting it.
            is KeyAction.Shift -> Log.w(TAG, "shift reached the service; view did not consume it")

            is KeyAction.ShowImePicker -> {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showInputMethodPicker()
            }
        }
    }

    /**
     * Symbols finalise whatever is composing and then go straight into the editor.
     *
     * They are not conversion input — nobody wants "あ(" offered as a candidate — and committing
     * first means the symbol lands after the kana rather than in the middle of it.
     */
    private fun handleSymbol(text: String) {
        if (isComposing) render(session.submit())
        currentInputConnection?.commitText(text, 1)
    }

    /**
     * Space, which mozc only handles while something is composing.
     *
     * With an empty composition it returns the key unconsumed on every plane — it has nowhere to
     * put a space and leaves the character to the client. Rendering that empty response was doing
     * nothing at all, so space appeared to work only as the second keystroke of a word.
     */
    private fun handleSpace() {
        val state = session.convertOrSpace()
        if (state == null || !state.consumed) {
            val fullWidth = layout?.inputStyle?.fullWidthSpace ?: false
            currentInputConnection?.commitText(if (fullWidth) FULL_WIDTH_SPACE else " ", 1)
        }
        render(state)
    }

    private fun handleBackspace() {
        val state = session.sendSpecialKey(KeyEvent.SpecialKey.BACKSPACE)
        // mozc reports an empty composition both when it consumed the delete and when there was
        // nothing to delete, so fall through to the editor whenever nothing was composing.
        if (state == null || (!state.hasComposition && state.committedText.isEmpty())) {
            currentInputConnection?.deleteSurroundingText(1, 0)
        }
        render(state)
    }

    /**
     * 確定 when something is composing, otherwise the editor's own Enter.
     *
     * The composing check has to happen *before* submitting — after a successful SUBMIT the
     * composition is by definition gone, so inspecting the resulting state would make every Enter
     * also fire the editor action and send the message you were only trying to finalise.
     */
    private fun handleEnter() {
        val ic = currentInputConnection ?: return
        if (isComposing) {
            render(session.submit())
            return
        }
        val info = currentInputEditorInfo
        val imeOptions = info?.imeOptions ?: 0
        val action = imeOptions and EditorInfo.IME_MASK_ACTION

        // Only a field that can hold a newline gets one. Deciding this first is the whole fix: a
        // search bar that never set imeOptions reports IME_ACTION_UNSPECIFIED, and treating that
        // as "no action, so type a newline" stuffed a line break into a single-line box instead of
        // searching.
        if ((info?.inputType ?: 0) and EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE != 0) {
            ic.commitText("\n", 1)
            return
        }

        val wantsAction = action != EditorInfo.IME_ACTION_NONE &&
            action != EditorInfo.IME_ACTION_UNSPECIFIED &&
            imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION == 0

        if (wantsAction) {
            ic.performEditorAction(action)
            return
        }

        // No action declared, but the field cannot take a newline either. Send a real Enter and let
        // the editor do whatever it does with one — that is how a hardware keyboard reaches these
        // fields, and it is what makes an unlabelled search box submit.
        val now = android.os.SystemClock.uptimeMillis()
        val enter = android.view.KeyEvent.KEYCODE_ENTER
        ic.sendKeyEvent(
            android.view.KeyEvent(now, now, android.view.KeyEvent.ACTION_DOWN, enter, 0)
        )
        ic.sendKeyEvent(
            android.view.KeyEvent(now, now, android.view.KeyEvent.ACTION_UP, enter, 0)
        )
    }

    private fun handleCursorMove(delta: Int, adjustSegment: Boolean) {
        session.stopKeyToggling()
        val special = if (delta < 0) KeyEvent.SpecialKey.LEFT else KeyEvent.SpecialKey.RIGHT
        // In conversion, ordinary arrows move the focused clause. A sideways space flick supplies
        // Shift+Arrow, Mozc's standard command for shrinking/expanding that clause boundary.
        val state = session.sendSpecialKey(special, shift = adjustSegment)
        if (state != null && state.hasComposition) {
            render(state)
            return
        }
        // Outside a composition, move the editor caret instead.
        val ic = currentInputConnection ?: return
        val extracted = ic.getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0) ?: return
        val target = (extracted.selectionStart + delta).coerceIn(0, extracted.text?.length ?: 0)
        ic.setSelection(target, target)
    }

    private fun switchLayout(layoutId: String) {
        // The layouts point inside their own family; the user's style decides which family the
        // kana and alphabet planes actually come from, so every switch goes through it.
        val resolved = effectiveKeyboardStyle().resolve(layoutId)
        val next = repository.loadLayout(resolved)
        if (next == null) {
            Log.w(TAG, "layout $resolved not found; staying on ${layout?.id}")
            return
        }
        // Finalise before swapping planes so a half-typed kana is not silently discarded.
        render(session.submit())
        layout = next
        keyboardView?.layout = adaptLayoutForStyle(next)
        session.applyInputStyle(next.inputStyle)
    }

    private fun initialLayoutId(policy: InputFieldPolicy): String = when (policy.plane) {
        InputFieldPolicy.Plane.USER_DEFAULT ->
            settings.activeLayoutId
                ?.takeIf { activeProfile == null }
                ?.takeIf { repository.loadLayout(it) != null }
                ?: effectiveKeyboardStyle().defaultLayoutId
        InputFieldPolicy.Plane.ASCII -> effectiveKeyboardStyle().resolve("qwerty_ascii")
        InputFieldPolicy.Plane.NUMERIC -> when (effectiveKeyboardStyle()) {
            me.zssu.ime.keyboard.KeyboardStyle.QWERTY -> "qwerty_symbol"
            else -> "flick_symbol"
        }
    }

    private fun render(state: MozcSession.State?) {
        val ic = currentInputConnection ?: return
        if (state == null) return

        isComposing = state.hasComposition
        isConverting = state.isConverting
        renderedPreeditCursor = state.preeditCursor
        keyboardView?.isConversionAvailable =
            state.hasComposition && layout?.inputStyle?.fullWidthSpace == true
        editorStateUpdateInProgress = true
        editorSelectionUpdateObserved = false
        ic.beginBatchEdit()
        try {
            if (state.committedText.isNotEmpty()) {
                ic.commitText(state.committedText, 1)
            }
            if (state.preedit.isEmpty()) {
                ic.finishComposingText()
            } else {
                val styled = SpannableString(state.preedit).apply {
                    if (state.isConverting && isNotEmpty()) {
                        setSpan(
                            BackgroundColorSpan(CONVERSION_RANGE_COLOR),
                            0,
                            length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                        )
                    }
                    var start = 0
                    for (segment in state.segments) {
                        val end = (start + segment.text.length).coerceAtMost(length)
                        if (end > start) {
                            setSpan(
                                UnderlineSpan(),
                                start,
                                end,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                            )
                            if (segment.highlighted) {
                                setSpan(
                                    BackgroundColorSpan(CONVERSION_FOCUSED_COLOR),
                                    start,
                                    end,
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                                )
                            }
                        }
                        start = end
                    }
                    if (state.segments.isEmpty() && isNotEmpty()) {
                        setSpan(UnderlineSpan(), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                }

                // Android can place newly supplied composing text only at/beyond its edges. Put it
                // down with the caret at the end, then explicitly restore Mozc's character offset.
                // Without this second step every interior offset collapses to the beginning or end,
                // leaving the editor and Mozc at different positions.
                ic.setComposingText(styled, 1)
                if (state.preeditCursor != state.preedit.length) {
                    val extracted = ic.getExtractedText(
                        android.view.inputmethod.ExtractedTextRequest(),
                        0,
                    )
                    if (extracted != null) {
                        val composingEnd = extracted.startOffset + extracted.selectionEnd
                        val target = SelectionUpdate.absolutePreeditCursor(
                            composingEnd = composingEnd,
                            preeditLength = state.preedit.length,
                            preeditCursor = state.preeditCursor,
                        )
                        ic.setSelection(target, target)
                    }
                }
            }
        } finally {
            ic.endBatchEdit()
            editorStateUpdateInProgress = false
            // If endBatchEdit already delivered the selection callback synchronously, there is no
            // future self-update to ignore. Otherwise one collapsed asynchronous callback is
            // expected from this composition render.
            ownSelectionUpdatePending = state.hasComposition && !editorSelectionUpdateObserved
        }

        lastCandidates = state.candidates
        lastFocusedCandidateIndex = state.focusedCandidateIndex
        candidateView?.setCandidates(state.candidates, state.focusedCandidateIndex)
    }

    /** Refreshes only explicit priority state; candidate ids and Mozc's conversion state stay put. */
    private fun refreshCandidatePriorities() {
        val focusedId = lastCandidates.getOrNull(lastFocusedCandidateIndex)?.id
        lastCandidates = lastCandidates.map { candidate ->
            candidate.copy(
                priorityMatch = priorityCandidates.match(
                    candidate.inputReading,
                    candidate.text,
                )
            )
        }.withIndex()
            .sortedWith(
                compareBy<IndexedValue<MozcSession.Candidate>> {
                    when (it.value.priorityMatch) {
                        PriorityMatch.EXACT -> 0
                        PriorityMatch.SIMILAR -> 1
                        PriorityMatch.NONE -> 2
                    }
                }.thenBy { it.index }
            )
            .map { it.value }
        lastFocusedCandidateIndex =
            focusedId?.let { id -> lastCandidates.indexOfFirst { it.id == id } } ?: -1
        candidateView?.setCandidates(lastCandidates, lastFocusedCandidateIndex)
    }

    private fun effectiveKeyboardStyle() =
        activeProfile?.resolvedKeyboardStyle ?: settings.keyboardStyle

    private fun effectiveOneHandMode() =
        activeProfile?.resolvedOneHandMode ?: settings.oneHandMode

    private fun effectiveKeyHeightScale() =
        activeProfile?.keyHeightScale ?: settings.keyHeightScale

    private fun effectivePolicy(
        base: InputFieldPolicy,
        profile: AppProfileStore.Profile?,
    ): InputFieldPolicy = base.copy(incognito = base.incognito || profile?.incognito == true)

    private fun combinedRevision(): Int = settings.revision * 31 + appProfiles.revision

    companion object {
        private const val TAG = "ZinnaImeService"

        /** '*' in mozc's flick/12-key tables. See data/preedit/flick-hiragana.tsv upstream. */
        private const val CYCLE_MODIFIER_KEY = "*"

        /** U+3000 IDEOGRAPHIC SPACE, what a Japanese input mode types for the space key. */
        private const val FULL_WIDTH_SPACE = "　"
        private const val ONE_HAND_WIDTH = 0.82f
        private val CONVERSION_RANGE_COLOR = Color.argb(72, 255, 152, 0)
        private val CONVERSION_FOCUSED_COLOR = Color.argb(184, 255, 122, 0)
    }
}
