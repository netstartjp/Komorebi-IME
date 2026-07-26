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

    fun interface OnCandidateSelectedListener {
        fun onCandidateSelected(candidate: MozcSession.Candidate)
    }

    var listener: OnCandidateSelectedListener? = null

    var theme: KeyboardTheme = KeyboardTheme.Default
        set(value) {
            field = value
            invalidate()
        }

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
            }
            container.addView(
                view,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                ),
            )
        }
    }

    fun clear() = setCandidates(emptyList(), -1)
}
