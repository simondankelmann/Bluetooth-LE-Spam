package de.simon.dankelmann.bluetoothlespam.ui.start

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.Database.AppDatabase
import de.simon.dankelmann.bluetoothlespam.Handlers.AdvertisementSetQueueHandler
import de.simon.dankelmann.bluetoothlespam.Helpers.BluetoothHelpers
import de.simon.dankelmann.bluetoothlespam.Helpers.BluetoothHelpers.Companion.bluetoothAdapter
import de.simon.dankelmann.bluetoothlespam.Helpers.BluetoothHelpers.Companion.isBluetooth5Supported
import de.simon.dankelmann.bluetoothlespam.PermissionCheck.PermissionCheck
import de.simon.dankelmann.bluetoothlespam.R
import de.simon.dankelmann.bluetoothlespam.databinding.FragmentStartBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.Exception
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class StartFragment : Fragment() {

    private val _logTag = "StartFragment"
    private lateinit var registerForResult:ActivityResultLauncher<Intent>

    private var _viewModel: StartViewModel? = null
    private val viewModel get() = _viewModel!!

    private var _binding: FragmentStartBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        _viewModel = ViewModelProvider(this)[StartViewModel::class.java]
        _binding = FragmentStartBinding.inflate(inflater, container, false)
        val root: View = binding.root

        viewModel.appVersion.postValue(getAppVersion(root.context))
        viewModel.bluetoothSupport.postValue(getBluetoothSupportText(root.context))

        // register for bt enable callback
        registerForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data
                // Handle the Intent
                checkBluetoothAdapter(false)
            }
        }

        setupUi(root.context)

        checkRequiredPermissions(true)
        checkBluetoothAdapter(true)
        checkAdvertisementService(root.context)
        checkDatabase()

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun getAppVersion(context: Context): String? {
        val manager = context.packageManager
        val info = manager.getPackageInfo(context.packageName, 0)
        val version = info.versionName
        return version
    }

    fun getBluetoothSupportText(context: Context): String {
        if (context.isBluetooth5Supported()) {
            return "Modern & Legacy"
        } else {
            return "Legacy only"
        }
    }

    fun setupUi(context: Context){
        // Seeding Animation
        val seedingAnimationView: View = binding.startFragmentDatabaseCardSeedingAnimation
        val databaseImageView: View = binding.startFragmentDatabaseCardIcon
        viewModel.isSeeding.observe(viewLifecycleOwner) {
            seedingAnimationView.visibility = when(it){
                true -> View.VISIBLE
                false -> View.GONE
            }

            databaseImageView.visibility = when(it){
                true -> View.GONE
                false -> View.VISIBLE
            }
        }

        // App Version
        val textViewAppVersion: TextView = binding.infoCard.startFragmentTextViewAppVersion
        viewModel.appVersion.observe(viewLifecycleOwner) {
            textViewAppVersion.text = "App Version: $it"
        }

        // Android Version
        val textViewAndroidVersion: TextView = binding.infoCard.startFragmentTextViewAndroidVersion
        viewModel.androidVersion.observe(viewLifecycleOwner) {
            textViewAndroidVersion.text = "Android Version: $it"
        }

        // SDK Version
        val textViewSdkVersion: TextView = binding.infoCard.startFragmentTextViewSdkVersion
        viewModel.sdkVersion.observe(viewLifecycleOwner) {
            textViewSdkVersion.text = "SDK Version: $it"
        }

        // Bluetooth Support
        val textViewBluetoothSupport: TextView = binding.infoCard.startFragmentTextViewBluetooth
        viewModel.bluetoothSupport.observe(viewLifecycleOwner) {
            textViewBluetoothSupport.text = "Bluetooth: $it"
        }

        // Missing Requirements Text
        val textViewRequirementsDescription: TextView = binding.startFragmentRequirementsTextView
        val startFragmentMissingRequirementsTextView:  TextView = binding.startFragmentMissingRequirementsTextView
        viewModel.missingRequirements.observe(viewLifecycleOwner) {missingRequirementsList ->
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

        val successBackground =
            ResourcesCompat.getDrawable(resources, R.drawable.gradient_success, context.theme)
        val errorBackground =
            ResourcesCompat.getDrawable(resources, R.drawable.gradient_error, context.theme)

        // Permissions CardView Content
        val startFragmentPermissionCardViewContentWrapper: LinearLayout = binding.startFragmentPermissionCardViewContentWrapper
        viewModel.allPermissionsGranted.observe(viewLifecycleOwner) {
            if(it == true){
                startFragmentPermissionCardViewContentWrapper.background = successBackground
            } else {
                startFragmentPermissionCardViewContentWrapper.background = errorBackground
            }
        }

        // Bluetooth CardView
        val startFragmentBluetoothCardView: CardView = binding.startFragmentBluetoothCardview
        startFragmentBluetoothCardView.setOnClickListener {
            checkBluetoothAdapter(true)
        }

        // Bluetooth CardView Content
        val startFragmentBluetoothCardViewContentWrapper: LinearLayout = binding.startFragmentBluetoothCardViewContentWrapper
        viewModel.bluetoothAdapterIsReady.observe(viewLifecycleOwner) {
            if(it == true){
                startFragmentBluetoothCardViewContentWrapper.background = successBackground
            } else {
                startFragmentBluetoothCardViewContentWrapper.background = errorBackground
            }
        }

        // Service CardView
        val startFragmentServiceCardview: CardView = binding.startFragmentServiceCardview
        startFragmentServiceCardview.setOnClickListener {
            checkAdvertisementService(startFragmentServiceCardview.context)
        }

        // Service CardView Content
        val startFragmentServiceCardViewContentWrapper: LinearLayout = binding.startFragmentServiceCardViewContentWrapper
        viewModel.advertisementServiceIsReady.observe(viewLifecycleOwner) {
            if(it == true){
                startFragmentServiceCardViewContentWrapper.background = successBackground
            } else {
                startFragmentServiceCardViewContentWrapper.background = errorBackground
            }
        }

        // Database CardView
        val startFragmentDatabaseCardview: CardView = binding.startFragmentDatabaseCardview
        startFragmentDatabaseCardview.setOnClickListener {
            checkDatabase()
        }

        // Service CardView Content
        val startFragmentDatabaseCardViewContentWrapper: LinearLayout = binding.startFragmentDatabaseCardViewContentWrapper
        viewModel.databaseIsReady.observe(viewLifecycleOwner) {
            if(it == true){
                startFragmentDatabaseCardViewContentWrapper.background = successBackground
            } else {
                startFragmentDatabaseCardViewContentWrapper.background = errorBackground
            }
        }
    }

    fun addMissingRequirement(missingRequirement:String){
        var newList = viewModel.missingRequirements.value!!
        if(!newList.contains(missingRequirement)){
            newList.add(missingRequirement)
        }
        viewModel.missingRequirements.postValue(newList)
    }

    fun removeMissingRequirement(missingRequirement:String){
        var newList = viewModel.missingRequirements.value!!
        newList.remove(missingRequirement)
        viewModel.missingRequirements.postValue(newList)
    }

    fun checkDatabase(){
        CoroutineScope(Dispatchers.IO).launch {
            var result = false
            var database = AppDatabase.getInstance()
            if(database != null){
                removeMissingRequirement("Database is not initialized")
                if(!database.isSeeding && !database.inTransaction()){
                    removeMissingRequirement("Database is Seeding")
                    viewModel.isSeeding.postValue(false)
                    var numberOfAdvertisementSetEntities = database.advertisementSetDao().getAll().count()
                    if(numberOfAdvertisementSetEntities > 0){
                        removeMissingRequirement("Database is empty")
                        result = true
                    } else {
                        addMissingRequirement("Database is empty")
                    }
                } else {
                    addMissingRequirement("Database is Seeding")
                    viewModel.isSeeding.postValue(true)
                }

            } else {
                addMissingRequirement("Database is not initialized")
            }
            viewModel.databaseIsReady.postValue(result)

            if(result == false){
                // Check again in a few seconds
                Executors.newSingleThreadScheduledExecutor().schedule({
                    checkDatabase()
                }, 2, TimeUnit.SECONDS)
            }
        }
    }

    fun checkBluetoothAdapter(promptIfAdapterIsDisabled:Boolean = false){
        val activity = requireActivity()
        viewModel.bluetoothAdapterIsReady.postValue(false)

        val bluetoothAdapter: BluetoothAdapter? = activity.bluetoothAdapter()
        if (bluetoothAdapter != null) {
            removeMissingRequirement("Bluetooth Adapter not found")
            if (bluetoothAdapter.isEnabled) {
                removeMissingRequirement("Bluetooth is disabled")
                viewModel.bluetoothAdapterIsReady.postValue(true)
            } else {
                addMissingRequirement("Bluetooth is disabled")
                if (promptIfAdapterIsDisabled) {
                    if (PermissionCheck.checkPermissionAndRequest(
                                Manifest.permission.BLUETOOTH_CONNECT, activity
                            )
                        ) {
                            promptEnableBluetooth(bluetoothAdapter)
                        }
                    }
                }
        } else {
            addMissingRequirement("Bluetooth Adapter not found")
        }
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
        )

        val notGrantedPermissions: MutableList<String> = mutableListOf()

        allPermissions.forEach { permission ->
            var missingRequirementString =
                "Permission " + permission.replace("android.permission.", "") + " not granted"
            val activity = requireActivity()
            val isGranted = PermissionCheck.checkPermissionAndRequest(permission, activity)

            if (isGranted) {
               removeMissingRequirement(missingRequirementString)
            } else {
                notGrantedPermissions.add(permission)
                addMissingRequirement(missingRequirementString)
            }
        }

        if(notGrantedPermissions.isEmpty()){
            val backgroundLocationAccessIsGranted = checkBackgroundLocationAccessPermission(promptForNotGranted)
            var missingRequirementStringBgLocation = "Permission " + Manifest.permission.ACCESS_BACKGROUND_LOCATION.replace("android.permission.", "") + " not granted"
            if(backgroundLocationAccessIsGranted){
                removeMissingRequirement(missingRequirementStringBgLocation)
                viewModel.allPermissionsGranted.postValue(true)
            } else {
                addMissingRequirement(missingRequirementStringBgLocation)
                checkBackgroundLocationAccessPermission(true)
            }
        } else {
            viewModel.allPermissionsGranted.postValue(false)
            // Request Missing Permissions
            if(promptForNotGranted){
                //PermissionCheck.requireAllPermissions(AppContext.getActivity(), notGrantedPermissions.toTypedArray())
                activityResultLauncher.launch(notGrantedPermissions.toTypedArray())
            }
        }
    }

    fun checkBackgroundLocationAccessPermission(promptForNotGranted:Boolean = false):Boolean{
        val isGranted = PermissionCheck.checkPermission(
            Manifest.permission.ACCESS_BACKGROUND_LOCATION, requireContext()
        )
        if (promptForNotGranted) {
            //PermissionCheck.requireAllPermissions(AppContext.getActivity(), notGrantedPermissions.toTypedArray())
            activityResultLauncher.launch(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
        }
        return isGranted
    }

    private var activityResultLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()) {result ->
            checkRequiredPermissions(false)
        }

    fun checkAdvertisementService(context: Context){
        var advertisementServiceIsReady = true

        if(!AppContext.advertisementServiceIsInitialized()){
            try {
                val advertisementService = BluetoothHelpers.getAdvertisementService(context)
                AppContext.setAdvertisementService(advertisementService)
            } catch (e:Exception){
                addMissingRequirement("Advertisement Service not initialized")
                advertisementServiceIsReady = false
            }
        }

        if(!AppContext.advertisementSetQueueHandlerIsInitialized()){
            try {
                val service = AppContext.getAdvertisementService()
                var advertisementSetQueueHandler = AdvertisementSetQueueHandler(context, service)
                AppContext.setAdvertisementSetQueueHandler(advertisementSetQueueHandler)
            } catch (e:Exception){
                addMissingRequirement("Queue Handler not initialized")
                advertisementServiceIsReady = false
            }
        }

        viewModel.advertisementServiceIsReady.postValue(advertisementServiceIsReady)
    }
}
