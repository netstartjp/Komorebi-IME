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

    @Test
    fun fullReadingConversionBeatsVerbatimAndCorrections() {
        val reading = "りょうかいです"
        val candidates = listOf(
            candidate(1, reading, inputReading = reading),
            candidate(
                2,
                "利用開始です",
                correction = true,
                inputReading = reading,
                sourceReading = "りようかいしです",
            ),
            candidate(
                3,
                "領海です",
                correction = true,
                inputReading = reading,
                sourceReading = "りょうかいでし",
            ),
            candidate(
                4,
                "了解です",
                prediction = true,
                inputReading = reading,
                sourceReading = "りょうかい",
            ),
        )

        val ranked = CandidateRanking.rankLiveJapaneseSuggestions(candidates, limit = 4)

        assertEquals(
            listOf("了解です", "りょうかいです", "利用開始です", "領海です"),
            ranked.map { it.text },
        )
    }

    @Test
    fun longerPredictionStaysBehindLiteralReading() {
        val reading = "りょうかいです"
        val candidates = listOf(
            candidate(
                1,
                "了解ですので",
                prediction = true,
                inputReading = reading,
                sourceReading = "りょうかいですので",
            ),
            candidate(2, reading, inputReading = reading),
            candidate(3, "了解です", inputReading = reading),
        )

        val ranked = CandidateRanking.rankLiveJapaneseSuggestions(candidates, limit = 3)

        assertEquals(
            listOf("了解です", "りょうかいです", "了解ですので"),
            ranked.map { it.text },
        )
    }

    @Test
    fun katakanaGrammarPredictionFallsBehindNaturalPrediction() {
        val reading = "りょうかい"
        val candidates = listOf(
            candidate(
                1,
                "了解デス",
                prediction = true,
                inputReading = reading,
                sourceReading = "りょうかいです",
            ),
            candidate(
                2,
                "了解です",
                prediction = true,
                inputReading = reading,
                sourceReading = "りょうかいです",
            ),
            candidate(3, reading, inputReading = reading),
        )

        val ranked = CandidateRanking.rankLiveJapaneseSuggestions(candidates, limit = 3)

        assertEquals(
            listOf("りょうかい", "了解です", "了解デス"),
            ranked.map { it.text },
        )
    }

    @Test
    fun legitimateKatakanaNounAfterKanjiIsNotPenalized() {
        val reading = "ろっぽんぎひるず"
        val candidates = listOf(
            candidate(
                1,
                "六本木ヒルズ",
                prediction = true,
                inputReading = reading,
                sourceReading = reading,
            ),
            candidate(2, reading, inputReading = reading),
        )

        val ranked = CandidateRanking.rankLiveJapaneseSuggestions(candidates, limit = 2)

        assertEquals(listOf("六本木ヒルズ", reading), ranked.map { it.text })
    }

    private fun candidate(
        id: Int,
        text: String,
        userDictionary: Boolean = false,
        correction: Boolean = false,
        prediction: Boolean = false,
        priorityMatch: PriorityMatch = PriorityMatch.NONE,
        inputReading: String = "",
        sourceReading: String = inputReading,
    ) = MozcSession.Candidate(
        id = id,
        text = text,
        fromUserDictionary = userDictionary,
        inputReading = inputReading,
        sourceReading = sourceReading,
        correction = correction,
        prediction = prediction,
        priorityMatch = priorityMatch,
    )
}
