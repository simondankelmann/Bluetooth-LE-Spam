package de.simon.dankelmann.bluetoothlespam.Navigation

/** Route constants for the navigation-compose `NavHost` in `MainActivity` (plan §6 step 4).
 * START, ADVERTISEMENT_COLLECTION, SPAM_DETECTOR, and PREFERENCES are the 4 top-level tabs --
 * they aren't separate NavHost destinations, but pages of the HorizontalPager hosted at START
 * (see MainActivity), identified by these same route strings via `floatingNavDestinations`. */
object SpecterDestinations {
    const val START = "start"
    const val ADVERTISEMENT_COLLECTION = "advertisementCollection"
    const val SPAM_DETECTOR = "spamDetector"
    const val ADVERTISEMENT = "advertisement"
    const val PREFERENCES = "preferences"
    const val GROUP_EDITOR = "groupEditor"
    const val MANAGE_QUICK_START = "manageQuickStart"
    const val MANAGE_DEVICES = "manageDevices"
}
