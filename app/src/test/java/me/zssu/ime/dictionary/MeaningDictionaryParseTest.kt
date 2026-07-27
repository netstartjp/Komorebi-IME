package me.zssu.ime.dictionary

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MeaningDictionaryParseTest {
    @Test
    fun bundledMeaningDictionariesParseAndContainUsableEntries() {
        val files = File("src/main/assets/meaning_dictionaries")
            .listFiles { file -> file.extension == "json" }
            .orEmpty()
        assertTrue(files.isNotEmpty())
        files.forEach { file ->
            val dictionary = Json.decodeFromString<MeaningDictionaryRepository.Dictionary>(
                file.readText()
            )
            assertEquals(file.nameWithoutExtension, dictionary.id)
            assertTrue(dictionary.entries.isNotEmpty())
            dictionary.entries.forEach {
                assertTrue(it.term.isNotBlank())
                assertTrue(it.meanings.all(String::isNotBlank))
            }
        }
    }
}
