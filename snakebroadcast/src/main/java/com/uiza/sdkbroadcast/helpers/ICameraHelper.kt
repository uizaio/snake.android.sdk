package com.uiza.sdkbroadcast.helpers

import android.content.Context
import android.view.MotionEvent
import com.pedro.encoder.input.gl.render.filters.BaseFilterRender
import com.pedro.encoder.input.video.CameraHelper.Facing
import com.pedro.rtplibrary.view.OpenGlView
import com.uiza.sdkbroadcast.interfaces.UZCameraChangeListener
import com.uiza.sdkbroadcast.interfaces.UZCameraOpenException
import com.uiza.sdkbroadcast.interfaces.UZRecordListener
import com.uiza.sdkbroadcast.interfaces.UZTakePhotoCallback
import com.uiza.sdkbroadcast.profile.AudioAttributes
import com.uiza.sdkbroadcast.profile.VideoAttributes
import com.uiza.sdkbroadcast.profile.VideoSize
import java.io.IOException

interface ICameraHelper {
    val mOpenGlView: OpenGlView?

    /**
     * @param reTries retry connect reTries times
     */
    fun setConnectReTries(reTries: Int)
    fun setUZCameraChangeListener(uzCameraChangeListener: UZCameraChangeListener?)
    fun setUZRecordListener(uzRecordListener: UZRecordListener?)
    fun replaceView(openGlView: OpenGlView?)
    fun replaceView(context: Context?)
    fun setVideoAttributes(attributes: VideoAttributes?)
    fun setAudioAttributes(attributes: AudioAttributes?)
    fun setLandscape(landscape: Boolean)

    /**
     * Set filter in position 0.
     *
     * @param filterReader filter to set. You can modify parameters to filter after set it to stream.
     */
    fun setFilter(filterReader: BaseFilterRender?)

    /**
     * @param filterPosition position of filter
     * @param filterReader   filter to set. You can modify parameters to filter after set it to stream.
     */
    fun setFilter(filterPosition: Int, filterReader: BaseFilterRender?)

    /**
     * Get Anti alias is enabled.
     *
     * @return true is enabled, false is disabled.
     */
    val isAAEnabled: Boolean

    /**
     * Enable or disable Anti aliasing (This method use FXAA).
     *
     * @param aAEnabled true is AA enabled, false is AA disabled. False by default.
     */
    fun enableAA(aAEnabled: Boolean)

    /**
     * get Stream Width
     */
    val streamWidth: Int

    /**
     * get Stream Height
     */
    val streamHeight: Int

    /**
     * Enable a muted microphone, can be called before, while and after broadcast.
     */
    fun enableAudio()

    /**
     * Mute microphone, can be called before, while and after broadcast.
     */
    fun disableAudio()

    /**
     * Get mute state of microphone.
     *
     * @return true if muted, false if enabled
     */
    val isAudioMuted: Boolean

    /**
     * You will do a portrait broadcast
     *
     * @return true if success, false if you get a error (Normally because the encoder selected
     * doesn't support any configuration seated or your device hasn't a H264 encoder).
     */
    fun prepareBroadCast(): Boolean

    /**
     * @param isLandscape boolean
     * @return true if success, false if you get a error (Normally because the encoder selected
     * doesn't support any configuration seated or your device hasn't a H264 encoder).
     */
    fun prepareBroadCast(isLandscape: Boolean): Boolean

    /**
     * Call this method before use [.startBroadCast].
     *
     * @param audioAttributes [AudioAttributes] If null you will do a broadcast without audio.
     * @param videoAttributes [VideoAttributes]
     * @param isLandscape     boolean you will broadcast is landscape
     * @return true if success, false if you get a error (Normally because the encoder selected
     * doesn't support any configuration seated or your device hasn't a AAC encoder).
     */
    fun prepareBroadCast(
        audioAttributes: AudioAttributes?,
        videoAttributes: VideoAttributes,
        isLandscape: Boolean
    ): Boolean

    /**
     * Get video camera state
     *
     * @return true if disabled, false if enabled
     */
    val isVideoEnabled: Boolean

    /**
     * Need be called after [.prepareBroadCast] or/and [.prepareBroadCast].
     *
     * @param broadCastUrl of the broadcast like: rtmp://ip:port/application/stream_name
     *
     *
     * RTMP: rtmp://192.168.1.1:1935/fmp4/live_stream_name
     * [.startPreview] to resolution seated in
     * [.prepareBroadCast].
     * If you never startPreview this method [.startPreview] for you to resolution seated in
     * [.prepareBroadCast].
     */
    fun startBroadCast(broadCastUrl: String?)

    /**
     * Stop BroadCast started with [.startBroadCast]
     */
    fun stopBroadCast()

    /**
     * Get broadcast state.
     *
     * @return true if broadcasting, false if not broadcasting.
     */
    val isBroadCasting: Boolean

    /**
     * @return list of [VideoSize]
     */
    val supportedResolutions: List<VideoSize?>?

    /**
     * Switch camera used. Can be called on preview or while stream, ignored with preview off.
     *
     * @throws UZCameraOpenException If the other camera doesn't support same resolution.
     */
    @Throws(UZCameraOpenException::class)
    fun switchCamera()

    /**
     * Start camera preview. Ignored, if stream or preview is started.
     * resolution of preview 640x480
     *
     * @param cameraFacing front or back camera. Like: [com.pedro.encoder.input.video.CameraHelper.Facing.BACK]
     * [com.pedro.encoder.input.video.CameraHelper.Facing.FRONT]
     */
    fun startPreview(cameraFacing: Facing?)

    /**
     * Start camera preview. Ignored, if stream or preview is started.
     *
     * @param cameraFacing front or back camera. Like: [com.pedro.encoder.input.video.CameraHelper.Facing.BACK]
     * [com.pedro.encoder.input.video.CameraHelper.Facing.FRONT]
     * @param width        of preview in px.
     * @param height       of preview in px.
     */
    fun startPreview(cameraFacing: Facing?, width: Int, height: Int)

    /**
     * is Front Camera
     */
    val isFrontCamera: Boolean

    /**
     * check is on preview
     *
     * @return true if onpreview, false if not preview.
     */
    val isOnPreview: Boolean

    /**
     * Stop camera preview. Ignored if streaming or already stopped. You need call it after
     *
     * stopStream to release camera properly if you will close activity.
     */
    fun stopPreview()

    /**
     * Get record state.
     *
     * @return true if recording, false if not recoding.
     */
    val isRecording: Boolean

    /**
     * Start record a MP4 video. Need be called while stream.
     *
     * @param savePath where file will be saved.
     * @throws IOException If you init it before start stream.
     */
    @Throws(IOException::class)
    fun startRecord(savePath: String?)

    /**
     * Stop record MP4 video started with @startRecord. If you don't call it file will be unreadable.
     */
    fun stopRecord()

    /**
     * take a photo
     *
     * @param callback [UZTakePhotoCallback]
     */
    fun takePhoto(callback: UZTakePhotoCallback?)

    /**
     * Set video bitrate of H264 in kb while stream.
     *
     * @param bitrate H264 in kb.
     */
    fun setVideoBitrateOnFly(bitrate: Int)

    /**
     * @return bitrate in kps
     */
    val bitrate: Int
    fun reTry(delay: Long, reason: String?): Boolean

    /**
     * Check support Flashlight
     * if use Camera1 always return false
     *
     * @return true if support, false if not support.
     */
    val isLanternSupported: Boolean

    /**
     * required: <uses-permission android:name="android.permission.FLASHLIGHT"></uses-permission>
     */
    @Throws(Exception::class)
    fun enableLantern()

    /**
     * required: <uses-permission android:name="android.permission.FLASHLIGHT"></uses-permission>
     */
    fun disableLantern()
    val isLanternEnabled: Boolean

    /**
     * Return max zoom level
     *
     * @return max zoom level
     */
    val maxZoom: Float
    /**
     * Return current zoom level
     *
     * @return current zoom level
     */
    /**
     * Set zoomIn or zoomOut to camera.
     * Use this method if you use a zoom slider.
     *
     * @param level Expected to be >= 1 and <= max zoom level
     * @see Camera2Base.getMaxZoom
     */
    var zoom: Float

    /**
     * Set zoomIn or zoomOut to camera.
     *
     * @param event motion event. Expected to get event.getPointerCount() > 1
     */
    fun setZoom(event: MotionEvent?)
}
