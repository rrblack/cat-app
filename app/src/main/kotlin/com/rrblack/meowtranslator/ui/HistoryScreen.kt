package com.rrblack.meowtranslator.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rrblack.meowtranslator.UiState
import com.rrblack.meowtranslator.data.HistoryEntry
import com.rrblack.meowtranslator.ui.i18n.LocalTexts
import com.rrblack.meowtranslator.ui.i18n.LocalUi
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    state: UiState,
    onOpen: (HistoryEntry) -> Unit,
    onPlay: (String) -> Unit,
    onDelete: (HistoryEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ui = LocalUi.current
    val texts = LocalTexts.current
    val dateFormat = remember(texts.language) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.forLanguageTag(texts.language.tag))
    }
    if (state.history.isEmpty()) {
        Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                ui.historyEmpty,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(ui.historyTitle, style = MaterialTheme.typography.headlineSmall)
            Text(
                ui.historyHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(state.history, key = { it.id }) { entry ->
            ElevatedCard(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(entry) },
            ) {
                Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            dateFormat.format(Date(entry.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(texts.headline(entry.headline), style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${entry.intent.emoji} ${texts.intentTitle(entry.intent)} · ${texts.vocalTypeName(entry.vocalType)} · ${pct(entry.confidence)}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        entry.userLabel?.let {
                            Text(
                                ui.youSaid("${it.emoji} ${texts.intentTitle(it)}"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    IconButton(onClick = { onPlay(entry.id) }) {
                        Icon(
                            if (state.playingId == entry.id) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                            contentDescription = ui.play,
                        )
                    }
                    IconButton(onClick = { onDelete(entry) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = ui.delete)
                    }
                }
            }
        }
    }
}
