package de.simon.dankelmann.bluetoothlespam.Services

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementError
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetType
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementTarget
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IAdvertisementServiceCallback
import de.simon.dankelmann.bluetoothlespam.MainActivity
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import de.simon.dankelmann.bluetoothlespam.R


class AdvertisementForegroundService: IAdvertisementServiceCallback, Service() {

    private val _logTag = "AdvertisementForegroundService"
    private val _channelId = "AdvertisementForegroundService"
    private val _channelName = "Advertisement Foreground Service"
    private val _channelDescription = "Advertisement Foreground Service Description"

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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //do heavy work on a background thread
        //val input = intent?.getStringExtra("inputExtra")
        createNotificationChannel()

        startForeground(1, createNotification(null))
        //stopSelf();

        // Setup Callback
        AppContext.getAdvertisementService().addAdvertisementServiceCallback(this)

        return START_STICKY
    }
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        AppContext.getAdvertisementService().removeAdvertisementServiceCallback(this)
        super.onDestroy()
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = AppContext.getActivity().getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val mChannel = NotificationChannel(_channelId, _channelName, NotificationManager.IMPORTANCE_HIGH)
            mChannel.description = _channelDescription
            mChannel.enableLights(true)
            mChannel.lightColor = Color.BLUE
            notificationManager.createNotificationChannel(mChannel)
        }
    }

    private fun createNotification(advertisementSet: AdvertisementSet?): Notification {

        val notificationIntent = Intent(AppContext.getActivity(), MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(AppContext.getActivity(), 0, notificationIntent, PendingIntent.FLAG_MUTABLE)

        // Custom Layout
        val notificationView = RemoteViews(packageName, R.layout.advertisement_foreground_service_notification)


        var title = ""
        var subtitle = ""
        var targetImageId = R.drawable.bluetooth

        if(advertisementSet != null){
            title = advertisementSet.title
            subtitle = when(advertisementSet.type){
                AdvertisementSetType.ADVERTISEMENT_TYPE_UNDEFINED -> "Undefined Type"

                AdvertisementSetType.ADVERTISEMENT_TYPE_EASY_SETUP_BUDS -> "Easy Setup Buds"
                AdvertisementSetType.ADVERTISEMENT_TYPE_EASY_SETUP_WATCH -> "Easy Setup Watch"

                AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_DEVICE -> "Fast Pairing"
                AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_NON_PRODUCTION -> "Fast Pairing"
                AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_PHONE_SETUP -> "Fast Pairing"
                AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_DEBUG -> "Fast Pairing"

                AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_DEVICE_POPUPS -> "Apple Device"
                AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_ACTION_MODALS -> "Apple Action"

                AdvertisementSetType.ADVERTISEMENT_TYPE_SWIFT_PAIRING -> "Swift Pairing"
            }

            targetImageId = when(advertisementSet.target){
                AdvertisementTarget.ADVERTISEMENT_TARGET_SAMSUNG -> R.drawable.samsung
                AdvertisementTarget.ADVERTISEMENT_TARGET_ANDROID -> R.drawable.ic_android
                AdvertisementTarget.ADVERTISEMENT_TARGET_IOS -> R.drawable.apple
                AdvertisementTarget.ADVERTISEMENT_TARGET_UNDEFINED -> R.drawable.bluetooth
                AdvertisementTarget.ADVERTISEMENT_TARGET_WINDOWS -> R.drawable.microsoft
                AdvertisementTarget.ADVERTISEMENT_TARGET_KITCHEN_SINK -> R.drawable.shuffle
            }
        }
        // Views for Custom Layout
        notificationView.setTextViewText(R.id.advertisementForegroundServiceNotificationTitleTextView, title)
        notificationView.setTextViewText(R.id.advertisementForegroundServiceNotificationSubTitleTextView, subtitle)
        notificationView.setImageViewResource(R.id.advertisementForegroundServiceNotificationTargetImageView, targetImageId)

        // Listeners for Custom Layout
        val pauseSwitchIntent = Intent(AppContext.getActivity(), pauseButtonListener::class.java)
        val pendingPauseSwitchIntent = PendingIntent.getBroadcast(AppContext.getActivity(), 0, pauseSwitchIntent, PendingIntent.FLAG_MUTABLE)

        val playSwitchIntent = Intent(AppContext.getActivity(), playButtonListener::class.java)
        val pendingPlaySwitchIntent = PendingIntent.getBroadcast(AppContext.getActivity(), 0, playSwitchIntent, PendingIntent.FLAG_MUTABLE)

        notificationView.setOnClickPendingIntent(R.id.advertisementForegroundServiceNotificationPauseImageView, pendingPauseSwitchIntent)
        notificationView.setOnClickPendingIntent(R.id.advertisementForegroundServiceNotificationPlayImageView, pendingPlaySwitchIntent)


        var contentText = "Bluetooth LE Spam"
        if(advertisementSet != null){
            contentText = advertisementSet.title
        }

        val notification = NotificationCompat.Builder(AppContext.getActivity(), _channelId)
            .setContentTitle("Bluetooth LE Spam")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.bluetooth)
            .setContentIntent(pendingIntent)
            .setColor(Color.BLUE)
            .setChannelId(_channelId)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            //.setCustomContentView(notificationView)
            .setCustomBigContentView(notificationView)
            .build()

        return notification
    }

    private fun updateNotification(advertisementSet: AdvertisementSet?){
        val notification = createNotification(advertisementSet)
        val notificationManager = AppContext.getActivity().getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        Log.d(_logTag, "Updating Notification")
        notificationManager.notify(1, notification)
    }

    // Button Handlers
    class pauseButtonListener : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            AppContext.getAdvertisementSetQueueHandler().deactivate()
        }
    }

    class playButtonListener : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            AppContext.getAdvertisementSetQueueHandler().activate()
        }
    }

    // Advertisement Service Callbacks
    override fun onAdvertisementSetStart(advertisementSet: AdvertisementSet?) {
        //TODO("Not yet implemented")
        updateNotification(advertisementSet)
    }

    override fun onAdvertisementSetStop(advertisementSet: AdvertisementSet?) {
        //TODO("Not yet implemented")
    }

    override fun onAdvertisementSetSucceeded(advertisementSet: AdvertisementSet?) {
        //TODO("Not yet implemented")
    }

    override fun onAdvertisementSetFailed(advertisementSet: AdvertisementSet?, advertisementError: AdvertisementError) {
        //TODO("Not yet implemented")
    }
}