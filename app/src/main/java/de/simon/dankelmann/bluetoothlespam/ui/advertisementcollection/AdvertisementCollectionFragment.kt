package de.simon.dankelmann.bluetoothlespam.ui.advertisementcollection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementQueueMode
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetType
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementTarget
import de.simon.dankelmann.bluetoothlespam.Enums.stringResId
import de.simon.dankelmann.bluetoothlespam.Helpers.DatabaseHelpers
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSetCollection
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSetList
import de.simon.dankelmann.bluetoothlespam.R
import de.simon.dankelmann.bluetoothlespam.databinding.FragmentAdvertisementCollectionBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class AdvertisementCollectionFragment : Fragment() {

    private val _logTag = "AdvertisementCollectionFragment"
    private var _binding: FragmentAdvertisementCollectionBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentAdvertisementCollectionBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupAdvertisementSetCollectionListView()

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun setupAdvertisementSetCollectionListView() {
        listOf(
            AdvertisementTarget.ADVERTISEMENT_TARGET_ANDROID,
            AdvertisementTarget.ADVERTISEMENT_TARGET_IOS,
            AdvertisementTarget.ADVERTISEMENT_TARGET_SAMSUNG,
            AdvertisementTarget.ADVERTISEMENT_TARGET_WINDOWS,
            AdvertisementTarget.ADVERTISEMENT_TARGET_LOVESPOUSE,
            AdvertisementTarget.ADVERTISEMENT_TARGET_KITCHEN_SINK
        ).forEach { target -> setupAdvertisementTarget(target) }
    }

    fun setupAdvertisementTarget(advertisementTarget: AdvertisementTarget) {
        // Get Layout View
        val advertisementSetCollectionView: View =
            layoutInflater.inflate(R.layout.listitem_advertisement_collection_start, null)
        val context = advertisementSetCollectionView.context

        // Insert Data
        var titleTextView: TextView =
            advertisementSetCollectionView.findViewById(R.id.listItemAdvertisementSetCollectionStartTitle)
        var targetTextView: TextView =
            advertisementSetCollectionView.findViewById(R.id.listItemAdvertisementSetCollectionStartTarget)
        var distanceTextView: TextView =
            advertisementSetCollectionView.findViewById(R.id.listItemAdvertisementSetCollectionStartDistance)
        var iconImageView: ImageView =
            advertisementSetCollectionView.findViewById(R.id.listItemAdvertisementSetCollectionStartImage)

        when (advertisementTarget) {
            AdvertisementTarget.ADVERTISEMENT_TARGET_ANDROID -> {
                titleTextView.text = "Fast Pair"
                targetTextView.text = "Target: Android"
                distanceTextView.text = "Distance: Close"
                iconImageView.setImageDrawable(
                    ResourcesCompat.getDrawable(resources, R.drawable.ic_android, context.theme)
                )
            }

            AdvertisementTarget.ADVERTISEMENT_TARGET_IOS -> {
                titleTextView.text = "Continuity"
                targetTextView.text = "Target: iOS"
                distanceTextView.text = "Distance: Mixed"
                iconImageView.setImageDrawable(
                    ResourcesCompat.getDrawable(resources, R.drawable.apple, context.theme)
                )
            }

            AdvertisementTarget.ADVERTISEMENT_TARGET_SAMSUNG -> {
                titleTextView.text = "Easy Setup"
                targetTextView.text = "Target: Samsung"
                distanceTextView.text = "Distance: Close"
                iconImageView.setImageDrawable(
                    ResourcesCompat.getDrawable(resources, R.drawable.samsung, context.theme)
                )
            }

            AdvertisementTarget.ADVERTISEMENT_TARGET_WINDOWS -> {
                titleTextView.text = "Swift Pair"
                targetTextView.text = "Target: Windows"
                distanceTextView.text = "Distance: Close"
                iconImageView.setImageDrawable(
                    ResourcesCompat.getDrawable(resources, R.drawable.microsoft, context.theme)
                )
            }

            AdvertisementTarget.ADVERTISEMENT_TARGET_KITCHEN_SINK -> {
                titleTextView.text = "Kitchen Sink"
                targetTextView.text = "Target: All"
                distanceTextView.text = "Distance: Mixed"
                iconImageView.setImageDrawable(
                    ResourcesCompat.getDrawable(resources, R.drawable.shuffle, context.theme)
                )
            }

            AdvertisementTarget.ADVERTISEMENT_TARGET_UNDEFINED -> {
                titleTextView.text = "Undefined"
                targetTextView.text = "Target: undefined"
                distanceTextView.text = "Distance: Undefined"
                iconImageView.setImageDrawable(
                    ResourcesCompat.getDrawable(resources, R.drawable.ic_info, context.theme)
                )
            }

            AdvertisementTarget.ADVERTISEMENT_TARGET_LOVESPOUSE -> {
                titleTextView.text = "Lovespouse"
                targetTextView.text = "Target: Lovespouse"
                distanceTextView.text = "Distance: Far"
                iconImageView.setImageDrawable(
                    ResourcesCompat.getDrawable(resources, R.drawable.heart, context.theme)
                )
            }
        }

        // Hookup Events
        var cardView: CardView =
            advertisementSetCollectionView.findViewById(R.id.listItemAdvertisementSetCollectionStartCardview)
        cardView.setOnClickListener {
            when (advertisementTarget) {
                AdvertisementTarget.ADVERTISEMENT_TARGET_ANDROID -> {
                    onFastPairCardViewClicked()
                }

                AdvertisementTarget.ADVERTISEMENT_TARGET_IOS -> {
                    onContinuityCardViewClicked()
                }

                AdvertisementTarget.ADVERTISEMENT_TARGET_SAMSUNG -> {
                    onEasySetupCardViewClicked()
                }

                AdvertisementTarget.ADVERTISEMENT_TARGET_WINDOWS -> {
                    onSwiftPairingCardViewClicked()
                }

                AdvertisementTarget.ADVERTISEMENT_TARGET_KITCHEN_SINK -> {
                    onKitchenSinkCardViewClicked()
                }

                AdvertisementTarget.ADVERTISEMENT_TARGET_LOVESPOUSE -> {
                    onLovespouseCardViewClicked()
                }

                else -> {
                    // Ignore
                }
            }
        }

        // Add to Parent View
        binding.advertisementCollectionListView.addView(advertisementSetCollectionView)
    }

    fun navigateToAdvertisementFragmentWithType(
        advertisementSetTypes: List<AdvertisementSetType>,
        advertisementSetCollectionTitle: String
    ) {
        // TODO: Move this work into a ViewModel
        lifecycleScope.launch(Dispatchers.IO) {
            // Run database work in background
            val advertisementSetCollection =
                buildAdvertisementCollection(advertisementSetTypes, advertisementSetCollectionTitle)

            activity?.let {
                it.runOnUiThread {
                    // Pass Collection to Advertisement Fragment
                    val navController = it.findNavController(R.id.nav_host_fragment)
                    AppContext.getAdvertisementSetQueueHandler().deactivate(it)
                    AppContext.getAdvertisementSetQueueHandler()
                        .setAdvertisementSetCollection(advertisementSetCollection)
                    navController.navigate(R.id.action_ad_coll_to_ad)
                }
            }
        }
    }

    fun buildAdvertisementCollection(
        advertisementSetTypes: List<AdvertisementSetType>,
        advertisementSetCollectionTitle: String
    ): AdvertisementSetCollection {
        // Initialize the Collection
        var advertisementSetCollection = AdvertisementSetCollection()
        advertisementSetCollection.title = advertisementSetCollectionTitle

        if (advertisementSetTypes.contains(AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_DEVICE) ||
            advertisementSetTypes.contains(AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_PHONE_SETUP) ||
            advertisementSetTypes.contains(AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_NON_PRODUCTION) ||
            advertisementSetTypes.contains(AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_DEBUG)
        ) {
            advertisementSetCollection.hints.add("Fast Pairing is patched on all modern devices due to this we no longer offer support for this feature")
        }

        if (advertisementSetTypes.contains(AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_IOS_17_CRASH)) {
            advertisementSetCollection.hints.add("Devices on iOS 18 or above will not crash but still get pop-ups")
        }

        advertisementSetTypes.forEach { advertisementSetType ->
            // Initialize the List
            val advertisementSetList = AdvertisementSetList()
            advertisementSetList.title = "${getString(advertisementSetType.stringResId())} List"

            val advertisementSets =
                DatabaseHelpers.getAllAdvertisementSetsForType(advertisementSetType)
            advertisementSetList.advertisementSets = advertisementSets.toMutableList()

            // Add List to the Collection
            advertisementSetCollection.advertisementSetLists.add(advertisementSetList)
        }

        return advertisementSetCollection
    }

    fun onFastPairCardViewClicked() {
        navigateToAdvertisementFragmentWithType(
            listOf(
                AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_DEVICE,
                AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_PHONE_SETUP,
                AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_NON_PRODUCTION,
                AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_DEBUG
            ), "Fast Pair Collection"
        )
    }

    fun onEasySetupCardViewClicked() {
        navigateToAdvertisementFragmentWithType(
            listOf(
                AdvertisementSetType.ADVERTISEMENT_TYPE_EASY_SETUP_WATCH,
                AdvertisementSetType.ADVERTISEMENT_TYPE_EASY_SETUP_BUDS
            ), "Easy Setup Collection"
        )
    }

    fun onContinuityCardViewClicked() {
        navigateToAdvertisementFragmentWithType(
            listOf(
                AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_NEW_DEVICE,
                AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_NOT_YOUR_DEVICE,
                AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_NEW_AIRTAG,
                AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_ACTION_MODALS,
                AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_IOS_17_CRASH
            ), "Continuity Collection"
        )
    }

    fun onSwiftPairingCardViewClicked() {
        navigateToAdvertisementFragmentWithType(
            listOf(AdvertisementSetType.ADVERTISEMENT_TYPE_SWIFT_PAIRING),
            "Swift Pair Collection"
        )
    }

    fun onLovespouseCardViewClicked() {
        navigateToAdvertisementFragmentWithType(
            listOf(
                AdvertisementSetType.ADVERTISEMENT_TYPE_LOVESPOUSE_PLAY,
                AdvertisementSetType.ADVERTISEMENT_TYPE_LOVESPOUSE_STOP
            ), "Lovespouse Collection"
        )
    }

    fun onKitchenSinkCardViewClicked() {
        // Set Random Mode for Kitchen Sink
        AppContext.getAdvertisementSetQueueHandler()
            .setAdvertisementQueueMode(AdvertisementQueueMode.ADVERTISEMENT_QUEUE_MODE_RANDOM)

        navigateToAdvertisementFragmentWithType(
            listOf(
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

                ), "Kitchen Sink Collection"
        )
    }
}
