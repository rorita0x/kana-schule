package moe.rorita.kanaschule.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import moe.rorita.kanaschule.srs.Unlock
import moe.rorita.kanaschule.store.Settings
import moe.rorita.kanaschule.store.ThemeMode
import moe.rorita.kanaschule.ui.drill.GroupInfo
import moe.rorita.kanaschule.ui.theme.LocalFeedbackColors
import moe.rorita.kanaschule.ui.theme.MetricTextStyle

@Composable
fun SettingsScreen(
    settings: Settings,
    groups: List<GroupInfo>,
    onChange: ((Settings) -> Settings) -> Unit,
    onUnlockThrough: (String) -> Unit,
    onLockFrom: (String) -> Unit,
    onClose: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "Einstellungen", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onClose) { Text("Fertig") }
        }
        HorizontalDivider()

        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = CONTENT_MAX.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                UnlockSection(
                    groups = groups,
                    onUnlockThrough = onUnlockThrough,
                    onLockFrom = onLockFrom,
                )

                Section("Lernen") {
                    ChoiceRow(
                        label = "Neue Zeichen pro Tag",
                        description = "Bremse gegen den Wiederholungsstau in drei Tagen.",
                        options = listOf(5, 10, 20, Unlock.NEW_ITEMS_UNLIMITED),
                        selected = settings.dailyNewLimit,
                        labelOf = { if (it >= Unlock.NEW_ITEMS_UNLIMITED) "alle" else "$it" },
                        onSelect = { value -> onChange { it.copy(dailyNewLimit = value) } },
                    )
                    ChoiceRow(
                        label = "Gruppen pro Tag",
                        description = "Wie viele Zeilen von selbst aufgehen dürfen.",
                        options = listOf(1, 2, 3, Unlock.GROUPS_PER_DAY_UNLIMITED),
                        selected = settings.groupsPerDay,
                        labelOf = {
                            if (it >= Unlock.GROUPS_PER_DAY_UNLIMITED) "ohne Grenze" else "$it"
                        },
                        onSelect = { value -> onChange { it.copy(groupsPerDay = value) } },
                    )
                    ChoiceRow(
                        label = "Fragen pro Runde",
                        description = null,
                        options = listOf(15, 20, 30, 40),
                        selected = settings.reviewSessionLength,
                        labelOf = { "$it" },
                        onSelect = { value -> onChange { it.copy(reviewSessionLength = value) } },
                    )
                }

                Section("Antworten") {
                    SwitchRow(
                        label = "Nur Hepburn als richtig zählen",
                        description = if (settings.strictHepburn) {
                            "„si“ für し gilt als Beinahe-Treffer und wird neu gefragt."
                        } else {
                            "Kunrei und Eingabe-Schreibweise zählen voll, mit Hinweis."
                        },
                        checked = settings.strictHepburn,
                        onToggle = { onChange { it.copy(strictHepburn = !it.strictHepburn) } },
                    )
                }

                Section("Ton") {
                    SwitchRow(
                        label = "Aussprache automatisch",
                        description = if (settings.muteAudio) {
                            "Aus - nur auf Knopfdruck."
                        } else {
                            "Spielt, sobald eine Lernkarte erscheint."
                        },
                        checked = !settings.muteAudio,
                        onToggle = { onChange { it.copy(muteAudio = !it.muteAudio) } },
                    )
                }

                Section("Darstellung") {
                    ChoiceRow(
                        label = "Design",
                        description = null,
                        options = ThemeMode.entries.toList(),
                        selected = settings.theme,
                        labelOf = {
                            when (it) {
                                ThemeMode.SYSTEM -> "System"
                                ThemeMode.LIGHT -> "Hell"
                                ThemeMode.DARK -> "Dunkel"
                            }
                        },
                        onSelect = { value -> onChange { it.copy(theme = value) } },
                    )
                    ChoiceRow(
                        label = "Bildschirmtastatur",
                        description = "Auf dem Handy nötig, am Rechner überflüssig.",
                        options = listOf(null, true, false),
                        selected = settings.onScreenKeyboard,
                        labelOf = {
                            when (it) {
                                null -> "automatisch"
                                true -> "immer"
                                false -> "nie"
                            }
                        },
                        onSelect = { value -> onChange { it.copy(onScreenKeyboard = value) } },
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------- Freischaltung

@Composable
private fun UnlockSection(
    groups: List<GroupInfo>,
    onUnlockThrough: (String) -> Unit,
    onLockFrom: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = LocalFeedbackColors.current
    val unlockedCount = groups.count { it.unlocked }
    val lastUnlocked = groups.lastOrNull { it.unlocked }

    Section("Freischaltung") {
        Text(
            text = "$unlockedCount von ${groups.size} Gruppen offen" +
                (lastUnlocked?.let { ", zuletzt ${it.labelDe}" } ?: ""),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Normalerweise geht eine Gruppe erst auf, wenn die vorige sitzt. " +
                "Wer die Zeichen schon kennt, kann hier vorspulen.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth().height(46.dp),
        ) {
            Text(if (expanded) "Liste zuklappen" else "Gruppen wählen")
        }

        if (expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                groups.forEach { group ->
                    GroupRow(
                        group = group,
                        correctColor = colors.correct,
                        onUnlock = { onUnlockThrough(group.id) },
                        onLock = { onLockFrom(group.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupRow(
    group: GroupInfo,
    correctColor: androidx.compose.ui.graphics.Color,
    onUnlock: () -> Unit,
    onLock: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (group.unlocked) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.surface
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = group.labelDe, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "${group.scriptDe} · ${group.itemCount} Zeichen" +
                        if (group.unlocked) " · ${group.seenCount} gesehen" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (group.mastered) {
                Text(
                    text = "sitzt",
                    style = MetricTextStyle,
                    color = correctColor,
                    modifier = Modifier.padding(end = 10.dp),
                )
            }
            if (group.unlocked) {
                TextButton(onClick = onLock) { Text("sperren") }
            } else {
                Button(onClick = onUnlock) { Text("bis hier") }
            }
        }
    }
}

// ------------------------------------------------------------------ Bausteine

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content()
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    description: String?,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun <T> ChoiceRow(
    label: String,
    description: String?,
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = { Text(labelOf(option)) },
                )
            }
        }
    }
}

private const val CONTENT_MAX = 520
