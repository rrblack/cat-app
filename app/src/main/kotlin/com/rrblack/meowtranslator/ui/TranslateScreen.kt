package com.rrblack.meowtranslator.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.rrblack.meowtranslator.MainViewModel
import com.rrblack.meowtranslator.Phase
import com.rrblack.meowtranslator.UiMessage
import com.rrblack.meowtranslator.UiState
import com.rrblack.meowtranslator.core.i18n.Language
import com.rrblack.meowtranslator.data.AppLanguage
import com.rrblack.meowtranslator.ui.i18n.LocalUi

@Composable
fun TranslateScreen(state: UiState, vm: MainViewModel, language: Language, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val ui = LocalUi.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.startListening() else vm.showMessage(UiMessage.MIC_PERMISSION)
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(vm::importAudio)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth()) {
            // One-tap switch between the two languages, labelled in the language you'd switch *to*.
            TextButton(
                onClick = { vm.setLanguage(if (language == Language.JAPANESE) AppLanguage.ENGLISH else AppLanguage.JAPANESE) },
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                Icon(Icons.Outlined.Translate, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (language == Language.JAPANESE) Language.ENGLISH.nativeName else Language.JAPANESE.nativeName)
            }
        }
        Text(ui.appTitle, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            ui.subtitle(state.profile.name),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))

        MicButton(
            phase = state.phase,
            level = state.level,
            onClick = {
                when (state.phase) {
                    Phase.Idle -> {
                        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                            PackageManager.PERMISSION_GRANTED
                        if (granted) vm.startListening() else permission.launch(Manifest.permission.RECORD_AUDIO)
                    }
                    Phase.Listening -> vm.stopListening()
                    Phase.Analyzing -> Unit
                }
            },
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = when (state.phase) {
                Phase.Idle -> ui.statusIdle
                Phase.Listening -> if (state.heardSound) ui.statusHeard else ui.statusListening(state.elapsedSec.toInt())
                Phase.Analyzing -> ui.statusAnalyzing
            },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = { picker.launch("audio/*") }, enabled = state.phase == Phase.Idle) {
            Icon(Icons.Outlined.AudioFile, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(ui.importFile)
        }
        Spacer(Modifier.height(8.dp))

        val result = state.result
        if (result != null) {
            ResultView(
                translation = result,
                state = state,
                onTeach = vm::teach,
                onNotMyCat = vm::notMyCat,
                onPlay = { state.resultEntry?.let { vm.togglePlayback(it.id) } },
            )
        } else {
            TipsCard()
        }
    }
}

@Composable
private fun MicButton(phase: Phase, level: Float, onClick: () -> Unit) {
    val ui = LocalUi.current
    val animatedLevel by animateFloatAsState(level, animationSpec = tween(80), label = "level")
    val pulse = rememberInfiniteTransition(label = "pulse")
    val breathe by pulse.animateFloat(0.92f, 1.08f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "breathe")
    val listening = phase == Phase.Listening

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
        if (listening) {
            Box(
                Modifier
                    .size(200.dp)
                    .scale(0.6f + 0.4f * animatedLevel)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
            )
            Box(
                Modifier
                    .size(150.dp)
                    .scale(breathe)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
            )
        }
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier.size(120.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (listening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            ),
            enabled = phase != Phase.Analyzing,
        ) {
            if (phase == Phase.Analyzing) {
                CircularProgressIndicator(Modifier.size(48.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Icon(
                    if (listening) Icons.Rounded.Stop else Icons.Rounded.Mic,
                    contentDescription = if (listening) ui.stopListening else ui.startListening,
                    modifier = Modifier.size(56.dp),
                )
            }
        }
    }
}

@Composable
private fun TipsCard() {
    val ui = LocalUi.current
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(ui.tipsTitle, style = MaterialTheme.typography.titleMedium)
            ui.tips.forEach { Bullet(it) }
            Text(
                ui.disclaimer,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
