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
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetType
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementState
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementTarget
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
        fun startService(context: Context, message: String) {
            val startIntent = Intent(context, AdvertisementForegroundService::class.java)
            startIntent.putExtra("inputExtra", message)
            ContextCompat.startForegroundService(context, startIntent)
        }
        fun stopService(context: Context) {
            val stopIntent = Intent(context, AdvertisementForegroundService::class.java)
            context.stopService(stopIntent)
        }
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        startForeground(1, createNotification(null))

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && AppContext.getActivity() != null) {
            val notificationManager = AppContext.getActivity().getSystemService(NOTIFICATION_SERVICE) as NotificationManager
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
            .setGraph(R.navigation.mobile_navigation)
            .setDestination(R.id.nav_advertisement)
            //.setArguments(bundle)
            .createPendingIntent()


        // Custom Layout
        val notificationView = RemoteViews(packageName, R.layout.advertisement_foreground_service_notification)

        var title = ""
        var subtitle = ""
        var targetImageId = R.drawable.bluetooth

        if (advertisementSet != null) {
            title = advertisementSet.title
            subtitle = when (advertisementSet.type) {
                AdvertisementSetType.ADVERTISEMENT_TYPE_UNDEFINED -> "Undefined Type"

                AdvertisementSetType.ADVERTISEMENT_TYPE_EASY_SETUP_BUDS -> "Easy Setup Buds"
                AdvertisementSetType.ADVERTISEMENT_TYPE_EASY_SETUP_WATCH -> "Easy Setup Watch"

                AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_DEVICE -> "Fast Pairing"
                AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_NON_PRODUCTION -> "Fast Pairing"
                AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_PHONE_SETUP -> "Fast Pairing"
                AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_DEBUG -> "Fast Pairing"

                AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_NEW_DEVICE -> "New Apple Device"
                AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_NEW_AIRTAG -> "New Airtag"
                AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_NOT_YOUR_DEVICE -> "Not your Device"

                AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_ACTION_MODALS -> "Apple Action"
                AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_IOS_17_CRASH -> "Apple iOs 17 Crash"

                AdvertisementSetType.ADVERTISEMENT_TYPE_SWIFT_PAIRING -> "Swift Pairing"

                AdvertisementSetType.ADVERTISEMENT_TYPE_LOVESPOUSE_PLAY -> "Lovespouse Play"
                AdvertisementSetType.ADVERTISEMENT_TYPE_LOVESPOUSE_STOP -> "Lovespouse Stop"
            }

            targetImageId = when (advertisementSet.target) {
                AdvertisementTarget.ADVERTISEMENT_TARGET_SAMSUNG -> R.drawable.samsung
                AdvertisementTarget.ADVERTISEMENT_TARGET_ANDROID -> R.drawable.ic_android
                AdvertisementTarget.ADVERTISEMENT_TARGET_IOS -> R.drawable.apple
                AdvertisementTarget.ADVERTISEMENT_TARGET_UNDEFINED -> R.drawable.bluetooth
                AdvertisementTarget.ADVERTISEMENT_TARGET_WINDOWS -> R.drawable.microsoft
                AdvertisementTarget.ADVERTISEMENT_TARGET_KITCHEN_SINK -> R.drawable.shuffle
                AdvertisementTarget.ADVERTISEMENT_TARGET_LOVESPOUSE -> R.drawable.heart
            }
        }

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

        if (advertisementSet != null) {
            var titleColor = when (advertisementSet.advertisementState) {
                AdvertisementState.ADVERTISEMENT_STATE_UNDEFINED -> resources.getColor(
                    R.color.color_title,
                    AppContext.getContext().theme
                )

                AdvertisementState.ADVERTISEMENT_STATE_STARTED -> resources.getColor(
                    R.color.color_title,
                    AppContext.getContext().theme
                )

                AdvertisementState.ADVERTISEMENT_STATE_SUCCEEDED -> resources.getColor(
                    R.color.color_title,
                    AppContext.getContext().theme
                )

                AdvertisementState.ADVERTISEMENT_STATE_FAILED -> resources.getColor(
                    R.color.log_error,
                    AppContext.getContext().theme
                )
            }

            notificationView.setTextColor(
                R.id.advertisementForegroundServiceNotificationTitleTextView,
                titleColor
            )
        }

        // Listeners for Custom Layout
        val toggleIntent = Intent(AppContext.getActivity(), ToggleButtonListener::class.java)
        val pendingToggleSwitchIntent = PendingIntent.getBroadcast(
            AppContext.getActivity(),
            0,
            toggleIntent,
            PendingIntent.FLAG_MUTABLE
        )

        notificationView.setOnClickPendingIntent(
            R.id.advertisementForegroundServiceNotificationToggleImageView,
            pendingToggleSwitchIntent
        )

        val stopIntent = Intent(AppContext.getActivity(), StopButtonListener::class.java)
        val pendingStopSwitchIntent = PendingIntent.getBroadcast(
            AppContext.getActivity(),
            0,
            stopIntent,
            PendingIntent.FLAG_MUTABLE
        )

        notificationView.setOnClickPendingIntent(
            R.id.advertisementForegroundServiceNotificationStopImageView,
            pendingStopSwitchIntent
        )

        var contentText = "Bluetooth LE Spam"
        if (advertisementSet != null) {
            contentText = advertisementSet.title
        }

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

    private fun updateNotification(advertisementSet: AdvertisementSet?){
        val notification = createNotification(advertisementSet)
        val notificationManager = AppContext.getActivity().getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notification)
    }

    // Button Handlers
    class ToggleButtonListener : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if(AppContext.getAdvertisementSetQueueHandler().isActive()){
                AppContext.getAdvertisementSetQueueHandler().deactivate()
            } else {
                AppContext.getAdvertisementSetQueueHandler().activate()
            }
        }
    }

    class StopButtonListener : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            AppContext.getAdvertisementSetQueueHandler().deactivate()
            stopService(AppContext.getActivity())
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