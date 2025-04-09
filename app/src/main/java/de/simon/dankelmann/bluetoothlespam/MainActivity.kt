package de.simon.dankelmann.bluetoothlespam

import android.content.SharedPreferences.OnSharedPreferenceChangeListener
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
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.onNavDestinationSelected
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.Enums.TxPowerLevel
import de.simon.dankelmann.bluetoothlespam.Enums.toStringId
import de.simon.dankelmann.bluetoothlespam.Helpers.LogFileManager
import de.simon.dankelmann.bluetoothlespam.Helpers.QueueHandlerHelpers
import de.simon.dankelmann.bluetoothlespam.databinding.ActivityMainBinding
import de.simon.dankelmann.bluetoothlespam.ui.setupEdgeToEdge


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val _logTag = "MainActivity"
    private lateinit var sharedPreferenceChangedListener: OnSharedPreferenceChangeListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize log file
        LogFileManager.getInstance(applicationContext).initializeLogFile(this)

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
                    val app = (applicationContext as BleSpamApplication)
                    var legacyAdvertisingKey =
                        resources.getString(R.string.preference_key_use_legacy_advertising)
                    if (key == legacyAdvertisingKey) {
                        app.setupAdvertisementService()
                    }

                    var intervalKey =
                        resources.getString(R.string.preference_key_interval_advertising_queue_handler)
                    if (key == intervalKey) {
                        val newInterval = QueueHandlerHelpers.getInterval(this)
                        Log.d(_logTag, "Setting new Interval: $newInterval")
                        app.queueHandler.setInterval(newInterval)
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
        val app = applicationContext as BleSpamApplication

        val dialogLayout = LayoutInflater.from(this).inflate(R.layout.dialog_set_tx_power, null)

        val seekBar: SeekBar = dialogLayout.findViewById(R.id.setTxPowerDialogSeekbar)
        val seekBarLabel: TextView = dialogLayout.findViewById(R.id.setTxPowerDialogTxPowerTextView)

        // Set Current TxPowerLevel
        val currentTxPowerLevel = app.advertisementService.getTxPowerLevel()
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
                app.advertisementService.setTxPowerLevel(newTxPowerLevel)
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
