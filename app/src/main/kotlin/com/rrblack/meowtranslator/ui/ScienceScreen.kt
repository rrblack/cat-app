package com.rrblack.meowtranslator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rrblack.meowtranslator.core.classify.ContextModel
import com.rrblack.meowtranslator.core.classify.VocalType

private val REFERENCES = listOf(
    "Ludovico L.A., Ntalampiras S., Presti G., Cannas S., Battini M., Mattiello S. (2021). CatMeows: a publicly-available dataset of cat vocalizations. MultiMedia Modeling (MMM 2021). Data: doi:10.5281/zenodo.4008297",
    "Ntalampiras S., Ludovico L.A., Presti G., Prato-Previde E., Battini M., Cannas S., Palestrini C., Mattiello S. (2019). Automatic classification of cat vocalizations emitted in different contexts. Animals 9(8):543.",
    "Prato-Previde E., Cannas S., Palestrini C., Ingraffia S., Battini M., Ludovico L.A., Ntalampiras S., Presti G., Mattiello S. (2020). What's in a meow? A study on human classification and interpretation of domestic cat vocalizations. Animals 10(12):2390.",
    "Schötz S., van de Weijer J. (2014). A study of human perception of intonation in domestic cat meows. Speech Prosody 7, Dublin.",
    "Schötz S., van de Weijer J., Eklund R. (2019). Melody matters: an acoustic study of domestic cat meows in six contexts and four mental states. PeerJ Preprints 7:e27926.",
    "Nicastro N., Owren M.J. (2003). Classification of domestic cat (Felis catus) vocalizations by naive and experienced human listeners. Journal of Comparative Psychology 117(1):44–52.",
    "Nicastro N. (2004). Perceptual and acoustic evidence for species-level differences in meow vocalizations by domestic cats and African wild cats. Journal of Comparative Psychology 118(3):287–296.",
    "McComb K., Taylor A.M., Wilson C., Charlton B.D. (2009). The cry embedded within the purr. Current Biology 19(13):R507–R508.",
    "Moelk M. (1944). Vocalizing in the house-cat; a phonetic and functional study. American Journal of Psychology 57:184–205.",
    "Bradshaw J., Cameron-Beaumont C. (2000). The signalling repertoire of the domestic cat and its undomesticated relatives. In: The Domestic Cat: The Biology of its Behaviour (2nd ed.).",
    "Tavernier C., Ahmed S., Houpt K.A., Yeon S.C. (2020). Feline vocal communication. Journal of Veterinary Science 21(1):e18.",
    "Sissom D.E.F., Rice D.A., Peters G. (1991). How cats purr. Journal of Zoology 223:67–78.",
    "de Cheveigné A., Kawahara H. (2002). YIN, a fundamental frequency estimator for speech and music. JASA 111(4):1917–1930.",
)

@Composable
fun ScienceScreen(modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("How it works", style = MaterialTheme.typography.headlineSmall)

        Section("Can a cat really be translated?") {
            Text(
                "Not word for word: cats don't have words. Adult cats rarely meow at each other; the meow is largely a signal " +
                    "they've developed for getting human attention. But meows do change with the situation and the cat's " +
                    "emotional state, in length, melody, loudness and voice quality, and attentive owners can learn to " +
                    "read them. This app measures those same cues and turns them into the most likely meaning, phrased " +
                    "the way your cat might say it.",
            )
        }

        Section("What happens when you tap the mic") {
            Bullet("Find the calls: the recording is split into separate sounds, measured against the room's background noise.")
            Bullet("Measure them: pitch melody (YIN pitch analysis with Viterbi smoothing), length, loudness shape, voice roughness, spectral shape (MFCCs) and rhythmic pulsing.")
            Bullet("Identify the call type: meow, trill, purr, hiss, growl, yowl or chatter, using acoustic descriptions from the research literature.")
            Bullet("Read the meow: a model trained on ${ContextModel.trainingCalls} real meows from ${ContextModel.trainingCats} cats recorded while waiting for food, being brushed, or alone in an unfamiliar room, combined with melody cues (rising pitch tends to mean positive or requesting, falling pitch stress).")
            Bullet("Adapt to your cat: meows you label are remembered, and similar future meows are read the way you taught.")
            Bullet("Everything runs on your phone. Recordings never leave the device.")
        }

        Section("How accurate is it?") {
            Text(
                "Honest numbers, measured on cats the model had never heard (leave-one-cat-out testing):",
                style = MaterialTheme.typography.bodyMedium,
            )
            Bullet("Telling food, brushing and isolation meows apart: ${pct(ContextModel.crossCatBalancedAccuracy)} balanced accuracy vs 33% by chance.")
            Bullet("When the model has already heard other meows from the same cat: ${pct(ContextModel.withinCatBalancedAccuracy)}.")
            ContextModel.personalizationCurve.forEach { (n, acc) ->
                if (n > 0) Bullet("After you teach it $n meows: ${pct(acc)}.")
            }
            Bullet("Call-type detection recognises ${pct(MEOW_RECALL)} of the dataset's meows as meows.")
            Text(
                "That is roughly where science is today. Meows are highly individual, and in a listening study on these same " +
                    "recordings people also found the situations hard to tell apart. Teaching the app about your own cat is " +
                    "the single biggest accuracy boost.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Section("The sounds cats make") {
            VocalType.entries.filter { it != VocalType.UNKNOWN }.forEach {
                Text(it.displayName, fontWeight = FontWeight.SemiBold)
                Text(it.description, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Section("Credits & sources") {
            Text(
                "Meow model trained on the CatMeows dataset by the University of Milan (CC BY-NC 4.0, non-commercial use).",
                style = MaterialTheme.typography.bodyMedium,
            )
            REFERENCES.forEach { Bullet(it) }
        }

        Text(
            "For fun and insight. Not a veterinary tool: if your cat's vocalising changes suddenly, talk to a vet.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Share of the 440 CatMeows meows that the call-type rules label MEOW (trainer: CheckVocalTypes). */
private const val MEOW_RECALL = 431.0 / 440.0
