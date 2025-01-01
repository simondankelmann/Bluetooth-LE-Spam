package de.simon.dankelmann.bluetoothlespam.Enums

import de.simon.dankelmann.bluetoothlespam.R


enum class AdvertisementSetType {
    ADVERTISEMENT_TYPE_UNDEFINED,
    ADVERTISEMENT_TYPE_FAST_PAIRING_DEVICE,
    ADVERTISEMENT_TYPE_FAST_PAIRING_NON_PRODUCTION,
    ADVERTISEMENT_TYPE_FAST_PAIRING_PHONE_SETUP,
    ADVERTISEMENT_TYPE_FAST_PAIRING_DEBUG,
    ADVERTISEMENT_TYPE_CONTINUITY_NEW_AIRTAG,
    ADVERTISEMENT_TYPE_CONTINUITY_NEW_DEVICE,
    ADVERTISEMENT_TYPE_CONTINUITY_NOT_YOUR_DEVICE,
    ADVERTISEMENT_TYPE_CONTINUITY_ACTION_MODALS,
    ADVERTISEMENT_TYPE_CONTINUITY_IOS_17_CRASH,
    ADVERTISEMENT_TYPE_SWIFT_PAIRING,
    ADVERTISEMENT_TYPE_EASY_SETUP_WATCH,
    ADVERTISEMENT_TYPE_EASY_SETUP_BUDS,
    ADVERTISEMENT_TYPE_LOVESPOUSE_PLAY,
    ADVERTISEMENT_TYPE_LOVESPOUSE_STOP
}

fun AdvertisementSetType.stringResId(): Int {
    return when (this) {
        AdvertisementSetType.ADVERTISEMENT_TYPE_UNDEFINED -> R.string.ad_set_type_undefined

        AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_DEVICE -> R.string.ad_set_type_fast_pairing_device
        AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_NON_PRODUCTION -> R.string.ad_set_type_fast_pairing_non_production
        AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_PHONE_SETUP -> R.string.ad_set_type_fast_pairing_phone_setup
        AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_DEBUG -> R.string.ad_set_type_fast_pairing_debug

        AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_NEW_AIRTAG -> R.string.ad_set_type_continuity_new_airtag
        AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_NEW_DEVICE -> R.string.ad_set_type_continuity_new_device
        AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_NOT_YOUR_DEVICE -> R.string.ad_set_type_continuity_not_your_device
        AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_ACTION_MODALS -> R.string.ad_set_type_continuity_action_modals
        AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_IOS_17_CRASH -> R.string.ad_set_type_continuity_ios17_crash

        AdvertisementSetType.ADVERTISEMENT_TYPE_SWIFT_PAIRING -> R.string.ad_set_type_swift_pairing

        AdvertisementSetType.ADVERTISEMENT_TYPE_EASY_SETUP_WATCH -> R.string.ad_set_type_easy_setup_watch
        AdvertisementSetType.ADVERTISEMENT_TYPE_EASY_SETUP_BUDS -> R.string.ad_set_type_easy_setup_buds

        AdvertisementSetType.ADVERTISEMENT_TYPE_LOVESPOUSE_PLAY -> R.string.ad_set_type_lovespouse_play
        AdvertisementSetType.ADVERTISEMENT_TYPE_LOVESPOUSE_STOP -> R.string.ad_set_type_lovespouse_stop
    }
}
