package me.zssu.ime.keyboard

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import kotlin.math.min

/**
 * A compact one-hand-mode indicator: the outer rounded rectangle is the display and the filled
 * keyboard block shows whether the input surface currently spans it or hugs its left/right edge.
 */
class OneHandModeDrawable(
    var mode: CandidateStripView.OneHandDisplayMode,
    color: Int,
    private val density: Float,
) : Drawable() {
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }

    override fun draw(canvas: Canvas) {
        val size = min(bounds.width(), bounds.height()).toFloat()
        stroke.strokeWidth = size * 0.07f
        val left = bounds.exactCenterX() - size * 0.36f
        val top = bounds.exactCenterY() - size * 0.44f
        val display = RectF(left, top, left + size * 0.72f, top + size * 0.88f)
        val radius = size * 0.09f
        canvas.drawRoundRect(display, radius, radius, stroke)

        val inset = size * 0.09f
        val keyboardTop = display.bottom - size * 0.29f
        val availableLeft = display.left + inset
        val availableRight = display.right - inset
        val keyboardWidth = when (mode) {
            CandidateStripView.OneHandDisplayMode.FULL -> availableRight - availableLeft
            CandidateStripView.OneHandDisplayMode.LEFT,
            CandidateStripView.OneHandDisplayMode.RIGHT -> (availableRight - availableLeft) * 0.62f
        }
        val keyboardLeft = when (mode) {
            CandidateStripView.OneHandDisplayMode.RIGHT -> availableRight - keyboardWidth
            else -> availableLeft
        }
        canvas.drawRoundRect(
            RectF(keyboardLeft, keyboardTop, keyboardLeft + keyboardWidth, display.bottom - inset),
            radius * 0.45f,
            radius * 0.45f,
            fill,
        )
    }

    override fun setAlpha(alpha: Int) {
        stroke.alpha = alpha
        fill.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        stroke.colorFilter = colorFilter
        fill.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Android")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun getIntrinsicWidth(): Int = (24 * density).toInt()
    override fun getIntrinsicHeight(): Int = (24 * density).toInt()
}
