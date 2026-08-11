package de.simon.dankelmann.bluetoothlespam.ui.start

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.simon.dankelmann.bluetoothlespam.BleSpamApplication
import de.simon.dankelmann.bluetoothlespam.Database.AppDatabase
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertisementSetCollectionEntity
import de.simon.dankelmann.bluetoothlespam.Helpers.BluetoothHelpers.Companion.bluetoothAdapter
import de.simon.dankelmann.bluetoothlespam.Helpers.BluetoothHelpers.Companion.isBluetooth5Supported
import de.simon.dankelmann.bluetoothlespam.Helpers.DatabaseHelpers
import de.simon.dankelmann.bluetoothlespam.PermissionCheck.PermissionCheck
import de.simon.dankelmann.bluetoothlespam.R
import de.simon.dankelmann.bluetoothlespam.ui.theme.FloatingNavBarClearance
import de.simon.dankelmann.bluetoothlespam.ui.theme.LocalExtendedColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Route entry point (plan §6 step 4) — owns the business logic `StartFragment` used to own. */
@Composable
fun StartRoute(
    onNavigateToAdvertisement: () -> Unit,
    onNavigateToManageQuickStart: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as Activity
    val viewModel: StartViewModel = viewModel()
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var quickStartCollections by remember { mutableStateOf<List<AdvertisementSetCollectionEntity>>(emptyList()) }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            checkBluetoothAdapter(viewModel, activity, null, promptIfAdapterIsDisabled = false)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.appVersion.value = getAppVersion(context)
        viewModel.bluetoothSupport.value = getBluetoothSupportText(context)
        // checkDatabase does synchronous Room queries — LaunchedEffect runs on the Compose/main
        // dispatcher by default, so this must hop to IO explicitly (Room asserts against
        // main-thread access). The original Fragment did this via CoroutineScope(Dispatchers.IO).
        withContext(Dispatchers.IO) {
            checkDatabase(viewModel)
            // Loaded once here -- Quick Start is now a manually pinned list (Manage Quick Start
            // screen), not recency-based, so nothing on this screen changes membership and it
            // never needs to reload on ON_RESUME (e.g. every tab switch back to this screen).
            quickStartCollections = AppDatabase.getInstance().advertisementSetCollectionDao().getQuickStartCollections()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkRequiredPermissions(context, viewModel)
                checkBluetoothAdapter(viewModel, activity, enableBluetoothLauncher, promptIfAdapterIsDisabled = true)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    StartScreen(
        viewModel = viewModel,
        onGrantPermissions = { requestRequiredPermissions(activity) },
        onRecheckPermissions = { checkRequiredPermissions(context, viewModel) },
        onRecheckBluetooth = { checkBluetoothAdapter(viewModel, activity, enableBluetoothLauncher, promptIfAdapterIsDisabled = true) },
        onRecheckDatabase = { scope.launch(Dispatchers.IO) { checkDatabase(viewModel) } },
        quickStartCollections = quickStartCollections,
        onQuickStartItemClicked = { collection ->
            scope.launch(Dispatchers.IO) {
                val database = AppDatabase.getInstance()
                val collectionWithLists = database.advertisementSetCollectionDao().getCollectionWithLists(collection.id)
                database.advertisementSetCollectionDao().updateLastUsedAt(collection.id, System.currentTimeMillis())

                val listEntities = collectionWithLists.lists
                val domainCollection = DatabaseHelpers.buildAdvertisementSetCollectionSkeletonFromEntity(collectionWithLists)
                    .apply { isLoadingSets = true }

                withContext(Dispatchers.Main) {
                    val app = context.applicationContext as BleSpamApplication
                    app.queueHandler.apply {
                        deactivate(context)
                        setAdvertisementSetCollection(domainCollection)
                    }
                    onNavigateToAdvertisement()

                    // Must outlive this composable (which unmounts the instant we navigate away),
                    // so a plain background Thread rather than this coroutine's own scope.
                    Thread {
                        val setsByList = listEntities.map { DatabaseHelpers.getAllAdvertisementSetsForList(it.id) }
                        Handler(Looper.getMainLooper()).post {
                            setsByList.forEachIndexed { index, sets ->
                                domainCollection.advertisementSetLists[index].advertisementSets = sets.toMutableList()
                            }
                            domainCollection.isLoadingSets = false
                            app.queueHandler.setAdvertisementSetCollection(domainCollection)
                        }
                    }.start()
                }
            }
        },
        onEditQuickStartClicked = onNavigateToManageQuickStart,
    )
}

private fun getAppVersion(context: Context): String? {
    return context.packageManager.getPackageInfo(context.packageName, 0).versionName
}

private fun getBluetoothSupportText(context: Context): String {
    return if (context.isBluetooth5Supported()) "Modern & Legacy" else "Legacy only"
}

// .postValue() (not .value =) — called from both the main thread (permission/bluetooth checks)
// and a background thread (checkDatabase, which must run off-main for its Room queries).
// LiveData.setValue() asserts main-thread; postValue() is safe from anywhere.
private fun addMissingRequirement(viewModel: StartViewModel, missingRequirement: String) {
    val newList = viewModel.missingRequirements.value!!
    if (!newList.contains(missingRequirement)) {
        newList.add(missingRequirement)
    }
    viewModel.missingRequirements.postValue(newList)
}

private fun removeMissingRequirement(viewModel: StartViewModel, missingRequirement: String) {
    val newList = viewModel.missingRequirements.value!!
    newList.remove(missingRequirement)
    viewModel.missingRequirements.postValue(newList)
}

private fun checkDatabase(viewModel: StartViewModel) {
    var result = false
    val database = AppDatabase.getInstance()
    if (database != null) {
        removeMissingRequirement(viewModel, "Database is not initialized")
        if (!database.isSeeding && !database.inTransaction()) {
            removeMissingRequirement(viewModel, "Database is Seeding")
            viewModel.isSeeding.postValue(false)
            val numberOfAdvertisementSetEntities = database.advertisementSetDao().getAll().count()
            if (numberOfAdvertisementSetEntities > 0) {
                removeMissingRequirement(viewModel, "Database is empty")
                result = true
            } else {
                addMissingRequirement(viewModel, "Database is empty")
            }
        } else {
            addMissingRequirement(viewModel, "Database is Seeding")
            viewModel.isSeeding.postValue(true)
        }
    } else {
        addMissingRequirement(viewModel, "Database is not initialized")
    }
    viewModel.databaseIsReady.postValue(result)

    if (!result) {
        // Check again in a few seconds
        Executors.newSingleThreadScheduledExecutor().schedule({
            checkDatabase(viewModel)
        }, 2, TimeUnit.SECONDS)
    }
}

private fun checkBluetoothAdapter(
    viewModel: StartViewModel,
    activity: Activity,
    enableBluetoothLauncher: androidx.activity.result.ActivityResultLauncher<Intent>?,
    promptIfAdapterIsDisabled: Boolean,
) {
    viewModel.bluetoothAdapterIsReady.value = false

    val bluetoothAdapter: BluetoothAdapter? = activity.bluetoothAdapter()
    if (bluetoothAdapter != null) {
        removeMissingRequirement(viewModel, "Bluetooth Adapter not found")
        if (bluetoothAdapter.isEnabled) {
            removeMissingRequirement(viewModel, "Bluetooth is disabled")
            viewModel.bluetoothAdapterIsReady.value = true
        } else {
            addMissingRequirement(viewModel, "Bluetooth is disabled")
            if (promptIfAdapterIsDisabled && enableBluetoothLauncher != null &&
                PermissionCheck.checkPermission(android.Manifest.permission.BLUETOOTH_CONNECT, activity)
            ) {
                enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }
        }
    } else {
        addMissingRequirement(viewModel, "Bluetooth Adapter not found")
    }
}

private fun checkRequiredPermissions(context: Context, viewModel: StartViewModel) {
    var allGranted = true

    PermissionCheck.getAllRelevantPermissions().forEach { permission ->
        val missingRequirementString = "Permission " + permission.replace("android.permission.", "") + " not granted"
        if (PermissionCheck.checkPermission(permission, context)) {
            removeMissingRequirement(viewModel, missingRequirementString)
        } else {
            allGranted = false
            addMissingRequirement(viewModel, missingRequirementString)
        }
    }
    viewModel.allPermissionsGranted.value = allGranted

    if (!allGranted) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.missing_permissions_title)
            .setMessage(R.string.missing_permissions_text)
            .setPositiveButton(R.string.missing_permissions_grant) { _, _ -> requestRequiredPermissions(context as Activity) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}

private fun requestRequiredPermissions(activity: Activity) {
    PermissionCheck.requestMissingPermissions(PermissionCheck.getAllRelevantPermissions(), activity)
}

/**
 * Core migration of `StartFragment`'s content (plan §5/§5a/§5b), plus the Quick Start section
 * (plan §8, T6b).
 */
@Composable
fun StartScreen(
    viewModel: StartViewModel,
    onGrantPermissions: () -> Unit,
    onRecheckPermissions: () -> Unit,
    onRecheckBluetooth: () -> Unit,
    onRecheckDatabase: () -> Unit,
    quickStartCollections: List<AdvertisementSetCollectionEntity>,
    onQuickStartItemClicked: (AdvertisementSetCollectionEntity) -> Unit,
    onEditQuickStartClicked: () -> Unit,
) {
    val isSeeding by viewModel.isSeeding.observeAsState(false)
    val appVersion by viewModel.appVersion.observeAsState("0.0.0")
    val androidVersion by viewModel.androidVersion.observeAsState("-")
    val sdkVersion by viewModel.sdkVersion.observeAsState("-")
    val bluetoothSupport by viewModel.bluetoothSupport.observeAsState("-")
    val allPermissionsGranted by viewModel.allPermissionsGranted.observeAsState(false)
    val bluetoothAdapterIsReady by viewModel.bluetoothAdapterIsReady.observeAsState(false)
    val databaseIsReady by viewModel.databaseIsReady.observeAsState(false)
    val missingRequirements by viewModel.missingRequirements.observeAsState(mutableListOf())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // Bottom padding includes FloatingNavBarClearance so the last card can scroll clear of
        // the floating nav bar pill instead of staying stuck underneath it.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp + FloatingNavBarClearance),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (allPermissionsGranted != true) {
            item { PermissionRationaleCard(onGrantPermissions = onGrantPermissions) }
        }

        item {
            SystemInfoCard(
                appVersion = appVersion,
                androidVersion = androidVersion ?: "-",
                sdkVersion = sdkVersion ?: "-",
                bluetoothSupport = bluetoothSupport ?: "-",
                databaseIsReady = databaseIsReady == true,
                isSeeding = isSeeding == true,
            )
        }

        item {
            QuickStartSection(
                quickStartCollections = quickStartCollections,
                onItemClicked = onQuickStartItemClicked,
                onEditClicked = onEditQuickStartClicked,
            )
        }

        item {
            RequirementsCard(
                missingRequirements = missingRequirements ?: emptyList(),
                allPermissionsGranted = allPermissionsGranted == true,
                bluetoothAdapterIsReady = bluetoothAdapterIsReady == true,
                databaseIsReady = databaseIsReady == true,
                onRecheckPermissions = onRecheckPermissions,
                onRecheckBluetooth = onRecheckBluetooth,
                onRecheckDatabase = onRecheckDatabase,
            )
        }
    }
}

@Composable
private fun PermissionRationaleCard(onGrantPermissions: () -> Unit) {
    val extendedColors = LocalExtendedColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = extendedColors.warning),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = extendedColors.onWarning,
                )
                Text(
                    text = stringResource(R.string.missing_permissions_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = extendedColors.onWarning,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Text(
                text = stringResource(R.string.missing_permissions_text),
                style = MaterialTheme.typography.bodyMedium,
                color = extendedColors.onWarning,
                modifier = Modifier.padding(top = 8.dp),
            )
            Button(
                onClick = onGrantPermissions,
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Text(stringResource(R.string.missing_permissions_grant))
            }
        }
    }
}

@Composable
private fun SystemInfoCard(
    appVersion: String?,
    androidVersion: String,
    sdkVersion: String,
    bluetoothSupport: String,
    databaseIsReady: Boolean,
    isSeeding: Boolean,
) {
    val expandedSections = remember { mutableStateMapOf("Device Info" to false, "App Info" to false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
            )

            ExpandableSection(
                title = "Device Info",
                expanded = expandedSections["Device Info"] == true,
                onToggle = { expandedSections["Device Info"] = expandedSections["Device Info"] != true },
            ) {
                InfoRow("Android Version", androidVersion)
                InfoRow("SDK Version", sdkVersion)
                InfoRow("Bluetooth", bluetoothSupport)
            }

            ExpandableSection(
                title = "App Info",
                expanded = expandedSections["App Info"] == true,
                onToggle = { expandedSections["App Info"] = expandedSections["App Info"] != true },
            ) {
                InfoRow("App Version", appVersion ?: "-")
                InfoRow("Database", if (isSeeding) "Seeding…" else if (databaseIsReady) "Ready" else "Not ready")
            }
        }
    }
}

@Composable
private fun ExpandableSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "chevron")

    Column(modifier = Modifier.padding(top = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .semantics {
                    stateDescription = if (expanded) "Expanded" else "Collapsed"
                }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = null,
                modifier = Modifier.rotate(rotation),
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column { content() }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun QuickStartSection(
    quickStartCollections: List<AdvertisementSetCollectionEntity>,
    onItemClicked: (AdvertisementSetCollectionEntity) -> Unit,
    onEditClicked: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "Quick Start", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onEditClicked, modifier = Modifier.size(48.dp)) {
                    Icon(imageVector = Icons.Filled.Edit, contentDescription = "Manage Quick Start")
                }
            }

            if (quickStartCollections.isEmpty()) {
                Text(
                    text = "No Quick Start groups yet — tap the pencil to add some",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            } else {
                quickStartCollections.forEach { collection ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onItemClicked(collection) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = collection.title, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun RequirementsCard(
    missingRequirements: List<String>,
    allPermissionsGranted: Boolean,
    bluetoothAdapterIsReady: Boolean,
    databaseIsReady: Boolean,
    onRecheckPermissions: () -> Unit,
    onRecheckBluetooth: () -> Unit,
    onRecheckDatabase: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Requirements", style = MaterialTheme.typography.titleMedium)
            if (missingRequirements.isEmpty()) {
                Text(
                    text = "All requirements are met",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            } else {
                Text(
                    text = "Missing Requirements:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
                missingRequirements.forEach { requirement ->
                    Text(
                        text = requirement,
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalExtendedColors.current.warning,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }

            Column(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatusRow(
                    label = "Permissions",
                    iconRes = R.drawable.key_24,
                    isReady = allPermissionsGranted,
                    onClick = onRecheckPermissions,
                )
                StatusRow(
                    label = "Bluetooth Adapter",
                    iconRes = R.drawable.bluetooth,
                    isReady = bluetoothAdapterIsReady,
                    onClick = onRecheckBluetooth,
                )
                StatusRow(
                    label = "Database",
                    iconRes = R.drawable.data_array,
                    isReady = databaseIsReady,
                    onClick = onRecheckDatabase,
                )
            }
        }
    }
}

@Composable
private fun StatusRow(
    label: String,
    iconRes: Int,
    isReady: Boolean,
    onClick: () -> Unit,
) {
    val extendedColors = LocalExtendedColors.current
    val (background, onColor) = if (isReady) {
        extendedColors.success to extendedColors.onSuccess
    } else {
        extendedColors.warning to extendedColors.onWarning
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { contentDescription = "$label: ${if (isReady) "OK" else "Needs attention"}" },
        colors = CardDefaults.cardColors(containerColor = background),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = onColor,
                modifier = Modifier.size(28.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = onColor,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
    }
}
