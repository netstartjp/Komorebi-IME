package me.zssu.ime.karukan

/**
 * Small JNI boundary around the upstream Karukan conversion core.
 *
 * Every call is made from [KarukanEngine]'s single executor. Keeping model creation, inference and
 * destruction on the same thread avoids concurrent access to llama.cpp state.
 */
internal object KarukanNative {
    val isAvailable: Boolean = runCatching {
        System.loadLibrary("karukan_android")
        true
    }.getOrDefault(false)

    external fun nativeCreate(
        modelPath: String,
        tokenizerPath: String,
        threads: Int,
    ): Long

    external fun nativeConvert(
        handle: Long,
        reading: String,
        context: String,
        candidateCount: Int,
    ): Array<String>

    external fun nativeDestroy(handle: Long)
}

/** Capability query used by settings before offering the engine switch. */
object KarukanPlatform {
    val isSupported: Boolean get() = KarukanNative.isAvailable
}
