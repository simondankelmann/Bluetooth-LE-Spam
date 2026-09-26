package de.simon.dankelmann.bluetoothlespam

import android.content.Intent
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.Datastore.SettingsKeys
import de.simon.dankelmann.bluetoothlespam.Datastore.SettingsRepository
import de.simon.dankelmann.bluetoothlespam.Helpers.LogFileManager
import de.simon.dankelmann.bluetoothlespam.Helpers.QueueHandlerHelpers
import de.simon.dankelmann.bluetoothlespam.Helpers.ThemeManager
import de.simon.dankelmann.bluetoothlespam.Navigation.SpecterDestinations
import de.simon.dankelmann.bluetoothlespam.Services.BluetoothLeScanForegroundService
import de.simon.dankelmann.bluetoothlespam.ui.advertisement.AdvertisementRoute
import de.simon.dankelmann.bluetoothlespam.ui.advertisementcollection.AdvertisementCollectionRoute
import de.simon.dankelmann.bluetoothlespam.ui.deviceselector.GroupEditorRoute
import de.simon.dankelmann.bluetoothlespam.ui.preferences.PreferencesRoute
import de.simon.dankelmann.bluetoothlespam.ui.quickstart.ManageQuickStartRoute
import de.simon.dankelmann.bluetoothlespam.ui.spamDetector.SpamDetectorRoute
import de.simon.dankelmann.bluetoothlespam.ui.start.StartRoute
import de.simon.dankelmann.bluetoothlespam.ui.theme.FloatingNavBar
import de.simon.dankelmann.bluetoothlespam.ui.theme.SpecterTheme
import de.simon.dankelmann.bluetoothlespam.ui.theme.floatingNavDestinations
import de.simon.dankelmann.bluetoothlespam.ui.theme.TxPowerSlider
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {

    private val _logTag = "MainActivity"
    private lateinit var sharedPreferenceChangedListener: OnSharedPreferenceChangeListener
    private var seedColorArgb by mutableIntStateOf(ThemeManager.THEME_SEED_COLOR_DEVICE)
    private var pendingDeepLinkDestination by mutableStateOf<String?>(null)

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

        // needs to be before setContent
        enableEdgeToEdge()

        // Initialize AppContext, Activity, Advertisement Service and QueHandler
        AppContext.setContext(applicationContext)

        // Listen to Preference changes
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        seedColorArgb = ThemeManager.getInstance().getSeedColor(this)
        pendingDeepLinkDestination = intent?.getStringExtra(EXTRA_DESTINATION)

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

        // Seed color now lives in DataStore, not SharedPreferences (T11) — recreate() re-runs
        // onCreate top to bottom, which re-reads the seed color for both the classic-View
        // DynamicColorsOptions (cards/icons — those only apply their theme overlay at
        // Activity-creation time) and Compose's seedColorArgb (FloatingNavBar). `drop(1)` skips
        // the value already reflected in this Activity instance at launch.
        lifecycleScope.launch {
            SettingsRepository.getInstance(this@MainActivity).preferencesFlow
                .map { it[SettingsKeys.THEME_SEED_COLOR] ?: ThemeManager.THEME_SEED_COLOR_DEVICE }
                .distinctUntilChanged()
                .drop(1)
                .collect { recreate() }
        }

        // oledActive below is a plain val, read once here, not reactive -- normally that's fine
        // because AppCompatDelegate.setDefaultNightMode() auto-recreates the Activity whenever
        // the night-mode VALUE actually changes. But Dark and OLED both map to the same
        // MODE_NIGHT_YES (applyThemeMode()), so switching directly between them changes no night
        // mode value and nothing recreates -- oledActive silently goes stale until some other
        // mode switch (e.g. to Light, a real night-mode change) happens to recreate anyway. This
        // listener recreates on any THEME_MODE change so Dark<->OLED works on its own.
        lifecycleScope.launch {
            SettingsRepository.getInstance(this@MainActivity).preferencesFlow
                .map { it[SettingsKeys.THEME_MODE] ?: ThemeManager.THEME_MODE_DEFAULT }
                .distinctUntilChanged()
                .drop(1)
                .collect { recreate() }
        }

        val oledActive = ThemeManager.getInstance().isOledActive(this)

        setContent {
            val settingsRepository = remember { SettingsRepository.getInstance(this) }
            val settings by settingsRepository.preferencesFlow.collectAsState()
            val dynamicColorEnabled = settings[SettingsKeys.DYNAMIC_COLOR_ENABLED] ?: true
            val blurEnabled = settings[SettingsKeys.BLUR_ENABLED] ?: true

            SpecterTheme(seedColorArgb = seedColorArgb, amoled = oledActive, dynamicColor = dynamicColorEnabled) {
                val hazeState = remember { HazeState() }
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                // The 4 top-level tabs are pages of one HorizontalPager hosted at START (free
                // finger-tracking swipes); everything else is a detail screen pushed on top.
                val isTopLevel = currentRoute == null || currentRoute == SpecterDestinations.START
                val pagerState = rememberPagerState(pageCount = { floatingNavDestinations.size })
                val coroutineScope = rememberCoroutineScope()

                LaunchedEffect(pendingDeepLinkDestination) {
                    pendingDeepLinkDestination?.let { destination ->
                        val tabIndex = floatingNavDestinations.indexOfFirst { it.route == destination }
                        if (tabIndex != -1) {
                            navController.popBackStack(SpecterDestinations.START, inclusive = false)
                            pagerState.scrollToPage(tabIndex)
                        } else {
                            navController.navigate(destination)
                        }
                        pendingDeepLinkDestination = null
                    }
                }

                // Plain Box, not Scaffold: Scaffold reserves background-painted strips for
                // topBar/bottomBar and pads content away from them. Both bars FLOAT/blur on top
                // of content instead, so content extends the full screen behind both and each
                // screen adds its own top/bottom clearance padding (SpecterTopAppBarClearance,
                // FloatingNavBarClearance) so nothing actually renders underneath either one.
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .hazeSource(state = hazeState),
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = SpecterDestinations.START,
                            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
                            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) },
                            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
                            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
                        ) {
                            composable(SpecterDestinations.START) {
                                // All pages stay composed so each tab keeps its state/scroll
                                // position when swiped away and back.
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxSize(),
                                    beyondViewportPageCount = floatingNavDestinations.lastIndex,
                                ) { page ->
                                    when (floatingNavDestinations[page].route) {
                                        SpecterDestinations.START -> StartRoute(
                                            onNavigateToAdvertisement = { navController.navigate(SpecterDestinations.ADVERTISEMENT) },
                                            onNavigateToManageQuickStart = { navController.navigate(SpecterDestinations.MANAGE_QUICK_START) },
                                        )
                                        SpecterDestinations.ADVERTISEMENT_COLLECTION -> AdvertisementCollectionRoute(
                                            onNavigateToAdvertisement = { navController.navigate(SpecterDestinations.ADVERTISEMENT) },
                                            onCreateCustomGroup = { navController.navigate(SpecterDestinations.GROUP_EDITOR) },
                                        )
                                        SpecterDestinations.SPAM_DETECTOR -> SpamDetectorRoute()
                                        SpecterDestinations.PREFERENCES -> PreferencesRoute(onTxPowerClicked = { showSetTxPowerDialog() })
                                    }
                                }
                            }
                            composable(SpecterDestinations.ADVERTISEMENT) { AdvertisementRoute() }
                            composable(SpecterDestinations.GROUP_EDITOR) {
                                GroupEditorRoute(onSaved = { navController.popBackStack() })
                            }
                            composable(SpecterDestinations.MANAGE_QUICK_START) {
                                ManageQuickStartRoute()
                            }
                        }
                    }

                    SpecterTopAppBar(
                        title = if (isTopLevel) {
                            stringResource(floatingNavDestinations[pagerState.currentPage].labelRes)
                        } else {
                            detailRouteTitles[currentRoute] ?: ""
                        },
                        hazeState = hazeState,
                        blurEnabled = blurEnabled,
                        onBackClicked = if (isTopLevel) null else ({ navController.navigateUp() }),
                        modifier = Modifier.align(Alignment.TopCenter),
                    )

                    if (isTopLevel) {
                        FloatingNavBar(
                            selectedIndex = pagerState.currentPage,
                            onTabSelected = { index -> coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                            hazeState = hazeState,
                            blurEnabled = blurEnabled,
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDeepLinkDestination = intent.getStringExtra(EXTRA_DESTINATION)
    }

    // Single-Activity app, so onStop() is a reliable "app left the foreground" signal. The
    // "Detect Spam in Background" preference (default off) gates whether scanning is allowed to
    // keep running past this point -- off means stop it here, matching the setting's summary
    // text ("Keep spam detection running once the app leaves the foreground").
    override fun onStop() {
        super.onStop()
        val scanService = (applicationContext as BleSpamApplication).scanService
        if (scanService.isScanning()) {
            val backgroundDetectionEnabled =
                SettingsRepository.getInstance(this).current[SettingsKeys.SPAM_DETECTION_BACKGROUND_ENABLED] ?: false
            if (!backgroundDetectionEnabled) {
                BluetoothLeScanForegroundService.stopService(this)
            }
        }
    }

    // Called from PreferencesRoute now that TX power moved out of the toolbar menu.
    fun showSetTxPowerDialog() {
        val app = applicationContext as BleSpamApplication
        val currentTxPowerLevel = app.advertisementService.getTxPowerLevel()

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val isOled = ThemeManager.getInstance().isOledActive(this@MainActivity)
                SpecterTheme(
                    darkTheme = isOled || isSystemInDarkTheme(),
                    seedColorArgb = ThemeManager.getInstance().getSeedColor(this@MainActivity),
                    amoled = isOled,
                    dynamicColor = ThemeManager.getInstance().isDynamicColorEnabled(this@MainActivity),
                ) {
                    TxPowerSlider(
                        initialLevel = currentTxPowerLevel,
                        onLevelChanged = { newTxPowerLevel ->
                            app.advertisementService.setTxPowerLevel(newTxPowerLevel)
                        },
                    )
                }
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.power_dialog_title))
            .setView(composeView)
            .setPositiveButton(getString(android.R.string.ok), null)
            .show()
    }

    companion object {
        /** Set on the launch [Intent] by the foreground-service notifications to deep-link into
         * a specific [SpecterDestinations] route once the app (re)opens — see
         * `AdvertisementForegroundService`/`BluetoothLeScanForegroundService`. */
        const val EXTRA_DESTINATION = "destination"
    }
}

private val detailRouteTitles = mapOf(
    SpecterDestinations.ADVERTISEMENT to "Advertisement",
    SpecterDestinations.GROUP_EDITOR to "Create custom group",
    SpecterDestinations.MANAGE_QUICK_START to "Manage Quick Start",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpecterTopAppBar(
    title: String,
    hazeState: HazeState,
    blurEnabled: Boolean,
    onBackClicked: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val hazeStyle = HazeMaterials.thin()
    val blurModifier = if (blurEnabled) Modifier.hazeEffect(state = hazeState) { style = hazeStyle } else Modifier

    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (onBackClicked != null) {
                IconButton(onClick = onBackClicked) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            // Transparent + blur when it renders; otherwise the same opaque tonal fallback the
            // floating nav bar uses, so the title stays legible either way.
            containerColor = if (blurEnabled) {
                Color.Transparent
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.96f)
            },
        ),
        // No statusBarsPadding here: TopAppBar's own default windowInsets already keep the
        // title/back-arrow below the status bar while letting its background (and the blur)
        // paint all the way up through it, for a real edge-to-edge frosted bar.
        modifier = modifier.fillMaxWidth().then(blurModifier),
    )
}
