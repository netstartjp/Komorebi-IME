package me.zssu.ime.style

/**
 * Public entry point for on-device text style correction.
 *
 * Transformation remains fully offline. The accuracy-first implementation uses phrase analysis,
 * dictionary-backed conjugation, clause boundaries, and subject-aware honorific selection.
 */
object TextStyleEngine {

    fun apply(text: String, style: TextStyle): String =
        HighAccuracyTextStyleTransformer.apply(text, style)
}
