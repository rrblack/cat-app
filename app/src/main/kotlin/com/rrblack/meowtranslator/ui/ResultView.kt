@file:OptIn(ExperimentalLayoutApi::class)

package com.rrblack.meowtranslator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rrblack.meowtranslator.UiState
import com.rrblack.meowtranslator.core.i18n.Language
import com.rrblack.meowtranslator.core.i18n.Texts
import com.rrblack.meowtranslator.ui.i18n.LocalTexts
import com.rrblack.meowtranslator.ui.i18n.LocalUi
import com.rrblack.meowtranslator.core.classify.VocalType
import com.rrblack.meowtranslator.core.translate.Intent
import com.rrblack.meowtranslator.core.translate.Translation
import kotlin.math.roundToInt

@Composable
fun ResultView(translation: Translation, state: UiState, onTeach: (Intent) -> Unit, onPlay: () -> Unit) {
    val ui = LocalUi.current
    val texts = LocalTexts.current
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HeadlineCard(translation, state, onPlay)
        if (translation.qualityWarnings.isNotEmpty()) WarningCard(translation.qualityWarnings.map(texts::warning))
        if (translation.intent != Intent.UNCLEAR) {
            if (state.canTeach) TeachCard(state, onTeach)
            Section(ui.sectionMood) { EmotionMeter(translation.emotion) }
            Section(ui.sectionOtherMeanings) { IntentBars(translation) }
            Section(ui.sectionSound) { SoundDetails(translation) }
            Section(ui.sectionWhy) {
                translation.evidence.forEach { Bullet(texts.evidence(it)) }
            }
            if (translation.notes.isNotEmpty()) {
                Section(ui.sectionGoodToKnow) { translation.notes.forEach { Bullet(texts.note(it)) } }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun HeadlineCard(t: Translation, state: UiState, onPlay: () -> Unit) {
    val ui = LocalUi.current
    val texts = LocalTexts.current
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                ui.says(texts.speaker(state.profile.name)),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                if (texts.language == Language.JAPANESE) "「${texts.headline(t.headline)}」" else "“${texts.headline(t.headline)}”",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            if (t.intent != Intent.UNCLEAR) {
                Text(
                    "${t.intent.emoji}  ${texts.intentTitle(t.intent)} — ${texts.intentSummary(t.intent)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = { Text(ui.confidenceChip(texts.confidence(t.confidenceLevel), pct(t.confidence))) })
                    AssistChip(onClick = {}, label = { Text(texts.vocalTypeName(t.vocalType)) })
                }
            }
            val entry = state.resultEntry
            if (entry != null) {
                val playing = state.playingId == entry.id
                OutlinedButton(onClick = onPlay) {
                    Icon(if (playing) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (playing) ui.stop else ui.playRecording)
                }
            }
        }
    }
}

@Composable
private fun TeachCard(state: UiState, onTeach: (Intent) -> Unit) {
    val ui = LocalUi.current
    val texts = LocalTexts.current
    val label = state.resultEntry?.userLabel
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(ui.teachTitle, style = MaterialTheme.typography.titleMedium)
            Text(
                ui.teachHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Intent.MEOW_INTENTS.forEach { intent ->
                    FilterChip(
                        selected = label == intent,
                        onClick = { onTeach(intent) },
                        label = { Text("${intent.emoji} ${texts.intentTitle(intent)}") },
                    )
                }
            }
            if (label != null) {
                Text(
                    ui.learnedSoFar(state.examples.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun WarningCard(warnings: List<String>) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Row(Modifier.padding(16.dp)) {
            Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                warnings.forEach { Text(it, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}

@Composable
fun Section(title: String, content: @Composable () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
fun Bullet(text: String) {
    Row {
        Text("•", modifier = Modifier.padding(end = 8.dp), color = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun IntentBars(t: Translation) {
    val ui = LocalUi.current
    val texts = LocalTexts.current
    t.intents.take(4).forEach { ranked ->
        ProbabilityBar("${ranked.item.emoji} ${texts.intentTitle(ranked.item)}", ranked.probability)
    }
    t.contexts?.let { contexts ->
        Spacer(Modifier.height(4.dp))
        Text(
            ui.closestMatch,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        contexts.entries.sortedByDescending { it.value }.forEach { (ctx, p) ->
            ProbabilityBar(texts.context(ctx), p, color = MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
private fun SoundDetails(t: Translation) {
    val ui = LocalUi.current
    val texts = LocalTexts.current
    Text("${texts.vocalTypeName(t.vocalType)}: ${texts.vocalTypeDescription(t.vocalType)}", style = MaterialTheme.typography.bodyMedium)
    val v = t.primary ?: return
    val f = v.features
    if (v.pitch.size >= 3) {
        PitchChart(v, t.analysis.envelopeDb)
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Stat(ui.statLength, ui.seconds(Texts.oneDecimal(f.durationSec)))
        Stat(ui.statPitch, if (f.f0MedianHz > 0) "${f.f0MedianHz.roundToInt()} Hz" else "—")
        Stat(ui.statMelody, texts.contour(f.contourShape))
        Stat(ui.statCalls, t.callCount.toString())
    }
    if (t.vocalTypes.size > 1 && t.vocalType != VocalType.UNKNOWN) {
        val alt = t.vocalTypes.drop(1).firstOrNull { it.probability > 0.08 }
        if (alt != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(6.dp))
                Text(
                    ui.couldAlsoBe(texts.vocalTypeName(alt.item), pct(alt.probability)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

fun pct(p: Double) = Texts.pct(p)
