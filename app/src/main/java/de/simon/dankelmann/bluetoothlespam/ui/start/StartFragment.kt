package de.simon.dankelmann.bluetoothlespam.ui.start

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext.Companion.bluetoothAdapter
import de.simon.dankelmann.bluetoothlespam.Database.AppDatabase
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementQueueMode
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetType
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementTarget
import de.simon.dankelmann.bluetoothlespam.Handlers.AdvertisementSetQueueHandler
import de.simon.dankelmann.bluetoothlespam.Helpers.BluetoothHelpers
import de.simon.dankelmann.bluetoothlespam.Helpers.DatabaseHelpers
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSetCollection
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSetList
import de.simon.dankelmann.bluetoothlespam.PermissionCheck.PermissionCheck
import de.simon.dankelmann.bluetoothlespam.R
import de.simon.dankelmann.bluetoothlespam.Services.BluetoothLeScanForegroundService
import de.simon.dankelmann.bluetoothlespam.databinding.FragmentStartBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.Exception
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class StartFragment : Fragment() {

    private val _logTag = "StartFragment"
    private var _viewModel: StartViewModel? = null
    private var _binding: FragmentStartBinding? = null
    private lateinit var registerForResult:ActivityResultLauncher<Intent>


    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!


    companion object {
        fun newInstance() = StartFragment()
    }

    private lateinit var viewModel: StartViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val viewModel = ViewModelProvider(this)[StartViewModel::class.java]
        _viewModel = viewModel
        _binding = FragmentStartBinding.inflate(inflater, container, false)
        val root: View = binding.root

        _viewModel!!.appVersion.postValue(getAppVersion())
        _viewModel!!.androidVersion.postValue(android.os.Build.VERSION.RELEASE)
        _viewModel!!.sdkVersion.postValue(android.os.Build.VERSION.SDK_INT.toString())
        _viewModel!!.bluetoothSupport.postValue(getBluetoothSupportText())

        // register for bt enable callback
        registerForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data
                // Handle the Intent
                checkBluetoothAdapter(false)
            }
        }

        setupUi()

        checkRequiredPermissions(true)
        checkBluetoothAdapter(true)
        checkAdvertisementService()
        checkDatabase()

        return root
    }

    fun getAppVersion():String{
        val manager = AppContext.getContext()!!.packageManager
        val info = manager.getPackageInfo(AppContext.getContext().packageName, 0)
        val version = info.versionName
        return version
    }

    fun getBluetoothSupportText():String{
        if(AppContext.isBluetooth5Supported()){
            return "Modern & Legacy"
        } else {
            return "Legacy only"
        }
    }

    fun setupAdvertisementSetCollectionListView(linearLayout: LinearLayout, advertisementTargets: List<AdvertisementTarget>){
        advertisementTargets.forEach { advertisementTarget ->
            // Get Layout View
            val advertisementSetCollectionView: View = layoutInflater.inflate(R.layout.listitem_advertisement_collection_start, null)

            // Insert Data
            var titleTextView:TextView = advertisementSetCollectionView.findViewById(R.id.listItemAdvertisementSetCollectionStartTitle)
            var targetTextView:TextView = advertisementSetCollectionView.findViewById(R.id.listItemAdvertisementSetCollectionStartTarget)
            var distanceTextView:TextView = advertisementSetCollectionView.findViewById(R.id.listItemAdvertisementSetCollectionStartDistance)
            var iconImageView:ImageView = advertisementSetCollectionView.findViewById(R.id.listItemAdvertisementSetCollectionStartImage)

            when(advertisementTarget){
                AdvertisementTarget.ADVERTISEMENT_TARGET_ANDROID -> {
                    titleTextView.text = "Fast Pair"
                    targetTextView.text = "Target: Android"
                    distanceTextView.text = "Distance: Close"
                    iconImageView.setImageDrawable(resources.getDrawable(R.drawable.ic_android, AppContext.getContext().theme))
                }

                AdvertisementTarget.ADVERTISEMENT_TARGET_IOS -> {
                    titleTextView.text = "Continuity"
                    targetTextView.text = "Target: iOS"
                    distanceTextView.text = "Distance: Mixed"
                    iconImageView.setImageDrawable(resources.getDrawable(R.drawable.apple, AppContext.getContext().theme))
                }

                AdvertisementTarget.ADVERTISEMENT_TARGET_SAMSUNG -> {
                    titleTextView.text = "Easy Setup"
                    targetTextView.text = "Target: Samsung"
                    distanceTextView.text = "Distance: Close"
                    iconImageView.setImageDrawable(resources.getDrawable(R.drawable.samsung, AppContext.getContext().theme))
                }

                AdvertisementTarget.ADVERTISEMENT_TARGET_WINDOWS -> {
                    titleTextView.text = "Swift Pair"
                    targetTextView.text = "Target: Windows"
                    distanceTextView.text = "Distance: Close"
                    iconImageView.setImageDrawable(resources.getDrawable(R.drawable.microsoft, AppContext.getContext().theme))
                }

                AdvertisementTarget.ADVERTISEMENT_TARGET_KITCHEN_SINK -> {
                    titleTextView.text = "Kitchen Sink"
                    targetTextView.text = "Target: All"
                    distanceTextView.text = "Distance: Mixed"
                    iconImageView.setImageDrawable(resources.getDrawable(R.drawable.shuffle, AppContext.getContext().theme))
                }

                AdvertisementTarget.ADVERTISEMENT_TARGET_UNDEFINED -> {
                    titleTextView.text = "Undefined"
                    targetTextView.text = "Target: undefined"
                    distanceTextView.text = "Distance: Undefined"
                    iconImageView.setImageDrawable(resources.getDrawable(R.drawable.ic_info, AppContext.getContext().theme))
                }

                AdvertisementTarget.ADVERTISEMENT_TARGET_LOVESPOUSE -> {
                    titleTextView.text = "Lovespouse"
                    targetTextView.text = "Target: Lovespouse"
                    distanceTextView.text = "Distance: Far"
                    iconImageView.setImageDrawable(resources.getDrawable(R.drawable.heart, AppContext.getContext().theme))
                }
            }

            // Hookup Events
            var cardView:CardView = advertisementSetCollectionView.findViewById(R.id.listItemAdvertisementSetCollectionStartCardview)
            cardView.setOnClickListener {
                when(advertisementTarget){
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
            linearLayout.addView(advertisementSetCollectionView)
        }
    }

    fun getDisplayableAdvertisementSetCollections():List<AdvertisementSetCollection>{
        var advertisementSetCollectionList:MutableList<AdvertisementSetCollection> = mutableListOf()


        return advertisementSetCollectionList.toList()
    }

    fun getAdvertisementSetCollectionForType(advertisementSetType: AdvertisementSetType):AdvertisementSetCollection{
        var advertisementSetCollection = AdvertisementSetCollection()



        return AdvertisementSetCollection()
    }

    fun setupUi(){

        // Loading Animation
        val loadingSpinnerLayout: View = binding.startFragmentLoadingSpinnerLayout
        _viewModel!!.isLoading.observe(viewLifecycleOwner) {
            loadingSpinnerLayout.visibility = when(it){
                true -> View.VISIBLE
                false -> View.GONE
            }
        }

        // Seeding Animation
        val seedingAnimationView: View = binding.startFragmentDatabaseCardSeedingAnimation
        val databaseImageView: View = binding.startFragmentDatabaseCardIcon
        _viewModel!!.isSeeding.observe(viewLifecycleOwner) {
            seedingAnimationView.visibility = when(it){
                true -> View.VISIBLE
                false -> View.GONE
            }

            databaseImageView.visibility = when(it){
                true -> View.GONE
                false -> View.VISIBLE
            }
        }

        var linearLayout:LinearLayout = binding.listView
        setupAdvertisementSetCollectionListView(linearLayout, listOf(AdvertisementTarget.ADVERTISEMENT_TARGET_ANDROID, AdvertisementTarget.ADVERTISEMENT_TARGET_IOS, AdvertisementTarget.ADVERTISEMENT_TARGET_SAMSUNG, AdvertisementTarget.ADVERTISEMENT_TARGET_WINDOWS,AdvertisementTarget.ADVERTISEMENT_TARGET_LOVESPOUSE, AdvertisementTarget.ADVERTISEMENT_TARGET_KITCHEN_SINK))

        // Loading Message
        val textViewLoadingMessage: TextView = binding.startFragmentLoadingSpinnerMessage
        _viewModel!!.loadingMessage.observe(viewLifecycleOwner) {
            textViewLoadingMessage.text = it
        }

        // App Version
        val textViewAppVersion: TextView = binding.startFragmentTextViewAppVersion
        _viewModel!!.appVersion.observe(viewLifecycleOwner) {
            textViewAppVersion.text = "App Version: $it"
        }

        // Android Version
        val textViewAndroidVersion: TextView = binding.startFragmentTextViewAndroidVersion
        _viewModel!!.androidVersion.observe(viewLifecycleOwner) {
            textViewAndroidVersion.text = "Android Version: $it"
        }

        // SDK Version
        val textViewSdkVersion: TextView = binding.startFragmentTextViewSdkVersion
        _viewModel!!.sdkVersion.observe(viewLifecycleOwner) {
            textViewSdkVersion.text = "SDK Version: $it"
        }

        // Bluetooth Support
        val textViewBluetoothSupport: TextView = binding.startFragmentTextViewBluetooth
        _viewModel!!.bluetoothSupport.observe(viewLifecycleOwner) {
            textViewBluetoothSupport.text = "Bluetooth: $it"
        }

        // Missing Requirements Text
        val textViewRequirementsDescription: TextView = binding.startFragmentRequirementsTextView
        val startFragmentMissingRequirementsTextView:  TextView = binding.startFragmentMissingRequirementsTextView
        _viewModel!!.missingRequirements.observe(viewLifecycleOwner) {missingRequirementsList ->
          if(missingRequirementsList.isEmpty()){
              startFragmentMissingRequirementsTextView.visibility = View.GONE
              //textViewRequirementsDescription.visibility = View.GONE
              startFragmentMissingRequirementsTextView.text = ""
              textViewRequirementsDescription.text = "All requirements are met"
          } else {
              startFragmentMissingRequirementsTextView.visibility = View.VISIBLE
              //textViewRequirementsDescription.visibility = View.VISIBLE
              textViewRequirementsDescription.text = "Missing Requirements:"
              var prepend = ""
              var missingRequirementsString = ""
              missingRequirementsList.forEach {missingRequirement ->
                  missingRequirementsString += prepend + missingRequirement
                  prepend = "\n"
              }
              startFragmentMissingRequirementsTextView.text = missingRequirementsString
          }
        }

        // Permissions CardView
        val startFragmentPermissionCardView: CardView = binding.startFragmentPermissionsCardview
        startFragmentPermissionCardView.setOnClickListener {
            checkRequiredPermissions(true)
        }

        // Permissions CardView Content
        val startFragmentPermissionCardViewContentWrapper: LinearLayout = binding.startFragmentPermissionCardViewContentWrapper
        _viewModel!!.allPermissionsGranted.observe(viewLifecycleOwner) {
            if(it == true){
                startFragmentPermissionCardViewContentWrapper.background = resources.getDrawable(R.drawable.gradient_success, AppContext.getContext().theme)
            } else {
                startFragmentPermissionCardViewContentWrapper.background = resources.getDrawable(R.drawable.gradient_error, AppContext.getContext().theme)
            }
        }

        // Bluetooth CardView
        val startFragmentBluetoothCardView: CardView = binding.startFragmentBluetoothCardview
        startFragmentBluetoothCardView.setOnClickListener {
            checkBluetoothAdapter(true)
        }

        // Bluetooth CardView Content
        val startFragmentBluetoothCardViewContentWrapper: LinearLayout = binding.startFragmentBluetoothCardViewContentWrapper
        _viewModel!!.bluetoothAdapterIsReady.observe(viewLifecycleOwner) {
            if(it == true){
                startFragmentBluetoothCardViewContentWrapper.background = resources.getDrawable(R.drawable.gradient_success, AppContext.getContext().theme)
            } else {
                startFragmentBluetoothCardViewContentWrapper.background = resources.getDrawable(R.drawable.gradient_error, AppContext.getContext().theme)
            }
        }

        // Service CardView
        val startFragmentServiceCardview: CardView = binding.startFragmentServiceCardview
        startFragmentServiceCardview.setOnClickListener {
            checkAdvertisementService()
        }

        // Service CardView Content
        val startFragmentServiceCardViewContentWrapper: LinearLayout = binding.startFragmentServiceCardViewContentWrapper
        _viewModel!!.advertisementServiceIsReady.observe(viewLifecycleOwner) {
            if(it == true){
                startFragmentServiceCardViewContentWrapper.background = resources.getDrawable(R.drawable.gradient_success, AppContext.getContext().theme)
            } else {
                startFragmentServiceCardViewContentWrapper.background = resources.getDrawable(R.drawable.gradient_error, AppContext.getContext().theme)
            }
        }

        // Database CardView
        val startFragmentDatabaseCardview: CardView = binding.startFragmentDatabaseCardview
        startFragmentDatabaseCardview.setOnClickListener {
            checkDatabase()
        }

        // Service CardView Content
        val startFragmentDatabaseCardViewContentWrapper: LinearLayout = binding.startFragmentDatabaseCardViewContentWrapper
        _viewModel!!.databaseIsReady.observe(viewLifecycleOwner) {
            if(it == true){
                startFragmentDatabaseCardViewContentWrapper.background = resources.getDrawable(R.drawable.gradient_success, AppContext.getContext().theme)
            } else {
                startFragmentDatabaseCardViewContentWrapper.background = resources.getDrawable(R.drawable.gradient_error, AppContext.getContext().theme)
            }
        }

    }

    fun navigateToAdvertisementFragmentWithType(advertisementSetTypes: List<AdvertisementSetType>, advertisementSetCollectionTitle:String){
        CoroutineScope(Dispatchers.IO).launch {
            showLoadingSpinner("Loading Devices from Database")
            // Initialize the Collection
            var advertisementSetCollection = AdvertisementSetCollection()
            advertisementSetCollection.title = advertisementSetCollectionTitle

            advertisementSetTypes.forEach { advertisementSetType ->
                var titlePrefix = when(advertisementSetType){
                    AdvertisementSetType.ADVERTISEMENT_TYPE_UNDEFINED -> "Undefined"

                    AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_NEW_DEVICE -> "New Device PopUps"
                    AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_NEW_AIRTAG -> "New Airtag PopUps"
                    AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_NOT_YOUR_DEVICE -> "Not your Device PopUps"

                    AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_ACTION_MODALS -> "iOS Action Modals"
                    AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_IOS_17_CRASH -> "iOs 17 Crash"

                    AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_DEVICE -> "Fast Pairing Device"
                    AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_PHONE_SETUP -> "Fast Pairing Phone Setup"
                    AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_NON_PRODUCTION -> "Fast Pairing Non Production"
                    AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_DEBUG -> "Fast Pairing Debug"

                    AdvertisementSetType.ADVERTISEMENT_TYPE_SWIFT_PAIRING -> "Swift Pairing"

                    AdvertisementSetType.ADVERTISEMENT_TYPE_EASY_SETUP_WATCH -> "Easy Setup Watch"
                    AdvertisementSetType.ADVERTISEMENT_TYPE_EASY_SETUP_BUDS -> "Easy Setup Buds"

                    AdvertisementSetType.ADVERTISEMENT_TYPE_LOVESPOUSE_PLAY -> "Lovespouse Play"
                    AdvertisementSetType.ADVERTISEMENT_TYPE_LOVESPOUSE_STOP -> "Lovespouse Stop"
                }

                // Initialize the List
                var advertisementSetList = AdvertisementSetList()
                advertisementSetList.title = "$titlePrefix List"

                var advertisementSets = DatabaseHelpers.getAllAdvertisementSetsForType(advertisementSetType)
                advertisementSetList.advertisementSets = advertisementSets.toMutableList()

                // Add List to the Collection
                advertisementSetCollection.advertisementSetLists.add(advertisementSetList)

                hideLoadingSpinner()
            }

            // Pass Collection to Advertisement Fragment
            navigateToAdvertisementFragment(advertisementSetCollection)
        }
    }

    fun showLoadingSpinner(message:String){
        _viewModel!!.loadingMessage.postValue(message)
        _viewModel!!.isLoading.postValue(true)
    }

    fun hideLoadingSpinner(){
        _viewModel!!.isLoading.postValue(false)
    }

    fun onFastPairCardViewClicked(){
        navigateToAdvertisementFragmentWithType(listOf(AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_DEVICE, AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_PHONE_SETUP, AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_NON_PRODUCTION, AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_DEBUG), "Fast Pair Collection")
    }

    fun onEasySetupCardViewClicked(){
        navigateToAdvertisementFragmentWithType(listOf(
            AdvertisementSetType.ADVERTISEMENT_TYPE_EASY_SETUP_WATCH,
            AdvertisementSetType.ADVERTISEMENT_TYPE_EASY_SETUP_BUDS
        ), "Easy Setup Collection")
    }

    fun onContinuityCardViewClicked(){
        navigateToAdvertisementFragmentWithType(listOf(AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_NEW_DEVICE,AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_NOT_YOUR_DEVICE, AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_NEW_AIRTAG, AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_ACTION_MODALS, AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_IOS_17_CRASH), "Continuity Collection")
    }

    fun onSwiftPairingCardViewClicked(){
        navigateToAdvertisementFragmentWithType(listOf(AdvertisementSetType.ADVERTISEMENT_TYPE_SWIFT_PAIRING), "Swift Pair Collection")
    }

    fun onLovespouseCardViewClicked(){
        navigateToAdvertisementFragmentWithType(listOf(AdvertisementSetType.ADVERTISEMENT_TYPE_LOVESPOUSE_PLAY, AdvertisementSetType.ADVERTISEMENT_TYPE_LOVESPOUSE_STOP), "Lovespouse Collection")
    }

    fun onKitchenSinkCardViewClicked(){
        // Set Random Mode for Kitchen Sink
        AppContext.getAdvertisementSetQueueHandler().setAdvertisementQueueMode(AdvertisementQueueMode.ADVERTISEMENT_QUEUE_MODE_RANDOM)

        navigateToAdvertisementFragmentWithType(listOf(
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

            ), "Kitchen Sink Collection")
    }

    fun navigateToAdvertisementFragment(advertisementSetCollection: AdvertisementSetCollection){
        AppContext.getActivity().runOnUiThread {
            //val bundle = bundleOf("advertisementSetCollection" to advertisementSetCollection)
            val navController = AppContext.getActivity().findNavController(R.id.nav_host_fragment_content_main)
            AppContext.getAdvertisementSetQueueHandler().deactivate()
            AppContext.getAdvertisementSetQueueHandler().setAdvertisementSetCollection(advertisementSetCollection)
            //navController.navigate(R.id.action_nav_start_to_nav_advertisement, bundle)
            navController.navigate(R.id.action_nav_start_to_nav_advertisement)
        }
    }

    fun addMissingRequirement(missingRequirement:String){
        var newList = _viewModel!!.missingRequirements.value!!
        if(!newList.contains(missingRequirement)){
            newList.add(missingRequirement)
        }
        _viewModel!!.missingRequirements.postValue(newList)
    }

    fun removeMissingRequirement(missingRequirement:String){
        var newList = _viewModel!!.missingRequirements.value!!
        newList.remove(missingRequirement)
        _viewModel!!.missingRequirements.postValue(newList)
    }

    fun checkDatabase(){
        CoroutineScope(Dispatchers.IO).launch {
            var result = false
            var database = AppDatabase.getInstance()
            if(database != null){
                removeMissingRequirement("Database is not initialized")
                if(!database.isSeeding && !database.inTransaction()){
                    removeMissingRequirement("Database is Seeding")
                    _viewModel!!.isSeeding.postValue(false)
                    var numberOfAdvertisementSetEntities = database.advertisementSetDao().getAll().count()
                    if(numberOfAdvertisementSetEntities > 0){
                        removeMissingRequirement("Database is empty")
                        result = true
                    } else {
                        addMissingRequirement("Database is empty")
                    }
                } else {
                    addMissingRequirement("Database is Seeding")
                    _viewModel!!.isSeeding.postValue(true)
                }

            } else {
                addMissingRequirement("Database is not initialized")
            }
            _viewModel!!.databaseIsReady.postValue(result)

            if(result == false){
                // Check again in a few seconds
                Executors.newSingleThreadScheduledExecutor().schedule({
                    checkDatabase()
                }, 2, TimeUnit.SECONDS)
            }
        }
    }

    fun checkBluetoothAdapter(promptIfAdapterIsDisabled:Boolean = false){
        var bluetoothIsReady = false
        // Get Bluetooth Adapter
        val bluetoothAdapter:BluetoothAdapter? = AppContext.getContext().bluetoothAdapter()
        if(bluetoothAdapter != null){
            removeMissingRequirement("Bluetooth Adapter not found")
            // Check if Bluetooth Adapter is enabled
                if(bluetoothAdapter.isEnabled){
                    removeMissingRequirement("Bluetooth is disabled")
                    bluetoothIsReady = true
                } else {
                    addMissingRequirement("Bluetooth is disabled")
                    if(promptIfAdapterIsDisabled){
                        promptEnableBluetooth(bluetoothAdapter)
                    }
                }
        } else {
            addMissingRequirement("Bluetooth Adapter not found")
        }

        _viewModel!!.bluetoothAdapterIsReady.postValue(bluetoothIsReady)
    }

    fun promptEnableBluetooth(bluetoothAdapter: BluetoothAdapter){
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        registerForResult.launch(enableBtIntent)
    }

    fun checkRequiredPermissions(promptForNotGranted:Boolean = false){
        val allPermissions = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )

        var notGrantedPermissions:MutableList<String> = mutableListOf()

        allPermissions.forEach {permission ->
            var missingRequirementString = "Permission " + permission.replace("android.permission.", "") + " not granted"
            val isGranted = PermissionCheck.checkPermission(permission, AppContext.getActivity(), false)

            if(isGranted){
               removeMissingRequirement(missingRequirementString)
            } else {
                notGrantedPermissions.add(permission)
                addMissingRequirement(missingRequirementString)
            }
        }

        if(notGrantedPermissions.isEmpty()){
            _viewModel!!.allPermissionsGranted.postValue(true)
        } else {
            _viewModel!!.allPermissionsGranted.postValue(false)
            // Request Missing Permissions
            if(promptForNotGranted){
                //PermissionCheck.requireAllPermissions(AppContext.getActivity(), notGrantedPermissions.toTypedArray())
                activityResultLauncher.launch(notGrantedPermissions.toTypedArray())
            }
        }
    }

    private var activityResultLauncher: ActivityResultLauncher<Array<String>>
    init{
        this.activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()) {result ->
            checkRequiredPermissions(false)
        }
    }

    fun checkAdvertisementService(){
        var advertisementServiceIsReady = true

        if(!AppContext.advertisementServiceIsInitialized()){
            try {
                val advertisementService = BluetoothHelpers.getAdvertisementService()
                AppContext.setAdvertisementService(advertisementService)
            } catch (e:Exception){
                addMissingRequirement("Advertisement Service not initialized")
                advertisementServiceIsReady = false
            }
        }

        if(!AppContext.bluetoothLeScanServiceIsInitialized()){
            try {
                BluetoothLeScanForegroundService.startService(AppContext.getContext(), "Bluetooth LE Scan Foreground Service is running...")
            } catch (e:Exception){
                addMissingRequirement("Bluetooth LE Scan Service not initialized")
                advertisementServiceIsReady = false
            }
        }

        if(!AppContext.advertisementSetQueueHandlerIsInitialized()){
            try {
                var advertisementSetQueueHandler = AdvertisementSetQueueHandler()
                AppContext.setAdvertisementSetQueueHandler(advertisementSetQueueHandler)
            } catch (e:Exception){
                addMissingRequirement("Queue Handler not initialized")
                advertisementServiceIsReady = false
            }
        }

        _viewModel!!.advertisementServiceIsReady.postValue(advertisementServiceIsReady)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(StartViewModel::class.java)
        // TODO: Use the ViewModel
    }


}