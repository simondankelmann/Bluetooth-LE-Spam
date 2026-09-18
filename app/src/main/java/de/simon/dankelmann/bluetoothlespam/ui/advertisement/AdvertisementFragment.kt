package de.simon.dankelmann.bluetoothlespam.ui.advertisement

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ExpandableListView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import de.simon.dankelmann.bluetoothlespam.Adapters.AdvertisementSetCollectionExpandableListViewAdapter
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.SwiftPairAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementError
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementQueueMode
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetRange
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetType
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementState
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementTarget
import de.simon.dankelmann.bluetoothlespam.Enums.getDrawableId
import de.simon.dankelmann.bluetoothlespam.Helpers.DatabaseHelpers
import de.simon.dankelmann.bluetoothlespam.Helpers.DeviceCustomizationHelper
import de.simon.dankelmann.bluetoothlespam.Helpers.StringHelpers
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IAdvertisementServiceCallback
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IAdvertisementSetQueueHandlerCallback
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSetCollection
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSetList
import de.simon.dankelmann.bluetoothlespam.R
import de.simon.dankelmann.bluetoothlespam.databinding.FragmentAdvertisementBinding
import de.simon.dankelmann.bluetoothlespam.ui.setupEdgeToEdge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class AdvertisementFragment : Fragment(), IAdvertisementServiceCallback, IAdvertisementSetQueueHandlerCallback,
    AdvertisementSetCollectionExpandableListViewAdapter.OnAdvertisementSetActionListener {

    private val _logTag = "AdvertisementFragment"

    companion object {
        // 3 header bytes + name must fit the 31 byte legacy advertising payload
        private const val MAX_CUSTOM_SWIFT_PAIR_NAME_BYTES = 24
    }

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
        setupUi()

        return root
    }

    override fun onResume() {
        super.onResume()
        AppContext.getAdvertisementSetQueueHandler().addAdvertisementServiceCallback(this)
        AppContext.getAdvertisementSetQueueHandler().addAdvertisementQueueHandlerCallback(this)
        syncWithQueueHandler()
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

    private fun syncWithQueueHandler(){
        setAdvertisementSetCollection(AppContext.getAdvertisementSetQueueHandler().getAdvertisementSetCollection())
        viewModel.advertisementQueueMode.postValue(AppContext.getAdvertisementSetQueueHandler().getAdvertisementQueueMode())
        viewModel.isAdvertising.postValue(AppContext.getAdvertisementSetQueueHandler().isActive())
    }

    fun onPlayButtonClicked(){
        if(viewModel.isAdvertising.value == true){
            AppContext.getAdvertisementSetQueueHandler().deactivate()
            viewModel.isAdvertising.postValue(false)
        } else {
            AppContext.getAdvertisementSetQueueHandler().activate(true)
            viewModel.isAdvertising.postValue(true)
        }
    }

    fun setAdvertisementSetCollection(advertisementSetCollection: AdvertisementSetCollection){
        viewModel.advertisementSetCollectionTitle.postValue(advertisementSetCollection.title)
        viewModel.advertisementSetCollectionSubTitle.postValue(getAdvertisementSetCollectionSubTitle(advertisementSetCollection))
        viewModel.advertisementSetCollectionHint.postValue(getAdvertisementSetCollectionHint(advertisementSetCollection))

        // Show the custom Swift Pair input only when the collection contains Swift Pairing sets
        _binding?.advertisementFragmentCustomSwiftPairContainer?.visibility =
            if (isSwiftPairCollection(advertisementSetCollection)) View.VISIBLE else View.GONE

        // Update UI
        setupExpandableListView(advertisementSetCollection)

        // Pass the Collection to the Queue Handler
        //AppContext.getAdvertisementSetQueueHandler().setAdvertisementSetCollection(advertisementSetCollection)
    }

    fun isSwiftPairCollection(advertisementSetCollection: AdvertisementSetCollection): Boolean {
        if (advertisementSetCollection.title.contains("Swift Pair")) return true
        return advertisementSetCollection.advertisementSetLists.any { advertisementSetList ->
            advertisementSetList.advertisementSets.any { advertisementSet ->
                advertisementSet.type == AdvertisementSetType.ADVERTISEMENT_TYPE_SWIFT_PAIRING
            }
        }
    }

    fun onAddCustomSwiftPairDeviceClicked() {
        val input = _binding?.advertisementFragmentCustomSwiftPairNameInput ?: return
        val addButton = _binding?.advertisementFragmentCustomSwiftPairAddButton ?: return
        val deviceName = input.text.toString().trim()

        if (deviceName.isEmpty()) {
            Toast.makeText(AppContext.getContext(), getString(R.string.swift_pair_custom_empty), Toast.LENGTH_SHORT).show()
            return
        }

        // Swift Pair manufacturer data is 3 header bytes + name and must fit the 31 byte legacy payload
        if (deviceName.toByteArray(Charsets.UTF_8).size > MAX_CUSTOM_SWIFT_PAIR_NAME_BYTES) {
            Toast.makeText(AppContext.getContext(), getString(R.string.swift_pair_custom_too_long), Toast.LENGTH_SHORT).show()
            return
        }

        val collection = AppContext.getAdvertisementSetQueueHandler().getAdvertisementSetCollection()
        val alreadyExists = collection.advertisementSetLists.any { advertisementSetList ->
            advertisementSetList.advertisementSets.any { advertisementSet ->
                advertisementSet.title.equals(deviceName, ignoreCase = true)
            }
        }
        if (alreadyExists) {
            Toast.makeText(AppContext.getContext(), getString(R.string.swift_pair_custom_exists), Toast.LENGTH_SHORT).show()
            return
        }

        addButton.isEnabled = false
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val newSets = SwiftPairAdvertisementSetGenerator()
                    .getAdvertisementSets(mapOf(deviceName to "Custom"))
                newSets.forEach { advertisementSet ->
                    DatabaseHelpers.saveAdvertisementSet(advertisementSet)
                }

                AppContext.getActivity().runOnUiThread {
                    input.text.clear()
                    Toast.makeText(AppContext.getContext(), getString(R.string.swift_pair_custom_added), Toast.LENGTH_SHORT).show()
                }
                refreshCurrentCollectionFromDatabase()
            } catch (e: Exception) {
                Log.e(_logTag, "Failed to add custom Swift Pair device: ${e.message}")
                AppContext.getActivity().runOnUiThread {
                    Toast.makeText(AppContext.getContext(), "Failed to add device", Toast.LENGTH_SHORT).show()
                }
            } finally {
                AppContext.getActivity().runOnUiThread {
                    _binding?.advertisementFragmentCustomSwiftPairAddButton?.isEnabled = true
                }
            }
        }
    }

    /**
     * Reloads every list of the current collection from the database
     * (hiding removed sets), pushes it to the queue handler and refreshes the UI.
     * Must be safe to call from any thread.
     */
    fun refreshCurrentCollectionFromDatabase() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val handler = AppContext.getAdvertisementSetQueueHandler()
                val collection = handler.getAdvertisementSetCollection()
                collection.advertisementSetLists.toList().forEach { advertisementSetList ->
                    val types = advertisementSetList.advertisementSets.map { it.type }.distinct()
                    val fresh = types.flatMap { type ->
                        DeviceCustomizationHelper.filterDeleted(
                            DatabaseHelpers.getAllAdvertisementSetsForType(type)
                        )
                    }
                    advertisementSetList.advertisementSets.clear()
                    advertisementSetList.advertisementSets.addAll(fresh)
                }
                collection.advertisementSetLists.removeAll { it.advertisementSets.isEmpty() }

                // Preserve multi-selection across the reload (matched by stable row id)
                val selectedIds = handler.getSelectedIds()
                val presentIds = mutableSetOf<Int>()
                collection.advertisementSetLists.forEach { list ->
                    list.advertisementSets.forEach {
                        presentIds.add(it.id)
                        it.selected = selectedIds.contains(it.id)
                    }
                }
                handler.retainSelection(presentIds)

                AppContext.getActivity().runOnUiThread {
                    if (collection.getTotalNumberOfAdvertisementSets() == 0 && handler.isActive()) {
                        handler.deactivate()
                    }
                    handler.setAdvertisementSetCollection(collection, false)
                    _binding?.let { setAdvertisementSetCollection(collection) }
                }
            } catch (e: Exception) {
                Log.e(_logTag, "Failed to refresh collection: ${e.message}")
            }
        }
    }

    // AdvertisementSet row actions (all categories)

    override fun onEditAdvertisementSet(advertisementSet: AdvertisementSet) {
        if (advertisementSet.id <= 0) return
        // DB access must stay off the UI thread (Room forbids main-thread queries)
        CoroutineScope(Dispatchers.IO).launch {
            val fresh = try {
                DatabaseHelpers.getAdvertisementSetById(advertisementSet.id)
            } catch (e: Exception) {
                Log.e(_logTag, "Failed to load device for editing: ${e.message}")
                null
            }
            AppContext.getActivity().runOnUiThread {
                if (fresh == null) {
                    Toast.makeText(AppContext.getContext(), "Failed to load device", Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                if (_binding == null) return@runOnUiThread
                EditAdvertisementSetDialog.show(AppContext.getActivity(), fresh) { values ->
                    saveEditedAdvertisementSet(fresh.id, values)
                }
            }
        }
    }

    fun saveEditedAdvertisementSet(setId: Int, values: EditAdvertisementSetDialog.EditedValues) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val current = DatabaseHelpers.getAdvertisementSetById(setId) ?: return@launch
                DeviceCustomizationHelper.saveBackupIfAbsent(current)

                current.title = values.title
                current.advertiseData.includeDeviceName = values.includeDeviceName
                current.advertiseData.includeTxPower = values.includeTxPower
                current.advertiseData.manufacturerData.forEachIndexed { index, entry ->
                    if (index < values.manufacturerIds.size) {
                        entry.manufacturerId = values.manufacturerIds[index]
                    }
                    if (index < values.manufacturerHex.size) {
                        entry.manufacturerSpecificData =
                            StringHelpers.decodeHex(values.manufacturerHex[index])
                    }
                }
                current.advertiseData.services.forEachIndexed { index, entry ->
                    if (index < values.serviceUuids.size) {
                        entry.serviceUuid = android.os.ParcelUuid.fromString(values.serviceUuids[index])
                    }
                    if (index < values.serviceHex.size) {
                        val hex = values.serviceHex[index]
                        entry.serviceData = if (hex == null) null else StringHelpers.decodeHex(hex)
                    }
                }

                DatabaseHelpers.updateAdvertisementSetContent(current)

                // If the user reverted everything back to the original, drop the backup
                val reloaded = DatabaseHelpers.getAdvertisementSetById(setId)
                if (reloaded != null && DeviceCustomizationHelper.matchesBackup(reloaded)) {
                    DeviceCustomizationHelper.clearBackup(setId)
                }

                AppContext.getActivity().runOnUiThread {
                    Toast.makeText(AppContext.getContext(), getString(R.string.device_updated), Toast.LENGTH_SHORT).show()
                }
                refreshCurrentCollectionFromDatabase()
            } catch (e: Exception) {
                Log.e(_logTag, "Failed to save edited device: ${e.message}")
                AppContext.getActivity().runOnUiThread {
                    Toast.makeText(AppContext.getContext(), "Failed to save device", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDeleteAdvertisementSet(advertisementSet: AdvertisementSet) {
        if (advertisementSet.id <= 0) return
        com.google.android.material.dialog.MaterialAlertDialogBuilder(AppContext.getActivity())
            .setTitle(getString(R.string.device_delete_title))
            .setMessage(getString(R.string.device_delete_message, advertisementSet.title))
            .setPositiveButton(getString(R.string.device_delete)) { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    DeviceCustomizationHelper.setDeleted(advertisementSet.id, true)
                    AppContext.getActivity().runOnUiThread {
                        Toast.makeText(AppContext.getContext(), getString(R.string.device_deleted), Toast.LENGTH_SHORT).show()
                    }
                    refreshCurrentCollectionFromDatabase()
                }
            }
            .setNegativeButton(getString(android.R.string.cancel), null)
            .show()
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
        _adapter.actionListener = this
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
            val handler = AppContext.getAdvertisementSetQueueHandler()
            // Multi-selection: tapping toggles the set, previously tapped sets stay selected
            val nowSelected = handler.toggleSelectedAdvertisementSet(advertisementSet)
            if (nowSelected) {
                handler.setSelectedAdvertisementSet(groupPosition, childPosition)
            } else {
                if (handler.getCurrentAdvertisementSet()?.id == advertisementSet.id) {
                    handler.clearCurrentAdvertisementSet()
                }
            }
            _adapter.notifyDataSetChanged()
            false
        }

        // Long-press (>500ms) on a category header toggles the whole category:
        // selects all of it when incomplete, deselects it entirely when complete
        _expandableListView.setOnItemLongClickListener { parent, view, position, id ->
            val packedPosition = _expandableListView.getExpandableListPosition(position)
            val packedType = ExpandableListView.getPackedPositionType(packedPosition)
            if (packedType == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
                val groupPosition = ExpandableListView.getPackedPositionGroup(packedPosition)
                if (groupPosition in titleList.indices) {
                    val groupSets = dataList[titleList[groupPosition]] ?: emptyList()
                    if (groupSets.isNotEmpty()) {
                        val handler = AppContext.getAdvertisementSetQueueHandler()
                        val allSelected = groupSets.all { handler.isSelected(it) }
                        handler.setSetsSelected(groupSets, !allSelected)
                        if (handler.getCurrentAdvertisementSet()?.let { !handler.isSelected(it) } == true) {
                            handler.clearCurrentAdvertisementSet()
                        }
                        _adapter.notifyDataSetChanged()
                    }
                }
                true
            } else {
                false
            }
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

    fun setupUi() {
        setupEdgeToEdge(binding.root, top = false)

        // Views
        var playButton = binding.advertisementFragmentPlayButton
        var queueModeButtonSingle = binding.advertisementFragmentQueueModeSingleButton
        var queueModeButtonLinear = binding.advertisementFragmentQueueModeLinearButton
        var queueModeButtonRandom = binding.advertisementFragmentQueueModeRandomButton
        var queueModeButtonList = binding.advertisementFragmentQueueModeListButton

        // Listeners
        playButton.setOnClickListener {
            onPlayButtonClicked()
        }
        binding.advertisementFragmentCustomSwiftPairAddButton.setOnClickListener {
            onAddCustomSwiftPairDeviceClicked()
        }
        binding.advertisementFragmentCustomSwiftPairNameInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                onAddCustomSwiftPairDeviceClicked()
                true
            } else {
                false
            }
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
                        resources, R.drawable.pause, AppContext.getContext().theme
                    )
                )
                advertisingAnimation.playAnimation()
            } else {
                playButton.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        resources, R.drawable.play_arrow, AppContext.getContext().theme
                    )
                )
                advertisingAnimation.cancelAnimation()
                advertisingAnimation.frame = 0
            }
        }

        viewModel.target.observe(viewLifecycleOwner) { target ->
            binding.advertisementFragmentTargetImage.setImageDrawable(
                ResourcesCompat.getDrawable(
                    resources, target.getDrawableId(), AppContext.getContext().theme
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
            Toast.makeText(AppContext.getContext(), "Advertisement Failed: $advertisementError", Toast.LENGTH_SHORT).show()
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
