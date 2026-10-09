package com.rrblack.meowtranslator.ui.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import com.rrblack.meowtranslator.UiMessage
import com.rrblack.meowtranslator.core.i18n.EnglishTexts
import com.rrblack.meowtranslator.core.i18n.Language
import com.rrblack.meowtranslator.core.i18n.Texts
import com.rrblack.meowtranslator.core.i18n.Texts.Companion.pct

/** All app-chrome text. Engine output (phrases, evidence, …) comes from [Texts]. */
interface UiStrings {
    val appTitle: String
    val tabTranslate: String
    val tabHistory: String
    val tabMyCat: String
    val tabScience: String

    // Translate screen
    fun subtitle(catName: String): String
    val statusIdle: String
    val statusHeard: String
    fun statusListening(seconds: Int): String
    val statusAnalyzing: String
    val importFile: String
    val startListening: String
    val stopListening: String
    val tipsTitle: String
    val tips: List<String>
    val disclaimer: String

    // Result
    fun says(speaker: String): String
    fun confidenceChip(level: String, percent: String): String
    val playRecording: String
    val stop: String
    val sectionMood: String
    val sectionOtherMeanings: String
    val sectionSound: String
    val sectionWhy: String
    val sectionGoodToKnow: String
    val closestMatch: String
    val teachTitle: String
    val teachHint: String
    fun learnedSoFar(count: Int): String
    val notMyCat: String
    val statLength: String
    val statPitch: String
    val statMelody: String
    val statCalls: String
    fun seconds(value: String): String
    fun couldAlsoBe(type: String, percent: String): String
    val moodUnhappy: String
    val moodHappy: String
    val moodCalm: String
    val moodExcited: String
    fun pitchCaption(seconds: String): String

    // History
    val historyTitle: String
    val historyEmpty: String
    val historyHint: String
    fun youSaid(intent: String): String
    val play: String
    val delete: String

    // My cat
    val myCatTitle: String
    val languageTitle: String
    val languageAutomatic: String
    val profileTitle: String
    val name: String
    val age: String
    val neutered: String
    val yes: String
    val no: String
    val notSure: String
    val profileHint: String
    val learnedTitle: String
    fun nothingLearnedYet(catName: String): String
    fun learnedSummary(count: Int, influence: Double): String
    fun whyTeach(examples: Int, from: Double, to: Double): String
    val forget: String
    val forgetTitle: String
    val forgetText: String
    val forgetConfirm: String
    val cancel: String

    // How it works
    val scienceTitle: String
    val canTranslateTitle: String
    val canTranslateBody: String
    val pipelineTitle: String
    fun pipeline(calls: Int, cats: Int): List<String>
    val accuracyTitle: String
    val accuracyIntro: String
    fun accuracyCrossCat(balanced: Double): String
    fun accuracyWithinCat(balanced: Double): String
    fun accuracyAfterTeaching(examples: Int, balanced: Double): String
    fun accuracyMeowRecall(recall: Double): String
    val detectorTitle: String
    fun detectorNumbers(catRecall: Double, otherCats: Double, speech: Double, humanSounds: Double, noise: Double): List<String>
    val accuracyOutro: String
    val soundsTitle: String
    val creditsTitle: String
    val creditsDataset: String
    val footer: String

    fun message(m: UiMessage, detail: String?): String

    companion object {
        fun of(language: Language): UiStrings = when (language) {
            Language.ENGLISH -> EnglishUi
            Language.JAPANESE -> JapaneseUi
        }
    }
}

val LocalUi = staticCompositionLocalOf<UiStrings> { EnglishUi }
val LocalTexts = staticCompositionLocalOf<Texts> { EnglishTexts }

object EnglishUi : UiStrings {
    override val appTitle = "Meow Translator"
    override val tabTranslate = "Translate"
    override val tabHistory = "History"
    override val tabMyCat = "My cat"
    override val tabScience = "How it works"

    override fun subtitle(catName: String) = if (catName.isBlank()) "Listening to your cat" else "Listening to $catName"
    override val statusIdle = "Tap, then hold the phone near your cat"
    override val statusHeard = "Got it… stops automatically when your cat is done"
    override fun statusListening(seconds: Int) = "Listening… ${seconds}s"
    override val statusAnalyzing = "Translating…"
    override val importFile = "Translate an audio file"
    override val startListening = "Start listening"
    override val stopListening = "Stop listening"
    override val tipsTitle = "For the best translation"
    override val tips = listOf(
        "Hold the phone 20–50 cm from your cat, in a quiet room.",
        "Start listening before your cat meows; it stops on its own.",
        "Tell the app what was really happening after each meow. It learns your cat's personal \"dialect\" and gets noticeably more accurate.",
    )
    override val disclaimer =
        "Translations are educated interpretations based on cat-vocalization research and real recorded meows, not literal words."

    override fun says(speaker: String) = "$speaker says:"
    override fun confidenceChip(level: String, percent: String) = "$level confidence · $percent"
    override val playRecording = "Play recording"
    override val stop = "Stop"
    override val sectionMood = "Mood"
    override val sectionOtherMeanings = "Other possible meanings"
    override val sectionSound = "The sound"
    override val sectionWhy = "Why this translation"
    override val sectionGoodToKnow = "Good to know"
    override val closestMatch = "Closest match among real recorded meows:"
    override val teachTitle = "Was this right?"
    override val teachHint = "Tap what was really going on. Every answer teaches the app your cat's own way of meowing."
    override fun learnedSoFar(count: Int) = "Thanks! Learned from $count labelled meow${if (count == 1) "" else "s"} so far."
    override val notMyCat = "Not my cat"
    override val statLength = "Length"
    override val statPitch = "Pitch"
    override val statMelody = "Melody"
    override val statCalls = "Calls"
    override fun seconds(value: String) = "$value s"
    override fun couldAlsoBe(type: String, percent: String) = "Could also be a ${type.lowercase()} ($percent)."
    override val moodUnhappy = "Unhappy"
    override val moodHappy = "Happy"
    override val moodCalm = "Calm"
    override val moodExcited = "Excited"
    override fun pitchCaption(seconds: String) = "Melody (line) over loudness (shaded), $seconds s"

    override val historyTitle = "History"
    override val historyEmpty = "No translations yet.\nRecord your cat on the Translate tab and they'll appear here."
    override val historyHint = "Tap an entry to translate it again with everything the app has learned since."
    override fun youSaid(intent: String) = "You said: $intent"
    override val play = "Play"
    override val delete = "Delete"

    override val myCatTitle = "My cat"
    override val languageTitle = "Language / 言語"
    override val languageAutomatic = "Automatic (phone setting)"
    override val profileTitle = "Profile"
    override val name = "Name"
    override val age = "Age"
    override val neutered = "Spayed / neutered?"
    override val yes = "Yes"
    override val no = "No"
    override val notSure = "Not sure"
    override val profileHint =
        "Age and neuter status change how some calls are read: e.g. yowling means different things in intact, adult and senior cats."
    override val learnedTitle = "What the app has learned"
    override fun nothingLearnedYet(catName: String) =
        "Nothing yet. After each meow, tap what was really happening; the app will start learning " +
            "${if (catName.isBlank()) "your cat" else catName}'s personal way of meowing."
    override fun learnedSummary(count: Int, influence: Double) =
        "$count labelled meow${if (count == 1) "" else "s"}. Your examples now carry ${pct(influence)} of the weight when reading a meow."
    override fun whyTeach(examples: Int, from: Double, to: Double) =
        "Why it's worth it: meows are very individual. In a test on cats the app had never heard, teaching it $examples " +
            "labelled meows raised accuracy from ${pct(from)} to ${pct(to)} (chance is 33%)."
    override val forget = "Forget what I taught"
    override val forgetTitle = "Forget all teaching?"
    override val forgetText = "The app will go back to its general model. Your recordings stay in History."
    override val forgetConfirm = "Forget"
    override val cancel = "Cancel"

    override val scienceTitle = "How it works"
    override val canTranslateTitle = "Can a cat really be translated?"
    override val canTranslateBody =
        "Not word for word: cats don't have words. Adult cats rarely meow at each other; the meow is largely a signal " +
            "they've developed for getting human attention. But meows do change with the situation and the cat's " +
            "emotional state, in length, melody, loudness and voice quality, and attentive owners can learn to " +
            "read them. This app measures those same cues and turns them into the most likely meaning, phrased " +
            "the way your cat might say it."
    override val pipelineTitle = "What happens when you tap the mic"
    override fun pipeline(calls: Int, cats: Int) = listOf(
        "Find the calls: the recording is split into separate sounds, measured against the room's background noise.",
        "Filter out people and noise: a detector trained on cats, human speech and everyday sounds keeps only the sounds your cat made.",
        "Measure them: pitch melody (YIN pitch analysis with Viterbi smoothing), length, loudness shape, voice roughness, spectral shape (MFCCs) and rhythmic pulsing.",
        "Identify the call type: meow, trill, purr, hiss, growl, yowl or chatter, using acoustic descriptions from the research literature.",
        "Read the meow: a model trained on $calls real meows from $cats cats recorded while waiting for food, being brushed, or alone in an unfamiliar room, combined with melody cues (rising pitch tends to mean positive or requesting, falling pitch stress).",
        "Adapt to your cat: meows you label are remembered, and similar future meows are read the way you taught.",
        "Everything runs on your phone. Recordings never leave the device.",
    )
    override val accuracyTitle = "How accurate is it?"
    override val accuracyIntro = "Honest numbers, measured on cats the model had never heard (leave-one-cat-out testing):"
    override fun accuracyCrossCat(balanced: Double) =
        "Telling food, brushing and isolation meows apart: ${pct(balanced)} balanced accuracy vs 33% by chance."
    override fun accuracyWithinCat(balanced: Double) = "When the model has already heard other meows from the same cat: ${pct(balanced)}."
    override fun accuracyAfterTeaching(examples: Int, balanced: Double) = "After you teach it $examples meows: ${pct(balanced)}."
    override fun accuracyMeowRecall(recall: Double) = "Call-type detection recognises ${pct(recall)} of the dataset's meows as meows."
    override val detectorTitle = "Ignoring people and noise"
    override fun detectorNumbers(catRecall: Double, otherCats: Double, speech: Double, humanSounds: Double, noise: Double) = listOf(
        "Before translating, a detector decides whether each sound is your cat, a person, or something else. It was trained on real meows, " +
            "600 recordings of people talking, and 2,000 everyday sounds, each also replayed with simulated room echo and noise.",
        "On recordings it had never heard: ${pct(speech)} of people talking and ${pct(noise)} of household and outdoor sounds are correctly ignored, " +
            "while ${pct(catRecall)} of meows (and ${pct(otherCats)} of cat recordings made with completely different equipment) still get through.",
        "Hardest to tell apart: other human sounds such as laughing or a crying baby (${pct(humanSounds)} ignored) — a baby's cry really is cat-like.",
        "If something that isn't your cat slips through, tap \"Not my cat\" and it's removed without being learned from.",
    )
    override val accuracyOutro =
        "That is roughly where science is today. Meows are highly individual, and in a listening study on these same " +
            "recordings people also found the situations hard to tell apart. Teaching the app about your own cat is " +
            "the single biggest accuracy boost."
    override val soundsTitle = "The sounds cats make"
    override val creditsTitle = "Credits & sources"
    override val creditsDataset =
        "Meow model trained on the CatMeows dataset by the University of Milan (CC BY-NC 4.0, non-commercial use); " +
            "cat/person/noise detector also uses ESC-50 (CC BY-NC 3.0) and LibriSpeech (CC BY 4.0)."
    override val footer = "For fun and insight. Not a veterinary tool: if your cat's vocalising changes suddenly, talk to a vet."

    override fun message(m: UiMessage, detail: String?) = when (m) {
        UiMessage.MIC_PERMISSION -> "Microphone access is needed to hear your cat."
        UiMessage.MIC_UNAVAILABLE -> "The microphone is unavailable or in use by another app."
        UiMessage.RECORDING_FAILED -> "Recording failed" + (detail?.let { ": $it" } ?: ".")
        UiMessage.FILE_UNREADABLE -> "Couldn't read that file" + (detail?.let { ": $it" } ?: ".")
        UiMessage.RECORDING_MISSING -> "That recording is no longer available."
        UiMessage.REMOVED_NOT_CAT -> "Removed. It won't be used for learning."
    }
}

object JapaneseUi : UiStrings {
    override val appTitle = "ねこ語翻訳"
    override val tabTranslate = "翻訳"
    override val tabHistory = "履歴"
    override val tabMyCat = "うちの子"
    override val tabScience = "しくみ"

    override fun subtitle(catName: String) = if (catName.isBlank()) "猫の気持ちを翻訳します" else "${catName}の気持ちを翻訳します"
    override val statusIdle = "タップして、スマホを猫に近づけてください"
    override val statusHeard = "聞こえました… 鳴き終わると自動で止まります"
    override fun statusListening(seconds: Int) = "聞いています… ${seconds}秒"
    override val statusAnalyzing = "翻訳中…"
    override val importFile = "音声ファイルを翻訳"
    override val startListening = "聞き取りを開始"
    override val stopListening = "聞き取りを停止"
    override val tipsTitle = "上手に翻訳するコツ"
    override val tips = listOf(
        "静かな部屋で、スマホを猫から20〜50cmくらいの距離に構えてください。",
        "猫が鳴く前に聞き取りを始めてください。鳴き終わると自動で止まります。",
        "鳴き声のあとに「実際はどうだったか」を教えてください。うちの子ならではの「方言」を学習して、どんどん正確になります。",
    )
    override val disclaimer = "翻訳は、猫の鳴き声の研究と実際に録音された鳴き声にもとづく推定で、文字どおりの言葉ではありません。"

    override fun says(speaker: String) = "${speaker}の気持ち："
    override fun confidenceChip(level: String, percent: String) = "信頼度：$level · $percent"
    override val playRecording = "録音を再生"
    override val stop = "停止"
    override val sectionMood = "気分"
    override val sectionOtherMeanings = "ほかに考えられる意味"
    override val sectionSound = "鳴き声の分析"
    override val sectionWhy = "この翻訳の根拠"
    override val sectionGoodToKnow = "知っておきたいこと"
    override val closestMatch = "実際に録音された鳴き声との近さ："
    override val teachTitle = "合っていましたか？"
    override val teachHint = "実際の状況をタップしてください。答えるたびに、うちの子の鳴き方を学習します。"
    override fun learnedSoFar(count: Int) = "ありがとう！これまでに${count}件の鳴き声から学習しました。"
    override val notMyCat = "うちの子の声じゃない"
    override val statLength = "長さ"
    override val statPitch = "音の高さ"
    override val statMelody = "メロディ"
    override val statCalls = "回数"
    override fun seconds(value: String) = "${value}秒"
    override fun couldAlsoBe(type: String, percent: String) = "「$type」の可能性もあります（$percent）。"
    override val moodUnhappy = "いや"
    override val moodHappy = "うれしい"
    override val moodCalm = "穏やか"
    override val moodExcited = "興奮"
    override fun pitchCaption(seconds: String) = "メロディ（線）と音の大きさ（影）、${seconds}秒"

    override val historyTitle = "履歴"
    override val historyEmpty = "まだ翻訳はありません。\n「翻訳」タブで猫の声を録音すると、ここに表示されます。"
    override val historyHint = "タップすると、その後に学習した内容も使って、もう一度翻訳します。"
    override fun youSaid(intent: String) = "あなたの回答：$intent"
    override val play = "再生"
    override val delete = "削除"

    override val myCatTitle = "うちの子"
    override val languageTitle = "言語 / Language"
    override val languageAutomatic = "自動（端末の設定）"
    override val profileTitle = "プロフィール"
    override val name = "名前"
    override val age = "年齢"
    override val neutered = "避妊・去勢手術"
    override val yes = "済み"
    override val no = "していない"
    override val notSure = "わからない"
    override val profileHint = "年齢と避妊・去勢の有無によって、一部の鳴き声の読み取り方が変わります（例：長鳴きの意味は、未手術の猫・成猫・シニア猫で異なります）。"
    override val learnedTitle = "学習した内容"
    override fun nothingLearnedYet(catName: String) =
        "まだありません。鳴き声のあとに実際の状況をタップすると、${catName.ifBlank { "うちの子" }}ならではの鳴き方を学習し始めます。"
    override fun learnedSummary(count: Int, influence: Double) =
        "${count}件の鳴き声を学習済み。鳴き声を読み取るとき、あなたが教えた例が${pct(influence)}の重みを持っています。"
    override fun whyTeach(examples: Int, from: Double, to: Double) =
        "教える価値がある理由：鳴き声は猫によって大きく違います。初めて聞く猫でのテストでは、${examples}件の鳴き声を教えると、" +
            "正解率が${pct(from)}から${pct(to)}に上がりました（偶然の正解率は33%）。"
    override val forget = "学習内容をリセット"
    override val forgetTitle = "学習内容をすべてリセットしますか？"
    override val forgetText = "一般的なモデルに戻ります。録音は履歴に残ります。"
    override val forgetConfirm = "リセット"
    override val cancel = "キャンセル"

    override val scienceTitle = "しくみ"
    override val canTranslateTitle = "猫の言葉は本当に翻訳できるの？"
    override val canTranslateBody =
        "言葉どおりの翻訳はできません。猫は言葉を持っていないからです。大人の猫どうしはほとんど「ニャー」と鳴き合わず、" +
            "ニャーは主に人間の気を引くために発達した合図です。それでも鳴き声は、状況や気持ちによって長さ・メロディ・" +
            "大きさ・声質が変わり、よく観察している飼い主はそれを読み取れるようになります。このアプリは同じ手がかりを測定し、" +
            "もっともありそうな意味を、猫が言いそうな言葉で表します。"
    override val pipelineTitle = "マイクをタップすると何が起きる？"
    override fun pipeline(calls: Int, cats: Int) = listOf(
        "鳴き声を見つける：録音を部屋の雑音と比べて、一つひとつの鳴き声に分けます。",
        "人の声や物音を除外：猫・人の話し声・日常の音で学習した判定器が、猫の声だけを残します。",
        "測定する：音の高さのメロディ（YINによるピッチ解析とビタビ平滑化）、長さ、音量の変化、声のかすれ、スペクトルの形（MFCC）、規則的な振動。",
        "鳴き声の種類を判定：ニャー、トリル、ゴロゴロ、シャー、うなり声、長鳴き、クラッキングを、研究論文にある音響的な特徴にもとづいて判定します。",
        "ニャーを読み取る：ごはん待ち・ブラッシング中・知らない部屋でひとり、という状況で録音された${cats}匹の猫の実際の鳴き声${calls}件で学習したモデルと、" +
            "メロディの手がかり（語尾が上がるとポジティブ・おねだり、下がるとストレスの傾向）を組み合わせます。",
        "うちの子に合わせる：あなたが教えた鳴き声を記憶し、似た鳴き声を教えられたとおりに読み取ります。",
        "すべてスマホの中で処理されます。録音が端末の外に出ることはありません。",
    )
    override val accuracyTitle = "どのくらい正確？"
    override val accuracyIntro = "正直な数字です。モデルが一度も聞いたことのない猫でテストしました（1匹ずつ除外する交差検証）："
    override fun accuracyCrossCat(balanced: Double) =
        "ごはん待ち・ブラッシング・ひとりぼっちの鳴き声の区別：バランス正解率 ${pct(balanced)}（偶然なら33%）。"
    override fun accuracyWithinCat(balanced: Double) = "同じ猫の別の鳴き声をすでに聞いている場合：${pct(balanced)}。"
    override fun accuracyAfterTeaching(examples: Int, balanced: Double) = "${examples}件の鳴き声を教えた後：${pct(balanced)}。"
    override fun accuracyMeowRecall(recall: Double) = "鳴き声の種類判定：データセットのニャーの${pct(recall)}を正しくニャーと認識。"
    override val detectorTitle = "人の声や物音を無視するしくみ"
    override fun detectorNumbers(catRecall: Double, otherCats: Double, speech: Double, humanSounds: Double, noise: Double) = listOf(
        "翻訳の前に、それぞれの音が「うちの子」「人」「それ以外」のどれかを判定します。実際の猫の鳴き声、人の話し声の録音600件、" +
            "日常の音2,000件で学習し、それぞれ部屋の反響や雑音を加えた音でも学習しています。",
        "一度も聞いたことのない録音でのテスト：人の話し声の${pct(speech)}、家の中や屋外の物音の${pct(noise)}を正しく無視し、" +
            "猫の鳴き声の${pct(catRecall)}（まったく別の機材で録音された猫の声では${pct(otherCats)}）は翻訳されます。",
        "いちばん区別が難しいのは、笑い声や赤ちゃんの泣き声などの人の声（${pct(humanSounds)}を無視）です。赤ちゃんの泣き声は本当に猫に似ています。",
        "猫以外の音が翻訳されてしまったら「うちの子の声じゃない」をタップしてください。学習には使われずに削除されます。",
    )
    override val accuracyOutro =
        "これが現在の科学の到達点です。鳴き声は猫ごとの個性が強く、同じ録音を使った聞き取り実験では、人間にとっても" +
            "状況の区別は難しいことがわかっています。うちの子について教えることが、正確さを上げるいちばんの近道です。"
    override val soundsTitle = "猫の鳴き声の種類"
    override val creditsTitle = "クレジットと出典"
    override val creditsDataset = "ニャーのモデルは、ミラノ大学のCatMeowsデータセット（CC BY-NC 4.0、非商用利用）で学習しています。" +
        "猫・人・物音の判定器には、ESC-50（CC BY-NC 3.0）とLibriSpeech（CC BY 4.0）も使用しています。"
    override val footer = "楽しみながら猫を理解するためのアプリです。獣医療の道具ではありません。鳴き方が急に変わったときは、動物病院に相談してください。"

    override fun message(m: UiMessage, detail: String?) = when (m) {
        UiMessage.MIC_PERMISSION -> "猫の声を聞くには、マイクへのアクセスが必要です。"
        UiMessage.MIC_UNAVAILABLE -> "マイクが使えないか、ほかのアプリが使用中です。"
        UiMessage.RECORDING_FAILED -> "録音に失敗しました。"
        UiMessage.FILE_UNREADABLE -> "そのファイルを読み込めませんでした。"
        UiMessage.RECORDING_MISSING -> "その録音はもう見つかりません。"
        UiMessage.REMOVED_NOT_CAT -> "削除しました。学習には使われません。"
    }
}
