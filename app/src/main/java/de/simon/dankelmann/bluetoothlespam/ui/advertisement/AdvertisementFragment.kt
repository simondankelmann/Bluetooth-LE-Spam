package de.simon.dankelmann.bluetoothlespam.ui.advertisement

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ExpandableListView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import de.simon.dankelmann.bluetoothlespam.Adapters.AdvertisementSetCollectionExpandableListViewAdapter
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementError
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementQueueMode
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetRange
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetType
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementState
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementTarget
import de.simon.dankelmann.bluetoothlespam.Enums.getDrawableId
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IAdvertisementServiceCallback
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IAdvertisementSetQueueHandlerCallback
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSetCollection
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSetList
import de.simon.dankelmann.bluetoothlespam.R
import de.simon.dankelmann.bluetoothlespam.databinding.FragmentAdvertisementBinding
import de.simon.dankelmann.bluetoothlespam.ui.setupEdgeToEdge


class AdvertisementFragment : Fragment(), IAdvertisementServiceCallback, IAdvertisementSetQueueHandlerCallback {

    private val _logTag = "AdvertisementFragment"

    private var _viewModel: AdvertisementViewModel? = null
    private val viewModel get() = _viewModel!!

    private var _binding: FragmentAdvertisementBinding? = null
    private val binding get() = _binding!!

    private lateinit var _expandableListView:ExpandableListView
    private lateinit var _adapter: AdvertisementSetCollectionExpandableListViewAdapter


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _viewModel = ViewModelProvider(this)[AdvertisementViewModel::class.java]
        _binding = FragmentAdvertisementBinding.inflate(inflater, container, false)
        val root: View = binding.root

        _expandableListView = binding.advertisementFragmentCollectionExpandableListview
        setupUi(root.context)

        return root
    }

    override fun onResume() {
        super.onResume()
        AppContext.getAdvertisementSetQueueHandler().addAdvertisementServiceCallback(this)
        AppContext.getAdvertisementSetQueueHandler().addAdvertisementQueueHandlerCallback(this)
        syncWithQueueHandler(requireContext())
    }

    override fun onPause() {
        super.onPause()
        AppContext.getAdvertisementSetQueueHandler().removeAdvertisementServiceCallback(this)
        AppContext.getAdvertisementSetQueueHandler().removeAdvertisementQueueHandlerCallback(this)
        //AppContext.getAdvertisementSetQueueHandler().deactivate()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
        //AppContext.getAdvertisementSetQueueHandler().deactivate(true)
    }

    private fun syncWithQueueHandler(context: Context) {
        val queue = AppContext.getAdvertisementSetQueueHandler()
        setAdvertisementSetCollection(context, queue.getAdvertisementSetCollection())
        viewModel.advertisementQueueMode.postValue(queue.getAdvertisementQueueMode())
        viewModel.isAdvertising.postValue(queue.isActive())
    }

    fun onPlayButtonClicked(context: Context) {
        if (viewModel.isAdvertising.value == true) {
            AppContext.getAdvertisementSetQueueHandler().deactivate(context)
        } else {
            AppContext.getAdvertisementSetQueueHandler().activate(context)
        }
    }

    fun setAdvertisementSetCollection(context: Context, advertisementSetCollection: AdvertisementSetCollection){
        viewModel.advertisementSetCollectionTitle.postValue(advertisementSetCollection.title)
        viewModel.advertisementSetCollectionSubTitle.postValue(getAdvertisementSetCollectionSubTitle(advertisementSetCollection))
        viewModel.advertisementSetCollectionHint.postValue(getAdvertisementSetCollectionHint(advertisementSetCollection))

        // Update UI
        setupExpandableListView(context, advertisementSetCollection)

        // Pass the Collection to the Queue Handler
        //AppContext.getAdvertisementSetQueueHandler().setAdvertisementSetCollection(advertisementSetCollection)
    }

    fun getAdvertisementSetCollectionHint(advertisementSetCollection: AdvertisementSetCollection):String{
        var hint = ""
        var sep = ""
        Log.d(_logTag, "Collection: " + advertisementSetCollection.advertisementSetLists.count())

        if(advertisementSetCollection.hints.isNotEmpty()){

            advertisementSetCollection.hints.forEach { it ->
                Log.d(_logTag, "CURRENT HINT: " +it)
                hint = hint + sep + it
                //hint += sep + it
                sep = ", "
                Log.d(_logTag, "HINT IS NOW: " + hint)
            }
        } else {
            hint = "-"
        }

        Log.d(_logTag, "Returning: " + hint)
        return hint
    }

    private fun setupExpandableListView(
        context: Context,
        advertisementSetCollection: AdvertisementSetCollection,
    ) {

        Log.d(_logTag, "Collection: " + advertisementSetCollection.advertisementSetLists.count())
        // Setup grouped Data
        var titleList = advertisementSetCollection.advertisementSetLists.toList()
        var dataList = HashMap<AdvertisementSetList, List<AdvertisementSet>>()
        advertisementSetCollection.advertisementSetLists.forEach{ advertisementSetList ->
            dataList[advertisementSetList] = advertisementSetList.advertisementSets
        }

        _adapter = AdvertisementSetCollectionExpandableListViewAdapter(context,titleList,dataList)
        _expandableListView.setAdapter(_adapter)

        if(_adapter.advertisementSetLists.isNotEmpty() && advertisementSetCollection.advertisementSetLists.size == 1){
            _expandableListView.expandGroup(0)
        }

        _expandableListView.setOnGroupExpandListener { groupPosition ->
            var advertisementSetList = titleList[groupPosition]
        }

        _expandableListView.setOnGroupCollapseListener { groupPosition ->
            var advertisementSetList = titleList[groupPosition]
        }

        _expandableListView.setOnChildClickListener { parent, v, groupPosition, childPosition, id ->
            var advertisementSetList = titleList[groupPosition]
            var advertisementSet = dataList[titleList[groupPosition]]!![childPosition]
            AppContext.getAdvertisementSetQueueHandler().setSelectedAdvertisementSet(groupPosition, childPosition)
            highlightCurrentAdverstisementSet(advertisementSet, AdvertisementState.ADVERTISEMENT_STATE_UNDEFINED)
            false
        }
    }

    fun getAdvertisementSetCollectionSubTitle(advertisementSetCollection: AdvertisementSetCollection):String{
        var subtitle = "${advertisementSetCollection.getTotalNumberOfAdvertisementSets()} Devices in ${advertisementSetCollection.getNumberOfLists()} Lists"
        return subtitle
    }

    fun getAdvertisementSetSubtitle(advertisementSet: AdvertisementSet):String{

        var type = when(advertisementSet.type){
            AdvertisementSetType.ADVERTISEMENT_TYPE_UNDEFINED -> "Undefined"
            AdvertisementSetType.ADVERTISEMENT_TYPE_SWIFT_PAIRING -> "Swift Pairing"

            AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_DEVICE -> "Fast Pairing Device"
            AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_PHONE_SETUP -> "Fast Pairing Phone Setup"
            AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_NON_PRODUCTION -> "Fast Pairing Non Production"
            AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_DEBUG -> "Fast Pairing Debug"

            AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_NEW_DEVICE -> "New Device Popup"
            AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_NOT_YOUR_DEVICE -> "Not your Device Popup"
            AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_NEW_AIRTAG -> "New Airtag Popup"


            AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_ACTION_MODALS -> "iOS Action Modal"
            AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_IOS_17_CRASH -> "iOS 17 Crash"

            AdvertisementSetType.ADVERTISEMENT_TYPE_EASY_SETUP_WATCH -> "Easy Setup Watch"
            AdvertisementSetType.ADVERTISEMENT_TYPE_EASY_SETUP_BUDS -> "Easy Setup Buds"

            AdvertisementSetType.ADVERTISEMENT_TYPE_LOVESPOUSE_PLAY -> "Lovespouse Play"
            AdvertisementSetType.ADVERTISEMENT_TYPE_LOVESPOUSE_STOP -> "Lovespouse Stop"
        }

        var range = when(advertisementSet.range){
            AdvertisementSetRange.ADVERTISEMENTSET_RANGE_CLOSE -> "Close"
            AdvertisementSetRange.ADVERTISEMENTSET_RANGE_MEDIUM -> "Medium"
            AdvertisementSetRange.ADVERTISEMENTSET_RANGE_FAR -> "Far"
            AdvertisementSetRange.ADVERTISEMENTSET_RANGE_UNKNOWN -> "Unknown"
        }

        return "Type: $type, Range: $range"
    }

    fun setAdvertisementQueueMode(advertisementQueueMode: AdvertisementQueueMode){
        AppContext.getAdvertisementSetQueueHandler().setAdvertisementQueueMode(advertisementQueueMode)
        viewModel.advertisementQueueMode.postValue(advertisementQueueMode)
    }

    fun setupUi(context: Context) {
        setupEdgeToEdge(binding.root, top = false)

        // Views
        var playButton = binding.advertisementFragmentPlayButton
        var queueModeButtonSingle = binding.advertisementFragmentQueueModeSingleButton
        var queueModeButtonLinear = binding.advertisementFragmentQueueModeLinearButton
        var queueModeButtonRandom = binding.advertisementFragmentQueueModeRandomButton
        var queueModeButtonList = binding.advertisementFragmentQueueModeListButton

        // Listeners
        playButton.setOnClickListener {
            onPlayButtonClicked(playButton.context)
        }
        queueModeButtonSingle.setOnClickListener{
            setAdvertisementQueueMode(AdvertisementQueueMode.ADVERTISEMENT_QUEUE_MODE_SINGLE)
        }
        queueModeButtonLinear.setOnClickListener{
            setAdvertisementQueueMode(AdvertisementQueueMode.ADVERTISEMENT_QUEUE_MODE_LINEAR)
        }
        queueModeButtonRandom.setOnClickListener{
            setAdvertisementQueueMode(AdvertisementQueueMode.ADVERTISEMENT_QUEUE_MODE_RANDOM)
        }
        queueModeButtonList.setOnClickListener{
            setAdvertisementQueueMode(AdvertisementQueueMode.ADVERTISEMENT_QUEUE_MODE_LIST)
        }

        // Observers
        val advertisingAnimation = binding.advertisementFragmentAdvertisingAnimation
        viewModel.isAdvertising.observe(viewLifecycleOwner) { isAdvertising ->
            if (isAdvertising) {
                playButton.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        resources, R.drawable.pause, context.theme
                    )
                )
                advertisingAnimation.playAnimation()
            } else {
                playButton.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        resources, R.drawable.play_arrow, context.theme
                    )
                )
                advertisingAnimation.cancelAnimation()
                advertisingAnimation.frame = 0
            }
        }

        viewModel.target.observe(viewLifecycleOwner) { target ->
            binding.advertisementFragmentTargetImage.setImageDrawable(
                ResourcesCompat.getDrawable(
                    resources, target.getDrawableId(), context.theme
                )
            )
        }

        viewModel.advertisementSetCollectionTitle.observe(viewLifecycleOwner) { value ->
            binding.advertisementFragmentCollectionTitle.text = value
        }
        viewModel.advertisementSetCollectionSubTitle.observe(viewLifecycleOwner) { value ->
            binding.advertisementFragmentCollectionSubtitle.text = value
        }
        viewModel.advertisementSetCollectionHint.observe(viewLifecycleOwner) { value ->
            binding.advertisementFragmentCollectionHint.text = value
        }
        viewModel.advertisementSetTitle.observe(viewLifecycleOwner) { value ->
            binding.advertisementFragmentCurrentSetTitle.text = value
        }
        viewModel.advertisementSetSubTitle.observe(viewLifecycleOwner) { value ->
            binding.advertisementFragmentCurrentSetSubTitle.text = value
        }

        viewModel.advertisementQueueMode.observe(viewLifecycleOwner) { mode ->
            val colorInactive = resources.getColor(R.color.text_color_light, context.theme)
            val colorActive = resources.getColor(R.color.blue_normal, context.theme)

            queueModeButtonSingle.setColorFilter(colorInactive)
            queueModeButtonLinear.setColorFilter(colorInactive)
            queueModeButtonRandom.setColorFilter(colorInactive)
            queueModeButtonList.setColorFilter(colorInactive)

            when(mode){
                AdvertisementQueueMode.ADVERTISEMENT_QUEUE_MODE_SINGLE -> queueModeButtonSingle.setColorFilter(colorActive)
                AdvertisementQueueMode.ADVERTISEMENT_QUEUE_MODE_LINEAR -> queueModeButtonLinear.setColorFilter(colorActive)
                AdvertisementQueueMode.ADVERTISEMENT_QUEUE_MODE_RANDOM -> queueModeButtonRandom.setColorFilter(colorActive)
                AdvertisementQueueMode.ADVERTISEMENT_QUEUE_MODE_LIST -> queueModeButtonList.setColorFilter(colorActive)
            }
        }
    }

    fun highlightCurrentAdverstisementSet(currentAdvertisementSet: AdvertisementSet, advertisementState: AdvertisementState){
        if(_adapter != null){
            _adapter.advertisementSetLists.forEachIndexed{ listIndex, advertisementList ->
                advertisementList.currentlyAdvertising = false
                advertisementList.advertisementSets.forEachIndexed{ setIndex, advertisementSet ->
                    if(advertisementSet == currentAdvertisementSet){
                        advertisementSet.advertisementState = advertisementState
                        advertisementSet.currentlyAdvertising = true
                        advertisementList.currentlyAdvertising = true
                    } else {
                        advertisementSet.currentlyAdvertising = false
                    }
                }
            }
            _adapter.notifyDataSetChanged()
        }
    }

    // AdvertismentServiceCallback
    override fun onAdvertisementSetStart(advertisementSet: AdvertisementSet?) {
        Log.d(_logTag, "onAdvertisementSetStart ${advertisementSet?.title}")
        if(advertisementSet != null){
            viewModel.target.postValue(advertisementSet.target)
            viewModel.advertisementSetTitle.postValue(advertisementSet.title)
            viewModel.advertisementSetSubTitle.postValue(getAdvertisementSetSubtitle(advertisementSet))
            highlightCurrentAdverstisementSet(advertisementSet, AdvertisementState.ADVERTISEMENT_STATE_STARTED)
        }
    }

    override fun onAdvertisementSetStop(advertisementSet: AdvertisementSet?) {
        Log.d(_logTag, "onAdvertisementSetStop")
    }

    override fun onAdvertisementSetSucceeded(advertisementSet: AdvertisementSet?) {
        if(advertisementSet != null){
            highlightCurrentAdverstisementSet(advertisementSet, AdvertisementState.ADVERTISEMENT_STATE_SUCCEEDED)
        }
    }

    override fun onAdvertisementSetFailed(advertisementSet: AdvertisementSet?, advertisementError: AdvertisementError) {
        if(advertisementSet != null){
            highlightCurrentAdverstisementSet(advertisementSet, AdvertisementState.ADVERTISEMENT_STATE_FAILED)
            Toast.makeText(requireContext(), "Advertisement Failed: $advertisementError", Toast.LENGTH_SHORT).show()
        }
    }
    // END: AdvertismentServiceCallback

    override fun onQueueHandlerActivated() {
        Log.d(_logTag, "onQueueHandlerActivated")
        viewModel.isAdvertising.postValue(true)
    }

    override fun onQueueHandlerDeactivated() {
        Log.d(_logTag, "onQueueHandlerDeactivated")
        viewModel.isAdvertising.postValue(false)
    }
}
