package de.simon.dankelmann.bluetoothlespam

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertisingSetCallback
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.google.android.material.navigation.NavigationView
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext.Companion.bluetoothAdapter
import de.simon.dankelmann.bluetoothlespam.Constants.Constants
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementError
import de.simon.dankelmann.bluetoothlespam.Handlers.AdvertisementSetQueueHandler
import de.simon.dankelmann.bluetoothlespam.Interfaces.Services.IAdvertisementService
import de.simon.dankelmann.bluetoothlespam.PermissionCheck.PermissionCheck
import de.simon.dankelmann.bluetoothlespam.Services.LegacyAdvertisementService
import de.simon.dankelmann.bluetoothlespam.Services.ModernAdvertisementService
import de.simon.dankelmann.bluetoothlespam.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val _logTag = "MainActivity"
    private lateinit var sharedPreferenceChangedListener:OnSharedPreferenceChangeListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize AppContext, Activity, Advertisement Service and QueHandler
        AppContext.setContext(this)
        AppContext.setActivity(this)

        initializeBluetooth()

        // Require all permissions at startup
        this.requestAllPermissions()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Custom toolbar
        val toolbar = findViewById<Toolbar>(R.id.customToolbar)
        setSupportActionBar(toolbar)

        /*
        supportActionBar!!.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar!!.setDisplayShowCustomEnabled(true)
        supportActionBar!!.setCustomView(R.layout.custom_actionbar)
*/
        //setSupportActionBar(binding.appBarMain.toolbar)

        // Listen to Preference changes
        var prefs = PreferenceManager.getDefaultSharedPreferences(this);

        sharedPreferenceChangedListener = OnSharedPreferenceChangeListener { sharedPreferences, key ->
            run {
                var legacyAdvertisingKey = AppContext.getActivity().resources.getString(R.string.preference_key_use_legacy_advertising)
                if (key == legacyAdvertisingKey) {
                    val advertisementService = getAdvertisementService()
                    AppContext.setAdvertisementService(advertisementService)
                    AppContext.getAdvertisementSetQueueHandler().setAdvertisementService(advertisementService)
                }
            }
        }

        prefs.registerOnSharedPreferenceChangeListener(sharedPreferenceChangedListener);

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_start,
                R.id.nav_fast_pairing,
                R.id.nav_swift_pair,
                R.id.nav_continuity_action_modals,
                R.id.nav_continuity_device_popups,
                R.id.nav_kitchen_sink
            ), drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        logHardwareDetails()

    }

    fun initializeBluetooth():Boolean{
        var bluetoothAdapter = AppContext.getContext().bluetoothAdapter()
        if(bluetoothAdapter != null){
            val advertisementService = getAdvertisementService()
            AppContext.setAdvertisementService(advertisementService)

            var advertisementSetQueueHandler = AdvertisementSetQueueHandler()
            AppContext.setAdvertisementSetQueueHandler(advertisementSetQueueHandler)
            return true
        }
        return false
    }

    private fun getAdvertisementService() : IAdvertisementService{

        var useLegacyAdvertisementService = true // <-- DEFAULT

        // Get from Settings, if present
        val preferences = PreferenceManager.getDefaultSharedPreferences(AppContext.getContext()).all
        preferences.forEach {
            if(it.key == AppContext.getActivity().resources.getString(R.string.preference_key_use_legacy_advertising)){
                useLegacyAdvertisementService = it.value as Boolean
            }
        }

        val advertisementService = when (useLegacyAdvertisementService) {
            true -> LegacyAdvertisementService()
            else -> {ModernAdvertisementService()}
        }

        Log.d(_logTag, "Setting up Advertisement Service. Legacy = $useLegacyAdvertisementService")

        return advertisementService
    }

    private fun logHardwareDetails(){
        var bluetoothAdapter:BluetoothAdapter = bluetoothAdapter
        if(bluetoothAdapter != null){
            Log.d(_logTag, "---------------Bluetooth Adapter Info: -----------")
            Log.d(_logTag, "isLeCodedPhySupported : " + bluetoothAdapter.isLeCodedPhySupported)
        }
    }

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    fun Context.hasPermission(permissionType: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permissionType) ==
                PackageManager.PERMISSION_GRANTED
    }

    fun Context.hasRequiredRuntimePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPermission(Manifest.permission.BLUETOOTH_SCAN) &&
                    hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    override fun onResume() {
        super.onResume()
        if (!bluetoothAdapter.isEnabled) {
            promptEnableBluetooth()
        }
    }

    private fun promptEnableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_CONNECT, this)){
                startActivityForResult(enableBtIntent, Constants.REQUEST_CODE_ENABLE_BLUETOOTH)
            }
        }
    }

    fun requestAllPermissions(){

        val allPermissions = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,

            /*Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,*/
        )

        PermissionCheck.requireAllPermissions(this, allPermissions)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        grantResults.forEachIndexed { index, element ->
            var permission = ""
            if(permissions.get(index) != null){
                permission = permissions.get(index)!!
            }

            if (element == PackageManager.PERMISSION_GRANTED) {
                Log.d(_logTag, "Permission granted for: ${permission}, Request Code: ${requestCode}")
            } else {
                Log.d(_logTag, "Permission denied for: ${permission}, Request Code: ${requestCode}")
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.action_settings -> {
                val navController = findNavController(R.id.nav_host_fragment_content_main)
                navController.navigate(R.id.open_settings_fragment)
            }
        }

        return super.onOptionsItemSelected(item)
    }
}