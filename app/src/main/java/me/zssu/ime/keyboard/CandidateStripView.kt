package me.zssu.ime.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
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

    /**
     * Label for the single one-hand toggle in the toolbar. The service renders the current mode
     * into it (全幅/左/右) so tapping always shows where you are and, by cycling, how to get back.
     */
    var oneHandLabel: String = "片手"

    private val container = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

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
        showItems(
            listOf(
                "📋" to ToolAction.CLIPBOARD,
                "😀" to ToolAction.EMOJI,
                oneHandLabel to ToolAction.ONE_HAND_CYCLE,
                "⚙" to ToolAction.SETTINGS,
            )
        )
    }

    fun showEmoji() {
        container.removeAllViews()
        scrollTo(0, 0)
        val padding = (12 * resources.displayMetrics.density).toInt()
        listOf("😀", "😂", "🥰", "😊", "😭", "👍", "🙏", "🎉", "❤️", "✨", "🔥", "✅", "💡", "👀").forEach { emoji ->
            container.addView(TextView(context).apply {
                text = emoji
                setTextSize(TypedValue.COMPLEX_UNIT_SP, theme.labelSizeSp + 4f)
                setPadding(padding, padding / 2, padding, padding / 2)
                gravity = Gravity.CENTER
                setOnClickListener { onEmojiSelected?.invoke(emoji) }
            })
        }
        container.addView(toolView("戻る") { showTools() })
    }

    var onEmojiSelected: ((String) -> Unit)? = null

    private fun showItems(items: List<Pair<String, ToolAction>>) {
        container.removeAllViews()
        scrollTo(0, 0)
        items.forEach { (label, action) ->
            container.addView(toolView(label) { onToolAction?.invoke(action) })
        }
    }

    private fun toolView(label: String, action: () -> Unit): TextView = TextView(context).apply {
        text = label
        setTextSize(TypedValue.COMPLEX_UNIT_SP, theme.labelSizeSp)
        setTextColor(theme.candidateTextColor)
        val horizontal = (18 * resources.displayMetrics.density).toInt()
        setPadding(horizontal, 0, horizontal, 0)
        gravity = Gravity.CENTER
        setOnClickListener {
            if (theme.hapticFeedback) it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            action()
        }
    }
}
