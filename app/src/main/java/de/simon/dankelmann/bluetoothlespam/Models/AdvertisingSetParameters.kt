package de.simon.dankelmann.bluetoothlespam.Models

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetParameters
import android.util.Log
import androidx.room.ColumnInfo
import de.simon.dankelmann.bluetoothlespam.Enums.PrimaryPhy
import de.simon.dankelmann.bluetoothlespam.Enums.SecondaryPhy
import de.simon.dankelmann.bluetoothlespam.Enums.TxPowerLevel
import java.io.Serializable

class AdvertisingSetParameters : Serializable {
    private var _logTag = "AdvertisingSetParametersModel"

    var id:Int = 0

    var legacyMode = true
    var interval = AdvertisingSetParameters.INTERVAL_MIN
    var txPowerLevel = TxPowerLevel.TX_POWER_HIGH
    var includeTxPowerLevel = false
    var primaryPhy: PrimaryPhy? = null
    var secondaryPhy: SecondaryPhy? = null
    var scanable = false
    var connectable = false
    var anonymous = false

    fun validate():Boolean{
        //@Todo: implement validation here
        return true
    }
    fun build(): AdvertisingSetParameters? {
        if(validate()){
            var params = AdvertisingSetParameters.Builder()

            params.setLegacyMode(legacyMode)
            params.setInterval(interval)

            when (txPowerLevel) {
                TxPowerLevel.TX_POWER_HIGH -> {
                    params.setTxPowerLevel(AdvertisingSetParameters.TX_POWER_HIGH)
                }
                TxPowerLevel.TX_POWER_LOW -> {
                    params.setTxPowerLevel(AdvertisingSetParameters.TX_POWER_LOW)
                }
                TxPowerLevel.TX_POWER_MEDIUM -> {
                    params.setTxPowerLevel(AdvertisingSetParameters.TX_POWER_MEDIUM)
                }
                TxPowerLevel.TX_POWER_ULTRA_LOW -> {
                    params.setTxPowerLevel(AdvertisingSetParameters.TX_POWER_ULTRA_LOW)
                }
            }

            params.setIncludeTxPower(includeTxPowerLevel)


            when(primaryPhy){
                PrimaryPhy.PHY_LE_1M -> {
                    params.setPrimaryPhy(BluetoothDevice.PHY_LE_1M)
                }
                PrimaryPhy.PHY_LE_CODED -> {
                    params.setPrimaryPhy(BluetoothDevice.PHY_LE_CODED)
                }
                else -> {
                    // Use System Default
                }
            }



            when(secondaryPhy){
                SecondaryPhy.PHY_LE_1M -> {
                    params.setSecondaryPhy(BluetoothDevice.PHY_LE_1M)
                }
                SecondaryPhy.PHY_LE_CODED -> {
                    params.setSecondaryPhy(BluetoothDevice.PHY_LE_CODED)
                }
                SecondaryPhy.PHY_LE_2M -> {
                    params.setSecondaryPhy(BluetoothDevice.PHY_LE_2M)
                }
                else -> {
                    // Use System Default
                }
            }

            params.setScannable(scanable)
            params.setConnectable(connectable)
            params.setAnonymous(anonymous)

            return params.build()
        } else {
            Log.d(_logTag, "AdvertisingSetParametersModel could not be built because its invalid")
        }
        return null
    }

}