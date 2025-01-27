package de.simon.dankelmann.bluetoothlespam

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.onNavDestinationSelected
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.Constants.Constants
import de.simon.dankelmann.bluetoothlespam.Enums.TxPowerLevel
import de.simon.dankelmann.bluetoothlespam.Enums.toStringId
import de.simon.dankelmann.bluetoothlespam.Helpers.BluetoothHelpers
import de.simon.dankelmann.bluetoothlespam.Helpers.QueueHandlerHelpers
import de.simon.dankelmann.bluetoothlespam.PermissionCheck.PermissionCheck
import de.simon.dankelmann.bluetoothlespam.databinding.ActivityMainBinding
import de.simon.dankelmann.bluetoothlespam.ui.setupEdgeToEdge


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val _logTag = "MainActivity"
    private lateinit var sharedPreferenceChangedListener: OnSharedPreferenceChangeListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // needs to be before setContentView
        enableEdgeToEdge()

        // Initialize AppContext, Activity, Advertisement Service and QueHandler
        AppContext.setContext(applicationContext)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupEdgeToEdge(binding.appBar, bottom = false)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Listen to Preference changes
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        sharedPreferenceChangedListener =
            OnSharedPreferenceChangeListener { sharedPreferences, key ->
                run {
                    var legacyAdvertisingKey =
                        resources.getString(R.string.preference_key_use_legacy_advertising)
                    if (key == legacyAdvertisingKey) {
                        val advertisementService = BluetoothHelpers.getAdvertisementService(this)
                        AppContext.setAdvertisementService(advertisementService)
                        AppContext.getAdvertisementSetQueueHandler()
                            .setAdvertisementService(advertisementService)
                    }

                    var intervalKey =
                        resources.getString(R.string.preference_key_interval_advertising_queue_handler)
                    if (key == intervalKey) {
                        val newInterval = QueueHandlerHelpers.getInterval(this)
                        Log.d(_logTag, "Setting new Interval: $newInterval")
                        AppContext.getAdvertisementSetQueueHandler().setInterval(newInterval)
                    }
                }
            }

        prefs.registerOnSharedPreferenceChangeListener(sharedPreferenceChangedListener)

        setupNavController()
    }

    fun setupNavController() {
        val navController = findNavController(R.id.nav_host_fragment)

        appBarConfiguration = AppBarConfiguration(
            topLevelDestinationIds = setOf(
                R.id.nav_start,
                R.id.nav_advertisement_collection,
                R.id.nav_spam_detector,
            ),
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.nav_start,
                R.id.nav_advertisement_collection,
                R.id.nav_spam_detector
                    -> binding.bottomNav.visibility = View.VISIBLE

                else -> binding.bottomNav.visibility = View.GONE
            }
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

    private fun promptEnableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_CONNECT, this)) {
                startActivityForResult(enableBtIntent, Constants.REQUEST_CODE_ENABLE_BLUETOOTH)
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val menuItems = listOf<MenuItem?>(
            menu?.findItem(R.id.nav_preferences),
            menu?.findItem(R.id.nav_set_tx_power)
        )

        menuItems.forEach { menuItem ->
            val actionSettingsMenuItem = menuItem
            val title = actionSettingsMenuItem?.title.toString()
            val spannable = SpannableString(title)

            val textColor = resources.getColor(R.color.text_color, theme)
            spannable.setSpan(
                ForegroundColorSpan(textColor),
                0,
                spannable.length,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
            actionSettingsMenuItem?.title = spannable
        }

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_preferences -> {
                val navController = findNavController(R.id.nav_host_fragment)
                onNavDestinationSelected(item, navController)
            }

            R.id.nav_set_tx_power -> {
                showSetTxPowerDialog()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    fun showSetTxPowerDialog() {
        if (AppContext.getAdvertisementService() == null) {
            Toast.makeText(this, "Advertisement Service not initialized", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogLayout = LayoutInflater.from(this).inflate(R.layout.dialog_set_tx_power, null)

        val seekBar: SeekBar = dialogLayout.findViewById(R.id.setTxPowerDialogSeekbar)
        val seekBarLabel: TextView = dialogLayout.findViewById(R.id.setTxPowerDialogTxPowerTextView)

        // Set Current TxPowerLevel
        val currentTxPowerLevel = AppContext.getAdvertisementService().getTxPowerLevel()
        val currentProgress = when (currentTxPowerLevel) {
            TxPowerLevel.TX_POWER_HIGH -> 3
            TxPowerLevel.TX_POWER_MEDIUM -> 2
            TxPowerLevel.TX_POWER_LOW -> 1
            TxPowerLevel.TX_POWER_ULTRA_LOW -> 0
        }
        seekBar.progress = currentProgress
        seekBarLabel.text = getString(currentTxPowerLevel.toStringId())

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val newTxPowerLevel = when (progress) {
                    3 -> TxPowerLevel.TX_POWER_HIGH
                    2 -> TxPowerLevel.TX_POWER_MEDIUM
                    1 -> TxPowerLevel.TX_POWER_LOW
                    0 -> TxPowerLevel.TX_POWER_ULTRA_LOW
                    else -> TxPowerLevel.TX_POWER_HIGH
                }
                seekBarLabel.text = getString(newTxPowerLevel.toStringId())
                AppContext.getAdvertisementService()!!.setTxPowerLevel(newTxPowerLevel)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // you can probably leave this empty
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // you can probably leave this empty
            }
        })

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.power_dialog_title))
            .setView(dialogLayout)
            .setPositiveButton(getString(android.R.string.ok), null)
            .show()
    }
}
