package de.simon.dankelmann.bluetoothlespam.Enums

import de.simon.dankelmann.bluetoothlespam.R


enum class SpamPackageType {
    UNKNOWN,
    FAST_PAIRING,
    CONTINUITY_NEW_AIRTAG,
    CONTINUITY_NEW_DEVICE,
    CONTINUITY_NOT_YOUR_DEVICE,
    CONTINUITY_ACTION_MODAL,
    CONTINUITY_IOS_17_CRASH,
    SWIFT_PAIRING,
    EASY_SETUP_WATCH,
    EASY_SETUP_BUDS,
    LOVESPOUSE_PLAY,
    LOVESPOUSE_STOP
}

fun SpamPackageType.stringRes(): Int {
    return when (this) {
        SpamPackageType.UNKNOWN -> R.string.spam_unknown

        SpamPackageType.FAST_PAIRING -> R.string.ad_set_type_fast_pairing
        SpamPackageType.CONTINUITY_NEW_AIRTAG -> R.string.ad_set_type_continuity_new_airtag
        SpamPackageType.CONTINUITY_NEW_DEVICE -> R.string.ad_set_type_continuity_new_device
        SpamPackageType.CONTINUITY_NOT_YOUR_DEVICE -> R.string.ad_set_type_continuity_not_your_device
        SpamPackageType.CONTINUITY_ACTION_MODAL -> R.string.ad_set_type_continuity_action_modals
        SpamPackageType.CONTINUITY_IOS_17_CRASH -> R.string.ad_set_type_continuity_ios17_crash
        SpamPackageType.SWIFT_PAIRING -> R.string.ad_set_type_swift_pairing
        SpamPackageType.EASY_SETUP_WATCH -> R.string.ad_set_type_easy_setup_watch
        SpamPackageType.EASY_SETUP_BUDS -> R.string.ad_set_type_easy_setup_buds
        SpamPackageType.LOVESPOUSE_PLAY -> R.string.ad_set_type_lovespouse_play
        SpamPackageType.LOVESPOUSE_STOP -> R.string.ad_set_type_lovespouse_stop
    }
}