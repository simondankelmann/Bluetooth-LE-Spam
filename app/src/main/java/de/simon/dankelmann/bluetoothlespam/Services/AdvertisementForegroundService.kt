package de.simon.dankelmann.bluetoothlespam.Services

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
import de.simon.dankelmann.bluetoothlespam.MainActivity
import de.simon.dankelmann.bluetoothlespam.R


class AdvertisementForegroundService: Service() {

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
        val input = intent?.getStringExtra("inputExtra")
        createNotificationChannel()
        val notificationIntent = Intent(AppContext.getActivity(), MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(AppContext.getActivity(), 0, notificationIntent, PendingIntent.FLAG_MUTABLE)

        // Custom Layout
        val notificationView = RemoteViews(packageName, R.layout.advertisement_foreground_service_notification)

        // Listeners for Custom Layout
        val pauseSwitchIntent = Intent(AppContext.getActivity(), pauseButtonListener::class.java)
        val pendingPauseSwitchIntent = PendingIntent.getBroadcast(AppContext.getActivity(), 0, pauseSwitchIntent, PendingIntent.FLAG_MUTABLE)

        val playSwitchIntent = Intent(AppContext.getActivity(), playButtonListener::class.java)
        val pendingPlaySwitchIntent = PendingIntent.getBroadcast(AppContext.getActivity(), 0, playSwitchIntent, PendingIntent.FLAG_MUTABLE)

        notificationView.setOnClickPendingIntent(R.id.pauseButton, pendingPauseSwitchIntent)
        notificationView.setOnClickPendingIntent(R.id.playButton, pendingPlaySwitchIntent)

        val notification = NotificationCompat.Builder(AppContext.getActivity(), _channelId)
            .setContentTitle("Bluetooth LE Spam")
            .setContentText(input)
            .setSmallIcon(R.drawable.bluetooth)
            .setContentIntent(pendingIntent)
            .setColor(Color.BLUE)
            .setChannelId(_channelId)
            .setOngoing(true)
            .setCustomContentView(notificationView)
            .build()


        startForeground(1, notification)

        //stopSelf();
        return START_STICKY
    }
    override fun onBind(intent: Intent): IBinder? {
        return null
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
}