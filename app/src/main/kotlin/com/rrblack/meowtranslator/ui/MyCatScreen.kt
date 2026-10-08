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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MyCatScreen(state: UiState, vm: MainViewModel, modifier: Modifier = Modifier) {
    val profile = state.profile
    var confirmReset by remember { mutableStateOf(false) }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("My cat", style = MaterialTheme.typography.headlineSmall)

        Section("Profile") {
            OutlinedTextField(
                value = profile.name,
                onValueChange = { vm.updateProfile(profile.copy(name = it.take(30))) },
                label = { Text("Name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Age", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AgeGroup.entries.forEach { age ->
                    FilterChip(
                        selected = profile.ageGroup == age,
                        onClick = { vm.updateProfile(profile.copy(ageGroup = age)) },
                        label = { Text(age.label) },
                    )
                }
            }
            Text("Spayed / neutered?", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(true to "Yes", false to "No", null to "Not sure").forEach { (value, label) ->
                    FilterChip(
                        selected = profile.neutered == value,
                        onClick = { vm.updateProfile(profile.copy(neutered = value)) },
                        label = { Text(label) },
                    )
                }
            }
            Text(
                "Age and neuter status change how some calls are read: e.g. yowling means different things in intact, adult and senior cats.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Section("What the app has learned") {
            val examples = state.examples
            val influence = PersonalModel(examples).weight
            Text(
                if (examples.isEmpty()) {
                    "Nothing yet. After each meow, tap what was really happening; the app will start learning ${if (profile.name.isBlank()) "your cat" else profile.name}'s personal way of meowing."
                } else {
                    "${examples.size} labelled meow${if (examples.size == 1) "" else "s"}. Your examples now carry ${pct(influence)} of the weight when reading a meow."
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            if (examples.isNotEmpty()) {
                val counts = Intent.MEOW_INTENTS.associateWith { i -> examples.count { it.intent == i } }
                val max = counts.values.max().coerceAtLeast(1)
                counts.forEach { (intent, n) ->
                    ProbabilityBar("${intent.emoji} ${intent.title} · $n", n.toDouble() / max, color = MaterialTheme.colorScheme.tertiary)
                }
            }
            val curve = ContextModel.personalizationCurve
            if (curve.size >= 2) {
                Text(
                    "Why it's worth it: meows are very individual. In a test on cats the app had never heard, teaching it " +
                        "${curve.last().first} labelled meows raised accuracy from ${pct(curve.first().second)} to ${pct(curve.last().second)} " +
                        "(chance is 33%).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (examples.isNotEmpty()) {
                OutlinedButton(onClick = { confirmReset = true }) { Text("Forget what I taught") }
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Forget all teaching?") },
            text = { Text("The app will go back to its general model. Your recordings stay in History.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.resetLearning()
                    confirmReset = false
                }) { Text("Forget") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }
}
