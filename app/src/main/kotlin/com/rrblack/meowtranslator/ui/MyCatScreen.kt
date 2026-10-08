package com.rrblack.meowtranslator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.rrblack.meowtranslator.MainViewModel
import com.rrblack.meowtranslator.UiState
import com.rrblack.meowtranslator.core.classify.ContextModel
import com.rrblack.meowtranslator.core.classify.PersonalModel
import com.rrblack.meowtranslator.core.translate.AgeGroup
import com.rrblack.meowtranslator.core.translate.Intent
import com.rrblack.meowtranslator.core.i18n.Language
import com.rrblack.meowtranslator.data.AppLanguage
import com.rrblack.meowtranslator.ui.i18n.LocalTexts
import com.rrblack.meowtranslator.ui.i18n.LocalUi

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MyCatScreen(state: UiState, vm: MainViewModel, modifier: Modifier = Modifier) {
    val profile = state.profile
    val ui = LocalUi.current
    val texts = LocalTexts.current
    var confirmReset by remember { mutableStateOf(false) }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(ui.myCatTitle, style = MaterialTheme.typography.headlineSmall)

        Section(ui.languageTitle) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    AppLanguage.SYSTEM to ui.languageAutomatic,
                    AppLanguage.ENGLISH to Language.ENGLISH.nativeName,
                    AppLanguage.JAPANESE to Language.JAPANESE.nativeName,
                ).forEach { (choice, label) ->
                    FilterChip(selected = state.language == choice, onClick = { vm.setLanguage(choice) }, label = { Text(label) })
                }
            }
        }

        Section(ui.profileTitle) {
            OutlinedTextField(
                value = profile.name,
                onValueChange = { vm.updateProfile(profile.copy(name = it.take(30))) },
                label = { Text(ui.name) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(ui.age, style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AgeGroup.entries.forEach { age ->
                    FilterChip(
                        selected = profile.ageGroup == age,
                        onClick = { vm.updateProfile(profile.copy(ageGroup = age)) },
                        label = { Text(texts.ageGroup(age)) },
                    )
                }
            }
            Text(ui.neutered, style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(true to ui.yes, false to ui.no, null to ui.notSure).forEach { (value, label) ->
                    FilterChip(
                        selected = profile.neutered == value,
                        onClick = { vm.updateProfile(profile.copy(neutered = value)) },
                        label = { Text(label) },
                    )
                }
            }
            Text(
                ui.profileHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Section(ui.learnedTitle) {
            val examples = state.examples
            val influence = PersonalModel(examples).weight
            Text(
                if (examples.isEmpty()) ui.nothingLearnedYet(profile.name) else ui.learnedSummary(examples.size, influence),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (examples.isNotEmpty()) {
                val counts = Intent.MEOW_INTENTS.associateWith { i -> examples.count { it.intent == i } }
                val max = counts.values.max().coerceAtLeast(1)
                counts.forEach { (intent, n) ->
                    ProbabilityBar("${intent.emoji} ${texts.intentTitle(intent)} · $n", n.toDouble() / max, color = MaterialTheme.colorScheme.tertiary)
                }
            }
            val curve = ContextModel.personalizationCurve
            if (curve.size >= 2) {
                Text(
                    ui.whyTeach(curve.last().first, curve.first().second, curve.last().second),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (examples.isNotEmpty()) {
                OutlinedButton(onClick = { confirmReset = true }) { Text(ui.forget) }
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(ui.forgetTitle) },
            text = { Text(ui.forgetText) },
            confirmButton = {
                TextButton(onClick = {
                    vm.resetLearning()
                    confirmReset = false
                }) { Text(ui.forgetConfirm) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(ui.cancel) } },
        )
    }
}
