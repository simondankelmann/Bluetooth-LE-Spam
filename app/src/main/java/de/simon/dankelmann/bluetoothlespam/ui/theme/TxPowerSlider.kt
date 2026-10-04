package de.simon.dankelmann.bluetoothlespam.ui.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.simon.dankelmann.bluetoothlespam.Enums.TxPowerLevel
import de.simon.dankelmann.bluetoothlespam.Enums.toStringId

private val txPowerLevels = listOf(
    TxPowerLevel.TX_POWER_ULTRA_LOW,
    TxPowerLevel.TX_POWER_LOW,
    TxPowerLevel.TX_POWER_MEDIUM,
    TxPowerLevel.TX_POWER_HIGH,
)

/** MD3 replacement for the classic `SeekBar` in `dialog_set_tx_power.xml` (plan §5 TX power dialog). */
@Composable
fun TxPowerSlider(
    initialLevel: TxPowerLevel,
    onLevelChanged: (TxPowerLevel) -> Unit,
) {
    var index by remember { mutableIntStateOf(txPowerLevels.indexOf(initialLevel).coerceAtLeast(0)) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(txPowerLevels[index].toStringId()),
            style = MaterialTheme.typography.bodyLarge,
        )
        Slider(
            value = index.toFloat(),
            valueRange = 0f..(txPowerLevels.size - 1).toFloat(),
            steps = txPowerLevels.size - 2,
            onValueChange = { newValue ->
                val newIndex = newValue.toInt()
                if (newIndex != index) {
                    index = newIndex
                    onLevelChanged(txPowerLevels[newIndex])
                }
            },
        )
    }
}
