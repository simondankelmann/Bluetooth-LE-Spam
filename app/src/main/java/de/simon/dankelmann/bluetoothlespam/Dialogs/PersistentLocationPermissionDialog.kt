package de.simon.dankelmann.bluetoothlespam.Dialogs

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import de.simon.dankelmann.bluetoothlespam.PermissionCheck.PermissionCheck
import de.simon.dankelmann.bluetoothlespam.R
import android.util.Log

class PersistentLocationPermissionDialog(private val activity: AppCompatActivity) {
    
    private val TAG = "LocationPermDialog"
    private var dialog: Dialog? = null
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var backgroundPermissionLauncher: ActivityResultLauncher<String>
    private val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
    private val PREF_KEY_DIALOG_SHOWN = "location_permission_dialog_shown"
    
    init {
        // Initialize permission launcher for foreground location
        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                // If foreground permission is granted, check if we need to request background permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // For Android 10+ we need to request background permission separately
                    requestBackgroundLocationPermission()
                } else {
                    // For older Android versions, foreground permission is sufficient
                    dismissDialog()
                    setDialogShownPreference(false)
                }
            } else {
                // Permission still denied, keep dialog showing
                showPermissionExplanationDialog()
                setDialogShownPreference(true)
            }
        }
        
        // Initialize permission launcher for background location
        backgroundPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            Log.d(TAG, "Background location permission granted: $isGranted")
            if (isGranted) {
                // Permission granted, no need to show dialog anymore
                setDialogShownPreference(false)
            } else {
                // Permission denied, keep showing dialog on next app start
                setDialogShownPreference(true)
            }
            dismissDialog()
        }
    }
    
    fun checkAndRequestLocationPermission() {
        val locationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Manifest.permission.BLUETOOTH_SCAN
        } else {
            Manifest.permission.ACCESS_FINE_LOCATION
        }
        
        // Check if we need background location permission
        val needsBackgroundPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && 
                !PermissionCheck.checkPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION, activity)
        
        // If either permission is missing or dialog was previously shown but dismissed without granting
        if (!PermissionCheck.checkPermission(locationPermission, activity) || 
            (needsBackgroundPermission && shouldShowDialog())) {
            showPermissionExplanationDialog()
            setDialogShownPreference(true)
        } else if (needsBackgroundPermission) {
            // If we only need background permission
            showBackgroundPermissionExplanationDialog()
            setDialogShownPreference(true)
        } else {
            // All permissions granted
            setDialogShownPreference(false)
        }
    }
    
    private fun shouldShowDialog(): Boolean {
        return sharedPreferences.getBoolean(PREF_KEY_DIALOG_SHOWN, false)
    }
    
    private fun setDialogShownPreference(shown: Boolean) {
        sharedPreferences.edit().putBoolean(PREF_KEY_DIALOG_SHOWN, shown).apply()
    }
    
    private fun showPermissionExplanationDialog() {
        if (dialog != null && dialog!!.isShowing) {
            return
        }
        
        dialog = Dialog(activity, R.style.Theme_BluetoothLESpam_Dialog)
        dialog!!.setContentView(R.layout.dialog_persistent_location_permission)
        
        // Prevent dialog from being dismissed by back button or touching outside
        dialog!!.setCancelable(false)
        dialog!!.setCanceledOnTouchOutside(false)
        
        // Prevent dialog from being dismissed with back button
        dialog!!.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                // Consume the event but don't dismiss
                Log.d(TAG, "Back button pressed, preventing dialog dismissal")
                true
            } else {
                false
            }
        }
        
        // Set up the buttons
        val grantButton = dialog!!.findViewById<Button>(R.id.btn_grant_permission)
        val settingsButton = dialog!!.findViewById<Button>(R.id.btn_open_settings)
        
        grantButton.setOnClickListener {
            // Request the permission directly using the same approach as in StartFragment
            val locationPermission = Manifest.permission.ACCESS_FINE_LOCATION
            
            Log.d(TAG, "Requesting location permission: $locationPermission")
            permissionLauncher.launch(locationPermission)
        }
        
        settingsButton.text = "Close App"
        settingsButton.setOnClickListener {
            activity.finishAffinity()
        }
        
        // Show dialog and make it persistent
        dialog!!.show()
        
        // Make sure dialog stays on top
        dialog!!.window?.apply {
            setFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }
    
    private fun requestLocationPermission() {
        // Always use ACCESS_FINE_LOCATION permission when grant button is pressed
        val locationPermission = Manifest.permission.ACCESS_FINE_LOCATION
        
        Log.d(TAG, "Requesting location permission: $locationPermission")
        permissionLauncher.launch(locationPermission)
    }
    
    private fun requestBackgroundLocationPermission() {
        // Only for Android 10 (Q) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Check if we already have background permission
            if (!PermissionCheck.checkPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION, activity)) {
                // Show a dialog explaining why we need background permission
                showBackgroundPermissionExplanationDialog()
            } else {
                // Already have background permission
                dismissDialog()
                setDialogShownPreference(false)
            }
        }
    }
    
    private fun showBackgroundPermissionExplanationDialog() {
        // Dismiss the current dialog if showing
        dialog?.dismiss()
        
        // Create a new dialog for background permission explanation
        dialog = Dialog(activity, R.style.Theme_BluetoothLESpam_Dialog)
        dialog!!.setContentView(R.layout.dialog_persistent_location_permission)
        
        // Prevent dialog from being dismissed by back button or touching outside
        dialog!!.setCancelable(false)
        dialog!!.setCanceledOnTouchOutside(false)
        
        // Prevent dialog from being dismissed with back button
        dialog!!.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                // Consume the event but don't dismiss
                Log.d(TAG, "Back button pressed, preventing dialog dismissal")
                true
            } else {
                false
            }
        }
        
        // Update dialog text to explain background permission
        val titleTextView = dialog!!.findViewById<TextView>(R.id.tv_permission_title)
        val messageTextView = dialog!!.findViewById<TextView>(R.id.tv_permission_message)
        
        titleTextView?.text = "Background Location Access"
        messageTextView?.text = "This app needs background location access to scan for Bluetooth devices even when the app is closed. Please select 'Allow all the time' in the next screen."
        
        // Set up the buttons
        val grantButton = dialog!!.findViewById<Button>(R.id.btn_grant_permission)
        val settingsButton = dialog!!.findViewById<Button>(R.id.btn_open_settings)
        
        grantButton.text = "Allow All The Time"
        grantButton.setOnClickListener {
            // Request background permission
            backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        
        settingsButton.text = "Close App"
        settingsButton.setOnClickListener {
            activity.finishAffinity()
        }
        
        // Show dialog and make it persistent
        dialog!!.show()
        
        // Make sure dialog stays on top
        dialog!!.window?.apply {
            setFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }
    
    private fun openAppSettings() {
        Log.d(TAG, "Opening app settings button clicked")
        // For Android 10 (Q) and above, try to open location settings directly
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                // Try to use a more direct intent for location permissions
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", activity.packageName, null)
                intent.data = uri
                
                // Add flags to ensure it opens in a new task and clears previous tasks
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                
                // Update dialog message to guide the user
                val messageTextView = dialog!!.findViewById<TextView>(R.id.tv_permission_message)
                messageTextView?.text = "Please tap 'Permissions' → 'Location' → select 'Allow all the time' option in the settings screen that will open."
                
                // Start the activity and log the action
                Log.d(TAG, "Launching application details settings")
                activity.startActivity(intent)
                
                // Don't dismiss the dialog yet - we'll check permission on resume
            } catch (e: Exception) {
                // Fallback to regular app settings if specific intent fails
                Log.e(TAG, "Failed to open specific location settings: ${e.message}")
                openRegularAppSettings()
            }
        } else {
            // For older versions, use the regular app settings
            openRegularAppSettings()
        }
    }
    
    private fun openRegularAppSettings() {
        // Open app-specific permission settings (fallback method)
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", activity.packageName, null)
        intent.data = uri
        activity.startActivity(intent)
    }
    
    private fun dismissDialog() {
        dialog?.dismiss()
        dialog = null
    }
    
    fun onResume() {
        // Check if permission has been granted when returning from settings
        val locationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Manifest.permission.BLUETOOTH_SCAN
        } else {
            Manifest.permission.ACCESS_FINE_LOCATION
        }
        
        val backgroundPermissionNeeded = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        
        if (PermissionCheck.checkPermission(locationPermission, activity) && 
            (!backgroundPermissionNeeded || PermissionCheck.checkPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION, activity))) {
            // All permissions granted
            dismissDialog()
            setDialogShownPreference(false)
        } else if (shouldShowDialog()) {
            // Still missing permissions and dialog should be shown
            if (!PermissionCheck.checkPermission(locationPermission, activity)) {
                showPermissionExplanationDialog()
            } else if (backgroundPermissionNeeded && 
                !PermissionCheck.checkPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION, activity)) {
                showBackgroundPermissionExplanationDialog()
            }
        }
    }
}