package com.uiza.sdkbroadcast.helpers

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.util.Size
import android.view.MotionEvent
import androidx.annotation.RequiresApi
import com.pedro.encoder.input.gl.render.filters.BaseFilterRender
import com.pedro.encoder.input.video.CameraHelper.Facing
import com.pedro.encoder.input.video.CameraOpenException
import com.pedro.rtplibrary.rtmp.RtmpCamera2
import com.pedro.rtplibrary.util.RecordController
import com.pedro.rtplibrary.view.OpenGlView
import com.uiza.sdkbroadcast.enums.RecordStatus.Companion.lookup
import com.uiza.sdkbroadcast.interfaces.UZCameraChangeListener
import com.uiza.sdkbroadcast.interfaces.UZCameraOpenException
import com.uiza.sdkbroadcast.interfaces.UZRecordListener
import com.uiza.sdkbroadcast.interfaces.UZTakePhotoCallback
import com.uiza.sdkbroadcast.profile.AudioAttributes
import com.uiza.sdkbroadcast.profile.VideoAttributes
import com.uiza.sdkbroadcast.profile.VideoSize
import com.uiza.sdkbroadcast.util.ListUtils.Pre
import com.uiza.sdkbroadcast.util.ListUtils.map
import net.ossrs.rtmp.ConnectCheckerRtmp
import java.io.IOException

/**
 * Wrapper to stream with camera2 api and microphone. Support stream with OpenGlView(Custom SurfaceView that use OpenGl) and Context(background mode).
 * All views use Surface to buffer encoding mode for H264.
 *
 * API requirements:
 * API 21+.
 *
 * Created by loitp on 31/08/2021.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class Camera2Helper(
    openGlView: OpenGlView,
    connectCheckerRtmp: ConnectCheckerRtmp?
) :
    ICameraHelper {
    private val logTag = javaClass.simpleName
    private val rtmpCamera2: RtmpCamera2 = RtmpCamera2(openGlView, connectCheckerRtmp)
    private var uzCameraChangeListener: UZCameraChangeListener? = null
    private var uzRecordListener: UZRecordListener? = null
    override var mOpenGlView: OpenGlView? = null
        private set
    private var videoAttributes: VideoAttributes? = null
    private var audioAttributes: AudioAttributes? = null
    private var isLandscape = false

    override fun replaceView(context: Context?) {
        mOpenGlView = null
        rtmpCamera2.replaceView(context)
    }

    override fun replaceView(openGlView: OpenGlView?) {
        if (mOpenGlView == null) rtmpCamera2.replaceView(openGlView)
    }

    override fun setConnectReTries(reTries: Int) {
        rtmpCamera2.setReTries(reTries)
    }

    override fun reTry(delay: Long, reason: String?): Boolean {
        return rtmpCamera2.reTry(delay, reason)
    }

    override fun setVideoAttributes(attributes: VideoAttributes?) {
        this.videoAttributes = attributes
    }

    override fun setAudioAttributes(attributes: AudioAttributes?) {
        this.audioAttributes = attributes
    }

    override fun setLandscape(landscape: Boolean) {
        isLandscape = landscape
    }

    override fun setUZCameraChangeListener(uzCameraChangeListener: UZCameraChangeListener?) {
        this.uzCameraChangeListener = uzCameraChangeListener
    }

    override fun setUZRecordListener(uzRecordListener: UZRecordListener?) {
        this.uzRecordListener = uzRecordListener
    }

    override fun setFilter(filterReader: BaseFilterRender?) {
        rtmpCamera2.glInterface.setFilter(filterReader)
    }

    override fun setFilter(filterPosition: Int, filterReader: BaseFilterRender?) {
        rtmpCamera2.glInterface.setFilter(filterPosition, filterReader)
    }

    override fun enableAA(aAEnabled: Boolean) {
        rtmpCamera2.glInterface.enableAA(aAEnabled)
    }

    override val isAAEnabled: Boolean
        get() = rtmpCamera2.glInterface.isAAEnabled

    override fun setVideoBitrateOnFly(bitrate: Int) {
        rtmpCamera2.setVideoBitrateOnFly(bitrate)
    }

    override val bitrate: Int
        get() = rtmpCamera2.bitrate

    override val streamWidth: Int
        get() = rtmpCamera2.streamWidth

    override val streamHeight: Int
        get() = rtmpCamera2.streamHeight

    override fun enableAudio() {
        rtmpCamera2.enableAudio()
    }

    override fun disableAudio() {
        rtmpCamera2.disableAudio()
    }

    override val isAudioMuted: Boolean
        get() = rtmpCamera2.isAudioMuted

    override fun prepareBroadCast(): Boolean {
        return prepareBroadCast(isLandscape)
    }

    override fun prepareBroadCast(isLandscape: Boolean): Boolean {
        if (videoAttributes == null) {
            Log.e(logTag, "Please set videoAttributes")
            return false
        }
        return prepareBroadCast(audioAttributes, videoAttributes!!, isLandscape)
    }

    override fun prepareBroadCast(
        audioAttributes: AudioAttributes?,
        videoAttributes: VideoAttributes,
        isLandscape: Boolean
    ): Boolean {
        this.audioAttributes = audioAttributes
        this.videoAttributes = videoAttributes
        this.isLandscape = isLandscape
        return if (audioAttributes == null) prepareVideo(
            videoAttributes,
            if (isLandscape) 0 else 90
        ) else prepareAudio(audioAttributes) && prepareVideo(
            videoAttributes,
            if (isLandscape) 0 else 90
        )
    }

    private fun prepareAudio(attrs: AudioAttributes): Boolean {
        return rtmpCamera2.prepareAudio(
            attrs.bitRate,
            attrs.sampleRate,
            attrs.isStereo,
            attrs.isEchoCanceler,
            attrs.isNoiseSuppressor
        )
    }

    override val isVideoEnabled: Boolean
        get() = rtmpCamera2.isVideoEnabled

    private fun prepareVideo(attrs: VideoAttributes, rotation: Int): Boolean {
        return rtmpCamera2.prepareVideo(
            attrs.size.width,
            attrs.size.height,
            attrs.frameRate,
            attrs.bitRate,
            false,
            attrs.frameInterval,
            rotation,
            attrs.aVCProfile,
            attrs.aVCProfileLevel
        )
    }

    override fun takePhoto(callback: UZTakePhotoCallback?) {
        rtmpCamera2.glInterface.takePhoto { bitmap: Bitmap? -> callback?.onTakePhoto(bitmap) }
    }

    override fun startBroadCast(broadCastUrl: String?) {
        rtmpCamera2.startStream(broadCastUrl)
    }

    override fun stopBroadCast() {
        rtmpCamera2.stopStream()
    }

    override val isBroadCasting: Boolean
        get() = rtmpCamera2.isStreaming

    override val isFrontCamera: Boolean
        get() = rtmpCamera2.isFrontCamera

    @Throws(UZCameraOpenException::class)
    override fun switchCamera() {
        try {
            rtmpCamera2.switchCamera()
            uzCameraChangeListener?.onCameraChange(rtmpCamera2.isFrontCamera)
        } catch (e: CameraOpenException) {
            throw UZCameraOpenException(e.message)
        }
    }

    override val supportedResolutions: List<VideoSize>
        get() {
            val sizes: List<Size> = if (rtmpCamera2.isFrontCamera) {
                rtmpCamera2.resolutionsFront
            } else {
                rtmpCamera2.resolutionsBack
            }
            return map(sizes, object : Pre<Size, VideoSize> {
                override fun get(item: Size): VideoSize {
                    return VideoSize.fromSize(item)
                }
            })
        }

    override fun startPreview(cameraFacing: Facing?) {
        rtmpCamera2.startPreview(cameraFacing)
    }

    override fun startPreview(cameraFacing: Facing?, width: Int, height: Int) {
        // because portrait
        rtmpCamera2.startPreview(cameraFacing, height, width)
    }

    override val isOnPreview: Boolean
        get() = rtmpCamera2.isOnPreview

    override fun stopPreview() {
        rtmpCamera2.stopPreview()
    }

    override val isRecording: Boolean
        get() = rtmpCamera2.isRecording

    @Throws(IOException::class)
    override fun startRecord(savePath: String?) {
        if (uzRecordListener != null) rtmpCamera2.startRecord(savePath) { status: RecordController.Status ->
            uzRecordListener?.onStatusChange(
                lookup(status)
            )
        } else rtmpCamera2.startRecord(savePath)
    }

    override fun stopRecord() {
        rtmpCamera2.stopRecord()
    }

    override val isLanternSupported: Boolean
        get() = rtmpCamera2.isLanternSupported

    @Throws(Exception::class)
    override fun enableLantern() {
        rtmpCamera2.enableLantern()
    }

    override fun disableLantern() {
        rtmpCamera2.disableLantern()
    }

    override val isLanternEnabled: Boolean
        get() = rtmpCamera2.isLanternEnabled

    override val maxZoom: Float
        get() = rtmpCamera2.maxZoom

    override var zoom: Float
        get() = rtmpCamera2.zoom
        set(level) {
            rtmpCamera2.zoom = level
        }

    override fun setZoom(event: MotionEvent?) {
        rtmpCamera2.setZoom(event)
    }

    init {
        mOpenGlView = openGlView
    }
}
