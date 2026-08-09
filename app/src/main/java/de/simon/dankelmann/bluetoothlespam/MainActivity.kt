package de.simon.dankelmann.bluetoothlespam

import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.Enums.TxPowerLevel
import de.simon.dankelmann.bluetoothlespam.Enums.toStringId
import de.simon.dankelmann.bluetoothlespam.Helpers.LogFileManager
import de.simon.dankelmann.bluetoothlespam.Helpers.QueueHandlerHelpers
import de.simon.dankelmann.bluetoothlespam.Helpers.ThemeManager
import de.simon.dankelmann.bluetoothlespam.databinding.ActivityMainBinding
import de.simon.dankelmann.bluetoothlespam.ui.theme.FloatingNavBar
import de.simon.dankelmann.bluetoothlespam.ui.theme.SpecterTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private var navController: NavController? = null
    private var navShellInitialized = false
    private val topLevelDestinationIds = setOf(
        R.id.nav_start,
        R.id.nav_advertisement_collection,
        R.id.nav_spam_detector,
        R.id.nav_preferences,
    )
    private val _logTag = "MainActivity"
    private lateinit var sharedPreferenceChangedListener: OnSharedPreferenceChangeListener
    private var seedColorArgb by mutableIntStateOf(ThemeManager.THEME_SEED_COLOR_DEVICE)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate() — DynamicColors applies its theme overlay at
        // Activity-creation time. Sourced from the theme-picker's custom seed color when one is
        // set, falling back to the real device wallpaper (the default) otherwise. This is what
        // makes the classic-View screens (cards, icons) follow the picked color, not just the
        // Compose FloatingNavBar.
        val seedColorPref = ThemeManager.getInstance().getSeedColor(this)
        val dynamicColorsOptionsBuilder = DynamicColorsOptions.Builder()
        if (seedColorPref != ThemeManager.THEME_SEED_COLOR_DEVICE) {
            dynamicColorsOptionsBuilder.setContentBasedSource(seedColorPref)
        }
        DynamicColors.applyToActivityIfAvailable(this, dynamicColorsOptionsBuilder.build())

        // Forces classic-View surfaces to pure black to match SpecterTheme's Compose-side amoled
        // override — DynamicColorsOptions has no native "force pure black" mode, so this layers
        // on top of it as a plain theme overlay.
        if (ThemeManager.getInstance().isOledActive(this)) {
            theme.applyStyle(R.style.ThemeOverlay_Specter_Amoled, true)
        }

        super.onCreate(savedInstanceState)

        // Initialize log file
        LogFileManager.getInstance(applicationContext).initializeLogFile(this)

        // needs to be before setContentView
        enableEdgeToEdge()

        // Initialize AppContext, Activity, Advertisement Service and QueHandler
        AppContext.setContext(applicationContext)

        // Listen to Preference changes
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        seedColorArgb = ThemeManager.getInstance().getSeedColor(this)

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

                    if (key == ThemeManager.THEME_SEED_COLOR_KEY) {
                        // recreate() re-runs onCreate top to bottom, which re-reads the seed
                        // color for both the classic-View DynamicColorsOptions (cards/icons —
                        // those only apply their theme overlay at Activity-creation time) and
                        // Compose's seedColorArgb (FloatingNavBar) — one mechanism covers both.
                        recreate()
                    }
                }
            }

        prefs.registerOnSharedPreferenceChangeListener(sharedPreferenceChangedListener)

        appBarConfiguration = AppBarConfiguration(topLevelDestinationIds = topLevelDestinationIds)

        val oledActive = ThemeManager.getInstance().isOledActive(this)

        setContent {
            SpecterTheme(seedColorArgb = seedColorArgb, amoled = oledActive) {
                val hazeState = remember { HazeState() }
                // TODO(T11): back these with the DataStore-backed settings repository once the
                // Preferences screen migration lands — defaults match plan §2/§4 for now.
                val dynamicColorEnabled = true
                var blurEnabled by remember { mutableStateOf(true) }
                var bottomNavVisible by remember { mutableStateOf(true) }
                var currentNavController by remember { mutableStateOf<NavController?>(null) }

                // Plain Box, not Scaffold: Scaffold reserves a background-painted strip for
                // bottomBar and pads content away from it. A FLOATING nav bar means content
                // extends the full screen and the pill overlays on top — nothing but the pill
                // itself should paint a background down here.
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidViewBinding(
                        factory = ActivityMainBinding::inflate,
                        modifier = Modifier
                            .fillMaxSize()
                            .hazeSource(state = hazeState),
                    ) {
                        if (!navShellInitialized) {
                            navShellInitialized = true
                            setSupportActionBar(toolbar)
                            supportActionBar?.setDisplayShowTitleEnabled(false)

                            val navHostFragment = supportFragmentManager
                                .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                            val navHostView = navHostFragment.requireView()
                            val nc = navHostFragment.navController
                            navController = nc
                            currentNavController = nc

                            setupActionBarWithNavController(nc, appBarConfiguration)

                            // appBar only exists now for the back arrow on detail screens —
                            // when it's hidden (top-level destinations), whichever view is
                            // actually at the top of the screen needs to absorb the status-bar
                            // inset instead, or content draws underneath the status bar/clock.
                            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
                                val bars = insets.getInsets(
                                    androidx.core.view.WindowInsetsCompat.Type.systemBars() +
                                        androidx.core.view.WindowInsetsCompat.Type.displayCutout(),
                                )
                                view.setPadding(bars.left, 0, bars.right, 0)
                                val appBarShown = appBar.visibility == View.VISIBLE
                                appBar.setPadding(0, if (appBarShown) bars.top else 0, 0, 0)
                                navHostView.setPadding(0, if (appBarShown) 0 else bars.top, 0, 0)
                                insets
                            }

                            nc.addOnDestinationChangedListener { _, destination, _ ->
                                val isTopLevel = destination.id in topLevelDestinationIds
                                bottomNavVisible = isTopLevel
                                appBar.visibility = if (isTopLevel) View.GONE else View.VISIBLE
                                androidx.core.view.ViewCompat.requestApplyInsets(root)
                            }
                        }
                    }

                    if (bottomNavVisible) {
                        currentNavController?.let { nc ->
                            FloatingNavBar(
                                navController = nc,
                                hazeState = hazeState,
                                blurEnabled = blurEnabled,
                                modifier = Modifier.align(Alignment.BottomCenter),
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val nc = navController ?: return super.onSupportNavigateUp()
        return nc.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    // Called from PreferencesFragment now that TX power moved out of the toolbar menu.
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
