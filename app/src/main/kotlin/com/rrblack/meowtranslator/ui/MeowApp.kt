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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rrblack.meowtranslator.MainViewModel
import com.rrblack.meowtranslator.core.i18n.Language
import com.rrblack.meowtranslator.core.i18n.Texts
import com.rrblack.meowtranslator.data.AppLanguage
import com.rrblack.meowtranslator.ui.i18n.LocalTexts
import com.rrblack.meowtranslator.ui.i18n.LocalUi
import com.rrblack.meowtranslator.ui.i18n.UiStrings
import com.rrblack.meowtranslator.ui.theme.MeowTheme

private enum class Tab(val icon: ImageVector) {
    Translate(Icons.Outlined.Mic),
    History(Icons.Outlined.History),
    MyCat(Icons.Outlined.Pets),
    Science(Icons.AutoMirrored.Outlined.MenuBook),
}

/** The language actually shown: the user's choice, or the phone's (per-app) locale when set to automatic. */
@Composable
fun resolveLanguage(choice: AppLanguage): Language = when (choice) {
    AppLanguage.ENGLISH -> Language.ENGLISH
    AppLanguage.JAPANESE -> Language.JAPANESE
    AppLanguage.SYSTEM -> Language.forLanguageTag(LocalConfiguration.current.locales[0].toLanguageTag())
}

@Composable
fun MeowApp(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val language = resolveLanguage(state.language)
    val ui = UiStrings.of(language)
    var tab by rememberSaveable { mutableStateOf(Tab.Translate) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(ui.message(it.kind, it.detail))
            viewModel.dismissMessage()
        }
    }

    MeowTheme(language) {
        CompositionLocalProvider(LocalUi provides ui, LocalTexts provides Texts.of(language)) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    NavigationBar {
                        Tab.entries.forEach { t ->
                            val label = when (t) {
                                Tab.Translate -> ui.tabTranslate
                                Tab.History -> ui.tabHistory
                                Tab.MyCat -> ui.tabMyCat
                                Tab.Science -> ui.tabScience
                            }
                            NavigationBarItem(
                                selected = tab == t,
                                onClick = { tab = t },
                                icon = { Icon(t.icon, contentDescription = null) },
                                label = { Text(label) },
                            )
                        }
                    }
                },
            ) { padding ->
                val modifier = Modifier.padding(padding)
                when (tab) {
                    Tab.Translate -> TranslateScreen(state, viewModel, language, modifier)
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
    }
}
