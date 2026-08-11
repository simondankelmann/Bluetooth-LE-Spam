package de.simon.dankelmann.bluetoothlespam.Database

import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetType

/**
 * The 6 built-in collections (plan §8) — single source of truth for which
 * [AdvertisementSetType]s belong to each, shared by the DB seeding pass
 * ([de.simon.dankelmann.bluetoothlespam.Helpers.DatabaseHelpers]) and the Advertisement
 * Collection screen's cards.
 */
data class BuiltInCollectionDefinition(
    val title: String,
    val types: List<AdvertisementSetType>,
)

val builtInCollectionDefinitions: List<BuiltInCollectionDefinition> = listOf(
    BuiltInCollectionDefinition(
        title = "Fast Pair Collection",
        types = listOf(
            AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_DEVICE,
            AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_PHONE_SETUP,
            AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_NON_PRODUCTION,
            AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_DEBUG,
        ),
    ),
    BuiltInCollectionDefinition(
        title = "Continuity Collection",
        types = listOf(
            AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_NEW_DEVICE,
            AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_NOT_YOUR_DEVICE,
            AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_NEW_AIRTAG,
            AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_ACTION_MODALS,
            AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_IOS_17_CRASH,
        ),
    ),
    BuiltInCollectionDefinition(
        title = "Easy Setup Collection",
        types = listOf(
            AdvertisementSetType.ADVERTISEMENT_TYPE_EASY_SETUP_WATCH,
            AdvertisementSetType.ADVERTISEMENT_TYPE_EASY_SETUP_BUDS,
        ),
    ),
    BuiltInCollectionDefinition(
        title = "Swift Pair Collection",
        types = listOf(AdvertisementSetType.ADVERTISEMENT_TYPE_SWIFT_PAIRING),
    ),
    BuiltInCollectionDefinition(
        title = "Lovespouse Collection",
        types = listOf(
            AdvertisementSetType.ADVERTISEMENT_TYPE_LOVESPOUSE_PLAY,
            AdvertisementSetType.ADVERTISEMENT_TYPE_LOVESPOUSE_STOP,
        ),
    ),
    BuiltInCollectionDefinition(
        title = "Kitchen Sink Collection",
        types = listOf(
            AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_DEVICE,
            AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_PHONE_SETUP,
            AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_NON_PRODUCTION,
            AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_DEBUG,
            AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_NEW_DEVICE,
            AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_NEW_AIRTAG,
            AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_NOT_YOUR_DEVICE,
            AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_ACTION_MODALS,
            AdvertisementSetType.ADVERTISEMENT_TYPE_EASY_SETUP_WATCH,
            AdvertisementSetType.ADVERTISEMENT_TYPE_EASY_SETUP_BUDS,
            AdvertisementSetType.ADVERTISEMENT_TYPE_SWIFT_PAIRING,
            AdvertisementSetType.ADVERTISEMENT_TYPE_LOVESPOUSE_PLAY,
            AdvertisementSetType.ADVERTISEMENT_TYPE_LOVESPOUSE_STOP,
        ),
    ),
)
