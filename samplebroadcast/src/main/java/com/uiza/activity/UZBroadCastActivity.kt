package com.uiza.activity

import android.Manifest
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PointF
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import androidx.preference.PreferenceManager
import com.uiza.common.Constant
import com.uiza.sdkbroadcast.enums.FilterRender
import com.uiza.sdkbroadcast.enums.RecordStatus
import com.uiza.sdkbroadcast.enums.Translate
import com.uiza.sdkbroadcast.interfaces.UZBroadCastListener
import com.uiza.sdkbroadcast.interfaces.UZCameraChangeListener
import com.uiza.sdkbroadcast.interfaces.UZCameraOpenException
import com.uiza.sdkbroadcast.interfaces.UZRecordListener
import com.uiza.sdkbroadcast.profile.AudioAttributes.Companion.create
import com.uiza.sdkbroadcast.profile.VideoAttributes
import com.uiza.sdkbroadcast.profile.VideoAttributes.Companion.FHD_1080p
import com.uiza.sdkbroadcast.profile.VideoAttributes.Companion.HD_720p
import com.uiza.sdkbroadcast.profile.VideoAttributes.Companion.SD_360p
import com.uiza.sdkbroadcast.profile.VideoAttributes.Companion.SD_480p
import kotlinx.android.synthetic.main.activity_broad_cast.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class UZBroadCastActivity : AppCompatActivity(), UZBroadCastListener,
    UZRecordListener, UZCameraChangeListener {

    companion object {
        private val logTag = UZBroadCastActivity::class.java.simpleName
        private const val RECORD_FOLDER = "uzbroadcast"
        private const val REQUEST_CODE = 1001
    }

    private var popupMenu: PopupMenu? = null
    private var preferences: SharedPreferences? = null
    private var broadCastUrl: String? = null
    private var currentDateAndTime = ""
    private var folder: File? = null

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
        setContentView(R.layout.activity_broad_cast)
        preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        setupViews()
    }

    private fun setupViews() {
        uzBroadCastView.setUZBroadcastListener(this)
        btnStartStop.setOnClickListener {
            if (uzBroadCastView.isBroadCasting) {
                uzBroadCastView.stopBroadCast()
            } else {
                if (uzBroadCastView.isRecording || uzBroadCastView.prepareBroadCast()) {
                    uzBroadCastView.startBroadCast(broadCastUrl)
                    btnStartStop.isChecked = true
                } else {
                    showToast("Error preparing stream, This device cannot do it")
                }
            }
        }
        btnStartStop.isEnabled = false
        btnRecord.setOnClickListener {
            if (uzBroadCastView.isRecording) {
                uzBroadCastView.stopRecord()
            } else {
                ActivityCompat.requestPermissions(
                    this@UZBroadCastActivity,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_CODE
                )
            }
        }
        btnAudio.setOnClickListener {
            if (uzBroadCastView.isAudioMuted) {
                uzBroadCastView.enableAudio()
            } else {
                uzBroadCastView.disableAudio()
            }
            btnAudio.isChecked = uzBroadCastView.isAudioMuted
        }
        btnMenu.setOnClickListener {
            if (popupMenu == null) {
                setPopupMenu()
            }
            popupMenu?.show()
        }
        btnSwitchCamera.setOnClickListener {
            try {
                uzBroadCastView.switchCamera()
            } catch (e: UZCameraOpenException) {
                e.message?.let {
                    showToast(it)
                }
            }
        }

        val movieFolder = getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        movieFolder?.let { file ->
            folder = File(file.absolutePath + RECORD_FOLDER)
        }

        broadCastUrl = intent.getStringExtra(Constant.EXTRA_STREAM_ENDPOINT)
        if (TextUtils.isEmpty(broadCastUrl)) {
            showToast("broadCastUrl cannot be null or empty")
            onBackPressed()
        }
        try {
            val profile =
                preferences?.getString(
                    Constant.PREF_CAMERA_PROFILE,
                    Constant.DEFAULT_CAMERA_PROFILE
                )
                    ?.toIntOrNull()

            val maxBitrate =
                preferences?.getString(Constant.PREF_VIDEO_BITRATE, Constant.DEFAULT_MAX_BITRATE)
                    ?.toIntOrNull()

            val fps = preferences?.getString(Constant.PREF_FPS, Constant.DEFAULT_FPS)?.toIntOrNull()

            val frameInterval =
                preferences?.getString(
                    Constant.PREF_FRAME_INTERVAL,
                    Constant.DEFAULT_FRAME_INTERVAL
                )
                    ?.toIntOrNull()

            val audioBitrate =
                preferences?.getString(Constant.PREF_AUDIO_BITRATE, Constant.DEFAULT_AUDIO_BITRATE)
                    ?.toIntOrNull()

            val audioSampleRate =
                preferences?.getString(Constant.PREF_SAMPLE_RATE, Constant.DEFAULT_SAMPLE_RATE)
                    ?.toIntOrNull()

            val stereo =
                preferences?.getBoolean(Constant.PREF_AUDIO_STEREO, Constant.DEFAULT_AUDIO_STEREO)

            if (profile == null || maxBitrate == null || fps == null || frameInterval == null
                || audioBitrate == null || audioSampleRate == null || stereo == null
            ) {
                showToast("Invalid config")
                return
            }

            val videoAttributes: VideoAttributes = when (profile) {
                1080 -> FHD_1080p(
                    frameRate = fps,
                    bitRate = maxBitrate,
                    frameInterval = frameInterval
                )
                480 -> SD_480p(
                    frameRate = fps,
                    bitRate = maxBitrate,
                    frameInterval = frameInterval
                )
                360 -> SD_360p(
                    frameRate = fps,
                    bitRate = maxBitrate,
                    frameInterval = frameInterval
                )
                else -> HD_720p(
                    frameRate = fps,
                    bitRate = maxBitrate,
                    frameInterval = frameInterval
                )
            }
            val audioAttributes = create(
                bitRate = audioBitrate,
                sampleRate = audioSampleRate,
                stereo = stereo
            )
            uzBroadCastView.setVideoAttributes(videoAttributes)
            uzBroadCastView.setAudioAttributes(audioAttributes)
            uzBroadCastView.setBackgroundAllowedDuration(10000)
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        uzBroadCastView.onResume()
        btnStartStop.isChecked = uzBroadCastView.isBroadCasting
        super.onResume()
    }

    override fun onBackPressed() {
        if (uzBroadCastView.isBroadCasting) {
            showExitDialog()
        } else {
            super.onBackPressed()
        }
    }

    private fun showExitDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Stop")
        builder.setMessage("Do you want to stop?")
        builder.setPositiveButton("OK") { dialog: DialogInterface, _: Int ->
            super.onBackPressed()
            uzBroadCastView.stopBroadCast()
            dialog.dismiss()
            onBackPressed()
        }
        builder.setNegativeButton("Cancel") { dialog: DialogInterface, _: Int ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun onMenuItemSelected(item: MenuItem): Boolean {
//        Stop listener for image, text and gif stream objects.
//        openGlView.setFilter(null);
        return when (item.itemId) {
            R.id.e_d_fxaa -> {
                uzBroadCastView.enableAA(enable = !uzBroadCastView.isAAEnabled())
                showToast("FXAA " + if (uzBroadCastView.isAAEnabled()) "enabled" else "disabled")
                true
            }
            R.id.no_filter -> {
                uzBroadCastView.setFilter(FilterRender.None)
                true
            }
            R.id.analog_tv -> {
                uzBroadCastView.setFilter(FilterRender.AnalogTV)
                true
            }
            R.id.android_view -> {
                val androidRender = FilterRender.AndroidView
                androidRender.setView(btnSwitchCamera)
                uzBroadCastView.setFilter(androidRender)
                true
            }
            R.id.basic_deformation -> {
                uzBroadCastView.setFilter(FilterRender.BasicDeformation)
                true
            }
            R.id.beauty -> {
                uzBroadCastView.setFilter(FilterRender.Beauty)
                true
            }
            R.id.black -> {
                uzBroadCastView.setFilter(FilterRender.Black)
                true
            }
            R.id.blur -> {
                uzBroadCastView.setFilter(FilterRender.Blur)
                true
            }
            R.id.brightness -> {
                uzBroadCastView.setFilter(FilterRender.Brightness)
                true
            }
            R.id.cartoon -> {
                uzBroadCastView.setFilter(FilterRender.Cartoon)
                true
            }
            R.id.circle -> {
                uzBroadCastView.setFilter(FilterRender.Circle)
                true
            }
            R.id.color -> {
                uzBroadCastView.setFilter(FilterRender.Color)
                true
            }
            R.id.contrast -> {
                uzBroadCastView.setFilter(FilterRender.Contrast)
                true
            }
            R.id.duotone -> {
                uzBroadCastView.setFilter(FilterRender.Duotone)
                true
            }
            R.id.early_bird -> {
                uzBroadCastView.setFilter(FilterRender.EarlyBird)
                true
            }
            R.id.edge_detection -> {
                uzBroadCastView.setFilter(FilterRender.EdgeDetection)
                true
            }
            R.id.exposure -> {
                uzBroadCastView.setFilter(FilterRender.Exposure)
                true
            }
            R.id.fire -> {
                uzBroadCastView.setFilter(FilterRender.Fire)
                true
            }
            R.id.gamma -> {
                uzBroadCastView.setFilter(FilterRender.Gamma)
                true
            }
            R.id.glitch -> {
                uzBroadCastView.setFilter(FilterRender.Glitch)
                true
            }
            R.id.grey_scale -> {
                uzBroadCastView.setFilter(FilterRender.GreyScale)
                true
            }
            R.id.halftone_lines -> {
                uzBroadCastView.setFilter(FilterRender.HalftoneLines)
                true
            }
            R.id.image_70s -> {
                uzBroadCastView.setFilter(FilterRender.Image70s)
                true
            }
            R.id.lamoish -> {
                uzBroadCastView.setFilter(FilterRender.Lamoish)
                true
            }
            R.id.money -> {
                uzBroadCastView.setFilter(FilterRender.Money)
                true
            }
            R.id.negative -> {
                uzBroadCastView.setFilter(FilterRender.Negative)
                true
            }
            R.id.pixelated -> {
                uzBroadCastView.setFilter(FilterRender.Pixelated)
                true
            }
            R.id.polygonization -> {
                uzBroadCastView.setFilter(FilterRender.Polygonization)
                true
            }
            R.id.rainbow -> {
                uzBroadCastView.setFilter(FilterRender.Rainbow)
                true
            }
            R.id.rgb_saturate -> {
                val rgbSaturation = FilterRender.RGBSaturation
                uzBroadCastView.setFilter(rgbSaturation)
                //Reduce green and blue colors 20%. Red will predominate.
                rgbSaturation.setRGBSaturation(r = 1f, g = 0.8f, b = 0.8f)
                true
            }
            R.id.ripple -> {
                uzBroadCastView.setFilter(FilterRender.Ripple)
                true
            }
            R.id.rotation -> {
                val rotationRender = FilterRender.Rotation
                uzBroadCastView.setFilter(rotationRender)
                rotationRender.setRotation(90)
                true
            }
            R.id.saturation -> {
                uzBroadCastView.setFilter(FilterRender.Saturation)
                true
            }
            R.id.sepia -> {
                uzBroadCastView.setFilter(FilterRender.Sepia)
                true
            }
            R.id.sharpness -> {
                uzBroadCastView.setFilter(FilterRender.Sharpness)
                true
            }
            R.id.snow -> {
                uzBroadCastView.setFilter(FilterRender.Snow)
                true
            }
            R.id.swirl -> {
                uzBroadCastView.setFilter(FilterRender.Swirl)
                true
            }
            R.id.temperature -> {
                uzBroadCastView.setFilter(FilterRender.Temperature)
                true
            }
            R.id.zebra -> {
                uzBroadCastView.setFilter(FilterRender.Zebra)
                true
            }
            R.id.clear_watermark -> {
                uzBroadCastView.clearWatermark()
                true
            }
            R.id.text -> {
                uzBroadCastView.setTextWatermark(
                    text = getString(R.string.app_name),
                    textSize = 22f,
                    color = Color.RED,
                    position = Translate.CENTER
                )
                true
            }
            R.id.image -> {
                uzBroadCastView.setImageWatermark(
                    imageRes = R.drawable.logo,
                    scale = PointF(20f, 15f),
                    position = Translate.TOP_LEFT
                )
                true
            }
            R.id.gif -> {
                uzBroadCastView.setGifWatermark(
                    gifRaw = R.raw.banana,
                    scale = PointF(20f, 15f),
                    position = Translate.CENTER
                )
                true
            }
            R.id.surface_filter -> {
                uzBroadCastView.setVideoWatermarkByResource(
                    videoRes = R.raw.big_bunny_240p,
                    position = Translate.CENTER
                )
                true
            }
            else -> false
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                actionRecord()
            }
            return
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun actionRecord() {
        try {
            folder?.let { fd ->
                if (!fd.exists()) {
                    try {
                        val result = fd.mkdir()
                        Log.d(logTag, "result$result")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                currentDateAndTime = sdf.format(Date())
                if (!uzBroadCastView.isBroadCasting) {
                    if (uzBroadCastView.prepareBroadCast()) {
                        uzBroadCastView.startRecord(savePath = fd.absolutePath + "/" + currentDateAndTime + ".mp4")
                    } else {
                        showToast("Error preparing stream, this device cannot do it")
                    }
                } else {
                    uzBroadCastView.startRecord(savePath = fd.absolutePath + "/" + currentDateAndTime + ".mp4")
                }
            }
        } catch (e: IOException) {
            uzBroadCastView.stopRecord()
            btnRecord.setImageDrawable(
                ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.ic_record_white_24,
                    null
                )
            )
            e.message?.let {
                showToast(it)
            }
        }
    }

    private fun setPopupMenu() {
        popupMenu = PopupMenu(this, btnMenu)
        popupMenu?.menuInflater?.inflate(R.menu.menu_gl, popupMenu?.menu)
        popupMenu?.setOnMenuItemClickListener { item: MenuItem ->
            this.onMenuItemSelected(item)
        }
    }

    override fun onInit(success: Boolean) {
        btnStartStop.isEnabled = success
        btnAudio.visibility = View.GONE
        if (success) {
            uzBroadCastView.setUZCameraChangeListener(this)
            uzBroadCastView.setUZRecordListener(this)
        }
    }

    override fun onConnectionSuccess() {
        btnStartStop.isChecked = true
        btnAudio.visibility = View.VISIBLE
        btnAudio.isChecked = false
        showToast("Connection success")
    }

    override fun onRetryConnection(delay: Long) {
        showToast("Retry " + delay / 1000 + " s")
    }

    override fun onConnectionFailed(reason: String?) {
        btnStartStop.isChecked = false
        showToast("Connection failed. $reason")
    }

    override fun onDisconnect() {
        btnStartStop.isChecked = false
        btnAudio.visibility = View.GONE
        btnAudio.isChecked = false
        showToast("Disconnected")
    }

    override fun onAuthError() {
        showToast("Auth error")
    }

    override fun onAuthSuccess() {
        showToast("Auth success")
    }

    override fun surfaceCreated() {
        Log.d(logTag, "surfaceCreated")
    }

    override fun surfaceChanged(format: Int, width: Int, height: Int) {
        Log.d(logTag, "surfaceChanged: {$format, $width, $height}")
    }

    override fun surfaceDestroyed() {
        Log.d(logTag, "surfaceDestroyed")
    }

    override fun onBackgroundTooLong() {
        Toast.makeText(this, "You go to background for a long time !", Toast.LENGTH_LONG).show()
    }

    override fun onCameraChange(isFrontCamera: Boolean) {
        Log.d(logTag, "onCameraChange: $isFrontCamera")
    }

    override fun onStatusChange(status: RecordStatus?) {
        runOnUiThread {
            btnRecord.isChecked = status === RecordStatus.RECORDING
            when {
                status === RecordStatus.RECORDING -> {
                    showToast("Recording...")
                }
                status === RecordStatus.STOPPED -> {
                    currentDateAndTime = ""
                    showToast("Stopped")
                }
                else -> {
                    showToast("Record $status")
                }
            }
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
