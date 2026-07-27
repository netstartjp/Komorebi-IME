package me.zssu.ime.ime

import org.junit.Assert.assertEquals
import org.junit.Test

class CandidateRankingTest {

    @Test
    fun latinUserDictionaryEntriesDoNotOccupyTopOfJapaneseSuggestions() {
        val candidates = listOf(
            candidate(1, "seal", userDictionary = true),
            candidate(2, "シール"),
            candidate(3, "知る"),
            candidate(4, "汁"),
            candidate(5, "印"),
            candidate(6, "標章"),
            candidate(7, "ステッカー"),
            candidate(8, "SEAL", userDictionary = true),
        )

        val ranked = CandidateRanking.rankLiveJapaneseSuggestions(candidates, limit = 8)

        assertEquals(
            listOf("シール", "知る", "汁", "印", "標章", "seal", "ステッカー", "SEAL"),
            ranked.map { it.text },
        )
    }

    @Test
    fun nativeEnglishAndJapaneseUserEntriesKeepMozcOrder() {
        val candidates = listOf(
            candidate(1, "Google"),
            candidate(2, "グーグル", userDictionary = true),
            candidate(3, "検索"),
        )

        val ranked = CandidateRanking.rankLiveJapaneseSuggestions(candidates, limit = 3)

        assertEquals(candidates, ranked)
    }

    @Test
    fun latinOnlyResultIsNotPointlesslyReordered() {
        val candidates = listOf(
            candidate(1, "HTTP", userDictionary = true),
            candidate(2, "HTTPS", userDictionary = true),
        )

        val ranked = CandidateRanking.rankLiveJapaneseSuggestions(candidates, limit = 2)

        assertEquals(candidates, ranked)
    }

    private fun candidate(
        id: Int,
        text: String,
        userDictionary: Boolean = false,
    ) = MozcSession.Candidate(
        id = id,
        text = text,
        fromUserDictionary = userDictionary,
    )
}
