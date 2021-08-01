package com.uiza.sdkbroadcast.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.text.TextUtils
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.uiza.sdkbroadcast.R
import com.uiza.sdkbroadcast.UZBroadCast.Companion.iconNotify
import com.uiza.sdkbroadcast.events.EventSignal
import com.uiza.sdkbroadcast.events.UZEvent
import com.uiza.sdkbroadcast.helpers.ICameraHelper
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class UZRTMPService : Service() {

    companion object {
        const val EXTRA_BROAD_CAST_URL = "uz_extra_broad_cast_url"
        var cameraHelper: ICameraHelper? = null
        var notificationManager: NotificationManager? = null
        var notifyId = 654321
        var channelId = "UZStreamChannel"

        @JvmStatic
        fun init(cameraHelper: ICameraHelper) {
            Companion.cameraHelper = cameraHelper
        }
    }

    private val logTag = javaClass.simpleName

    private fun showNotification(content: String?) {
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(iconNotify)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(content)
            .build()
        notificationManager?.notify(notifyId, notification)
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelId,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager?.createNotificationChannel(channel)
        }
        keepAliveTrick()
    }

    private fun keepAliveTrick() {
        val id = 101
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            val notification = NotificationCompat.Builder(this, channelId)
                .setOngoing(true)
                .setContentTitle("")
                .setContentText("")
                .build()
            startForeground(id, notification)
        } else {
            startForeground(id, Notification())
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        EventBus.getDefault().register(this)
        val mBroadCastUrl = intent.getStringExtra(EXTRA_BROAD_CAST_URL)
        if (!TextUtils.isEmpty(mBroadCastUrl)) {
            cameraHelper?.let { cam ->
                cam.stopBroadCast()
                if (!cam.isBroadCasting) {
                    if (cam.prepareBroadCast()) {
                        cam.startBroadCast(mBroadCastUrl)
                    }
                } else {
                    showNotification("You are already broadcasting.")
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
        showNotification("Stream stopped")
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.BACKGROUND)
    fun handleEvent(event: UZEvent) {
        Log.e(logTag, "#handleEvent: called for " + event.message)
        if (event.signal === EventSignal.STOP) {
            stopSelf()
        } else {
            showNotification(event.message)
        }
    }
}
