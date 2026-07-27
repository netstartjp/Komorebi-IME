package me.zssu.ime.style

/**
 * Large-scale rule-based text style transformation engine.
 *
 * Designed to approximate machine-learning quality through exhaustive pattern coverage:
 * systematic verb conjugation, a complete keigo dictionary, expression-level rewriting,
 * and multi-pass sentence-aware transformation.
 *
 * Architecture:
 *   1. normalize()  — strip current style down to canonical plain form
 *   2. stylize()    — re-dress with the target style's markers
 *
 * Each pass is a priority-ordered cascade. Longer / more specific patterns fire first
 * so "いらっしゃいました" resolves to the right base verb rather than being half-matched
 * by a generic ました→た rule.
 */
object TextStyleEngine {

    // ══════════════════════════════════════════════════════════════════════
    //  Public API
    // ══════════════════════════════════════════════════════════════════════

    fun apply(text: String, style: TextStyle): String {
        if (style == TextStyle.ORIGINAL || text.isBlank()) return text
        val canonical = normalize(text)
        return stylize(canonical, style)
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Phase 1 — Normalize to canonical plain form
    // ══════════════════════════════════════════════════════════════════════

    private val fixedPoliteExpressions = listOf(
        "おはようございます" to "おはよう",
        "ありがとうございます" to "ありがとう",
        "ありがとうございました" to "ありがとう",
        "すみません" to "すまない",
        "すいません" to "すまない",
        "ごめんなさい" to "ごめん",
        "おやすみなさい" to "おやすみ",
        "いただきます" to "食べる",
        "ごちそうさまでした" to "ごちそうさま",
        "お疲れ様です" to "お疲れ",
        "お疲れ様でした" to "お疲れ",
        "お世話になります" to "世話になる",
        "お世話になっています" to "世話になっている",
        "お世話になりました" to "世話になった",
        "お邪魔します" to "邪魔する",
        "お邪魔しました" to "邪魔した",
        "お待たせしました" to "待たせた",
        "失礼します" to "失礼する",
        "失礼しました" to "失礼した",
        "承知しました" to "承知した",
        "かしこまりました" to "承知した",
        "恐れ入ります" to "恐れ入る",
        "恐れ入りました" to "恐れ入った",
        "申し訳ありません" to "悪い",
        "申し訳ございません" to "悪い",
        "申し訳ない" to "悪い",
        "お願いします" to "頼む",
        "お願いいたします" to "頼む",
        "よろしくお願いします" to "よろしく",
        "よろしくお願いいたします" to "よろしく",
        "恐縮です" to "恐縮だ",
        "恐縮でした" to "恐縮だった",
        "幸いです" to "幸いだ",
        "幸いでした" to "幸いだった",
        "存じ上げております" to "知っている",
        "存じ上げておりました" to "知っていた",
        "存じ上げません" to "知らない",
        "ご了承ください" to "了承してくれ",
        "ご了承願います" to "了承してくれ",
        "ご確認ください" to "確認してくれ",
        "ご確認願います" to "確認してくれ",
        "ご検討ください" to "検討してくれ",
        "ご検討願います" to "検討してくれ",
        "お待ちください" to "待ってくれ",
        "お待ち願います" to "待ってくれ",
        "お聞きください" to "聞いてくれ",
        "ご注意ください" to "注意してくれ",
        "ご遠慮ください" to "遠慮してくれ",
        "お入りください" to "入ってくれ",
        "お座りください" to "座ってくれ",
        "お掛けください" to "掛けてくれ",
        "お越しください" to "来てくれ",
        "お申し込みください" to "申し込んでくれ",
        "お問い合わせください" to "問い合わせてくれ",
        "お使いください" to "使ってくれ",
        "ご利用ください" to "利用してくれ",
        "ご活用ください" to "活用してくれ",
        "お送りください" to "送ってくれ",
        "ご一読ください" to "一読してくれ",
        "ご参照ください" to "参照してくれ",
        "ご登録ください" to "登録してくれ",
        "ご入力ください" to "入力してくれ",
    ).sortedByDescending { it.first.length }

    private fun normalize(text: String): String {
        var result = text
        // Whole expressions must be canonicalized before a generic ます→る pass can split them.
        for ((expression, replacement) in fixedPoliteExpressions) {
            result = result.replace(expression, replacement)
        }
        for ((pattern, replacement) in normalizeRules) {
            result = pattern.replace(result, replacement)
        }
        return result
    }

    // Priority-ordered: special keigo verbs → endings → particles → cleanup.
    private val normalizeRules: List<Pair<Regex, String>> = buildList {

        // ── 尊敬語 (respectful) → 普通形 ──
        val sonkeigo = listOf(
            "いらっしゃい" to "来",
            "いらっしゃいます" to "来る",
            "いらっしゃいました" to "来た",
            "いらっしゃいません" to "来ない",
            "いらっしゃらなかった" to "来なかった",
            "いらっしゃって" to "来て",
            "いらっしゃれば" to "来れば",
            "いらっしゃる" to "いる",
            "おっしゃい" to "言え",
            "おっしゃいます" to "言う",
            "おっしゃいました" to "言った",
            "おっしゃいません" to "言わない",
            "おっしゃって" to "言って",
            "おっしゃる" to "言う",
            "なさい" to "しろ",
            "なさいます" to "する",
            "なさいました" to "した",
            "なさいません" to "しない",
            "なさって" to "して",
            "なさる" to "する",
            "召し上がります" to "食べる",
            "召し上がりました" to "食べた",
            "召し上がりません" to "食べない",
            "召し上がって" to "食べて",
            "召し上がる" to "食べる",
            "ご覧になります" to "見る",
            "ご覧になりました" to "見た",
            "ご覧になって" to "見て",
            "ご覧になる" to "見る",
            "ご存じです" to "知っている",
            "ご存じでした" to "知っていた",
            "ご存じ" to "知っている",
            "お休みになります" to "寝る",
            "お休みになる" to "寝る",
            "お帰りになります" to "帰る",
            "お帰りになる" to "帰る",
            "お越しになります" to "来る",
            "お越しになる" to "来る",
            "お聞きになります" to "聞く",
            "お聞きになる" to "聞く",
            "お読みになります" to "読む",
            "お読みになる" to "読む",
            "お書きになります" to "書く",
            "お書きになる" to "書く",
            "お待ちになります" to "待つ",
            "お待ちになる" to "待つ",
            "お考えになります" to "考える",
            "お考えになる" to "考える",
            "お使いになります" to "使う",
            "お使いになる" to "使う",
            "お召しになります" to "着る",
            "お召しになる" to "着る",
            "お求めになります" to "求める",
            "お求めになる" to "求める",
            "お申し込みになります" to "申し込む",
            "お申し込みになる" to "申し込む",
            "お会いになります" to "会う",
            "お会いになる" to "会う",
            "お見えになります" to "見える",
            "お見えになる" to "見える",
            "お出かけになります" to "出かける",
            "お出かけになる" to "出かける",
            "お泊まりになります" to "泊まる",
            "お泊まりになる" to "泊まる",
        )
        for ((keigo, plain) in sonkeigo.sortedByDescending { it.first.length }) {
            add(Regex(Regex.escape(keigo)) to plain)
        }

        // ── 謙譲語 (humble) → 普通形 ──
        val kenjogo = listOf(
            "申します" to "言う",
            "申しました" to "言った",
            "申しません" to "言わない",
            "申して" to "言って",
            "申す" to "言う",
            "いたします" to "する",
            "いたしました" to "した",
            "いたしません" to "しない",
            "いたして" to "して",
            "いたす" to "する",
            "致します" to "する",
            "致しました" to "した",
            "参ります" to "行く",
            "参りました" to "行った",
            "参りません" to "行かない",
            "参って" to "行って",
            "参る" to "行く",
            "頂きます" to "もらう",
            "頂きました" to "もらった",
            "頂きません" to "もらわない",
            "頂いて" to "もらって",
            "頂く" to "もらう",
            "いただきます" to "もらう",
            "いただきました" to "もらった",
            "いただいて" to "もらって",
            "いただく" to "もらう",
            "存じます" to "知る",
            "存じました" to "知った",
            "存じません" to "知らない",
            "存じて" to "知って",
            "存じる" to "知る",
            "承ります" to "聞く",
            "承りました" to "聞いた",
            "承る" to "聞く",
            "伺います" to "聞く",
            "伺いました" to "聞いた",
            "伺って" to "聞いて",
            "伺う" to "聞く",
            "拝見します" to "見る",
            "拝見しました" to "見た",
            "拝見して" to "見て",
            "拝見する" to "見る",
            "拝借します" to "借りる",
            "拝借しました" to "借りた",
            "拝借する" to "借りる",
            "差し上げます" to "あげる",
            "差し上げました" to "あげた",
            "差し上げて" to "あげて",
            "差し上げる" to "あげる",
            "下さいます" to "くれる",
            "下さいました" to "くれた",
            "下さって" to "くれて",
            "下さる" to "くれる",
            "お目にかかります" to "会う",
            "お目にかかりました" to "会った",
            "お目にかかる" to "会う",
            "お伺いします" to "訪ねる",
            "お伺いしました" to "訪ねた",
            "お伺いする" to "訪ねる",
            "お伝えいたします" to "伝える",
            "お伝えいたしました" to "伝えた",
            "お届けいたします" to "届ける",
            "お届けいたしました" to "届けた",
            "お返しいたします" to "返す",
            "お返しいたしました" to "返した",
            "お待たせいたしました" to "待たせた",
            "失礼いたします" to "失礼する",
            "失礼いたしました" to "失礼した",
        )
        for ((keigo, plain) in kenjogo.sortedByDescending { it.first.length }) {
            add(Regex(Regex.escape(keigo)) to plain)
        }

        // ── 丁寧語 (polite) → 普通形: sentence-final patterns ──
        // Order: longer/more specific first
        add(Regex(Regex.escape("ませんでした")) to "なかった")
        add(Regex(Regex.escape("ません")) to "ない")
        add(Regex(Regex.escape("でした")) to "だった")
        add(Regex(Regex.escape("ました")) to "た")
        add(Regex(Regex.escape("でしょう")) to "だろう")
        add(Regex(Regex.escape("ましょう")) to "おう")
        add(Regex(Regex.escape("でございます")) to "だ")
        add(Regex(Regex.escape("でございました")) to "だった")
        add(Regex(Regex.escape("です")) to "だ")
        add(Regex(Regex.escape("ます")) to "る")

        // ── 〜て form politeness ──
        add(Regex("てください") to "てくれ")
        add(Regex("でください") to "でくれ")
        add(Regex("て下さい") to "てくれ")
        add(Regex("ています") to "ている")
        add(Regex("ていました") to "ていた")
        add(Regex("ていません") to "ていない")
        add(Regex("ております") to "ている")
        add(Regex("ておりました") to "ていた")
        add(Regex("てまいります") to "ていく")
        add(Regex("てまいりました") to "ていった")

        // ── Desu/masu in mid-sentence conjunctions ──
        for (conj in listOf("ので", "から", "が", "し", "のに")) {
            add(Regex("ます$conj") to "る$conj")
            add(Regex("ません$conj") to "ない$conj")
        }

        // ── Adjective polite → plain ──
        add(Regex("([くい])でした") to "${'$'}1かった")
        add(Regex("([くい])です") to "${'$'}1")
        add(Regex("([くい])でしょう") to "${'$'}1だろう")

    }

    // ══════════════════════════════════════════════════════════════════════
    //  Phase 2 — Stylize from canonical plain form
    // ══════════════════════════════════════════════════════════════════════

    private fun stylize(plain: String, style: TextStyle): String {
        var result = plain
        val rules = styleRules[style] ?: return result
        for ((pattern, replacement) in rules) {
            result = pattern.replace(result, replacement)
        }
        return result
    }

    private val styleRules: Map<TextStyle, List<Pair<Regex, String>>> by lazy {
        mapOf(
            TextStyle.PLAIN to plainStyleRules,
            TextStyle.CASUAL to casualStyleRules,
            TextStyle.NORMAL_KEIGO to normalKeigoStyleRules,
            TextStyle.HARD_KEIGO to hardKeigoStyleRules,
            TextStyle.SOFT_KEIGO to softKeigoStyleRules,
        )
    }

    // ── PLAIN (ふつう): canonical plain is the target ──

    private val plainStyleRules: List<Pair<Regex, String>> = listOf(
        Regex(Regex.escape("おる")) to "いる",
        Regex(Regex.escape("ござる")) to "ある",
    )

    // ── CASUAL (ゆるく): plain + casual sentence-ending particles ──

    private val casualStyleRules: List<Pair<Regex, String>> = listOf(
        // Add casual particles to plain-form endings
        Regex("だ(?=[。、])") to "だよ",
        Regex("だ$") to "だよ",
        Regex("る(?=[。、])") to "るよ",
        Regex("る$") to "るよ",
        Regex("た(?=[。、\\s])") to "たよ",
        Regex("た$") to "たよ",
        Regex("ない(?=[。、])") to "ないよ",
        Regex("ない$") to "ないよ",
        Regex("かった(?=[。、])") to "かったよ",
        Regex("かった$") to "かったよ",
        Regex("だろう(?=[。、])") to "だろうね",
        Regex("だろう$") to "だろうね",
        Regex("おう(?=[。、])") to "おうよ",
        Regex("おう$") to "おうよ",
        Regex("くれ(?=[。、])") to "くれよ",
        Regex("くれ$") to "くれよ",
        // contractions
        Regex("ている") to "てる",
        Regex("ていた") to "てた",
        Regex("ていく") to "てく",
        Regex("ておく") to "とく",
        Regex("てしまう") to "ちゃう",
        Regex("でしまう") to "じゃう",
        Regex("では") to "じゃ",
        Regex("ては") to "ちゃ",
        Regex("なければ") to "なきゃ",
        Regex("なくては") to "なくちゃ",
        // wave dash normalization
        Regex("～") to "〜",
    )

    // ── NORMAL KEIGO (ノーマル敬語): plain → です・ます体 ──

    private val normalKeigoStyleRules: List<Pair<Regex, String>> = buildList {
        // copula
        add(Regex("だ(?=[。、\\s]|$)") to "です")
        add(Regex("だった(?=[。、\\s]|$)") to "でした")
        add(Regex("である(?=[。、\\s]|$)") to "です")
        add(Regex("であった(?=[。、\\s]|$)") to "でした")

        // する verb
        add(Regex("している(?=[。、\\s]|$)") to "しています")
        add(Regex("していた(?=[。、\\s]|$)") to "していました")
        add(Regex("していない(?=[。、\\s]|$)") to "していません")
        add(Regex("する(?=[。、\\s]|$)") to "します")
        add(Regex("した(?=[。、\\s]|$)") to "しました")
        add(Regex("しない(?=[。、\\s]|$)") to "しません")
        add(Regex("しなかった(?=[。、\\s]|$)") to "しませんでした")

        // 来る → 来ます
        add(Regex("来る(?=[。、\\s]|$)") to "来ます")
        add(Regex("来た(?=[。、\\s]|$)") to "来ました")
        add(Regex("来ない(?=[。、\\s]|$)") to "来ません")

        // ている → ています
        add(Regex("ている(?=[。、\\s]|$)") to "ています")
        add(Regex("ていた(?=[。、\\s]|$)") to "ていました")
        add(Regex("ていない(?=[。、\\s]|$)") to "ていません")
        add(Regex("ていなかった(?=[。、\\s]|$)") to "ていませんでした")

        // common godan/ichidan verbs: plain → polite, exact pairs
        val verbPairs = listOf(
            "ある" to "あります", "あった" to "ありました", "ない" to "ありません",
            "書く" to "書きます", "書いた" to "書きました", "書かない" to "書きません",
            "行く" to "行きます", "行った" to "行きました", "行かない" to "行きません",
            "読む" to "読みます", "読んだ" to "読みました", "読まない" to "読みません",
            "話す" to "話します", "話した" to "話しました", "話さない" to "話しません",
            "思う" to "思います", "思った" to "思いました", "思わない" to "思いません",
            "送る" to "送ります", "送った" to "送りました", "送らない" to "送りません",
            "作る" to "作ります", "作った" to "作りました", "作らない" to "作りません",
            "使う" to "使います", "使った" to "使いました", "使わない" to "使いません",
            "見る" to "見ます", "見た" to "見ました", "見ない" to "見ません",
            "食べる" to "食べます", "食べた" to "食べました", "食べない" to "食べません",
            "出る" to "出ます", "出た" to "出ました", "出ない" to "出ません",
            "寝る" to "寝ます", "寝た" to "寝ました", "寝ない" to "寝ません",
            "聞く" to "聞きます", "聞いた" to "聞きました", "聞かない" to "聞きません",
            "買う" to "買います", "買った" to "買いました", "買わない" to "買いません",
            "待つ" to "待ちます", "待った" to "待ちました", "待たない" to "待ちません",
            "持つ" to "持ちます", "持った" to "持ちました", "持たない" to "持ちません",
            "分かる" to "分かります", "分かった" to "分かりました", "分からない" to "分かりません",
            "帰る" to "帰ります", "帰った" to "帰りました", "帰らない" to "帰りません",
            "始める" to "始めます", "始めた" to "始めました", "始めない" to "始めません",
            "終わる" to "終わります", "終わった" to "終わりました", "終わらない" to "終わりません",
            "考える" to "考えます", "考えた" to "考えました", "考えない" to "考えません",
            "知る" to "知ります", "知った" to "知りました", "知らない" to "知りません",
            "入る" to "入ります", "入った" to "入りました", "入らない" to "入りません",
            "出かける" to "出かけます", "出かけた" to "出かけました",
            "付ける" to "付けます", "付けた" to "付けました",
        )
        for ((plain, polite) in verbPairs) {
            add(Regex(Regex.escape(plain) + "(?=[。、\\s]|$)") to polite)
        }

        // て form politeness
        add(Regex("てくれ(?=[。、\\s]|$)") to "てください")
        add(Regex("でくれ(?=[。、\\s]|$)") to "でください")

        // 〜と思う etc
        add(Regex("と思う(?=[。、\\s]|$)") to "と思います")
        add(Regex("と思った(?=[。、\\s]|$)") to "と思いました")
        add(Regex("と考える(?=[。、\\s]|$)") to "と考えます")
        add(Regex("と考えた(?=[。、\\s]|$)") to "と考えました")

        // volitional
        add(Regex("だろう(?=[。、\\s]|$)") to "でしょう")
        add(Regex("おう(?=[。、\\s]|$)") to "ましょう")

        // adjective endings
        add(Regex("([くい])い(?=[。、\\s]|$)") to "${'$'}1いです")
        add(Regex("([くい])かった(?=[。、\\s]|$)") to "${'$'}1かったです")

        // のだ / んだ
        add(Regex("のだ(?=[。、\\s]|$)") to "のです")
        add(Regex("んだ(?=[。、\\s]|$)") to "んです")
        add(Regex("のだった(?=[。、\\s]|$)") to "のでした")
        add(Regex("んだった(?=[。、\\s]|$)") to "んでした")

        // quoted
        add(Regex("とのことだ(?=[。、\\s]|$)") to "とのことです")
        add(Regex("とのことだった(?=[。、\\s]|$)") to "とのことでした")
        add(Regex("ということだ(?=[。、\\s]|$)") to "ということです")
    }

    // ── HARD KEIGO (堅い敬語): 尊敬語／謙譲語を最大化 ──

    private val hardKeigoStyleRules: List<Pair<Regex, String>> by lazy {
        normalKeigoStyleRules + listOf(
            // Specific verb uplifts (humble/respectful)
            Regex("言います(?=[。、\\s]|$)") to "申します",
            Regex("言いました(?=[。、\\s]|$)") to "申しました",
            Regex("食べます(?=[。、\\s]|$)") to "召し上がります",
            Regex("食べました(?=[。、\\s]|$)") to "召し上がりました",
            Regex("見ます(?=[。、\\s]|$)") to "拝見します",
            Regex("見ました(?=[。、\\s]|$)") to "拝見しました",
            Regex("行きます(?=[。、\\s]|$)") to "参ります",
            Regex("行きました(?=[。、\\s]|$)") to "参りました",
            Regex("来ます(?=[。、\\s]|$)") to "参ります",
            Regex("来ました(?=[。、\\s]|$)") to "参りました",
            Regex("もらいます(?=[。、\\s]|$)") to "頂きます",
            Regex("もらいました(?=[。、\\s]|$)") to "頂きました",
            Regex("聞きます(?=[。、\\s]|$)") to "承ります",
            Regex("聞きました(?=[。、\\s]|$)") to "承りました",
            Regex("知っています(?=[。、\\s]|$)") to "存じ上げております",
            Regex("知っていますか(?=[。、\\s]|$)") to "ご存じですか",
            Regex("知っています") to "存じ上げております",
            // Generic います → おります (only when not preceded by keigo stem)
            Regex("(?<!申し|頂き|存じ|承り|伺い|拝見|参り|おり)います(?=[。、\\s]|$)") to "おります",
            // copula upgrades
            Regex("です(?=[。、\\s]|$)") to "でございます",
            Regex("でした(?=[。、\\s]|$)") to "でございました",
            Regex("でしょう(?=[。、\\s]|$)") to "でございましょう",
            // ください → くださいませ
            Regex("ください(?=[。、\\s]|$)") to "くださいませ",
        )
    }

    // ── SOFT KEIGO (緩い敬語): です・ます + やわらかい語尾 ──

    private val softKeigoStyleRules: List<Pair<Regex, String>> by lazy {
        normalKeigoStyleRules + listOf(
            Regex("です(?=[。])") to "ですね",
            Regex("です(?=\\s|$)") to "ですね",
            Regex("ます(?=[。])") to "ますね",
            Regex("ます(?=\\s|$)") to "ますね",
            Regex("でした(?=[。])") to "でしたね",
            Regex("でした(?=\\s|$)") to "でしたね",
            Regex("ました(?=[。])") to "ましたね",
            Regex("ました(?=\\s|$)") to "ましたね",
            Regex("ません(?=[。])") to "ませんね",
            Regex("ません(?=\\s|$)") to "ませんね",
            Regex("ましょう(?=[。])") to "ましょうね",
            Regex("ましょう(?=\\s|$)") to "ましょうね",
            Regex("ください(?=[。])") to "くださいね",
            Regex("ください(?=\\s|$)") to "くださいね",
            // 〜と思う → 〜かなと思う
            Regex("と思います(?=[。]|$)") to "かなと思います",
            Regex("と思いました(?=[。]|$)") to "かなと思いました",
            // 〜でしょう → 〜でしょうね
            Regex("でしょう(?=[。]\\s|$)") to "でしょうね",
        )
    }
}
