package de.simon.dankelmann.bluetoothlespam.Services

import android.app.Notification
import android.app.Notification.FOREGROUND_SERVICE_IMMEDIATE
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavDeepLinkBuilder
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementError
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementState
import de.simon.dankelmann.bluetoothlespam.Enums.getDrawableId
import de.simon.dankelmann.bluetoothlespam.Enums.stringResId
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IAdvertisementServiceCallback
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IAdvertisementSetQueueHandlerCallback
import de.simon.dankelmann.bluetoothlespam.MainActivity
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import de.simon.dankelmann.bluetoothlespam.R

class AdvertisementForegroundService: IAdvertisementServiceCallback, IAdvertisementSetQueueHandlerCallback, Service() {

    private val _logTag = "AdvertisementForegroundService"
    private val _channelId = "BluetoothLeSpam"
    private val _channelName = "Bluetooth Le Spam"
    private val _channelDescription = "Bluetooth Le Spam Notifications"
    private var _currentAdvertisementSet:AdvertisementSet? = null
    private val _binder: IBinder = LocalBinder()

    companion object {
        private const val NOTIFICATION_ID = 1

        fun startService(context: Context) {
            val startIntent = Intent(context, AdvertisementForegroundService::class.java)
            ContextCompat.startForegroundService(context, startIntent)
        }

        fun stopService(context: Context) {
            val stopIntent = Intent(context, AdvertisementForegroundService::class.java)
            context.stopService(stopIntent)

            val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NOTIFICATION_ID)
        }
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        startForeground(NOTIFICATION_ID, createNotification(null))

        // Setup Callbacks
        AppContext.getAdvertisementService().addAdvertisementServiceCallback(this)
        AppContext.getAdvertisementSetQueueHandler().addAdvertisementQueueHandlerCallback(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return _binder
    }

    inner class LocalBinder : Binder() {
        val service: AdvertisementForegroundService
            get() = this@AdvertisementForegroundService
    }

    override fun onDestroy() {
        AppContext.getAdvertisementService().removeAdvertisementServiceCallback(this)
        AppContext.getAdvertisementSetQueueHandler().removeAdvertisementQueueHandlerCallback(this)
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val mChannel = NotificationChannel(_channelId, _channelName, NotificationManager.IMPORTANCE_HIGH)
            mChannel.description = _channelDescription
            mChannel.enableLights(true)
            mChannel.lightColor = Color.BLUE
            notificationManager.createNotificationChannel(mChannel)
        }
    }

    private fun createNotification(advertisementSet: AdvertisementSet?): Notification {

        val pendingIntentTargeted = NavDeepLinkBuilder(this)
            .setComponentName(MainActivity::class.java)
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.nav_advertisement)
            //.setArguments(bundle)
            .createPendingIntent()

        // Custom Layout
        val notificationView = RemoteViews(packageName, R.layout.advertisement_foreground_service_notification)

        val toggleImageSrc = when (AppContext.getAdvertisementSetQueueHandler().isActive()) {
            true -> R.drawable.pause
            false -> R.drawable.play_arrow
        }

        // Views for Custom Layout
        notificationView.setTextViewText(
            R.id.advertisementForegroundServiceNotificationTitleTextView,
            advertisementSet?.title ?: ""
        )
        advertisementSet?.type?.stringResId()?.let { resId ->
            notificationView.setTextViewText(
                R.id.advertisementForegroundServiceNotificationSubTitleTextView,
                getString(resId)
            )
        }
        notificationView.setImageViewResource(
            R.id.advertisementForegroundServiceNotificationTargetImageView,
            advertisementSet?.target?.getDrawableId() ?: R.drawable.bluetooth
        )

        notificationView.setImageViewResource(
            R.id.advertisementForegroundServiceNotificationToggleImageView,
            toggleImageSrc
        )

        val targetIconColor = resources.getColor(R.color.tint_target_icon, theme)
        val buttonActiveColor = resources.getColor(R.color.tint_button_active, theme)
        val buttonInActiveColor = resources.getColor(R.color.tint_button_inactive, theme)

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

        if (advertisementSet != null) {
            val titleColorRes = when (advertisementSet.advertisementState) {
                AdvertisementState.ADVERTISEMENT_STATE_FAILED -> R.color.log_error
                else -> R.color.color_title
            }
            notificationView.setTextColor(
                R.id.advertisementForegroundServiceNotificationTitleTextView,
                resources.getColor(titleColorRes, theme)
            )
        }

        // Listeners for Custom Layout
        val toggleIntent = Intent(this, ToggleButtonListener::class.java)
        val pendingToggleSwitchIntent = PendingIntent.getBroadcast(
            this,
            0,
            toggleIntent,
            PendingIntent.FLAG_MUTABLE
        )

        notificationView.setOnClickPendingIntent(
            R.id.advertisementForegroundServiceNotificationToggleImageView,
            pendingToggleSwitchIntent
        )

        val stopIntent = Intent(this, StopButtonListener::class.java)
        val pendingStopSwitchIntent = PendingIntent.getBroadcast(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_MUTABLE
        )

        notificationView.setOnClickPendingIntent(
            R.id.advertisementForegroundServiceNotificationStopImageView,
            pendingStopSwitchIntent
        )

        val appName = getString(R.string.app_name)
        val contentText = advertisementSet?.title ?: appName

        return NotificationCompat.Builder(this, _channelId)
            .setContentTitle(appName)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.bluetooth)
            .setContentIntent(pendingIntentTargeted)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setChannelId(_channelId)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCustomBigContentView(notificationView)
            .setCustomContentView(notificationView)
            .setForegroundServiceBehavior(FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun updateNotification(advertisementSet: AdvertisementSet?) {
        val notification = createNotification(advertisementSet)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    // Button Handlers
    class ToggleButtonListener : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val queue = AppContext.getAdvertisementSetQueueHandler()
            if (queue.isActive()) {
                queue.deactivate(context)
            } else {
                queue.activate(context)
            }
        }
    }

    class StopButtonListener : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            AppContext.getAdvertisementSetQueueHandler().deactivate(context, true)
            stopService(context)
        }
    }

    // Advertisement Service Callbacks
    override fun onAdvertisementSetStart(advertisementSet: AdvertisementSet?) {
        _currentAdvertisementSet = advertisementSet
        updateNotification(advertisementSet)
    }

    override fun onAdvertisementSetStop(advertisementSet: AdvertisementSet?) {
        _currentAdvertisementSet = advertisementSet
        updateNotification(advertisementSet)
    }

    override fun onAdvertisementSetSucceeded(advertisementSet: AdvertisementSet?) {
        _currentAdvertisementSet = advertisementSet
        updateNotification(advertisementSet)
    }

    override fun onAdvertisementSetFailed(advertisementSet: AdvertisementSet?, advertisementError: AdvertisementError) {
        _currentAdvertisementSet = advertisementSet
        updateNotification(advertisementSet)
    }

    override fun onQueueHandlerActivated() {
        updateNotification(_currentAdvertisementSet)
    }

    override fun onQueueHandlerDeactivated() {
        updateNotification(_currentAdvertisementSet)
    }
}