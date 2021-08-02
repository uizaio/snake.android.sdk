package com.uiza.sdkbroadcast.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.text.TextUtils
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.os.HandlerCompat
import androidx.core.os.HandlerCompat.postDelayed
import com.uiza.sdkbroadcast.R
import com.uiza.sdkbroadcast.UZBroadCast.Companion.iconNotify
import com.uiza.sdkbroadcast.events.EventSignal
import com.uiza.sdkbroadcast.events.UZEvent
import com.uiza.sdkbroadcast.view.UZBroadCastView
import com.uiza.sdkbroadcast.view.UZDisplayBroadCast
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class UZDisplayService : Service() {

    companion object {
        const val EXTRA_BROAD_CAST_URL = "uz_extra_broad_cast_url"
        const val EXTRA_VIDEO_ATTRIBUTES = "uz_extra_video_attributes"
        const val EXTRA_AUDIO_ATTRIBUTES = "uz_extra_audio_attributes"
        const val EXTRA_BROADCAST_LANDSCAPE = "uz_extra_broad_cast_landscape"
        private const val channelId = "UZDisplayStreamChannel"

        var notificationManager: NotificationManager? = null

        @SuppressLint("StaticFieldLeak")
        private var displayBroadCast: UZDisplayBroadCast? = null

        @JvmStatic
        fun init(displayBroadCast: UZDisplayBroadCast) {
            Companion.displayBroadCast = displayBroadCast
        }
    }

    private val logTag = javaClass.simpleName

    private fun showNotification(content: String) {
        val notification = NotificationCompat.Builder(baseContext, channelId)
            .setSmallIcon(iconNotify)
            .setContentTitle(baseContext.getString(R.string.app_name))
            .setContentText(content)
            .build()
        val notifyId = 123456
        notificationManager?.notify(notifyId, notification)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
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

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
        showNotification(getString(R.string.stream_stopped))
    }

    private fun keepAliveTrick() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            val notification = NotificationCompat.Builder(this, channelId)
                .setOngoing(true)
                .setContentTitle("")
                .setContentText("").build()
            startForeground(1, notification)
        } else {
            startForeground(1, Notification())
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        EventBus.getDefault().register(this)
        val mBroadCastUrl = intent.getStringExtra(EXTRA_BROAD_CAST_URL)
        if (!TextUtils.isEmpty(mBroadCastUrl)) {
            //TODO loitp check this sometimes error
            //tai hien, UZDisplayActivity start -> stop stream -> start again -> error
            try {
                displayBroadCast?.rtmpDisplay?.let { rtmp ->
                    if (rtmp.isStreaming) {
                        showNotification(getString(R.string.you_are_already_broadcasting))
                    } else {
                        rtmp.startStream(mBroadCastUrl)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return START_STICKY
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.BACKGROUND)
    fun handleEvent(event: UZEvent) {
        Log.e(logTag, "#handleEvent: called for " + event.message)
        if (event.signal === EventSignal.STOP) {
            stopSelf()
        } else {
            event.message?.let {
                showNotification(it)
            }
        }
    }

}
