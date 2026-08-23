package moe.rorita.kanaschule.ui.nav

import androidx.compose.runtime.Composable

/**
 * Die Rückwärtstaste des Systems, soweit es eine gibt.
 *
 * Auf Android soll sie eine Ebene zurückgehen statt die App zu beenden; ist
 * [enabled] falsch, greift die Voreinstellung und die App verschwindet. Auf
 * dem Desktop gibt es keine solche Taste - dort tut Escape dieselbe Arbeit.
 */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
