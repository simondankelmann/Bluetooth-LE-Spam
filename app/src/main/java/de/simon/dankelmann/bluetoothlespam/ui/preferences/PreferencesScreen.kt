package de.simon.dankelmann.bluetoothlespam.ui.preferences

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.simon.dankelmann.bluetoothlespam.R
import de.simon.dankelmann.bluetoothlespam.ui.theme.FloatingNavBarClearance
import de.simon.dankelmann.bluetoothlespam.ui.theme.ThemeModeOption
import de.simon.dankelmann.bluetoothlespam.ui.theme.ThemeModePicker
import de.simon.dankelmann.bluetoothlespam.ui.theme.ThemeSwatchPicker

/**
 * Hand-rolled replacement for `PreferenceFragmentCompat` (plan §5/§11 — no Compose equivalent
 * of the classic Preference widget system exists, so every row is a plain composable).
 *
 * Theme mode/seed color/dynamic-color/blur are DataStore-backed (plan §11); use-legacy-advertising,
 * advertising interval, and logging stay on their existing stores (SharedPreferences /
 * `LogFileManager`'s own store respectively) — see [de.simon.dankelmann.bluetoothlespam.Datastore.SettingsRepository]'s
 * doc for why those weren't folded into DataStore too.
 */
@Composable
fun PreferencesScreen(
    themeMode: ThemeModeOption,
    onThemeModeSelected: (ThemeModeOption) -> Unit,
    seedColorArgb: Int,
    onSeedColorSelected: (Int) -> Unit,
    dynamicColorEnabled: Boolean,
    onDynamicColorEnabledChanged: (Boolean) -> Unit,
    blurEnabled: Boolean,
    onBlurEnabledChanged: (Boolean) -> Unit,
    allowCustomSwiftPairNames: Boolean,
    onAllowCustomSwiftPairNamesChanged: (Boolean) -> Unit,
    useLegacyAdvertising: Boolean,
    onUseLegacyAdvertisingChanged: (Boolean) -> Unit,
    advertisingIntervalMs: String,
    onAdvertisingIntervalChanged: (String) -> Unit,
    onTxPowerClicked: () -> Unit,
    loggingEnabled: Boolean,
    onLoggingEnabledChanged: (Boolean) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        // Bottom padding includes FloatingNavBarClearance so the last card can scroll clear of
        // the floating nav bar pill instead of staying stuck underneath it.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp + FloatingNavBarClearance),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { SectionHeader("Appearance") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeModePicker(selected = themeMode, onSelected = onThemeModeSelected)
                ThemeSwatchPicker(selectedSeedColor = seedColorArgb, onSeedColorSelected = onSeedColorSelected)
            }
        }
        item {
            SwitchRow(
                title = "Dynamic Color",
                summary = "Follow Material You colors derived from your wallpaper or picked swatch",
                checked = dynamicColorEnabled,
                onCheckedChange = onDynamicColorEnabledChanged,
            )
        }
        item {
            SwitchRow(
                title = "Blur Effects",
                summary = "Frosted-glass backdrop behind the floating navigation bar",
                checked = blurEnabled,
                onCheckedChange = onBlurEnabledChanged,
            )
        }

        item { SectionHeader("Advertising Settings") }
        item {
            SwitchRow(
                title = "Use Legacy Advertising",
                summary = "Recommended for maximum device compatibility",
                checked = useLegacyAdvertising,
                onCheckedChange = onUseLegacyAdvertisingChanged,
                iconRes = R.drawable.ic_settings_bluetooth,
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings_duration),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                    )
                    OutlinedTextField(
                        value = advertisingIntervalMs,
                        onValueChange = onAdvertisingIntervalChanged,
                        label = { Text("Advertisement Duration (ms)") },
                        supportingText = { Text("Recommended: 1000 - 10000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp),
                    )
                }
            }
        }
        item {
            ClickRow(title = "Set TX Power", onClick = onTxPowerClicked, iconRes = R.drawable.ic_tx_power)
        }

        item { SectionHeader("Swift Pair") }
        item {
            SwitchRow(
                title = "Allow Custom Swift Pair Names",
                summary = "Shows an edit button on each Swift Pair entry to set the advertised device name",
                checked = allowCustomSwiftPairNames,
                onCheckedChange = onAllowCustomSwiftPairNamesChanged,
            )
        }

        item { SectionHeader("Debug Settings") }
        item {
            SwitchRow(
                title = "Enable Logging",
                summary = "Write advertisement/scan activity to a log file",
                checked = loggingEnabled,
                onCheckedChange = onLoggingEnabledChanged,
                iconRes = R.drawable.ic_settings_logging,
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun SwitchRow(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    iconRes: Int? = null,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    // padding BEFORE size: chaining size().padding() would fix the outer box at
                    // 28dp and then let padding eat into that same box from the inside, shrinking
                    // the rendered icon to ~12dp -- this order applies the gap outside the icon.
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(28.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = summary, style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun ClickRow(title: String, onClick: () -> Unit, iconRes: Int? = null) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    // padding BEFORE size: chaining size().padding() would fix the outer box at
                    // 28dp and then let padding eat into that same box from the inside, shrinking
                    // the rendered icon to ~12dp -- this order applies the gap outside the icon.
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(28.dp),
                )
            }
            Text(text = title, style = MaterialTheme.typography.titleMedium)
        }
    }
}
