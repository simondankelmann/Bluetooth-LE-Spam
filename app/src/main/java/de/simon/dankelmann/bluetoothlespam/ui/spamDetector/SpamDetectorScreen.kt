package de.simon.dankelmann.bluetoothlespam.ui.spamDetector

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import de.simon.dankelmann.bluetoothlespam.Enums.FlipperDeviceType
import de.simon.dankelmann.bluetoothlespam.Enums.SpamPackageType
import de.simon.dankelmann.bluetoothlespam.Models.FlipperDeviceScanResult
import de.simon.dankelmann.bluetoothlespam.Models.SpamPackageScanResult
import de.simon.dankelmann.bluetoothlespam.R
import de.simon.dankelmann.bluetoothlespam.ui.theme.FloatingNavBarClearance

/**
 * Core migration of `SpamDetectorFragment`'s content (plan §5/§5a/§5b) — single LazyColumn with
 * two named item groups (detected devices, spam packages) replacing the RecyclerView split.
 */
@Composable
fun SpamDetectorScreen(
    isDetecting: Boolean,
    flipperDevices: List<FlipperDeviceScanResult>,
    spamPackages: List<SpamPackageScanResult>,
    onToggleClicked: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // Bottom padding includes FloatingNavBarClearance so the last card can scroll clear of
        // the floating nav bar pill instead of staying stuck underneath it.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp + FloatingNavBarClearance),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HeaderCard(isDetecting = isDetecting, onToggleClicked = onToggleClicked)
        }

        item {
            SectionHeader("Detected Devices", flipperDevices.size)
        }
        if (flipperDevices.isEmpty()) {
            item { EmptyListMessage(isDetecting = isDetecting) }
        } else {
            items(flipperDevices, key = { it.address }) { device ->
                FlipperDeviceRow(device)
            }
        }

        item {
            SectionHeader("Spam Packages", spamPackages.size)
        }
        if (spamPackages.isEmpty()) {
            item { EmptyListMessage(isDetecting = isDetecting) }
        } else {
            items(spamPackages, key = { it.address }) { spamPackage ->
                SpamPackageRow(spamPackage)
            }
        }
    }
}

@Composable
private fun HeaderCard(isDetecting: Boolean, onToggleClicked: () -> Unit) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.detect))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        isPlaying = isDetecting,
    )

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Spam Detector", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Detect Flippers and other BLE Spamming Devices",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                FilledIconButton(onClick = onToggleClicked) {
                    Icon(
                        imageVector = if (isDetecting) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isDetecting) "Stop scanning" else "Start scanning",
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Text(
        text = "$title ($count)",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
private fun EmptyListMessage(isDetecting: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (isDetecting) "No devices found yet" else "Start scanning to detect devices",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Composable
private fun FlipperDeviceRow(device: FlipperDeviceScanResult) {
    val deviceTypeLabel = when (device.flipperDeviceType) {
        FlipperDeviceType.FLIPPER_ZERO_WHITE -> "Flipper Zero White"
        FlipperDeviceType.FLIPPER_ZERO_BLACK -> "Flipper Zero Black"
        FlipperDeviceType.FLIPPER_ZERO_TRANSPARENT -> "Flipper Zero Transparent"
        FlipperDeviceType.UNKNOWN -> "Unknown Flipper Zero"
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = device.deviceName.ifEmpty { deviceTypeLabel }, style = MaterialTheme.typography.titleSmall)
            Text(text = device.address, style = MaterialTheme.typography.bodySmall)
            Text(text = "${device.rssi} dBm · $deviceTypeLabel", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SpamPackageRow(spamPackage: SpamPackageScanResult) {
    val label = when (spamPackage.spamPackageType) {
        SpamPackageType.UNKNOWN -> "Unknown Spam"
        SpamPackageType.FAST_PAIRING -> "Fast Pairing"
        SpamPackageType.CONTINUITY_NEW_AIRTAG -> "Continuity Airtag"
        SpamPackageType.CONTINUITY_NEW_DEVICE -> "Continuity new Device"
        SpamPackageType.CONTINUITY_NOT_YOUR_DEVICE -> "Continuity not your Device"
        SpamPackageType.CONTINUITY_ACTION_MODAL -> "Continuity Action Modal"
        SpamPackageType.CONTINUITY_IOS_17_CRASH -> "Continuity iOS 17 Crash"
        SpamPackageType.SWIFT_PAIRING -> "Swift Pairing"
        SpamPackageType.EASY_SETUP_WATCH -> "Easy Setup Watch"
        SpamPackageType.EASY_SETUP_BUDS -> "Easy Setup Buds"
        SpamPackageType.LOVESPOUSE_PLAY -> "Lovespouse Play"
        SpamPackageType.LOVESPOUSE_STOP -> "Lovespouse Stop"
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(spamPackage.spamPackageType.iconRes()),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(text = label, style = MaterialTheme.typography.titleSmall)
                Text(text = spamPackage.address, style = MaterialTheme.typography.bodySmall)
                Text(text = "${spamPackage.rssi} dBm", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun SpamPackageType.iconRes(): Int = when (this) {
    SpamPackageType.UNKNOWN -> R.drawable.bluetooth
    SpamPackageType.FAST_PAIRING -> R.drawable.ic_android
    SpamPackageType.CONTINUITY_NEW_AIRTAG -> R.drawable.apple
    SpamPackageType.CONTINUITY_NEW_DEVICE -> R.drawable.apple
    SpamPackageType.CONTINUITY_NOT_YOUR_DEVICE -> R.drawable.apple
    SpamPackageType.CONTINUITY_ACTION_MODAL -> R.drawable.apple
    SpamPackageType.CONTINUITY_IOS_17_CRASH -> R.drawable.apple
    SpamPackageType.SWIFT_PAIRING -> R.drawable.microsoft
    SpamPackageType.EASY_SETUP_WATCH -> R.drawable.samsung
    SpamPackageType.EASY_SETUP_BUDS -> R.drawable.samsung
    SpamPackageType.LOVESPOUSE_PLAY -> R.drawable.heart
    SpamPackageType.LOVESPOUSE_STOP -> R.drawable.heart
}
