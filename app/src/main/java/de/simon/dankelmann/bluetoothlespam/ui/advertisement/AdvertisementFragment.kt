package de.simon.dankelmann.bluetoothlespam.ui.advertisement

import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ExpandableListView
import android.widget.Toast
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
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IAdvertisementServiceCallback
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IAdvertisementSetQueueHandlerCallback
import de.simon.dankelmann.bluetoothlespam.MainActivity
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSetCollection
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSetList
import de.simon.dankelmann.bluetoothlespam.R
import de.simon.dankelmann.bluetoothlespam.databinding.FragmentAdvertisementBinding


class AdvertisementFragment : Fragment(), IAdvertisementServiceCallback, IAdvertisementSetQueueHandlerCallback {

    private val _logTag = "AdvertisementFragment"
    private var _viewModel: AdvertisementViewModel? = null
    private var _binding: FragmentAdvertisementBinding? = null

    private lateinit var _expandableListView:ExpandableListView
    private lateinit var _adapter: AdvertisementSetCollectionExpandableListViewAdapter

    companion object {
        fun newInstance() = AdvertisementFragment()
    }

    private lateinit var viewModel: AdvertisementViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(_logTag, "onCreate")
        val viewModel = ViewModelProvider(this)[AdvertisementViewModel::class.java]
        _viewModel = viewModel
        _binding = FragmentAdvertisementBinding.inflate(inflater, container, false)
        val root: View = _binding!!.root

        _expandableListView = _binding!!.advertisementFragmentCollectionExpandableListview
        setupUi()

        return root
    }

    override fun onResume() {
        super.onResume()
        Log.d(_logTag, "onResume")
        AppContext.getAdvertisementSetQueueHandler().addAdvertisementServiceCallback(this)
        AppContext.getAdvertisementSetQueueHandler().addAdvertisementQueueHandlerCallback(this)
        syncWithQueueHandler()
    }

    override fun onPause() {
        Log.d(_logTag, "onPause")
        super.onPause()
        AppContext.getAdvertisementSetQueueHandler().removeAdvertisementServiceCallback(this)
        AppContext.getAdvertisementSetQueueHandler().removeAdvertisementQueueHandlerCallback(this)
        //AppContext.getAdvertisementSetQueueHandler().deactivate()
    }

    override fun onDestroy() {
        super.onDestroy()
        //AppContext.getAdvertisementSetQueueHandler().deactivate(true)
    }

    private fun syncWithQueueHandler(){
        setAdvertisementSetCollection(AppContext.getAdvertisementSetQueueHandler().getAdvertisementSetCollection())
        _viewModel!!.advertisementQueueMode.postValue(AppContext.getAdvertisementSetQueueHandler().getAdvertisementQueueMode())
        _viewModel!!.isAdvertising.postValue(AppContext.getAdvertisementSetQueueHandler().isActive())
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(AdvertisementViewModel::class.java)
        // TODO: Use the ViewModel
    }

    fun onPlayButtonClicked(){
        if(_viewModel!!.isAdvertising.value!!){
            AppContext.getAdvertisementSetQueueHandler().deactivate()
            _viewModel!!.isAdvertising.postValue(false)
        } else {
            AppContext.getAdvertisementSetQueueHandler().activate(true)
            _viewModel!!.isAdvertising.postValue(true)
        }
    }

    fun setAdvertisementSetCollection(advertisementSetCollection: AdvertisementSetCollection){
        _viewModel!!.advertisementSetCollectionTitle.postValue(advertisementSetCollection.title)
        _viewModel!!.advertisementSetCollectionSubTitle.postValue(getAdvertisementSetCollectionSubTitle(advertisementSetCollection))
        _viewModel!!.advertisementSetCollectionHint.postValue(getAdvertisementSetCollectionHint(advertisementSetCollection))

        // Update UI
        setupExpandableListView(advertisementSetCollection)

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

    private fun setupExpandableListView(advertisementSetCollection: AdvertisementSetCollection) {

        Log.d(_logTag, "Collection: " + advertisementSetCollection.advertisementSetLists.count())
        // Setup grouped Data
        var titleList = advertisementSetCollection.advertisementSetLists.toList()
        var dataList = HashMap<AdvertisementSetList, List<AdvertisementSet>>()
        advertisementSetCollection.advertisementSetLists.forEach{ advertisementSetList ->
            dataList[advertisementSetList] = advertisementSetList.advertisementSets
        }

        _adapter = AdvertisementSetCollectionExpandableListViewAdapter(AppContext.getContext(),titleList,dataList)
        _expandableListView.setAdapter(_adapter)


        if(_adapter.advertisementSetLists.isNotEmpty() && advertisementSetCollection.advertisementSetLists.size == 1){
            _expandableListView.expandGroup(0)
        }

        _expandableListView.setOnGroupExpandListener { groupPosition ->
            var advertisementSetList = titleList[groupPosition]
            //Toast.makeText(AppContext.getContext(), advertisementSetList.title + " List Expanded.", Toast.LENGTH_SHORT).show()
        }

        _expandableListView.setOnGroupCollapseListener { groupPosition ->
            var advertisementSetList = titleList[groupPosition]
            //Toast.makeText(AppContext.getContext(), advertisementSetList.title + " List Collapsed.", Toast.LENGTH_SHORT).show()
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


            AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_ACTION_MODALS -> "iOs Action Modal"
            AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_IOS_17_CRASH -> "iOs 17 Crash"

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
        _viewModel!!.advertisementQueueMode.postValue(advertisementQueueMode)
    }

    fun setupUi(){

        // Views
        var playButton = _binding!!.advertisementFragmentPlayButton
        var advertisingAnimation = _binding!!.advertisementFragmentAdvertisingAnimation
        var advertisingTargetImage = _binding!!.advertisementFragmentTargetImage
        var advertisementSetCollectionTitle = _binding!!.advertisementFragmentCollectionTitle
        var advertisementSetCollectionSubTitle = _binding!!.advertisementFragmentCollectionSubtitle
        var advertisementSetTitle = _binding!!.advertisementFragmentCurrentSetTitle
        var advertisementSetSubTitle = _binding!!.advertisementFragmentCurrentSetSubTitle
        var advertisementSetCollectionHint = _binding!!.advertisementFragmentCollectionHint
        var queueModeButtonSingle = _binding!!.advertisementFragmentQueueModeSingleButton
        var queueModeButtonLinear = _binding!!.advertisementFragmentQueueModeLinearButton
        var queueModeButtonRandom = _binding!!.advertisementFragmentQueueModeRandomButton
        var queueModeButtonList = _binding!!.advertisementFragmentQueueModeListButton

        // Listeners
        playButton.setOnClickListener{
            onPlayButtonClicked()
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
        _viewModel!!.isAdvertising.observe(viewLifecycleOwner) { isAdvertising ->
            if(isAdvertising){
                playButton.setImageDrawable(resources.getDrawable(R.drawable.pause, AppContext.getContext().theme))
                advertisingAnimation.playAnimation()
            } else {
                playButton.setImageDrawable(resources.getDrawable(R.drawable.play_arrow, AppContext.getContext().theme))
                advertisingAnimation.cancelAnimation()
                advertisingAnimation.frame = 0
            }
        }

        _viewModel!!.target.observe(viewLifecycleOwner) { target ->
            var targetImageDrawable:Drawable = when(target){
                AdvertisementTarget.ADVERTISEMENT_TARGET_UNDEFINED -> resources.getDrawable(R.drawable.bluetooth, AppContext.getContext().theme)
                AdvertisementTarget.ADVERTISEMENT_TARGET_IOS -> resources.getDrawable(R.drawable.apple, AppContext.getContext().theme)
                AdvertisementTarget.ADVERTISEMENT_TARGET_ANDROID -> resources.getDrawable(R.drawable.ic_android, AppContext.getContext().theme)
                AdvertisementTarget.ADVERTISEMENT_TARGET_WINDOWS -> resources.getDrawable(R.drawable.microsoft, AppContext.getContext().theme)
                AdvertisementTarget.ADVERTISEMENT_TARGET_SAMSUNG -> resources.getDrawable(R.drawable.samsung, AppContext.getContext().theme)
                AdvertisementTarget.ADVERTISEMENT_TARGET_KITCHEN_SINK -> resources.getDrawable(R.drawable.shuffle, AppContext.getContext().theme)
                AdvertisementTarget.ADVERTISEMENT_TARGET_LOVESPOUSE -> resources.getDrawable(R.drawable.heart, AppContext.getContext().theme)
            }
            advertisingTargetImage.setImageDrawable(targetImageDrawable)
        }

        _viewModel!!.advertisementSetCollectionTitle.observe(viewLifecycleOwner) { value ->
            advertisementSetCollectionTitle.text = value
        }

        _viewModel!!.advertisementSetCollectionSubTitle.observe(viewLifecycleOwner) { value ->
            advertisementSetCollectionSubTitle.text = value
        }

        _viewModel!!.advertisementSetCollectionHint.observe(viewLifecycleOwner) { value ->
            advertisementSetCollectionHint.text = value
        }

        _viewModel!!.advertisementSetTitle.observe(viewLifecycleOwner) { value ->
            advertisementSetTitle.text = value
        }

        _viewModel!!.advertisementSetSubTitle.observe(viewLifecycleOwner) { value ->
            advertisementSetSubTitle.text = value
        }

        _viewModel!!.advertisementQueueMode.observe(viewLifecycleOwner) { mode ->
            val colorInactive = resources.getColor(R.color.text_color_light, AppContext.getContext().theme)
            val colorActive = resources.getColor(R.color.blue_normal, AppContext.getContext().theme)

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
            _viewModel!!.target.postValue(advertisementSet.target)
            _viewModel!!.advertisementSetTitle.postValue(advertisementSet.title)
            _viewModel!!.advertisementSetSubTitle.postValue(getAdvertisementSetSubtitle(advertisementSet))
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
            Toast.makeText(AppContext.getContext(), "Advertisement Failed: $advertisementError", Toast.LENGTH_SHORT).show()
        }
    }
    // END: AdvertismentServiceCallback

    override fun onQueueHandlerActivated() {
        Log.d(_logTag, "onQueueHandlerActivated")
        _viewModel!!.isAdvertising.postValue(true)
    }

    override fun onQueueHandlerDeactivated() {
        Log.d(_logTag, "onQueueHandlerDeactivated")
        _viewModel!!.isAdvertising.postValue(false)
    }
}