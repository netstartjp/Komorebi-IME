package me.zssu.ime.keyboard

import kotlin.math.abs
import kotlin.math.max

/**
 * Pure geometry for "which way did the finger go".
 *
 * Split out of [FlickKeyboardView] because this is the one piece of the touch path with real edge
 * cases — diagonal drags, unassigned directions, sub-threshold jitter — and testing it through a
 * View means instrumenting a device to assert on arithmetic.
 */
object FlickResolver {

    /**
     * Retains the farthest point of a gesture instead of only its release point.
     *
     * A thumb commonly travels in a shallow arc and may curl back toward the key before release.
     * Keeping the peak excursion makes that curved motion resolve like the straight flick it was
     * aiming for, without allocating a list of every MotionEvent sample.
     */
    class PathTracker(
        private val originX: Float,
        private val originY: Float,
    ) {
        var peakDx: Float = 0f
            private set
        var peakDy: Float = 0f
            private set
        private var peakDistanceSquared: Float = 0f

        fun record(x: Float, y: Float) {
            val dx = x - originX
            val dy = y - originY
            val distanceSquared = dx * dx + dy * dy
            if (distanceSquared > peakDistanceSquared) {
                peakDistanceSquared = distanceSquared
                peakDx = dx
                peakDy = dy
            }
        }
    }

    /**
     * @param key the key under the finger, used to reject directions it has nothing assigned to
     * @param dx horizontal travel from the touch-down point, in pixels
     * @param dy vertical travel, in pixels (screen coordinates: positive is down)
     * @param thresholdPx distance the finger must travel before a flick counts
     */
    fun resolve(key: KeySpec, dx: Float, dy: Float, thresholdPx: Float): FlickDirection {
        if (max(abs(dx), abs(dy)) < thresholdPx) return FlickDirection.CENTER

        // Ties go to the horizontal axis. Thumbs drift vertically more than horizontally, so
        // treating an exact diagonal as horizontal matches how people actually aim at い/え.
        val candidate = if (abs(dx) >= abs(dy)) {
            if (dx < 0) FlickDirection.LEFT else FlickDirection.RIGHT
        } else {
            if (dy < 0) FlickDirection.UP else FlickDirection.DOWN
        }

        // Falling back to CENTER keeps a sloppy flick usable instead of silently eating the
        // keystroke, which is what a strict reading of the gesture would do.
        return if (key.output(candidate) != null) candidate else FlickDirection.CENTER
    }
}
