package com.uiza.sdkbroadcast.view

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Resources
import android.graphics.*
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import android.view.View.OnTouchListener
import android.widget.RelativeLayout
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.pedro.encoder.input.gl.SpriteGestureController
import com.pedro.encoder.input.gl.render.ManagerRender
import com.pedro.encoder.input.gl.render.filters.NoFilterRender
import com.pedro.encoder.input.gl.render.filters.`object`.GifObjectFilterRender
import com.pedro.encoder.input.gl.render.filters.`object`.ImageObjectFilterRender
import com.pedro.encoder.input.gl.render.filters.`object`.SurfaceFilterRender
import com.pedro.encoder.input.gl.render.filters.`object`.TextObjectFilterRender
import com.pedro.encoder.input.video.CameraHelper.Facing
import com.pedro.rtplibrary.util.BitrateAdapter
import com.uiza.sdkbroadcast.R
import com.uiza.sdkbroadcast.enums.AspectRatio
import com.uiza.sdkbroadcast.enums.FilterRender
import com.uiza.sdkbroadcast.enums.Translate
import com.uiza.sdkbroadcast.events.EventSignal
import com.uiza.sdkbroadcast.events.UZEvent
import com.uiza.sdkbroadcast.helpers.Camera1Helper
import com.uiza.sdkbroadcast.helpers.Camera2Helper
import com.uiza.sdkbroadcast.helpers.ICameraHelper
import com.uiza.sdkbroadcast.interfaces.UZBroadCastListener
import com.uiza.sdkbroadcast.interfaces.UZCameraChangeListener
import com.uiza.sdkbroadcast.interfaces.UZRecordListener
import com.uiza.sdkbroadcast.interfaces.UZTakePhotoCallback
import com.uiza.sdkbroadcast.profile.AudioAttributes
import com.uiza.sdkbroadcast.profile.VideoAttributes
import com.uiza.sdkbroadcast.services.UZRTMPService
import com.uiza.sdkbroadcast.services.UZRTMPService.Companion.init
import com.uiza.sdkbroadcast.util.ValidValues.isMyServiceRunning
import com.uiza.sdkbroadcast.util.ViewUtil.blinking
import kotlinx.android.synthetic.main.layout_uiza_glview.view.*
import net.ossrs.rtmp.ConnectCheckerRtmp
import org.greenrobot.eventbus.EventBus
import java.io.IOException
import java.io.InputStream
import kotlin.math.min

/**
 * required: <uses-permission android:name="android.permission.CAMERA"></uses-permission> and
 * <uses-permission android:name="android.permission.RECORD_AUDIO"></uses-permission>
 */
class UZBroadCastView @TargetApi(Build.VERSION_CODES.LOLLIPOP) constructor(
    context: Context,
    attrs: AttributeSet,
    defStyleAttr: Int,
    defStyleRes: Int
) : RelativeLayout(context, attrs, defStyleAttr, defStyleRes), OnTouchListener {

    companion object {
        private const val SECOND: Long = 1000
        private const val MINUTE = 60 * SECOND
        private const val WATERMARK_POSITION = 1
        private const val FILTER_POSITION = 0
        private const val DELAY_IN_MLS = 5000L
    }

    private val logTag = javaClass.simpleName
    var mAspectRatio = AspectRatio.RATIO_16_9
    private var mainBroadCastUrl: String? = null
    private var cameraHelper: ICameraHelper? = null
    private var useCamera2 = false
    private var isRunInBackground = false
    private var startCamera = Facing.FRONT
    private var uzBroadCastListener: UZBroadCastListener? = null
    private var backgroundAllowedDuration = 2 * MINUTE // default is 2 minutes
    private var backgroundTimer: CountDownTimer? = null
    private var isBroadcastingBeforeGoingBackground = false
    private var isFromBackgroundTooLong = false
    private var isAAEnabled = false
    private var keepAspectRatio = false
    private var isFlipHorizontal = false
    private var isFlipVertical = false
    private var adaptiveBitrate = true
    private var bitrateAdapter: BitrateAdapter? = null
    private var videoAttributes: VideoAttributes? = null
    private var audioAttributes: AudioAttributes? = null

    private val spriteGestureController = SpriteGestureController()

    private val surfaceCallback: SurfaceHolder.Callback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            uzBroadCastListener?.surfaceCreated()
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            val mHeight = min(a = (width * mAspectRatio.aspectRatio).toInt(), b = height)
            cameraHelper?.startPreview(cameraFacing = startCamera, width = width, height = mHeight)
            if (isRunInBackground) {
                cameraHelper?.replaceView(openGlView)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && useCamera2) {
                postDelayed(
                    {
                        switchCamera()
                    },
                    100
                ) // fix Note10 with camera2
            }
            uzBroadCastListener?.surfaceChanged(
                format = format,
                width = width,
                height = mHeight
            )
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            cameraHelper?.let { ch ->
                if (isRunInBackground) {
                    if (ch.isBroadCasting) ch.replaceView(getContext().applicationContext)
                    if (ch.isOnPreview) ch.stopPreview()
                } else {
                    if (ch.isRecording) ch.stopRecord()
                    if (ch.isBroadCasting) ch.stopBroadCast()
                    if (ch.isOnPreview) ch.stopPreview()
                    startBackgroundTimer()
                }
            }
            uzBroadCastListener?.surfaceDestroyed()
        }
    }

    private val connectCheckerRtmp: ConnectCheckerRtmp = object : ConnectCheckerRtmp {
        override fun onConnectionSuccessRtmp() {
            if (adaptiveBitrate) {
                bitrateAdapter =
                    BitrateAdapter { bitrate: Int -> cameraHelper?.setVideoBitrateOnFly(bitrate) }
                cameraHelper?.bitrate?.let {
                    bitrateAdapter?.setMaxBitrate(it)
                }
            }
            if (getContext() is Activity) {
                (getContext() as Activity).runOnUiThread {
                    showLiveStatus()
                    pb.visibility = GONE
                    invalidate()
                    requestLayout()
                    uzBroadCastListener?.onConnectionSuccess()
                }
            }

            if (isRunInBackground) {
                EventBus.getDefault()
                    .postSticky(UZEvent(context.getString(R.string.stream_started)))
            } else {
                isBroadcastingBeforeGoingBackground = true
            }
        }

        override fun onConnectionFailedRtmp(reason: String) {
            cameraHelper?.let { ch ->
                if (ch.reTry(delay = DELAY_IN_MLS, reason = reason)) {
                    if (isRunInBackground) {
                        EventBus.getDefault()
                            .postSticky(UZEvent(context.getString(R.string.retry_connecting)))
                    }
                    if (getContext() is Activity) {
                        (getContext() as Activity).runOnUiThread {
                            uzBroadCastListener?.onRetryConnection(DELAY_IN_MLS)
                        }
                    }
                } else {
                    cameraHelper?.stopBroadCast()
                    if (getContext() is Activity) {
                        (getContext() as Activity).runOnUiThread {
                            hideLiveStatus()
                            invalidate()
                            requestLayout()
                            pb.visibility = GONE
                            uzBroadCastListener?.onConnectionFailed(reason)
                        }
                    }
                }
            }
        }

        override fun onNewBitrateRtmp(bitrate: Long) {
            if (bitrateAdapter != null && adaptiveBitrate) {
                bitrateAdapter?.adaptBitrate(bitrate)
            }
        }

        override fun onDisconnectRtmp() {
            if (getContext() is Activity) {
                (getContext() as Activity).runOnUiThread {
                    hideLiveStatus()
                    pb.visibility = GONE
                    invalidate()
                    requestLayout()
                    uzBroadCastListener?.onDisconnect()
                }
            }

            // with runInBackground does not post event because service has stopped
            if (isRunInBackground) {
                EventBus.getDefault().postSticky(UZEvent(EventSignal.STOP, ""))
            }
        }

        override fun onAuthErrorRtmp() {
            if (getContext() is Activity) {
                (getContext() as Activity).runOnUiThread {
                    pb.visibility = GONE
                    invalidate()
                    requestLayout()
                    uzBroadCastListener?.onAuthError()
                }
            }

            if (isRunInBackground) {
                EventBus.getDefault()
                    .postSticky(UZEvent(context.getString(R.string.stream_auth_error)))
            }
        }

        override fun onAuthSuccessRtmp() {
            if (getContext() is Activity) {
                (getContext() as Activity).runOnUiThread {
                    pb.visibility = GONE
                    invalidate()
                    requestLayout()
                    uzBroadCastListener?.onAuthSuccess()
                }
            }

            if (isRunInBackground) {
                EventBus.getDefault()
                    .postSticky(UZEvent(context.getString(R.string.stream_auth_success)))
            }
        }
    }

    init {
        initView(attrs = attrs, defStyleAttr = defStyleAttr, defStyleRes = defStyleRes)
    }

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int = 0) : this(
        context = context,
        attrs = attrs,
        defStyleAttr = defStyleAttr,
        defStyleRes = 0
    ) {
        initView(attrs = attrs, defStyleAttr = defStyleAttr, defStyleRes = 0)
    }

    /**
     * Call twice time
     * Node: Don't call inflate in this method
     */
    private fun initView(attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        if (attrs != null) {
            val a = context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.UZBroadCastView,
                defStyleAttr,
                defStyleRes
            )
            try {
                val hasLollipop = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                useCamera2 = a.getBoolean(R.styleable.UZBroadCastView_useCamera2, hasLollipop)
                isRunInBackground = a.getBoolean(R.styleable.UZBroadCastView_runInBackground, false)
                require(!(!useCamera2 && isRunInBackground)) { "the supportRunBackground support camera2 only" }
                startCamera = Facing.values()[a.getInt(R.styleable.UZBroadCastView_startCamera, 1)]
                // for openGL
                keepAspectRatio = a.getBoolean(R.styleable.UZBroadCastView_keepAspectRatio, true)
                isAAEnabled = a.getBoolean(R.styleable.UZBroadCastView_AAEnabled, false)
                // ManagerRender.numFilters = a.getInt(R.styleable.UZBroadCastView_numFilters, 1);
                isFlipHorizontal = a.getBoolean(R.styleable.UZBroadCastView_isFlipHorizontal, false)
                isFlipVertical = a.getBoolean(R.styleable.UZBroadCastView_isFlipVertical, false)
                ManagerRender.numFilters = 2
            } finally {
                a.recycle()
            }
        } else {
            useCamera2 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
            isRunInBackground = false
            startCamera = Facing.FRONT
            // for OpenGL
            keepAspectRatio = true
            isAAEnabled = false
            isFlipHorizontal = false
            isFlipVertical = false
            ManagerRender.numFilters = 2
        }
    }

    /**
     * Call one time
     * Note: you must call inflate in this method
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun onCreateView() {
        inflate(context, R.layout.layout_uiza_glview, this)
        cameraHelper =
            if (useCamera2) {
                Camera2Helper(openGlView = openGlView, connectCheckerRtmp = connectCheckerRtmp)
            } else {
                Camera1Helper(openGlView = openGlView, connectCheckerRtmp = connectCheckerRtmp)
            }
        if (isRunInBackground) {
            cameraHelper?.let {
                init(it)
            }
        }
        openGlView.init()
        openGlView.holder.addCallback(surfaceCallback)
        openGlView.setCameraFlip(isFlipHorizontal, isFlipVertical)
        openGlView.isKeepAspectRatio = keepAspectRatio
        openGlView.enableAA(isAAEnabled)
        openGlView.setOnTouchListener(this)

        pb.indeterminateDrawable.colorFilter =
            PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY)
        cameraHelper?.setConnectReTries(8)
    }

    /**
     * Set AspectRatio
     *
     * @param mAspectRatio One of [AspectRatio.RATIO_19_9],
     * [AspectRatio.RATIO_18_9],
     * [AspectRatio.RATIO_16_9] or [AspectRatio.RATIO_4_3]
     */
    fun setAspectRatio(aspectRatio: AspectRatio) {
        this.mAspectRatio = aspectRatio
        val screenWidth = Resources.getSystem().displayMetrics.widthPixels
        openGlView.layoutParams.width = screenWidth
        openGlView.layoutParams.height = (screenWidth * mAspectRatio.aspectRatio).toInt()
        openGlView.requestLayout()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        checkLivePermission()
    }

    private fun checkLivePermission() {
        Dexter.withContext(context).withPermissions(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).withListener(object : MultiplePermissionsListener {
            override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                when {
                    report.areAllPermissionsGranted() -> {
                        onCreateView()
                        uzBroadCastListener?.onInit(true)
                    }
                    report.isAnyPermissionPermanentlyDenied -> {
                        showSettingsDialog()
                    }
                    else -> {
                        showShouldAcceptPermission()
                    }
                }
            }

            override fun onPermissionRationaleShouldBeShown(
                permissions: List<PermissionRequest>,
                token: PermissionToken
            ) {
                token.continuePermissionRequest()
            }
        }).onSameThread()
            .check()
    }

    private fun showShouldAcceptPermission() {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.need_permission)
        builder.setMessage(R.string.this_app_needs_permission)
        builder.setPositiveButton(R.string.okay) { _: DialogInterface?, _: Int ->
            checkLivePermission()
        }
        builder.setNegativeButton(R.string.cancel) { _: DialogInterface?, _: Int ->
            uzBroadCastListener?.onInit(false)
        }
        val dialog = builder.create()
        dialog.setCancelable(false)
        dialog.show()
    }

    private fun showSettingsDialog() {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.need_permission)
        builder.setMessage(R.string.this_app_needs_permission_grant_it)
        builder.setPositiveButton(R.string.goto_settings) { _: DialogInterface?, _: Int ->
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", context.packageName, null)
            intent.data = uri
            if (context is Activity) {
                (context as Activity).startActivityForResult(intent, 101)
            }
        }
        builder.setNegativeButton(R.string.cancel) { _: DialogInterface?, _: Int ->
            uzBroadCastListener?.onInit(false)
        }
        val dialog = builder.create()
        dialog.setCancelable(false)
        dialog.show()
    }

    /**
     * @param adaptiveBitrate boolean
     * Default true
     */
    fun setAdaptiveBitrate(adaptiveBitrate: Boolean) {
        this.adaptiveBitrate = adaptiveBitrate
    }

    /**
     * @param uzBroadCastListener [UZBroadCastListener]
     */
    fun setUZBroadcastListener(uzBroadCastListener: UZBroadCastListener?) {
        this.uzBroadCastListener = uzBroadCastListener
    }

    fun setLandscape(isLandscape: Boolean) {
        cameraHelper?.setLandscape(isLandscape)
    }

    /**
     * Must be called when the app go to resume state
     */
    fun onResume() {
        if (isRunInBackground && useCamera2) {
            // nothing
        } else {
            checkAndResumeLiveStreamIfNeeded()
            if (isFromBackgroundTooLong) {
                uzBroadCastListener?.onBackgroundTooLong()
                isFromBackgroundTooLong = false
            }
        }
    }

    /**
     * Set duration which allows broadcasting to keep the info
     *
     * @param duration the duration which allows broadcasting to keep the info
     */
    fun setBackgroundAllowedDuration(duration: Long) {
        backgroundAllowedDuration = duration
    }

    private fun checkAndResumeLiveStreamIfNeeded() {
        cancelBackgroundTimer()
        if (!isBroadcastingBeforeGoingBackground) {
            return
        }
        isBroadcastingBeforeGoingBackground = false
        // We delay a second because the surface need to be resumed before we can prepare something
        // Improve this method whenever you can
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                stopBroadCast() // make sure stop stream and start it again
                if (prepareBroadCast() && !TextUtils.isEmpty(mainBroadCastUrl)) {
                    startBroadCast(
                        mainBroadCastUrl
                    )
                }
            } catch (exception: Exception) {
//                Cannot resume broadcasting right now!
                exception.printStackTrace()
            }
        }, SECOND)
    }

    private fun startBackgroundTimer() {
        if (backgroundTimer == null) {
            backgroundTimer = object : CountDownTimer(backgroundAllowedDuration, SECOND) {
                override fun onTick(millisUntilFinished: Long) {
                    // Nothing
                }

                override fun onFinish() {
                    isBroadcastingBeforeGoingBackground = false
                    isFromBackgroundTooLong = true
                }
            }
        }
        backgroundTimer?.start()
    }

    private fun cancelBackgroundTimer() {
        if (backgroundTimer != null) {
            backgroundTimer?.cancel()
            backgroundTimer = null
        }
    }

    /**
     * you must call in onInit()
     *
     * @param uzCameraChangeListener : [UZCameraChangeListener] camera witch listener
     */
    fun setUZCameraChangeListener(uzCameraChangeListener: UZCameraChangeListener?) {
        cameraHelper?.setUZCameraChangeListener(uzCameraChangeListener)
    }

    /**
     * you must call in oInit()
     *
     * @param recordListener : record status listener [UZRecordListener]
     */
    fun setUZRecordListener(recordListener: UZRecordListener?) {
        cameraHelper?.setUZRecordListener(recordListener)
    }

    fun hideLiveStatus() {
        tvLiveStatus.visibility = GONE
        tvLiveStatus.clearAnimation()
    }

    /**
     * run on main Thread
     */
    fun showLiveStatus() {
        tvLiveStatus.visibility = VISIBLE
        blinking(v = tvLiveStatus)
    }

    /**
     * Each video encoder configuration corresponds to a set of video parameters, including the resolution, frame rate, bitrate, and video orientation.
     * The parameters specified in this method are the maximum values under ideal network conditions.
     * If the video engine cannot render the video using the specified parameters due to poor network conditions,
     * the parameters further down the list are considered until a successful configuration is found.
     *
     *
     * If you do not set the video encoder configuration after joining the channel,
     * you can call this method before calling the enableVideo method to reduce the render time of the first video frame.
     *
     * @param attributes The local video encoder configuration
     */
    fun setVideoAttributes(attributes: VideoAttributes?) {
        videoAttributes = attributes
        cameraHelper?.setVideoAttributes(attributes)
    }

    fun setAudioAttributes(audioAttributes: AudioAttributes?) {
        this.audioAttributes = audioAttributes
        cameraHelper?.setAudioAttributes(audioAttributes)
    }

    /**
     * Please call [.prepareBroadCast] before use
     *
     * @param broadCastUrl: Stream Url
     */
    fun startBroadCast(broadCastUrl: String?) {
        mainBroadCastUrl = broadCastUrl
        pb.visibility = VISIBLE
        if (isRunInBackground && useCamera2) {
            val intent = Intent(context.applicationContext, UZRTMPService::class.java)
            intent.putExtra(UZRTMPService.EXTRA_BROAD_CAST_URL, broadCastUrl)
            context.startService(intent)
        } else {
            cameraHelper?.startBroadCast(broadCastUrl)
        }
    }

    val isBroadCasting: Boolean
        get() = if (isRunInBackground) isMyServiceRunning(
            context,
            UZRTMPService::class.java
        ) else {
            cameraHelper != null && cameraHelper?.isBroadCasting == true
        }

    fun stopBroadCast() {
        cameraHelper?.stopBroadCast()
    }

    fun switchCamera() {
        cameraHelper?.switchCamera()
    }

    /**
     * @param savePath path of save file
     * throws IOException
     * required: <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"></uses-permission>
     */
    @Throws(IOException::class)
    fun startRecord(savePath: String?) {
        cameraHelper?.startRecord(savePath)
    }

    /**
     * Check recording
     *
     * @return true if recording
     */
    val isRecording: Boolean
        get() = cameraHelper?.isRecording ?: false

    fun stopRecord() {
        cameraHelper?.stopRecord()
    }

    /**
     * Take a photo
     *
     *
     * param callback
     */
    fun takePhoto(callback: UZTakePhotoCallback) {
        cameraHelper?.takePhoto(callback)
    }

    /**
     * Call this method before use [.startBroadCast].
     * Auto detect rotation to prepare for BroadCast
     *
     * @return true if success, false if you get a error (Normally because the encoder selected
     * * doesn't support any configuration seated or your device hasn't a AAC encoder).
     */
    fun prepareBroadCast(): Boolean {
        if (cameraHelper == null) {
            return false
        }
        cameraHelper?.setAudioAttributes(audioAttributes)
        cameraHelper?.setVideoAttributes(videoAttributes)
        return cameraHelper?.prepareBroadCast() ?: false
    }

    /**
     * Call this method before use [.startBroadCast].
     *
     * @param isLandscape:
     * @return true if success, false if you get a error (Normally because the encoder selected
     * * doesn't support any configuration seated or your device hasn't a AAC encoder).
     */
    fun prepareBroadCast(isLandscape: Boolean): Boolean {
        if (cameraHelper == null) {
            return false
        }
        cameraHelper?.setAudioAttributes(audioAttributes)
        cameraHelper?.setVideoAttributes(videoAttributes)
        return cameraHelper?.prepareBroadCast(isLandscape = isLandscape) ?: false
    }

    /**
     * @param audioAttributes [AudioAttributes] null with out audio
     * @param videoAttributes [VideoAttributes]
     * @param isLandscape:    true if broadcast landing
     * @return true if success, false if you get a error (Normally because the encoder selected
     * doesn't support any configuration seated or your device hasn't a AAC encoder).
     */
    fun prepareBroadCast(
        audioAttributes: AudioAttributes?,
        videoAttributes: VideoAttributes,
        isLandscape: Boolean
    ): Boolean {
        this.videoAttributes = videoAttributes
        this.audioAttributes = audioAttributes
        return cameraHelper?.prepareBroadCast(
            audioAttributes = audioAttributes,
            videoAttributes = videoAttributes,
            isLandscape = isLandscape
        )
            ?: false
    }

    fun enableAA(enable: Boolean) {
        spriteGestureController.baseObjectFilterRender = null
        cameraHelper?.enableAA(enable)
    }

    fun isAAEnabled(): Boolean {
        return cameraHelper?.isAAEnabled ?: false
    }

    fun setFilter(filterRender: FilterRender) {
        cameraHelper?.setFilter(
            filterPosition = FILTER_POSITION,
            filterReader = filterRender.filterRender
        )
    }

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        if (spriteGestureController.spriteTouched(view, motionEvent)) {
            spriteGestureController.moveSprite(view, motionEvent)
            spriteGestureController.scaleSprite(motionEvent)
            return true
        }
        //        else {
//            int action = motionEvent.getAction();
//            if (motionEvent.getPointerCount() > 1) {
//                if (action == MotionEvent.ACTION_MOVE) {
//                    cameraHelper.setZoom(motionEvent);
//                }
//            }
//        }
        return false
    }

    val streamWidth: Int
        get() = cameraHelper?.streamWidth ?: 0

    // SETTER
    val streamHeight: Int
        get() = cameraHelper?.streamHeight ?: 0

    fun setVideoBitrateOnFly(bitrate: Int) {
        cameraHelper?.setVideoBitrateOnFly(bitrate)
    }

    fun enableAudio() {
        cameraHelper?.enableAudio()
    }

    fun disableAudio() {
        cameraHelper?.disableAudio()
    }

    val isAudioMuted: Boolean
        get() = cameraHelper?.isAudioMuted ?: false

    val isVideoEnabled: Boolean
        get() = cameraHelper?.isVideoEnabled ?: false

    /**
     * Check support Flashlight
     * if use Camera1 always return false
     *
     * @return true if support, false if not support.
     */
    val isLanternSupported: Boolean
        get() = cameraHelper?.isLanternSupported ?: false

    /**
     * required: <uses-permission android:name="android.permission.FLASHLIGHT"></uses-permission>
     */
    @Throws(Exception::class)
    fun enableLantern() {
        cameraHelper?.enableLantern()
    }

    /**
     * required: <uses-permission android:name="android.permission.FLASHLIGHT"></uses-permission>
     */
    fun disableLantern() {
        cameraHelper?.disableLantern()
    }

    val isLanternEnabled: Boolean
        get() = cameraHelper?.isLanternEnabled ?: false

    /**
     * Clear Watermark
     */
    fun clearWatermark() {
        if (cameraHelper == null) {
            return
        }
        spriteGestureController.baseObjectFilterRender = null
        cameraHelper?.setFilter(
            filterPosition = WATERMARK_POSITION,
            filterReader = NoFilterRender()
        )
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
        if (cameraHelper == null) {
            return
        }
        spriteGestureController.baseObjectFilterRender = null
        val textRender = TextObjectFilterRender()
        textRender.setText(text, textSize, color)
        cameraHelper?.let {
            it.setFilter(WATERMARK_POSITION, textRender)
            textRender.setDefaultScale(it.streamWidth, it.streamHeight)
        }
        textRender.setPosition(position.translateTo)
        spriteGestureController.baseObjectFilterRender = textRender
    }

    /**
     * Watermark with image
     *
     * @param imageRes The resource id of the image data
     * @param scale    Scale in percent
     * @param position of image
     */
    fun setImageWatermark(@DrawableRes imageRes: Int, scale: PointF, position: Translate) {
        setImageWatermark(BitmapFactory.decodeResource(resources, imageRes), scale, position)
    }

    /**
     * Watermark with image
     *
     * @param bitmap   the decoded bitmap
     * @param scale    Scale in percent
     * @param position of image
     */
    fun setImageWatermark(bitmap: Bitmap, scale: PointF, position: Translate) {
        if (cameraHelper == null) {
            return
        }
        spriteGestureController.baseObjectFilterRender = null
        val imageRender = ImageObjectFilterRender()
        cameraHelper?.setFilter(WATERMARK_POSITION, imageRender)
        imageRender.setImage(bitmap)
        imageRender.setScale(scale.x, scale.y)
        imageRender.setPosition(position.translateTo)
        spriteGestureController.baseObjectFilterRender = imageRender //Optional
        spriteGestureController.setPreventMoveOutside(false) //Optional
    }

    /**
     * Watermark with gif
     *
     * @param gifRaw   The resource identifier to open, as generated by the aapt tool.
     * @param scale    Scale in percent
     * @param position of gif
     */
    fun setGifWatermark(@RawRes gifRaw: Int, scale: PointF, position: Translate) {
        setGifWatermark(
            inputStream = resources.openRawResource(gifRaw),
            scale = scale,
            position = position
        )
    }

    /**
     * Watermark with gif
     *
     * @param inputStream Access to the resource data.
     * @param scale       Scale in percent
     * @param position    of gif
     */
    fun setGifWatermark(inputStream: InputStream?, scale: PointF, position: Translate) {
        if (cameraHelper == null) {
            return
        }
        spriteGestureController.baseObjectFilterRender = null
        try {
            val gifRender = GifObjectFilterRender()
            gifRender.setGif(inputStream)
            cameraHelper?.setFilter(WATERMARK_POSITION, gifRender)
            gifRender.setScale(scale.x, scale.y)
            gifRender.setPosition(position.translateTo)
            spriteGestureController.baseObjectFilterRender = gifRender
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Watermark with video from resource
     *
     * @param videoRes Resource of video ex: raw file
     * @param position of video
     */
    fun setVideoWatermarkByResource(@RawRes videoRes: Int, position: Translate) {
        //Video is 360x240 so select a percent to keep aspect ratio (50% x 33.3% screen)
        setVideoWatermarkByResource(videoRes, PointF(50f, 33.3f), position)
    }

    /**
     * Watermark with video
     *
     * @param videoRes the raw resource id (<var>R.raw.&lt;something></var>) for
     * the resource to use as the datasource
     * @param scale    Scale in percent
     * @param position of video
     */
    fun setVideoWatermarkByResource(@RawRes videoRes: Int, scale: PointF, position: Translate) {
        if (cameraHelper == null) {
            return
        }
        spriteGestureController.baseObjectFilterRender = null
        val surfaceReadyCallback =
            SurfaceFilterRender.SurfaceReadyCallback { surfaceTexture: SurfaceTexture? ->
                //You can render this filter with other api that draw in a surface. for example you can use VLC
                val mediaPlayer: MediaPlayer =
                    MediaPlayer.create(this@UZBroadCastView.context, videoRes)
                mediaPlayer.setSurface(Surface(surfaceTexture))
                mediaPlayer.start()
            }
        setVideoWatermarkCallback(
            surfaceReadyCallback = surfaceReadyCallback,
            scale = scale,
            position = position
        )
    }

    /**
     * Watermark with video
     *
     * @param videoUri the Uri from which to get the datasource
     * @param position of video
     */
    fun setVideoWatermarkByUri(videoUri: Uri, position: Translate) {
        //Video is 360x240 so select a percent to keep aspect ratio (50% x 33.3% screen)
        setVideoWatermarkByUri(videoUri = videoUri, scale = PointF(50f, 33.3f), position = position)
    }

    /**
     * Watermark with video
     *
     * @param videoUri the Uri from which to get the datasource
     * @param scale    Scale in percent
     * @param position of video
     */
    fun setVideoWatermarkByUri(videoUri: Uri, scale: PointF, position: Translate) {
        if (cameraHelper == null) {
            return
        }
        spriteGestureController.baseObjectFilterRender = null
        val surfaceReadyCallback =
            SurfaceFilterRender.SurfaceReadyCallback { surfaceTexture: SurfaceTexture? ->
                val mediaPlayer: MediaPlayer =
                    MediaPlayer.create(this@UZBroadCastView.context, videoUri)
                mediaPlayer.setSurface(Surface(surfaceTexture))
                mediaPlayer.start()
            }
        setVideoWatermarkCallback(
            surfaceReadyCallback = surfaceReadyCallback,
            scale = scale,
            position = position
        )
    }

    /**
     * Watermark with video
     *
     * @param surfaceReadyCallback SurfaceReadyCallback
     * @param scale                Scale in percent
     * @param position             of video
     */
    private fun setVideoWatermarkCallback(
        surfaceReadyCallback: SurfaceFilterRender.SurfaceReadyCallback,
        scale: PointF,
        position: Translate
    ) {
        val surfaceFilterRender = SurfaceFilterRender(surfaceReadyCallback)
        cameraHelper?.setFilter(WATERMARK_POSITION, surfaceFilterRender)
        surfaceFilterRender.setScale(scale.x, scale.y)
        surfaceFilterRender.setPosition(position.translateTo)
        spriteGestureController.baseObjectFilterRender = surfaceFilterRender //Optional
    }
}
