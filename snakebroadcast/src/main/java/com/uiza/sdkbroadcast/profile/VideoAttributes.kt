package com.uiza.sdkbroadcast.profile

import android.content.res.Resources
import android.media.MediaCodecInfo
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import com.uiza.sdkbroadcast.profile.VideoSize
import com.uiza.sdkbroadcast.util.ValidValues.check
import java.util.*

/**
 * Video Profile for Live
 * Codec H264
 */
class VideoAttributes : Parcelable {

    /**
     * Returns the video size for the encoding process.
     *
     * @return The video size for the encoding process.
     */
    /**
     * The video size for the encoding process.
     * default 1080p.
     */
    var size: VideoSize = VideoSize.FHD_1080p()
        private set
    /**
     * Returns the bitrate value for the encoding process.
     *
     * @return The bitrate value for the encoding process.
     */
    /**
     * The bitrate value for the encoding process.
     * default 6000000 (6MB)
     */
    var bitRate = MAX_BITRATE
        private set
    /**
     * Returns the frame rate value for the encoding process.
     *
     * @return The frame rate value for the encoding process.
     */
    /**
     * The frame rate value for the encoding process.
     * default 30 fps
     */
    var frameRate = 30
        private set
    /**
     * @return the Frame Interval value for the encoding process.
     */
    /**
     * The frame interval for the encoding process.
     * default 2 seconds.
     */
    var frameInterval = 2 // sec
        private set

    /**
     * @return dpi of your screen device.
     */
    // dpi of your screen device.
    var dpi = 0
        private set

    private constructor() {}

    private constructor(`in`: Parcel) {
        size = `in`.readParcelable(VideoSize::class.java.classLoader)!!
        bitRate = `in`.readInt()
        frameRate = `in`.readInt()
        frameInterval = `in`.readInt()
        dpi = `in`.readInt()
    }

    private constructor(size: VideoSize, frameRate: Int, bitRate: Int, frameInterval: Int) {
        require(size.isValid) { "You must set size in [0,0] to [1920, 1080]" }
        check(value = frameRate, min = 1, max = 60)
        check(value = bitRate, min = 1, max = 6000000) // max 6MB
        check(value = frameInterval, min = 1, max = 10)
        this.size = size
        this.bitRate = bitRate
        this.frameRate = frameRate
        this.frameInterval = frameInterval
        dpi = Resources.getSystem().displayMetrics.densityDpi
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(size, flags)
        dest.writeInt(bitRate)
        dest.writeInt(frameRate)
        dest.writeInt(frameInterval)
        dest.writeInt(dpi)
    }

    override fun describeContents(): Int {
        return 0
    }

    /**
     * Sets the bitrate value for the encoding process. If null or not specified
     * a default value will be picked.
     *
     * @param bitRate The bitrate value for the encoding process.
     * @return this instance
     */
    fun setBitRate(bitRate: Int): VideoAttributes {
        this.bitRate = bitRate
        return this
    }

    /**
     * Sets the video size for the encoding process. If null or not specified
     * the source video size will not be modified.
     *
     * @param size he video size for the encoding process.
     * @return this instance
     */
    fun setSize(size: VideoSize): VideoAttributes {
        this.size = size
        return this
    }

    /**
     * Sets the frame rate value for the encoding process. If null or not
     * specified a default value will be picked.
     *
     * @param frameRate The frame rate value for the encoding process.
     * @return this instance
     */
    fun setFrameRate(frameRate: Int): VideoAttributes {
        this.frameRate = frameRate
        return this
    }

    fun setFrameInterval(frameInterval: Int): VideoAttributes {
        this.frameInterval = frameInterval
        return this
    }

    val aVCProfile: Int
        get() = if (size.isHighResolution) MediaCodecInfo.CodecProfileLevel.AVCProfileHigh else MediaCodecInfo.CodecProfileLevel.AVCProfileMain
    val aVCProfileLevel: Int
        get() = if (size.isHighResolution) MediaCodecInfo.CodecProfileLevel.AVCLevel4 else MediaCodecInfo.CodecProfileLevel.AVCLevel31

    /**
     * Sets dpi of your screen device.
     *
     * @param dpi dpi of your screen device.
     * @return this instance
     */
    fun setDpi(dpi: Int): VideoAttributes {
        this.dpi = dpi
        return this
    }

    override fun toString(): String {
        return String.format(
            Locale.getDefault(),
            "VideoAttributes (res: %s, fps: %d, bitrate: %d, iFrameInterval: %d, dpi: %d)",
            size.toString(), frameRate, bitRate, frameInterval, dpi
        )
    }

    companion object {

        private const val MAX_BITRATE = 6000000 // 6000 kps

        @JvmField
        val CREATOR: Creator<VideoAttributes?> = object : Creator<VideoAttributes?> {
            override fun createFromParcel(`in`: Parcel): VideoAttributes {
                return VideoAttributes(`in`)
            }

            override fun newArray(size: Int): Array<VideoAttributes?> {
                return arrayOfNulls(size)
            }
        }

        @JvmStatic
        fun FHD_1080p(frameRate: Int, bitRate: Int): VideoAttributes {
            return VideoAttributes(VideoSize.FHD_1080p(), frameRate, bitRate, 2)
        }

        @JvmStatic
        fun FHD_1080p(frameRate: Int, bitRate: Int, frameInterval: Int): VideoAttributes {
            return VideoAttributes(VideoSize.FHD_1080p(), frameRate, bitRate, frameInterval)
        }

        @JvmStatic
        fun HD_720p(frameRate: Int, bitRate: Int): VideoAttributes {
            return VideoAttributes(VideoSize.HD_720p(), frameRate, bitRate, 2)
        }

        @JvmStatic
        fun HD_720p(frameRate: Int, bitRate: Int, frameInterval: Int): VideoAttributes {
            return VideoAttributes(VideoSize.HD_720p(), frameRate, bitRate, frameInterval)
        }

        @JvmStatic
        fun SD_480p(frameRate: Int, bitRate: Int): VideoAttributes {
            return VideoAttributes(VideoSize.SD_480p(), frameRate, bitRate, 2)
        }

        @JvmStatic
        fun SD_480p(frameRate: Int, bitRate: Int, frameInterval: Int): VideoAttributes {
            return VideoAttributes(VideoSize.SD_480p(), frameRate, bitRate, frameInterval)
        }

        @JvmStatic
        fun SD_360p(frameRate: Int, bitRate: Int): VideoAttributes {
            return VideoAttributes(VideoSize.SD_360p(), frameRate, bitRate, 2)
        }

        @JvmStatic
        fun SD_360p(frameRate: Int, bitRate: Int, frameInterval: Int): VideoAttributes {
            return VideoAttributes(VideoSize.SD_360p(), frameRate, bitRate, frameInterval)
        }
    }
}
