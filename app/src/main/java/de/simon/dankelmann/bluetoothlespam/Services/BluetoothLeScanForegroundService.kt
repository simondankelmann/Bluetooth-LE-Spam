package de.simon.dankelmann.bluetoothlespam.Services

import android.Manifest
import android.app.Notification
import android.app.Notification.FOREGROUND_SERVICE_IMMEDIATE
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer.OnSubtitleDataListener
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavDeepLinkBuilder
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext.Companion.bluetoothAdapter
import de.simon.dankelmann.bluetoothlespam.Enums.SpamPackageType
import de.simon.dankelmann.bluetoothlespam.Helpers.BluetoothHelpers
import de.simon.dankelmann.bluetoothlespam.Helpers.StringHelpers.Companion.toHexString
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IBluetoothLeScanCallback
import de.simon.dankelmann.bluetoothlespam.Interfaces.Services.IBluetoothLeScanService
import de.simon.dankelmann.bluetoothlespam.MainActivity
import de.simon.dankelmann.bluetoothlespam.Models.BluetoothLeScanResult
import de.simon.dankelmann.bluetoothlespam.Models.FlipperDeviceScanResult
import de.simon.dankelmann.bluetoothlespam.Models.SpamPackageScanResult
import de.simon.dankelmann.bluetoothlespam.PermissionCheck.PermissionCheck
import de.simon.dankelmann.bluetoothlespam.R

class BluetoothLeScanForegroundService: IBluetoothLeScanCallback, Service() {

    private val _logTag = "AdvertisementScanForegroundService"
    private val _channelId = "BluetoothLeSpamScanService"
    private val _channelName = "Bluetooth Le Spam Scan Service"
    private val _channelDescription = "Bluetooth Le Spam Notifications"
    private val _binder: IBinder = LocalBinder()
    private var notifyOnNewSpam = true
    private var notifyOnNewFlipper = true

    companion object {
        private val _logTag = "AdvertisementScanForegroundService"
        fun startService(context: Context, message: String) {
            val startIntent = Intent(context, BluetoothLeScanForegroundService::class.java)
            startIntent.putExtra("inputExtra", message)
            //AppContext.getActivity().startForegroundService(startIntent)
            ContextCompat.startForegroundService(context, startIntent)
        }
        fun stopService(context: Context) {
            val stopIntent = Intent(context, BluetoothLeScanForegroundService::class.java)
            context.stopService(stopIntent)
        }
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
        startForeground(2, createNotification("Detecting Spam", "Searching Flippers and others", false))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(_logTag, "Started BLE Scan Foreground Service")
        AppContext.getBluetoothLeScanService().addBluetoothLeScanServiceCallback(this)
        AppContext.getBluetoothLeScanService().startScanning()
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return _binder
    }

    inner class LocalBinder : Binder() {
        val service: BluetoothLeScanForegroundService
            get() = this@BluetoothLeScanForegroundService
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop Scanning
        AppContext.getBluetoothLeScanService().stopScanning()
        // Remove any Callbacks
        AppContext.getBluetoothLeScanService().removeBluetoothLeScanServiceCallback(this)
        Log.d(Companion._logTag, "Destroying the Service")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && AppContext.getActivity() != null) {
            val notificationManager = AppContext.getActivity().getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val mChannel = NotificationChannel(_channelId, _channelName, NotificationManager.IMPORTANCE_HIGH)
            mChannel.description = _channelDescription
            mChannel.enableLights(true)
            mChannel.lightColor = Color.BLUE
            notificationManager.createNotificationChannel(mChannel)
        }
    }

    private fun createNotification(title:String, subTitle: String, alertOnlyOnce:Boolean): Notification {

        val pendingIntentTargeted = NavDeepLinkBuilder(this)
            .setComponentName(MainActivity::class.java)
            .setGraph(R.navigation.mobile_navigation)
            .setDestination(R.id.nav_spam_detector)
            //.setArguments(bundle)
            .createPendingIntent()


        // Custom Layout
        val notificationView = RemoteViews(packageName, R.layout.bluetooth_le_scan_foreground_service_notification)

        // Views for Custom Layout
        notificationView.setTextViewText(
            R.id.bluetoothLeScanningForegroundNotificationTitle,
            title
        )
        notificationView.setTextViewText(
            R.id.bluetoothLeScanningForegroundNotificationSubTitle,
            subTitle
        )


        //var contentText = "Bluetooth LE Spam"

        return NotificationCompat.Builder(AppContext.getActivity(), _channelId)
            .setContentTitle("Bluetooth LE Spam")
            .setContentText(subTitle)
            .setSmallIcon(R.drawable.bluetooth)
            .setContentIntent(pendingIntentTargeted)
            //.setColor(resources.getColor(R.color.blue_normal, AppContext.getContext().theme))
            //.setColorized(true)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setChannelId(_channelId)
            .setOngoing(true)
            .setOnlyAlertOnce(alertOnlyOnce)
            .setCustomBigContentView(notificationView)
            .setCustomContentView(notificationView)
            .setForegroundServiceBehavior(FOREGROUND_SERVICE_IMMEDIATE).build()
    }

    private fun updateNotification(title:String, subTitle: String, alertOnlyOnce:Boolean, id:Int){
        if(AppContext.getBluetoothLeScanService().isScanning()){
            val notification = createNotification(title, subTitle, alertOnlyOnce)
            val notificationManager = AppContext.getActivity().getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(id, notification)
        }
    }

    override fun onScanResult(scanResult: ScanResult) {
        //var model = BluetoothLeScanResult.parseFromScanResult(scanResult)
        //updateNotification(model.deviceName)
    }

    override fun onFlipperDeviceDetected(flipperDeviceScanResult: FlipperDeviceScanResult, alreadyKnown: Boolean) {
        if(!alreadyKnown || notifyOnNewFlipper){
            updateNotification("New Flipper: " + flipperDeviceScanResult.deviceName, "${flipperDeviceScanResult.address} | ${flipperDeviceScanResult.rssi} dBm", !notifyOnNewFlipper, 2)
        }
        notifyOnNewFlipper = false
    }

    override fun onFlipperListUpdated() {
        if(AppContext.getBluetoothLeScanService().getFlipperDevicesList().isEmpty()){
            notifyOnNewFlipper = true
        }
    }

    override fun onSpamResultPackageDetected(spamPackageScanResult: SpamPackageScanResult, alreadyKnown: Boolean) {
        val spamPackageTypeText = when(spamPackageScanResult.spamPackageType){
            SpamPackageType.UNKNOWN -> "Unknown Spam"
            SpamPackageType.FAST_PAIRING -> "Fast Pairing"
            SpamPackageType.CONTINUITY_NEW_AIRTAG -> "Continuity Airtag"
            SpamPackageType.CONTINUITY_NEW_DEVICE -> "Continuity new Device"
            SpamPackageType.CONTINUITY_NOT_YOUR_DEVICE -> "Continuity not your Device"
            SpamPackageType.CONTINUITY_ACTION_MODAL -> "Continuity Action Modal"
            SpamPackageType.CONTINUITY_IOS_17_CRASH -> "Continuity iOS 17 Crash"
            SpamPackageType.SWIFT_PAIRING -> "Swift Pairing"
            SpamPackageType.EASY_SETUP_WATCH -> "Easy Setup Watch"
            SpamPackageType.EASY_SETUP_BUDS -> "Easy Setup Buds"
            SpamPackageType.LOVESPOUSE_PLAY -> "Lovespouse Play"
            SpamPackageType.LOVESPOUSE_STOP -> "Lovespouse Stop"
        }
        updateNotification("Spam Detected", spamPackageTypeText + " | " + spamPackageScanResult.address, !notifyOnNewSpam, 2)
        notifyOnNewSpam = false
    }

    override fun onSpamResultPackageListUpdated() {
        if(AppContext.getBluetoothLeScanService().getSpamPackageScanResultList().isEmpty()){
            notifyOnNewSpam = true
        }
    }
}