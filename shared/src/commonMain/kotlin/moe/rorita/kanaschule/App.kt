package moe.rorita.kanaschule

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.lifecycle.viewmodel.compose.viewModel
import moe.rorita.kanaschule.store.prefersOnScreenKeyboard
import moe.rorita.kanaschule.ui.drill.DrillScreen
import moe.rorita.kanaschule.ui.drill.KanaViewModel
import moe.rorita.kanaschule.ui.home.HomeScreen
import moe.rorita.kanaschule.ui.home.SummaryScreen
import moe.rorita.kanaschule.ui.keyboard.RomajiKeyLayout
import moe.rorita.kanaschule.ui.layout.LocalWindowClass
import moe.rorita.kanaschule.ui.layout.WindowClass
import moe.rorita.kanaschule.ui.learn.LearnScreen
import moe.rorita.kanaschule.ui.settings.SettingsScreen
import moe.rorita.kanaschule.ui.theme.KanaTheme
import moe.rorita.kanaschule.ui.keyboard.SystemKeyboardSink
import moe.rorita.kanaschule.ui.nav.PlatformBackHandler

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun App(viewModel: KanaViewModel = viewModel { KanaViewModel() }) {
    val state = viewModel.ui
    val focus = remember { FocusRequester() }

    // Eine Ebene zurück statt App beenden. Auf dem Startbildschirm gibt die
    // Taste ans System ab.
    PlatformBackHandler(enabled = !state.loading && !state.atHome) {
        viewModel.goBack()
    }

    val onScreenKeyboard = state.settings.onScreenKeyboard ?: prefersOnScreenKeyboard

    // Wer die eigene Tastatur abschaltet, will auf dem Handy die des Systems -
    // sonst gibt es überhaupt keine Möglichkeit zu tippen.
    val systemKeyboard = prefersOnScreenKeyboard && !onScreenKeyboard && state.kana != null

    KanaTheme(mode = viewModel.theme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            // Ohne das liegt der Inhalt unter Status- und Navigationsleiste,
            // denn die Activity läuft randlos. Die Fläche wird nach dem
            // Abzug gemessen, damit die Fensterklasse den nutzbaren Bereich
            // beschreibt.
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
            ) {
                val window = WindowClass(width = maxWidth, height = maxHeight)

                CompositionLocalProvider(LocalWindowClass provides window) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .focusRequester(focus)
                            .focusable()
                            .onPreviewKeyEvent { event -> handleKey(event, viewModel) },
                    ) {
                        when {
                            state.loading -> Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }

                            state.settingsOpen -> SettingsScreen(
                                settings = state.settings,
                                groups = state.groups,
                                newItemsUsedToday = state.newItemsUsedToday,
                                pendingResetGroupId = state.pendingResetGroupId,
                                onResetDailyBudget = viewModel::resetDailyBudget,
                                onChange = viewModel::updateSettings,
                                onUnlockThrough = viewModel::unlockThrough,
                                onLockFrom = viewModel::lockFrom,
                                onAskResetGroup = viewModel::askResetGroup,
                                onCancelResetGroup = viewModel::cancelResetGroup,
                                onResetGroup = viewModel::resetGroup,
                                onClose = viewModel::closeSettings,
                            )

                            state.result != null -> SummaryScreen(
                                result = state.result,
                                onDone = viewModel::leaveSummary,
                                onAgain = {
                                    viewModel.leaveSummary()
                                    viewModel.startSession()
                                },
                            )

                            state.learn != null -> LearnScreen(
                                state = state.learn,
                                onPlay = viewModel::playCurrentAudio,
                                onNext = viewModel::nextLearnCard,
                                onPrevious = viewModel::previousLearnCard,
                                onToggleMute = viewModel::toggleMute,
                                onToggleShowAll = viewModel::toggleShowAll,
                                onQuit = {
                                    if (state.learn.standalone) {
                                        viewModel.leaveLearning()
                                    } else {
                                        viewModel.abandonSession()
                                    }
                                },
                            )

                            state.kana != null -> DrillScreen(
                                state = state,
                                showKeyboard = onScreenKeyboard,
                                onKey = viewModel::type,
                                onBackspace = viewModel::backspace,
                                onSubmit = viewModel::submit,
                                onSkip = viewModel::skip,
                                onQuit = viewModel::abandonSession,
                                onAdvance = viewModel::advance,
                            )

                            else -> HomeScreen(
                                info = state.home,
                                notice = state.notice,
                                muted = state.muted,
                                onDrill = viewModel::startSession,
                                onLearn = { viewModel.startLearning() },
                                onToggleMute = viewModel::toggleMute,
                                onSettings = viewModel::openSettings,
                            )
                        }

                        SystemKeyboardSink(
                            active = systemKeyboard,
                            typed = state.typed,
                            onAnswer = viewModel::setAnswer,
                            onSubmit = viewModel::submit,
                        )
                    }
                }
            }
        }
    }

    // Der Fokus muss nach jedem Fragenwechsel zurück auf die Wurzel, sonst
    // verschluckt der Desktop die nächste Eingabe. Erst einen Frame abwarten:
    // vor der ersten Platzierung ist der Fokusknoten noch nicht bereit.
    LaunchedEffect(state.kana, state.learn, state.result, state.loading, systemKeyboard) {
        withFrameNanos { }
        // Ausser wenn die Tastatur des Systems dran ist: dann gehört der Fokus
        // der Eingabesenke, und ein Griff danach würde die Tastatur einklappen.
        if (!systemKeyboard) runCatching { focus.requestFocus() }
    }
}

/**
 * Physische Tastatur. Gefiltert auf die 22 Romaji-Buchstaben plus
 * Backspace, Enter, Leertaste und Escape - alles andere passiert nicht.
 */
@OptIn(ExperimentalComposeUiApi::class)
private fun handleKey(event: KeyEvent, viewModel: KanaViewModel): Boolean {
    if (event.type != KeyEventType.KeyDown) return false

    // Beim Vorstellen neuer Zeichen gibt es nichts zu tippen: Enter blättert
    // weiter, Leertaste spielt die Aussprache noch einmal.
    val learn = viewModel.ui.learn
    if (learn != null) {
        return when (event.key) {
            Key.Enter, Key.NumPadEnter, Key.DirectionRight -> {
                viewModel.nextLearnCard()
                true
            }

            Key.DirectionLeft -> {
                viewModel.previousLearnCard()
                true
            }

            Key.Spacebar -> {
                viewModel.playCurrentAudio()
                true
            }

            Key.Escape -> {
                if (learn.standalone) viewModel.leaveLearning() else viewModel.abandonSession()
                true
            }

            else -> false
        }
    }

    return when (event.key) {
        Key.Backspace -> {
            viewModel.backspace()
            true
        }

        Key.Enter, Key.NumPadEnter -> {
            viewModel.submit()
            true
        }

        Key.Spacebar -> {
            viewModel.skip()
            true
        }

        Key.Escape -> {
            viewModel.clearInput()
            true
        }

        else -> {
            val char = event.utf16CodePoint.toChar().lowercaseChar()
            if (RomajiKeyLayout.accepts(char)) {
                viewModel.type(char)
                true
            } else {
                false
            }
        }
    }
}
