package com.uiza.sdkbroadcast.view

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.os.Handler
import android.util.Log
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.pedro.encoder.input.gl.render.filters.NoFilterRender
import com.pedro.encoder.input.gl.render.filters.`object`.GifObjectFilterRender
import com.pedro.encoder.input.gl.render.filters.`object`.ImageObjectFilterRender
import com.pedro.encoder.input.gl.render.filters.`object`.TextObjectFilterRender
import com.pedro.encoder.input.video.CameraHelper
import com.pedro.rtplibrary.rtmp.RtmpDisplay
import com.pedro.rtplibrary.util.BitrateAdapter
import com.uiza.sdkbroadcast.R
import com.uiza.sdkbroadcast.enums.Translate
import com.uiza.sdkbroadcast.events.EventSignal
import com.uiza.sdkbroadcast.events.UZEvent
import com.uiza.sdkbroadcast.interfaces.UZBroadCastListener
import com.uiza.sdkbroadcast.profile.AudioAttributes
import com.uiza.sdkbroadcast.profile.VideoAttributes
import com.uiza.sdkbroadcast.services.UZDisplayService
import net.ossrs.rtmp.ConnectCheckerRtmp
import org.greenrobot.eventbus.EventBus
import java.io.IOException
import java.io.InputStream

class UZDisplayBroadCast(
    private val activity: Activity
) {

    companion object {
        private const val REQUEST_CODE_STREAM = 2021 //random num
        private const val REQUEST_CODE_RECORD = 2023
        private const val DELAY_IN_MLS = 5000L
    }

    private val logTag = javaClass.simpleName

    var rtmpDisplay: RtmpDisplay? = null
        private set
    private var mBroadCastUrl: String? = null
    private var uzBroadCastListener: UZBroadCastListener? = null
    private var bitrateAdapter: BitrateAdapter? = null
    private var adaptiveBitrate = true
    private var audioPermission = false
    var videoAttributes: VideoAttributes? = null
    var audioAttributes: AudioAttributes? = null
    private val connectCheckerRtmp: ConnectCheckerRtmp

    init {
        // IMPLEMENT from ConnectCheckerRtmp
        connectCheckerRtmp = object : ConnectCheckerRtmp {
            // IMPLEMENT from ConnectCheckerRtmp
            override fun onConnectionSuccessRtmp() {
                if (adaptiveBitrate) {
                    bitrateAdapter =
                        BitrateAdapter { bitrate: Int ->
                            rtmpDisplay?.setVideoBitrateOnFly(bitrate)
                        }
                    rtmpDisplay?.bitrate?.let {
                        bitrateAdapter?.setMaxBitrate(it)
                    }
                }
                uzBroadCastListener?.onConnectionSuccess()
                EventBus.getDefault()
                    .postSticky(
                        UZEvent(activity.getString(R.string.stream_started))
                    )
            }

            override fun onConnectionFailedRtmp(reason: String) {
                if (rtmpDisplay?.reTry(DELAY_IN_MLS, reason) == true) {
                    EventBus.getDefault()
                        .postSticky(
                            UZEvent(activity.getString(R.string.retry_connecting))
                        )
                    uzBroadCastListener?.onRetryConnection(DELAY_IN_MLS)
                } else {
                    rtmpDisplay?.stopStream()
                    EventBus.getDefault()
                        .postSticky(UZEvent(activity.getString(R.string.connection_failed)))
                    uzBroadCastListener?.onConnectionFailed(reason)
                }
            }

            override fun onNewBitrateRtmp(bitrate: Long) {
                if (bitrateAdapter != null && adaptiveBitrate) {
                    bitrateAdapter?.adaptBitrate(bitrate)
                }
            }

            override fun onDisconnectRtmp() {
                uzBroadCastListener?.onDisconnect()
                EventBus.getDefault().postSticky(
                    UZEvent(EventSignal.STOP, activity.getString(R.string.stop))
                )
            }

            override fun onAuthErrorRtmp() {
                uzBroadCastListener?.onAuthError()
                EventBus.getDefault().postSticky(
                    UZEvent(EventSignal.STOP, activity.getString(R.string.stop))
                )
            }

            override fun onAuthSuccessRtmp() {
                uzBroadCastListener?.onAuthSuccess()
                EventBus.getDefault().postSticky(UZEvent(EventSignal.STOP, ""))
            }
        }
        rtmpDisplay = RtmpDisplay(activity.applicationContext, true, connectCheckerRtmp)
        rtmpDisplay?.setReTries(8)
        UZDisplayService.init(this)
        checkLivePermission()
    }

    fun reCreateDisplay() {
        rtmpDisplay = RtmpDisplay(activity.applicationContext, true, connectCheckerRtmp)
        rtmpDisplay?.setReTries(8)
    }

    private fun checkLivePermission() {
        Dexter.withContext(activity).withPermission(Manifest.permission.RECORD_AUDIO)
            .withListener(object : PermissionListener {
                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest,
                    token: PermissionToken
                ) {
                    token.continuePermissionRequest()
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse) {
                    audioPermission = false
                    uzBroadCastListener?.onInit(false)
                }

                override fun onPermissionGranted(response: PermissionGrantedResponse) {
                    audioPermission = true
                    uzBroadCastListener?.onInit(true)
                }
            }).onSameThread()
            .check()
    }

    /**
     * @param adaptiveBitrate boolean
     * Default true
     */
    fun setAdaptiveBitrate(adaptiveBitrate: Boolean) {
        this.adaptiveBitrate = adaptiveBitrate
    }

    fun setUZBroadCastListener(uzBroadCastListener: UZBroadCastListener?) {
        this.uzBroadCastListener = uzBroadCastListener
    }

    /**
     * Clear Watermark
     */
    fun clearWatermark() {
        if (rtmpDisplay == null) {
            return
        }
        rtmpDisplay?.glInterface?.setFilter(NoFilterRender())
    }

    /**
     * @param text     content of watermark
     * @param textSize size of text
     * @param color    color of text
     * @param position of text
     */
    fun setTextWatermark(
        text: String?,
        textSize: Float,
        @ColorInt color: Int,
        position: Translate
    ) {
        if (rtmpDisplay == null) {
            return
        }
        val textRender = TextObjectFilterRender()
        textRender.setText(text, textSize, color)
        rtmpDisplay?.let {
            it.glInterface?.setFilter(textRender)
            textRender.setDefaultScale(it.streamWidth, it.streamHeight)
        }
        textRender.setPosition(position.translateTo)
    }

    /**
     * Watermark with image
     *
     * @param imageRes The resource id of the image data
     * @param scale    Scale in percent
     * @param position of image
     */
    fun setImageWatermark(@DrawableRes imageRes: Int, scale: PointF, position: Translate) {
        setImageWatermark(
            bitmap = BitmapFactory.decodeResource(activity.resources, imageRes),
            scale = scale,
            position = position
        )
    }

    /**
     * Watermark with image
     *
     * @param bitmap   the decoded bitmap
     * @param scale    Scale in percent
     * @param position of image
     */
    fun setImageWatermark(bitmap: Bitmap, scale: PointF, position: Translate) {
        if (rtmpDisplay == null) {
            return
        }
        val imageRender = ImageObjectFilterRender()
        rtmpDisplay?.glInterface?.setFilter(imageRender)
        imageRender.setImage(bitmap)
        imageRender.setScale(scale.x, scale.y)
        imageRender.setPosition(position.translateTo)
    }

    /**
     * Watermark with gif
     *
     * @param gifRaw   The resource identifier to open, as generated by the aapt tool.
     * @param scale    Scale in percent
     * @param position of gif
     */
    fun setGifWatermark(@RawRes gifRaw: Int, scale: PointF, position: Translate) {
        setGifWatermark(activity.resources.openRawResource(gifRaw), scale, position)
    }

    /**
     * Watermark with gif
     *
     * @param inputStream Access to the resource data.
     * @param scale       Scale in percent
     * @param position    of gif
     */
    fun setGifWatermark(inputStream: InputStream?, scale: PointF, position: Translate) {
        if (rtmpDisplay == null) {
            return
        }
        try {
            val gifRender = GifObjectFilterRender()
            gifRender.setGif(inputStream)
            rtmpDisplay?.glInterface?.setFilter(gifRender)
            gifRender.setScale(scale.x, scale.y)
            gifRender.setPosition(position.translateTo)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Call this method before use [.startBroadCast].
     * Auto detect rotation to prepare for BroadCast
     *
     * @return true if success, false if you get a error (Normally because the encoder selected
     * * doesn't support any configuration seated or your device hasn't a AAC encoder).
     */
    fun prepareBroadCast(): Boolean {
        val rotation: Int = CameraHelper.getCameraOrientation(activity)
        return prepareBroadCast(isLandscape = rotation == 0 || rotation == 180)
    }

    /**
     * Call this method before use [.startBroadCast].
     *
     * @param isLandscape:
     * @return true if success, false if you get a error (Normally because the encoder selected
     * * doesn't support any configuration seated or your device hasn't a AAC encoder).
     */
    fun prepareBroadCast(isLandscape: Boolean): Boolean {
        if (videoAttributes == null) {
            Log.e(logTag, "Please set videoAttributes")
            return false
        }
        return prepareBroadCast(audioAttributes, videoAttributes!!, isLandscape)
    }

    fun prepareBroadCast(
        audioAttributes: AudioAttributes?,
        videoAttributes: VideoAttributes,
        isLandscape: Boolean
    ): Boolean {
        this.videoAttributes = videoAttributes
        this.audioAttributes = audioAttributes
        return if (audioAttributes == null) prepareVideo(
            attrs = videoAttributes,
            isLandscape = isLandscape
        ) else prepareAudio(audioAttributes) && prepareVideo(
            attrs = videoAttributes,
            isLandscape = isLandscape
        )
    }

    private fun prepareVideo(attrs: VideoAttributes, isLandscape: Boolean): Boolean {
        return rtmpDisplay?.prepareVideo(
            attrs.size.width,
            attrs.size.height,
            attrs.frameRate,
            attrs.bitRate,
            if (isLandscape) 0 else 90,
            attrs.dpi,
            attrs.aVCProfile,
            attrs.aVCProfileLevel,
            attrs.frameInterval
        ) ?: false
    }

    private fun prepareAudio(attributes: AudioAttributes): Boolean {
        return audioPermission && rtmpDisplay?.prepareAudio(
            attributes.bitRate,
            attributes.sampleRate,
            attributes.isStereo,
            attributes.isEchoCanceler,
            attributes.isNoiseSuppressor
        ) ?: false
    }

    /**
     * Please call [.prepareBroadCast] before use
     *
     * @param broadCastUrl: Stream Url
     */
    fun startBroadCast(broadCastUrl: String) {
        rtmpDisplay?.let { rtmp ->
            mBroadCastUrl = broadCastUrl
            activity.startActivityForResult(rtmp.sendIntent(), REQUEST_CODE_STREAM)
        }
    }

    val isBroadCasting: Boolean
        get() = rtmpDisplay != null && rtmpDisplay?.isStreaming ?: false

    fun stopBroadCast() {
        rtmpDisplay?.stopStream()
    }

    /**
     * call this method in onActivityResult
     *
     * @param requestCode int
     * @param resultCode  int
     * @param data        Intent
     */
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if ((requestCode == REQUEST_CODE_STREAM || requestCode == REQUEST_CODE_RECORD) && resultCode == Activity.RESULT_OK) {
            rtmpDisplay?.setIntentResult(resultCode, data)

            val intent = Intent(activity, UZDisplayService::class.java)
            intent.putExtra(UZDisplayService.EXTRA_BROAD_CAST_URL, mBroadCastUrl)
            activity.startService(intent)
        }
    }

}
