package com.uiza.sdkbroadcast.helpers

import android.content.Context
import android.graphics.Bitmap
import android.hardware.Camera
import android.util.Log
import android.view.MotionEvent
import com.pedro.encoder.input.gl.render.filters.BaseFilterRender
import com.pedro.encoder.input.video.CameraHelper.Facing
import com.pedro.encoder.input.video.CameraOpenException
import com.pedro.rtplibrary.rtmp.RtmpCamera1
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
 * Wrapper to stream with camera1 api and microphone. Support stream with OpenGlView(Custom SurfaceView that use OpenGl).
 * OpenGlView use Surface to buffer mode(This mode is generally
 * better because skip buffer processing).
 *
 * API requirements:
 * OpenGlView: API 18+.
 *
 * Created by loitp on 01/08/2021.
 */
class Camera1Helper(
    openGlView: OpenGlView,
    connectCheckerRtmp: ConnectCheckerRtmp?
) :

    ICameraHelper {
    private val logTag = javaClass.simpleName
    private val rtmpCamera1: RtmpCamera1 = RtmpCamera1(openGlView, connectCheckerRtmp)
    private var uzCameraChangeListener: UZCameraChangeListener? = null
    private var uzRecordListener: UZRecordListener? = null
    override var mOpenGlView: OpenGlView? = null
        private set
    private var videoAttributes: VideoAttributes? = null
    private var audioAttributes: AudioAttributes? = null
    private var isLandscape = false

    init {
        this.mOpenGlView = openGlView
    }

    override fun replaceView(openGlView: OpenGlView?) {
        this.mOpenGlView = openGlView
        rtmpCamera1.replaceView(openGlView)
    }

    override fun replaceView(context: Context?) {
        this.mOpenGlView = null
        rtmpCamera1.replaceView(context)
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

    override fun setConnectReTries(reTries: Int) {
        rtmpCamera1.setReTries(reTries)
    }

    override fun reTry(delay: Long, reason: String?): Boolean {
        return rtmpCamera1.reTry(delay, reason)
    }

    override fun setUZCameraChangeListener(uzCameraChangeListener: UZCameraChangeListener?) {
        this.uzCameraChangeListener = uzCameraChangeListener
    }

    override fun setUZRecordListener(uzRecordListener: UZRecordListener?) {
        this.uzRecordListener = uzRecordListener
    }

    override fun setFilter(filterReader: BaseFilterRender?) {
        rtmpCamera1.glInterface.setFilter(filterReader)
    }

    override fun setFilter(filterPosition: Int, filterReader: BaseFilterRender?) {
        rtmpCamera1.glInterface.setFilter(filterPosition, filterReader)
    }

    override fun enableAA(aAEnabled: Boolean) {
        rtmpCamera1.glInterface.enableAA(aAEnabled)
    }

    override val isAAEnabled: Boolean
        get() = rtmpCamera1.glInterface.isAAEnabled

    override val streamWidth: Int
        get() = rtmpCamera1.streamHeight

    override val streamHeight: Int
        get() = rtmpCamera1.streamWidth

    override fun enableAudio() {
        rtmpCamera1.enableAudio()
    }

    override fun disableAudio() {
        rtmpCamera1.disableAudio()
    }

    override val isAudioMuted: Boolean
        get() = rtmpCamera1.isAudioMuted

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
        return rtmpCamera1.prepareAudio(
            attrs.bitRate,
            attrs.sampleRate,
            attrs.isStereo,
            attrs.isEchoCanceler,
            attrs.isNoiseSuppressor
        )
    }

    override val isVideoEnabled: Boolean
        get() = rtmpCamera1.isVideoEnabled

    private fun prepareVideo(attrs: VideoAttributes, rotation: Int): Boolean {
        return rtmpCamera1.prepareVideo(
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

    override fun startBroadCast(broadCastUrl: String?) {
        rtmpCamera1.startStream(broadCastUrl)
    }

    override fun stopBroadCast() {
        rtmpCamera1.stopStream()
    }

    override val isBroadCasting: Boolean
        get() = rtmpCamera1.isStreaming

    override fun setVideoBitrateOnFly(bitrate: Int) {
        rtmpCamera1.setVideoBitrateOnFly(bitrate)
    }

    override val bitrate: Int
        get() = rtmpCamera1.bitrate

    override val isFrontCamera: Boolean
        get() = rtmpCamera1.isFrontCamera

    @Throws(UZCameraOpenException::class)
    override fun switchCamera() {
        try {
            rtmpCamera1.switchCamera()
        } catch (e: CameraOpenException) {
            throw UZCameraOpenException(e.message)
        }
        uzCameraChangeListener?.onCameraChange(rtmpCamera1.isFrontCamera)
    }

    override val supportedResolutions: List<VideoSize>
        get() {
            val sizes: List<Camera.Size> = if (rtmpCamera1.isFrontCamera) {
                rtmpCamera1.resolutionsFront
            } else {
                rtmpCamera1.resolutionsBack
            }
            return map(
                sizes,
                object : Pre<Camera.Size, VideoSize> {
                    override fun get(item: Camera.Size): VideoSize {
                        return VideoSize.fromSize(item)
                    }
                })
        }

    override fun startPreview(cameraFacing: Facing?) {
        // because portrait
        rtmpCamera1.startPreview(cameraFacing, 480, 854)
    }

    override fun startPreview(cameraFacing: Facing?, width: Int, height: Int) {
        // because portrait
        rtmpCamera1.startPreview(cameraFacing, height, width)
    }

    override val isOnPreview: Boolean
        get() = rtmpCamera1.isOnPreview

    override fun stopPreview() {
        rtmpCamera1.stopPreview()
    }

    override val isRecording: Boolean
        get() = rtmpCamera1.isRecording

    @Throws(IOException::class)
    override fun startRecord(savePath: String?) {
        if (uzRecordListener != null) rtmpCamera1.startRecord(savePath) { status: RecordController.Status ->
            uzRecordListener?.onStatusChange(
                lookup(
                    status
                )
            )
        } else {
            rtmpCamera1.startRecord(savePath)
        }
    }

    override fun stopRecord() {
        rtmpCamera1.stopRecord()
        rtmpCamera1.startPreview()
    }

    override fun takePhoto(callback: UZTakePhotoCallback?) {
        rtmpCamera1.glInterface.takePhoto { bitmap: Bitmap? -> callback?.onTakePhoto(bitmap) }
    }

    override val isLanternSupported: Boolean
        get() = false

    @Throws(Exception::class)
    override fun enableLantern() {
        rtmpCamera1.enableLantern()
    }

    override fun disableLantern() {
        rtmpCamera1.disableLantern()
    }

    override val isLanternEnabled: Boolean
        get() = rtmpCamera1.isLanternEnabled

    override val maxZoom: Float
        get() = 1.0f

    override var zoom: Float
        get() = 1.0f
        set(level) {}

    override fun setZoom(event: MotionEvent?) {
        rtmpCamera1.setZoom(event)
    }
}
