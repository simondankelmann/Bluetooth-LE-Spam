package de.simon.dankelmann.bluetoothlespam.ui.start

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.opengl.Visibility
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.preference.PreferenceManager
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.ContinuityActionModalAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.ContinuityDevicePopUpAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.GoogleFastPairAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.IAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.SwiftPairAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext.Companion.bluetoothAdapter
import de.simon.dankelmann.bluetoothlespam.Database.AppDatabase
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetType
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementTarget
import de.simon.dankelmann.bluetoothlespam.Handlers.AdvertisementSetQueueHandler
import de.simon.dankelmann.bluetoothlespam.Helpers.BluetoothHelpers
import de.simon.dankelmann.bluetoothlespam.Helpers.DatabaseHelpers
import de.simon.dankelmann.bluetoothlespam.Interfaces.Services.IAdvertisementService
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSetCollection
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSetList
import de.simon.dankelmann.bluetoothlespam.PermissionCheck.PermissionCheck
import de.simon.dankelmann.bluetoothlespam.R
import de.simon.dankelmann.bluetoothlespam.Services.LegacyAdvertisementService
import de.simon.dankelmann.bluetoothlespam.Services.ModernAdvertisementService
import de.simon.dankelmann.bluetoothlespam.databinding.FragmentStartBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.Exception
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.reflect.typeOf


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

    fun setupUi(){

        // Loading Animation
        val loadingSpinnerLayout: View = binding.startFragmentLoadingSpinnerLayout
        _viewModel!!.isLoading.observe(viewLifecycleOwner) {
            loadingSpinnerLayout.visibility = when(it){
                true -> View.VISIBLE
                false -> View.GONE
            }
        }

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
            textViewBluetoothSupport.text = "Bluetooth Version: $it"
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

        // Fast Pairing Cardview
        val startFragmentFastPairingCard: CardView = binding.startFragmentFastPairingCard
        startFragmentFastPairingCard.setOnClickListener {
            onFastPairingCardViewClicked()
        }

        // Easy Setup Cardview
        val startFragmentEasySetupCard: CardView = binding.startFragmentEasySetupCard
        startFragmentEasySetupCard.setOnClickListener {
            onEasySetupCardViewClicked()
        }

        // Continuity Device PopUps Cardview
        val startFragmentDevicePopUpsCard: CardView = binding.startFragmentDevicePopUpsCard
        startFragmentDevicePopUpsCard.setOnClickListener {
            onDevicePopUpsCardViewClicked()
        }

        // Continuity ActionModals Cardview
        val startFragmentActionModalsCard: CardView = binding.startFragmentActionModalsCard
        startFragmentActionModalsCard.setOnClickListener {
            onActionModalsCardViewClicked()
        }

        // Swift Pairing Cardview
        val startFragmentSwiftPairingCard: CardView = binding.startFragmentSwiftPairingCard
        startFragmentSwiftPairingCard.setOnClickListener {
            onSwiftPairingCardViewClicked()
        }

        // Fast Pairing Cardview
        val startFragmentFastPairingDebugCard: CardView = binding.startFragmentFastPairingDebugCard
        startFragmentFastPairingDebugCard.setOnClickListener {
            onFastPairingDebugCardViewClicked()
        }

        // Kitchen Sink Cardview
        val startFragmentKitchenSinkCard: CardView = binding.startFragmentKitchenSinkCard
        startFragmentKitchenSinkCard.setOnClickListener {
            onKitchenSinkCardViewClicked()
        }
    }

    fun navigateToAdvertisementFragmentWithType(advertisementSetTypes: List<AdvertisementSetType>, advertisementSetCollectionTitle:String){
        CoroutineScope(Dispatchers.IO).launch {
            showLoadingSpinner("Loading Advertisement Sets from Database")
            // Initialize the Collection
            var advertisementSetCollection = AdvertisementSetCollection()
            advertisementSetCollection.title = advertisementSetCollectionTitle

            advertisementSetTypes.forEach { advertisementSetType ->
                var titlePrefix = when(advertisementSetType){
                    AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_DEVICE_POPUPS -> "iOS Device PopUps"
                    AdvertisementSetType.ADVERTISEMENT_TYPE_UNDEFINED -> "Undefined"
                    AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_ACTION_MODALS -> "iOS Action Modals"
                    AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING -> "Fast Pairing"
                    AdvertisementSetType.ADVERTISEMENT_TYPE_SWIFT_PAIRING -> "Swift Pairing"
                    AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_DEBUG -> "Fast Pairing Debug"
                    AdvertisementSetType.ADVERTISEMENT_TYPE_EASY_SETUP -> "Easy Setup"
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

    fun onFastPairingCardViewClicked(){
        navigateToAdvertisementFragmentWithType(listOf(AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING), "Fast Pairing")
    }

    fun onEasySetupCardViewClicked(){
        navigateToAdvertisementFragmentWithType(listOf(AdvertisementSetType.ADVERTISEMENT_TYPE_EASY_SETUP), "Easy Setup")
    }

    fun onDevicePopUpsCardViewClicked(){
        navigateToAdvertisementFragmentWithType(listOf(AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_DEVICE_POPUPS), "iOs Device PopUps")
    }
    fun onActionModalsCardViewClicked(){
        navigateToAdvertisementFragmentWithType(listOf(AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_ACTION_MODALS), "iOs Action Modals")
    }

    fun onSwiftPairingCardViewClicked(){
        navigateToAdvertisementFragmentWithType(listOf(AdvertisementSetType.ADVERTISEMENT_TYPE_SWIFT_PAIRING), "Swift Pairing")
    }

    fun onFastPairingDebugCardViewClicked(){
        navigateToAdvertisementFragmentWithType(listOf(AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_DEBUG), "Fast Pairing Debug")
    }

    fun onKitchenSinkCardViewClicked(){
        navigateToAdvertisementFragmentWithType(listOf(
            AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING,
            AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_DEVICE_POPUPS,
            AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_ACTION_MODALS,
            AdvertisementSetType.ADVERTISEMENT_TYPE_SWIFT_PAIRING,
            AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_DEBUG
            ), "Kitchen Sink")
    }

    fun navigateToAdvertisementFragment(advertisementSetCollection: AdvertisementSetCollection){
        AppContext.getActivity().runOnUiThread {
            val bundle = bundleOf("advertisementSetCollection" to advertisementSetCollection)
            val navController = AppContext.getActivity().findNavController(R.id.nav_host_fragment_content_main)
            navController.navigate(R.id.action_nav_start_to_nav_advertisement, bundle)
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

                if(!database.isSeeding){
                    removeMissingRequirement("Database is Seeding")
                    var numberOfAdvertisementSetEntities = database.advertisementSetDao().getAll().count()
                    if(numberOfAdvertisementSetEntities > 0){
                        removeMissingRequirement("Database is empty")
                        result = true
                    } else {
                        addMissingRequirement("Database is empty")
                    }
                } else {
                    addMissingRequirement("Database is Seeding")
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

        try {
            val advertisementService = BluetoothHelpers.getAdvertisementService()
            AppContext.setAdvertisementService(advertisementService)
        } catch (e:Exception){
            addMissingRequirement("Advertisement Service not initialized")
            advertisementServiceIsReady = false
        }

        try {
            var advertisementSetQueueHandler = AdvertisementSetQueueHandler()
            AppContext.setAdvertisementSetQueueHandler(advertisementSetQueueHandler)
        } catch (e:Exception){
            addMissingRequirement("Queue Handler not initialized")
            advertisementServiceIsReady = false
        }

        _viewModel!!.advertisementServiceIsReady.postValue(advertisementServiceIsReady)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(StartViewModel::class.java)
        // TODO: Use the ViewModel
    }


}