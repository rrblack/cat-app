package com.rrblack.meowtranslator.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rrblack.meowtranslator.MainViewModel

private enum class Tab(val label: String, val icon: ImageVector) {
    Translate("Translate", Icons.Outlined.Mic),
    History("History", Icons.Outlined.History),
    MyCat("My cat", Icons.Outlined.Pets),
    Science("How it works", Icons.AutoMirrored.Outlined.MenuBook),
}

@Composable
fun MeowApp(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(Tab.Translate) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = null) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (tab) {
            Tab.Translate -> TranslateScreen(state, viewModel, modifier)
            Tab.History -> HistoryScreen(
                state = state,
                onOpen = {
                    viewModel.openHistory(it)
                    tab = Tab.Translate
                },
                onPlay = viewModel::togglePlayback,
                onDelete = viewModel::deleteHistory,
                modifier = modifier,
            )
            Tab.MyCat -> MyCatScreen(state, viewModel, modifier)
            Tab.Science -> ScienceScreen(modifier)
        }
    }
}
