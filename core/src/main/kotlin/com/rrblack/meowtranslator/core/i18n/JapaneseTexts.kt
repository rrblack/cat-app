package com.rrblack.meowtranslator.core.i18n

import com.rrblack.meowtranslator.core.analysis.ContourShape
import com.rrblack.meowtranslator.core.classify.MeowContext
import com.rrblack.meowtranslator.core.classify.VocalType
import com.rrblack.meowtranslator.core.i18n.Texts.Companion.oneDecimal
import com.rrblack.meowtranslator.core.i18n.Texts.Companion.pct
import com.rrblack.meowtranslator.core.translate.AgeGroup
import com.rrblack.meowtranslator.core.translate.ConfidenceLevel
import com.rrblack.meowtranslator.core.translate.EmotionLabel
import com.rrblack.meowtranslator.core.translate.Evidence
import com.rrblack.meowtranslator.core.translate.Intensity
import com.rrblack.meowtranslator.core.translate.Intent
import com.rrblack.meowtranslator.core.translate.Note
import com.rrblack.meowtranslator.core.translate.PhraseKey
import com.rrblack.meowtranslator.core.translate.QualityWarning
import kotlin.math.abs
import kotlin.math.roundToInt

/** 日本語。猫のセリフはくだけた話し言葉で、ときどき「にゃ」を添えています。 */
object JapaneseTexts : Texts {
    override val language = Language.JAPANESE
    override val insistentSuffix = "（まだまだ言うよ）"
    override val defaultSpeaker = "うちの子"

    private fun set(low: List<String>, medium: List<String>, high: List<String>) =
        mapOf(Intensity.LOW to low, Intensity.MEDIUM to medium, Intensity.HIGH to high)

    private val phraseBank: Map<PhraseKey, Map<Intensity, List<String>>> = mapOf(
        PhraseKey.GREETING to set(
            listOf("あ、どうも。", "おっ、きみか。", "んー、おかえり。"),
            listOf("おかえり！待ってたよ。", "やあ、こんにちは！", "あ、きみだ！よかった。"),
            listOf("おかえりー！会いたかったにゃ！", "やっと帰ってきた！おかえり！", "こんにちは！こんにちは！こんにちは！"),
        ),
        PhraseKey.FOOD to set(
            listOf("そろそろごはんの時間かな…", "ちょっとお腹すいたかも。", "お皿、からっぽだよ…"),
            listOf("ごはんちょうだい！", "ごはんの時間だよ、まちがいない。", "キッチン行こうよ。ね？"),
            listOf("ごはん！いますぐ！おねがい！", "お腹ぺこぺこだにゃ！お皿！お皿！", "はやく！もう限界！"),
        ),
        PhraseKey.ATTENTION to set(
            listOf("ねえ…ちょっと来て？", "ねえねえ、こっち見て。", "ちょっとお願いがあるんだけど。"),
            listOf("ねえ！かまって！", "こっち来て、してほしいことがあるの。", "ドア開けて？それとも遊んでくれる？"),
            listOf("ねえ！ねえってば！こっち！", "いますぐ来て！", "手を止めて、こっち来てにゃ！"),
        ),
        PhraseKey.HANDLING_POSITIVE to set(
            listOf("ん、悪くないね。", "そのまま続けていいよ。", "まあ、いやじゃないよ。"),
            listOf("あ、そこそこ。", "それ、もっとして。", "気持ちいい〜。"),
            listOf("やめないで！", "もっと！もっと！", "そこ！最高にゃ！"),
        ),
        PhraseKey.HANDLING_NEGATIVE to set(
            listOf("うーん、これはちょっと…", "もうそのくらいでいいかな。", "そっとね…"),
            listOf("もういいってば。", "それ、あんまり好きじゃない。", "やめてくれる？"),
            listOf("やめて！もう十分！", "はなしてー！", "もうがまんできないにゃ！"),
        ),
        PhraseKey.DISTRESS to set(
            listOf("みんなどこ？", "なんだか落ち着かない…", "だれかいる？"),
            listOf("ここ、いやだよ。迎えに来て。", "どこにいるの？不安だよ。", "ひとりにしないで。"),
            listOf("助けて！ここから出して！", "こわいよ！どこにいるの！？", "戻ってきて！こんなのいやだ！"),
        ),
        PhraseKey.COMPLAINT to set(
            listOf("はぁ…", "ふん。気に入らない。", "それ、賛成できないな。"),
            listOf("これ、不満なんだけど。", "えっ、うそでしょ。いやだ。", "ありえないにゃ。"),
            listOf("ほんっとに怒ってるからね！", "ぜったいいや！", "やめて！大きらい！"),
        ),
        PhraseKey.CONTENT to set(
            listOf("平和だね。ぬくぬく。", "いま、すごく居心地いい。", "しあわせ〜。"),
            listOf("幸せでリラックスしてるよ。", "いいね。ここにいて。", "きみといると安心する。"),
            listOf("最高。ほんとに最高。", "これ大好き！動かないで！", "人生で一番いい時間にゃ。"),
        ),
        PhraseKey.SOLICITING to set(
            listOf("かわいいでしょ？…おやつは？", "ゴロゴロ…おやつがあるとうれしいな。", "いい子にしてるのには理由があるの。"),
            listOf("ゴロゴロ…それで、ごはんは？", "かわいくしてるから、ごはんちょうだい。", "お皿、満たしたくならない？"),
            listOf("ゴロゴロ…ごはん！いま！おねがい！", "このゴロゴロには勝てないでしょ？ごはん！", "全力でゴロゴロしてるよ — 朝ごはん！"),
        ),
        PhraseKey.FRIENDLY to set(
            listOf("ルルッ？あ、どうも。", "やあ、なかよし。", "ンルッ。"),
            listOf("ルルッ！やあ！ついてきて！", "こっち来て、見てほしいものがあるの！", "ねえ、きみ！一緒に行こう！"),
            listOf("ルルルッ！会えてうれしい！", "やっほー！こっちこっち！", "ブルルッ！来てくれた！"),
        ),
        PhraseKey.HUNTING to set(
            listOf("おや…あれは何？", "なにか見えるぞ…", "ふむ、鳥だ。"),
            listOf("鳥だ！ほしい！", "見て見て！あれ捕まえたい！", "獲物発見！"),
            listOf("ぜったい捕まえる！", "鳥！鳥！鳥！鳥！", "外に出して、捕まえなきゃ！"),
        ),
        PhraseKey.DEFENSIVE to set(
            listOf("ちょっと離れて。", "それ以上近づかないで。", "なんか信用できない。"),
            listOf("近寄らないで！こわいんだから。", "あっち行って！", "警告するよ — ほっといて。"),
            listOf("来るなー！", "離れろ！いますぐ！", "やれるもんならやってみな！"),
        ),
        PhraseKey.THREAT to set(
            listOf("落ち着かないんだけど。", "ほっといて。", "あんまり調子に乗らないで。"),
            listOf("警告するよ。ほっといて。", "ゆっくり下がりなさい。", "これが最後の警告だよ。"),
            listOf("本気でやるよ！", "あと一歩でパンチするから！", "出ていけ。いますぐ。"),
        ),
        PhraseKey.CALLING_OUT_DISTRESS to set(
            listOf("だれかいるー？", "おーい…", "みんなどこ行ったの？"),
            listOf("みんなどこ！？見つけて！", "呼んでるんだよ — 返事して！", "おーい？おーいってば！"),
            listOf("助けて！どこにいるの！？", "だれかいますぐ来て！", "迷子になっちゃった、いやだよ！"),
        ),
        PhraseKey.CALLING_OUT_MATING to set(
            listOf("このへんに猫いない？", "だれか相手してくれないかな。", "おーい、外のだれか…"),
            listOf("だれか、会いに来て！", "恋人募集中にゃ！", "見つけに来て！"),
            listOf("運命の相手はどこ！？", "外のきみ、返事して！", "どこにいるの、愛しい人！？"),
        ),
        PhraseKey.UNCLEAR to listOf("よく聞き取れませんでした。猫にもう少し近づいて、もう一度試してください。").let { set(it, it, it) },
    )

    override fun phrases(key: PhraseKey, intensity: Intensity) = phraseBank.getValue(key).getValue(intensity)

    override fun intentTitle(intent: Intent) = when (intent) {
        Intent.GREETING -> "あいさつ"
        Intent.FOOD -> "ごはんの催促"
        Intent.ATTENTION -> "かまってほしい"
        Intent.HANDLING -> "さわられることへの反応"
        Intent.DISTRESS -> "不安・さみしい"
        Intent.COMPLAINT -> "不満"
        Intent.CONTENT -> "満足"
        Intent.SOLICITING -> "おねだりゴロゴロ"
        Intent.FRIENDLY -> "親しみのあいさつ"
        Intent.HUNTING -> "獲物に興奮"
        Intent.DEFENSIVE -> "近づかないで"
        Intent.THREAT -> "警告"
        Intent.CALLING_OUT -> "呼びかけ"
        Intent.UNCLEAR -> "不明"
    }

    override fun intentSummary(intent: Intent) = when (intent) {
        Intent.GREETING -> "こんにちは、または「わかったよ」という返事。"
        Intent.FOOD -> "ごはんをねだっています。食事の時間の前やお皿の近くでよく見られます。"
        Intent.ATTENTION -> "注目してほしい、遊んでほしい、ドアを開けてほしい、来てほしいなどのお願い。"
        Intent.HANDLING -> "なでられる・ブラッシングされる・抱っこされることへの反応。"
        Intent.DISTRESS -> "落ち着かない、ひとりぼっち、知らない場所にいる — 安心したがっています。"
        Intent.COMPLAINT -> "何かに不満があって抗議しています。"
        Intent.CONTENT -> "リラックスしていて心地よい状態。"
        Intent.SOLICITING -> "泣き声のような響きが混じったゴロゴロ — たいていごはんのおねだりです。"
        Intent.FRIENDLY -> "うれしいあいさつ。「ついてきて」という誘いのこともあります。"
        Intent.HUNTING -> "見えているのに届かない獲物に、興奮ともどかしさを感じています。"
        Intent.DEFENSIVE -> "身の危険を感じて、相手を遠ざけようとしています。"
        Intent.THREAT -> "怒っていて、身を守る準備ができています。"
        Intent.CALLING_OUT -> "長く大きな鳴き声 — 不安、発情期の呼びかけ、なわばり、または混乱。"
        Intent.UNCLEAR -> "解釈できるほどはっきりした猫の声がありません。"
    }

    override fun vocalTypeName(type: VocalType) = when (type) {
        VocalType.MEOW -> "ニャー"
        VocalType.TRILL -> "トリル（ルルッ）"
        VocalType.PURR -> "ゴロゴロ"
        VocalType.HISS -> "シャー"
        VocalType.GROWL -> "うなり声（ウー）"
        VocalType.YOWL -> "長鳴き（アオーン）"
        VocalType.CHATTER -> "クラッキング（カカカッ）"
        VocalType.UNKNOWN -> "不明な音"
    }

    override fun vocalTypeDescription(type: VocalType) = when (type) {
        VocalType.MEOW -> "口を開けてから閉じる鳴き声。大人の猫どうしではあまり使わず、主に人間に向けた声です。"
        VocalType.TRILL -> "口を閉じたまま出す、短く転がるような、たいてい語尾の上がる声。うれしいあいさつや「ついてきて」の合図です。"
        VocalType.PURR -> "毎秒20〜30回の低く続く振動。ふつうは満足のしるしですが、自分を落ち着かせるときやおねだりのときにも出します。"
        VocalType.HISS -> "口を開けて出す、音程のない息の音。身を守るための警告です。"
        VocalType.GROWL -> "低く荒い、長く続く声。攻撃・防御のための威嚇です。"
        VocalType.YOWL -> "長く大きく引き伸ばした鳴き声。発情期、なわばり争い、不安、混乱などで見られます。"
        VocalType.CHATTER -> "短い音を素早く連続させる声。窓の外の鳥など、獲物を見ているときによく出します。"
        VocalType.UNKNOWN -> "何の声か判断できるほどの猫の声がありませんでした。"
    }

    override fun emotion(label: EmotionLabel) = when (label) {
        EmotionLabel.EXCITED_HAPPY -> "ワクワク・うれしい"
        EmotionLabel.CALM_CONTENT -> "穏やか・満足"
        EmotionLabel.STRESSED -> "ストレス・動揺"
        EmotionLabel.UNEASY -> "不安・不満"
        EmotionLabel.ALERT -> "興味津々"
        EmotionLabel.NEUTRAL -> "ふつう"
    }

    override fun contour(shape: ContourShape) = when (shape) {
        ContourShape.RISING -> "上昇（語尾が上がる）"
        ContourShape.FALLING -> "下降（語尾が下がる）"
        ContourShape.RISE_FALL -> "山型（上がって下がる）"
        ContourShape.FALL_RISE -> "谷型（下がって上がる）"
        ContourShape.FLAT -> "平坦"
        ContourShape.UNVOICED -> "音程なし"
    }

    override fun confidence(level: ConfidenceLevel) = when (level) {
        ConfidenceLevel.HIGH -> "高い"
        ConfidenceLevel.MODERATE -> "ふつう"
        ConfidenceLevel.LOW -> "低い"
    }

    override fun context(context: MeowContext) = when (context) {
        MeowContext.FOOD -> "ごはん待ち"
        MeowContext.BRUSHING -> "ブラッシング中"
        MeowContext.ISOLATION -> "知らない場所でひとりぼっち"
    }

    override fun ageGroup(age: AgeGroup) = when (age) {
        AgeGroup.KITTEN -> "子猫（1歳未満）"
        AgeGroup.ADULT -> "成猫"
        AgeGroup.SENIOR -> "シニア（11歳以上）"
    }

    override fun evidence(e: Evidence): String = when (e) {
        is Evidence.CallType -> {
            val pitch = if (e.f0Hz > 0) "音の高さ 約${e.f0Hz.roundToInt()} Hz" else "はっきりした音程なし"
            "「${vocalTypeName(e.type)}」と判定（一致度 ${pct(e.probability)}）：長さ ${oneDecimal(e.durationSec)} 秒、$pitch。"
        }
        is Evidence.DataModel -> {
            val situation = when (e.context) {
                MeowContext.FOOD -> "ごはんを待っている猫"
                MeowContext.BRUSHING -> "飼い主にブラッシングされている猫"
                MeowContext.ISOLATION -> "知らない部屋にひとりで置かれた猫"
            }
            "データモデル（${e.trainingCats}匹の猫の実際の鳴き声${e.trainingCalls}件で学習）：${situation}の鳴き声に最も近い（${pct(e.probability)}）。"
        }
        is Evidence.Melody -> {
            val dir = when {
                e.endMinusStartSt > 1 -> "終わりが始まりより高い（+${oneDecimal(e.endMinusStartSt)} 半音）"
                e.endMinusStartSt < -1 -> "終わりが始まりより低い（−${oneDecimal(abs(e.endMinusStartSt))} 半音）"
                else -> "始まりと終わりがほぼ同じ高さ"
            }
            "メロディ：${contour(e.shape)}。$dir。語尾が上がる鳴き声はポジティブ・おねだり寄り、下がる鳴き声はストレス寄りの傾向があります（Schötz ほか）。"
        }
        Evidence.ShortCall -> "短くあっさりした鳴き声 — あいさつや返事によく見られます。"
        Evidence.LongCall -> "長く引き伸ばした鳴き声 — 切実さが強いサインです。"
        Evidence.RoughVoice -> "かすれた荒い声質 — ネガティブな気持ちや興奮と結びつきやすい傾向があります。"
        is Evidence.PurrPulsing -> "約${e.rateHz.roundToInt()} Hzの規則的な振動 — ゴロゴロの特徴です。"
        is Evidence.TrillFlutter -> "約${e.rateHz.roundToInt()} Hzの震えを伴う短い鳴き声 — トリル（ルルッ）の特徴です。"
        Evidence.HissNoise -> "音程のない雑音のような音で、エネルギーが広い周波数に分布 — シャーという威嚇の特徴です。"
        Evidence.GrowlLowRough -> "低くて荒い音で、低い周波数にエネルギーが集中 — うなり声の特徴です。"
        Evidence.YowlLong -> "音程が大きく動く、とても長い鳴き声 — 長鳴き（アオーン）の特徴です。"
        Evidence.ChatterBurst -> "ごく短い鳴き声が素早く続いています — クラッキング（カカカッ）の特徴です。"
        is Evidence.Repeated -> "${e.count}回くり返し — くり返すのは「しつこくお願いしている」サインです。"
        is Evidence.PersonalMatch -> "あなたが「${intentTitle(e.intent)}」と教えた鳴き声に似ています（教えた例：${e.examples}件）。"
        is Evidence.PersonalWeight -> "あなたが教えた${e.examples}件の例も考慮しています（影響度 ${pct(e.weight)}）。"
    }

    override fun note(note: Note) = when (note) {
        Note.PURR_CAN_MEAN_STRESS -> "猫はストレスや体調不良、痛みがあるときにもゴロゴロ言うことがあります。様子がいつもと違うときは、しぐさも確認しましょう。"
        Note.GIVE_SPACE -> "距離をとって、猫が逃げられるようにしてあげましょう。威嚇を叱るのは逆効果です — ケンカを避けるための大事なサインです。"
        Note.SENIOR_YOWLING -> "高齢猫の夜鳴きは、甲状腺機能亢進症、高血圧、難聴、認知機能の低下などのサインのことがあります。一度動物病院で相談してみましょう。"
        Note.INTACT_YOWLING -> "避妊・去勢手術をしていない猫は、相手を呼ぶために大きな声で鳴きます。手術をするとたいてい治まります。"
        Note.CHATTER_PLAY -> "窓の外を眺めるのは良い刺激です。猫じゃらしで遊んで「狩り」を最後までさせてあげると満足します。"
        Note.KITTEN_MEOWING -> "子猫はごはん・ぬくもり・さみしさからよく鳴きます。この時期によく鳴くのはふつうです。"
        Note.DISTRESS_VET -> "不安そうな鳴き声が新しく始まった、頻繁になった、隠れる・食欲が変わるなどがある場合は、動物病院に相談しましょう。"
        Note.BODY_LANGUAGE -> "鳴き声はメッセージの一部にすぎません。耳・しっぽ・目、そしてその場の状況も同じくらい大切です。"
    }

    override fun warning(warning: QualityWarning) = when (warning) {
        QualityWarning.NO_SOUND -> "背景の雑音と区別できる音が見つかりませんでした。"
        QualityWarning.CLIPPED -> "音が大きすぎて割れています。スマホを少し離してください。"
        QualityWarning.NOISY -> "周りの雑音が多いため、結果の信頼度が下がります。"
        QualityWarning.TOO_QUIET -> "録音がとても小さいです。もう少し猫に近づいてみてください。"
    }
}
