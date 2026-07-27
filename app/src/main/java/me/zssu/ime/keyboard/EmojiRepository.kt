package me.zssu.ime.keyboard

import android.content.Context
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** Small, local-only usage store for the emoji/kaomoji palette. */
class EmojiRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    private val json = Json

    fun recents(): List<String> = read(KEY_RECENTS)
    fun favorites(): List<String> = read(KEY_FAVORITES)

    fun recordUsed(value: String) {
        write(KEY_RECENTS, (listOf(value) + recents().filterNot { it == value }).take(MAX_RECENTS))
    }

    fun toggleFavorite(value: String): Boolean {
        val old = favorites()
        val added = value !in old
        write(KEY_FAVORITES, if (added) old + value else old.filterNot { it == value })
        return added
    }

    private fun read(key: String): List<String> = runCatching {
        json.decodeFromString(ListSerializer(String.serializer()), prefs.getString(key, "[]")!!)
    }.getOrDefault(emptyList())

    private fun write(key: String, values: List<String>) {
        prefs.edit().putString(
            key,
            json.encodeToString(ListSerializer(String.serializer()), values),
        ).apply()
    }

    companion object {
        private const val NAME = "emoji_palette"
        private const val KEY_RECENTS = "recents"
        private const val KEY_FAVORITES = "favorites"
        private const val MAX_RECENTS = 30

        val EMOJI = listOf(
            "😀", "😂", "🥰", "😊", "😭", "😢", "😎", "🤔", "🫡", "🥳",
            "👍", "👎", "👏", "🙏", "💪", "👌", "✌️", "👀", "❤️", "💛",
            "✨", "🔥", "🎉", "✅", "❌", "⚠️", "💡", "📌", "🚀", "🌿",
        )
        val KAOMOJI = listOf(
            "(^^)", "(^_^)", "(*´ω｀*)", "(・∀・)", "(｀・ω・´)", "(´・ω・｀)",
            "＼(^o^)／", "m(_ _)m", "(>_<)", "(T_T)", "(￣▽￣)", "(；´Д｀)",
            "Σ(ﾟДﾟ)", "ｷﾀ━━━━(ﾟ∀ﾟ)━━━━!!", "ﾉｼ", "乙", "w", "草",
        )
    }
}
