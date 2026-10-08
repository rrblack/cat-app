package com.rrblack.meowtranslator.ui

import android.text.format.DateUtils
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rrblack.meowtranslator.UiState
import com.rrblack.meowtranslator.data.HistoryEntry

@Composable
fun HistoryScreen(
    state: UiState,
    onOpen: (HistoryEntry) -> Unit,
    onPlay: (String) -> Unit,
    onDelete: (HistoryEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.history.isEmpty()) {
        Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                "No translations yet.\nRecord your cat on the Translate tab and they'll appear here.",
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
            Text("History", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Tap an entry to translate it again with everything the app has learned since.",
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
                            DateUtils.getRelativeTimeSpanString(entry.timestamp).toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text("“${entry.headline}”", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${entry.intent.emoji} ${entry.intent.title} · ${entry.vocalType.displayName} · ${pct(entry.confidence)}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        entry.userLabel?.let {
                            Text(
                                "You said: ${it.emoji} ${it.title}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    IconButton(onClick = { onPlay(entry.id) }) {
                        Icon(
                            if (state.playingId == entry.id) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                            contentDescription = "Play",
                        )
                    }
                    IconButton(onClick = { onDelete(entry) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete")
                    }
                }
            }
        }
    }
}
