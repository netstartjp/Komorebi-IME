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

    @Test
    fun literalThenCorrectionThenPredictionUseSeparateStableLanes() {
        val candidates = listOf(
            candidate(1, "電話番号", prediction = true),
            candidate(2, "電波", correction = true),
            candidate(3, "電話"),
            candidate(4, "伝話"),
            candidate(5, "電場", correction = true),
        )

        val ranked = CandidateRanking.rankLiveJapaneseSuggestions(candidates, limit = 5)

        assertEquals(
            listOf("電話", "伝話", "電波", "電場", "電話番号"),
            ranked.map { it.text },
        )
    }

    @Test
    fun exactPriorityBeatsLiteralAndSimilarRecovery() {
        val candidates = listOf(
            candidate(1, "通常"),
            candidate(2, "類似登録", priorityMatch = PriorityMatch.SIMILAR),
            candidate(3, "完全登録", priorityMatch = PriorityMatch.EXACT),
        )

        val ranked = CandidateRanking.rankLiveJapaneseSuggestions(candidates, limit = 3)

        assertEquals(listOf("完全登録", "類似登録", "通常"), ranked.map { it.text })
    }

    private fun candidate(
        id: Int,
        text: String,
        userDictionary: Boolean = false,
        correction: Boolean = false,
        prediction: Boolean = false,
        priorityMatch: PriorityMatch = PriorityMatch.NONE,
    ) = MozcSession.Candidate(
        id = id,
        text = text,
        fromUserDictionary = userDictionary,
        correction = correction,
        prediction = prediction,
        priorityMatch = priorityMatch,
    )
}
