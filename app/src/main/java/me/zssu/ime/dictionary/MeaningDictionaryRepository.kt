package me.zssu.ime.dictionary

import android.content.Context
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Human-editable supplemental meanings, deliberately independent from Mozc conversion data.
 *
 * Any JSON file in assets/meaning_dictionaries or the app-owned imported directory is merged.
 * Entries use exact surface-form matching so a definition is never shown for a different word.
 */
class MeaningDictionaryRepository(private val context: Context) {
    @Serializable
    data class Dictionary(
        val id: String,
        val label: String = id,
        val entries: List<Entry>,
    )

    @Serializable
    data class Entry(
        val term: String,
        val reading: String? = null,
        val meanings: List<String>,
        val tags: List<String> = emptyList(),
        val source: String? = null,
    )

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }
    private val userDir = File(context.filesDir, DIRECTORY)

    fun lookup(term: String): List<Entry> =
        dictionaries().flatMap { it.entries }.filter { it.term == term }

    fun dictionaries(): List<Dictionary> {
        val assetNames = runCatching {
            context.assets.list(DIRECTORY)?.filter { it.endsWith(".json") }
        }.getOrNull().orEmpty()
        val bundled = assetNames.mapNotNull { name ->
            decode(runCatching {
                context.assets.open("$DIRECTORY/$name").bufferedReader().use { it.readText() }
            }.getOrNull(), name)
        }
        val user = userDir.listFiles { file -> file.extension == "json" }
            ?.sortedBy { it.name }
            ?.mapNotNull { decode(runCatching { it.readText() }.getOrNull(), it.name) }
            .orEmpty()
        // A user file with the same id intentionally overrides the bundled starter dictionary.
        return (user + bundled).distinctBy { it.id }
    }

    fun importJson(text: String): Result<Dictionary> = runCatching {
        require(text.toByteArray().size <= MAX_BYTES) { "意味辞書は1 MB以下にしてください" }
        val dictionary = json.decodeFromString<Dictionary>(text)
        validate(dictionary)
        userDir.mkdirs()
        File(userDir, "${dictionary.id}.json").writeText(
            json.encodeToString(Dictionary.serializer(), dictionary)
        )
        dictionary
    }

    fun deleteUserDictionary(id: String): Boolean = safeId(id) &&
        File(userDir, "$id.json").delete()

    fun isUserDictionary(id: String): Boolean = safeId(id) &&
        File(userDir, "$id.json").isFile

    fun exampleJson(): String = json.encodeToString(
        Dictionary.serializer(),
        Dictionary(
            id = "my_dictionary",
            label = "自分の意味辞書",
            entries = listOf(
                Entry(
                    term = "木漏れ日",
                    reading = "こもれび",
                    meanings = listOf("木々の葉の間から差し込む日光。"),
                    tags = listOf("名詞"),
                    source = "自作",
                )
            ),
        ),
    )

    private fun decode(text: String?, name: String): Dictionary? {
        if (text == null) return null
        return runCatching { json.decodeFromString<Dictionary>(text).also(::validate) }
            .onFailure { Log.w(TAG, "意味辞書を読み込めません: $name", it) }
            .getOrNull()
    }

    private fun validate(value: Dictionary) {
        require(safeId(value.id)) { "idは英数字・ハイフン・アンダースコアで指定してください" }
        require(value.entries.size <= MAX_ENTRIES) { "登録語数は${MAX_ENTRIES}語以下にしてください" }
        require(value.entries.all {
            it.term.isNotBlank() && it.term.length <= 100 &&
                it.meanings.isNotEmpty() && it.meanings.all { meaning ->
                    meaning.isNotBlank() && meaning.length <= 500
                }
        }) { "語句または意味が空か、長すぎます" }
    }

    private fun safeId(id: String): Boolean = ID.matches(id)

    companion object {
        const val DIRECTORY = "meaning_dictionaries"
        private const val MAX_BYTES = 1024 * 1024
        private const val MAX_ENTRIES = 10_000
        private val ID = Regex("[A-Za-z0-9_-]{1,64}")
        private const val TAG = "MeaningDictionary"
    }
}
