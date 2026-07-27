package me.zssu.ime.keyboard

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import me.zssu.ime.R
import me.zssu.ime.ime.MozcSession
import me.zssu.ime.theme.KeyboardTheme

/**
 * Horizontal strip of conversion candidates.
 *
 * Built out of plain TextViews rather than a RecyclerView: a candidate list is short and fully
 * replaced on every keystroke, so view recycling buys nothing and costs an adapter's worth of
 * latency on the hottest path in the app.
 */
@SuppressLint("ViewConstructor")
class CandidateStripView(context: Context) : HorizontalScrollView(context) {

    enum class ToolAction { CLIPBOARD, EMOJI, ONE_HAND_CYCLE, SETTINGS }
    enum class OneHandDisplayMode { FULL, LEFT, RIGHT }
    enum class EmojiPage { RECENT, EMOJI, KAOMOJI, FAVORITE }

    fun interface OnCandidateSelectedListener {
        fun onCandidateSelected(candidate: MozcSession.Candidate)
    }

    var listener: OnCandidateSelectedListener? = null
    var onCandidateLongPressed: ((MozcSession.Candidate) -> Unit)? = null
    var onToolAction: ((ToolAction) -> Unit)? = null

    var theme: KeyboardTheme = KeyboardTheme.Default
        set(value) {
            field = value
            invalidate()
        }

    var oneHandMode: OneHandDisplayMode = OneHandDisplayMode.FULL

    private val container = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    private var currentEmojiPage = EmojiPage.RECENT

    init {
        isHorizontalScrollBarEnabled = false
        isFillViewport = true
        addView(
            container,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT),
        )
        // Transparent so the panel behind — colour or the user's background image — shows through.
    }

    fun setCandidates(candidates: List<MozcSession.Candidate>, focusedIndex: Int) {
        container.removeAllViews()
        scrollTo(0, 0)
        val padding = (12 * resources.displayMetrics.density).toInt()

        candidates.forEachIndexed { index, candidate ->
            val view = TextView(context).apply {
                text = candidate.text
                setTextSize(TypedValue.COMPLEX_UNIT_SP, theme.labelSizeSp)
                setTextColor(theme.candidateTextColor)
                setPadding(padding, padding / 2, padding, padding / 2)
                gravity = Gravity.CENTER
                isSingleLine = true
                if (index == focusedIndex) setBackgroundColor(theme.keyPressedColor)
                setOnClickListener { view ->
                    // Picking a candidate commits text just as a keypress does, so it gets the same
                    // confirmation. Without it the strip is the one part of the keyboard that feels
                    // dead under the thumb.
                    if (theme.hapticFeedback) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    }
                    listener?.onCandidateSelected(candidate)
                }
                setOnLongClickListener {
                    if (theme.hapticFeedback) {
                        it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    }
                    onCandidateLongPressed?.invoke(candidate)
                    true
                }
            }
            container.addView(
                view,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                ),
            )
        }
        if (candidates.isEmpty()) showTools()
    }

    fun clear() = setCandidates(emptyList(), -1)

    fun showTools() {
        container.removeAllViews()
        scrollTo(0, 0)
        container.addView(
            iconButton(R.drawable.ic_material_content_paste, "クリップボードを貼り付け") {
                onToolAction?.invoke(ToolAction.CLIPBOARD)
            }
        )
        container.addView(
            iconButton(R.drawable.ic_material_face, "絵文字") {
                onToolAction?.invoke(ToolAction.EMOJI)
            }
        )
        container.addView(
            oneHandButton {
                onToolAction?.invoke(ToolAction.ONE_HAND_CYCLE)
            }
        )
        container.addView(
            iconButton(R.drawable.ic_material_settings, "設定") {
                onToolAction?.invoke(ToolAction.SETTINGS)
            }
        )
    }

    fun showEmoji(
        recents: List<String>,
        favorites: List<String>,
        page: EmojiPage? = null,
    ) {
        val selectedPage = page ?: currentEmojiPage
        currentEmojiPage = selectedPage
        container.removeAllViews()
        scrollTo(0, 0)
        val padding = (12 * resources.displayMetrics.density).toInt()
        val pages = listOf(
            EmojiPage.RECENT to "最近",
            EmojiPage.EMOJI to "絵文字",
            EmojiPage.KAOMOJI to "顔文字",
            EmojiPage.FAVORITE to "★",
        )
        pages.forEach { (target, label) ->
            container.addView(TextView(context).apply {
                text = if (target == selectedPage) "[$label]" else label
                setTextColor(theme.candidateTextColor)
                setPadding(padding, padding / 2, padding, padding / 2)
                gravity = Gravity.CENTER
                setOnClickListener { showEmoji(recents, favorites, target) }
            })
        }
        val values = when (selectedPage) {
            EmojiPage.RECENT -> recents.ifEmpty { EmojiRepository.EMOJI.take(10) }
            EmojiPage.EMOJI -> EmojiRepository.EMOJI
            EmojiPage.KAOMOJI -> EmojiRepository.KAOMOJI
            EmojiPage.FAVORITE -> favorites
        }
        values.forEach { emoji ->
            container.addView(TextView(context).apply {
                text = if (emoji in favorites) "$emoji★" else emoji
                setTextColor(theme.candidateTextColor)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, theme.labelSizeSp + 4f)
                setPadding(padding, padding / 2, padding, padding / 2)
                gravity = Gravity.CENTER
                setOnClickListener { onEmojiSelected?.invoke(emoji) }
                setOnLongClickListener {
                    onEmojiFavoriteToggled?.invoke(emoji)
                    true
                }
            })
        }
        container.addView(
            iconButton(R.drawable.ic_material_arrow_back, "ツールバーに戻る") { showTools() }
        )
    }

    var onEmojiSelected: ((String) -> Unit)? = null
    var onEmojiFavoriteToggled: ((String) -> Unit)? = null

    /**
     * Reuses the strip as a compact candidate-inspector, avoiding a dialog window above the IME.
     */
    fun showCandidateDetails(
        candidate: MozcSession.Candidate,
        definitions: List<String>,
    ) {
        container.removeAllViews()
        scrollTo(0, 0)
        val padding = (12 * resources.displayMetrics.density).toInt()
        container.addView(
            iconButton(R.drawable.ic_material_arrow_back, "候補一覧に戻る") {
                onCandidateDetailsClosed?.invoke()
            }
        )
        container.addView(TextView(context).apply {
            text = buildString {
                append(candidate.text)
                if (definitions.isNotEmpty()) {
                    append("　")
                    append(definitions.joinToString(" / "))
                } else {
                    append("　意味辞書には登録されていません")
                }
            }
            setTextColor(theme.candidateTextColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, theme.labelSizeSp)
            setPadding(padding, padding / 2, padding, padding / 2)
            gravity = Gravity.CENTER_VERTICAL
            isSingleLine = true
        })
        if (candidate.deletable) {
            container.addView(TextView(context).apply {
                text = "学習削除"
                setTextColor(theme.candidateTextColor)
                setPadding(padding, padding / 2, padding, padding / 2)
                gravity = Gravity.CENTER
                setOnClickListener { onCandidateDeleteRequested?.invoke(candidate) }
            })
        }
    }

    var onCandidateDeleteRequested: ((MozcSession.Candidate) -> Unit)? = null
    var onCandidateDetailsClosed: (() -> Unit)? = null

    private fun iconButton(
        @DrawableRes icon: Int,
        description: String,
        action: () -> Unit,
    ): ImageButton = ImageButton(context).apply {
        setImageDrawable(ContextCompat.getDrawable(context, icon))
        imageTintList = ColorStateList.valueOf(theme.candidateTextColor)
        contentDescription = description
        background = null
        val padding = (12 * resources.displayMetrics.density).toInt()
        setPadding(padding, padding, padding, padding)
        scaleType = ImageView.ScaleType.CENTER_INSIDE
        setOnClickListener {
            if (theme.hapticFeedback) it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            action()
        }
        layoutParams = toolbarItemLayoutParams()
    }

    private fun oneHandButton(action: () -> Unit): ImageButton = ImageButton(context).apply {
        setImageDrawable(
            OneHandModeDrawable(
                oneHandMode,
                theme.candidateTextColor,
                resources.displayMetrics.density,
            )
        )
        contentDescription = when (oneHandMode) {
            OneHandDisplayMode.FULL -> "片手モード: 全幅。タップして左寄せ"
            OneHandDisplayMode.LEFT -> "片手モード: 左寄せ。タップして右寄せ"
            OneHandDisplayMode.RIGHT -> "片手モード: 右寄せ。タップして全幅"
        }
        background = null
        val padding = (11 * resources.displayMetrics.density).toInt()
        setPadding(padding, padding, padding, padding)
        scaleType = ImageView.ScaleType.CENTER_INSIDE
        setOnClickListener {
            if (theme.hapticFeedback) it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            action()
        }
        layoutParams = toolbarItemLayoutParams()
    }

    private fun toolbarItemLayoutParams() = LinearLayout.LayoutParams(
        (56 * resources.displayMetrics.density).toInt(),
        LinearLayout.LayoutParams.MATCH_PARENT,
    )
}
