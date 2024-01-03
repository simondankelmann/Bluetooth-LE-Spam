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
import de.simon.dankelmann.bluetoothlespam.Helpers.BluetoothHelpers
import de.simon.dankelmann.bluetoothlespam.Helpers.StringHelpers.Companion.toHexString
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IBluetoothLeScanCallback
import de.simon.dankelmann.bluetoothlespam.Interfaces.Services.IBluetoothLeScanService
import de.simon.dankelmann.bluetoothlespam.MainActivity
import de.simon.dankelmann.bluetoothlespam.Models.BluetoothLeScanResult
import de.simon.dankelmann.bluetoothlespam.Models.FlipperDeviceScanResult
import de.simon.dankelmann.bluetoothlespam.PermissionCheck.PermissionCheck
import de.simon.dankelmann.bluetoothlespam.R

class BluetoothLeScanForegroundService: IBluetoothLeScanCallback, Service() {

    private val _logTag = "AdvertisementScanForegroundService"
    private val _channelId = "AdvertisementScanForegroundService"
    private val _channelName = "Advertisement Scan Foreground Service"
    private val _channelDescription = "Advertisement Scan Foreground Service Description"
    private val _binder: IBinder = LocalBinder()

    companion object {
        fun startService(context: Context, message: String) {
            val startIntent = Intent(context, BluetoothLeScanForegroundService::class.java)
            startIntent.putExtra("inputExtra", message)
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

        startForeground(1, createNotification("Initial"))
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
        // Remove any Callbacks
        AppContext.getBluetoothLeScanService().removeBluetoothLeScanServiceCallback(this)
        super.onDestroy()
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

    private fun createNotification(text:String): Notification {

        val pendingIntentTargeted = NavDeepLinkBuilder(this)
            .setComponentName(MainActivity::class.java)
            .setGraph(R.navigation.mobile_navigation)
            .setDestination(R.id.nav_advertisement)
            //.setArguments(bundle)
            .createPendingIntent()


        // Custom Layout
        val notificationView = RemoteViews(packageName, R.layout.advertisement_foreground_service_notification)

        var title = "Test 1"
        var subtitle = text
        var targetImageId = R.drawable.bluetooth

        val toggleImageSrc = when(AppContext.getAdvertisementSetQueueHandler().isActive()){
            true -> R.drawable.pause
            false -> R.drawable.play_arrow
        }

        // Views for Custom Layout
        notificationView.setTextViewText(
            R.id.advertisementForegroundServiceNotificationTitleTextView,
            title
        )
        notificationView.setTextViewText(
            R.id.advertisementForegroundServiceNotificationSubTitleTextView,
            subtitle
        )
        notificationView.setImageViewResource(
            R.id.advertisementForegroundServiceNotificationTargetImageView,
            targetImageId
        )

        notificationView.setImageViewResource(
            R.id.advertisementForegroundServiceNotificationToggleImageView,
            toggleImageSrc
        )

        val targetIconColor =
            resources.getColor(R.color.tint_target_icon, AppContext.getContext().theme)
        val buttonActiveColor =
            resources.getColor(R.color.tint_button_active, AppContext.getContext().theme)
        val buttonInActiveColor =
            resources.getColor(R.color.tint_button_inactive, AppContext.getContext().theme)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            notificationView.setColorInt(
                R.id.advertisementForegroundServiceNotificationTargetImageView,
                "setColorFilter",
                targetIconColor,
                targetIconColor
            )

            notificationView.setColorInt(
                R.id.advertisementForegroundServiceNotificationToggleImageView,
                "setColorFilter",
                buttonActiveColor,
                buttonActiveColor
            )

            notificationView.setColorInt(
                R.id.advertisementForegroundServiceNotificationStopImageView,
                "setColorFilter",
                buttonActiveColor,
                buttonActiveColor
            )
        } else {
            notificationView.setInt(
                R.id.advertisementForegroundServiceNotificationTargetImageView,
                "setColorFilter",
                targetIconColor
            )

            notificationView.setInt(
                R.id.advertisementForegroundServiceNotificationStopImageView,
                "setColorFilter",
                buttonActiveColor
            )

            notificationView.setInt(
                R.id.advertisementForegroundServiceNotificationToggleImageView,
                "setColorFilter",
                buttonActiveColor
            )
        }

        var contentText = "Bluetooth LE Spam"

        return NotificationCompat.Builder(AppContext.getActivity(), _channelId)
            .setContentTitle("Bluetooth LE Spam")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.bluetooth)
            .setContentIntent(pendingIntentTargeted)
            //.setColor(resources.getColor(R.color.blue_normal, AppContext.getContext().theme))
            //.setColorized(true)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setChannelId(_channelId)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCustomBigContentView(notificationView)
            .setCustomContentView(notificationView)
            .setForegroundServiceBehavior(FOREGROUND_SERVICE_IMMEDIATE).build()
    }

    private fun updateNotification(text:String){
        val notification = createNotification(text)
        val notificationManager = AppContext.getActivity().getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notification)
    }

    override fun onScanResult(scanResult: ScanResult) {
        //var model = BluetoothLeScanResult.parseFromScanResult(scanResult)
        //updateNotification(model.deviceName)
    }

    override fun onFlipperDeviceDetected(flipperDeviceScanResult: FlipperDeviceScanResult, alreadyKnown: Boolean) {
        if(!alreadyKnown){
            updateNotification("Found new Flipper: " + flipperDeviceScanResult.deviceName)
        }
    }
}