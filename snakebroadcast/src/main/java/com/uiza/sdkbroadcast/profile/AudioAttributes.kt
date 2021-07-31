package com.uiza.sdkbroadcast.profile

import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import android.util.Log
import com.uiza.sdkbroadcast.util.ValidValues.check
import java.util.*

class AudioAttributes : Parcelable {
    /**
     * Returns the bitrate value for the encoding process.
     *
     * @return The bitrate value for the encoding process.
     */
    /**
     * AAC in kb.
     * The bitrate value for the encoding process.
     * default value 64*1024 (64kps)
     */
    var bitRate = 64 * 1024
        private set
    /**
     * Returns the samplingRate value for the encoding process.
     *
     * @return the sampleRate value for the encoding process.
     */
    /**
     * The sampleRate value for the audio encoding process.
     * default value 44100 (44.1 KHz)
     */
    var sampleRate = 44100 // Hz
        private set
    /**
     * Returns the channels value (1=mono, 2=stereo, 4=quad) for the encoding process.
     *
     * @return The channels value (1=mono, 2=stereo, 4=quad) for the encoding process.
     */
    /**
     * true stereo, false mono.
     * default value true (stereo)
     */
    var isStereo = true
        private set

    /**
     * true enable echo canceler, false disable.
     */
    var isEchoCanceler = false
        private set

    /**
     * true enable noise suppressor, false  disable.
     */
    var isNoiseSuppressor = false
        private set

    private constructor() {}

    private constructor(`in`: Parcel) {
        bitRate = `in`.readInt()
        sampleRate = `in`.readInt()
        isStereo = `in`.readInt() == 1
        isEchoCanceler = `in`.readInt() == 1
        isNoiseSuppressor = `in`.readInt() == 1
    }

    private constructor(
        bitRate: Int,
        sampleRate: Int,
        stereo: Boolean,
        echoCanceler: Boolean,
        noiseSuppressor: Boolean
    ) {
        check(value = bitRate, min = 1, max = 256 * 1024) // max 256 Kbps
        check(value = sampleRate, min = 1, max = 48000) // max 48 KHz
        this.bitRate = bitRate
        this.sampleRate = sampleRate
        isStereo = stereo
        isEchoCanceler = echoCanceler
        isNoiseSuppressor = noiseSuppressor
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(bitRate)
        dest.writeInt(sampleRate)
        dest.writeInt(if (isStereo) 1 else 0)
        dest.writeInt(if (isEchoCanceler) 1 else 0)
        dest.writeInt(if (isNoiseSuppressor) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    /**
     * Sets the bitrate value for the encoding process.
     *
     * @param bitRate The bitrate value for the encoding process.
     * @return this instance
     */
    fun setBitRate(bitRate: Int): AudioAttributes {
        this.bitRate = bitRate
        return this
    }

    /**
     * Sets the samplingRate value for the encoding process.
     *
     * @param sampleRate The samplingRate value for the encoding process.
     * @return this instance
     */
    fun setSampleRate(sampleRate: Int): AudioAttributes {
        this.sampleRate = sampleRate
        return this
    }

    /**
     * Sets the value (false=mono, true=stereo) for the encoding process.
     *
     * @param stereo The value (false=mono, true=stereo) for the encoding
     * process.
     * @return this instance
     */
    fun setStereo(stereo: Boolean): AudioAttributes {
        isStereo = stereo
        return this
    }

    /**
     * see [AcousticEchoCanceler.isAvailable]
     *
     * @param echoCanceler boolean value
     * @return this instance
     */
    fun setEchoCanceler(echoCanceler: Boolean): AudioAttributes {
        isEchoCanceler = echoCanceler && AcousticEchoCanceler.isAvailable()
        return this
    }
    ///
    /**
     * see [NoiseSuppressor.isAvailable]
     *
     * @param noiseSuppressor boolean value
     * @return this instance
     */
    fun setNoiseSuppressor(noiseSuppressor: Boolean): AudioAttributes {
        isNoiseSuppressor = noiseSuppressor && NoiseSuppressor.isAvailable()
        return this
    }

    override fun toString(): String {
        return String.format(
            Locale.getDefault(),
            "AudioAttributes:(bitRate: %d, sampleRate: %d, stereo: %b, echoCanceler: %b, noiseSuppressor: %b)",
            bitRate, sampleRate, isStereo, isEchoCanceler, isNoiseSuppressor
        )
    }

    companion object {
        private val tag = AudioAttributes::class.java.simpleName

        @JvmField
        val CREATOR: Creator<AudioAttributes?> = object : Creator<AudioAttributes?> {
            override fun createFromParcel(`in`: Parcel): AudioAttributes {
                return AudioAttributes(`in`)
            }

            override fun newArray(size: Int): Array<AudioAttributes?> {
                return arrayOfNulls(size)
            }
        }

        @JvmStatic
        fun create(bitRate: Int, sampleRate: Int, stereo: Boolean): AudioAttributes {
            Log.e(tag, "echo = %b" + AcousticEchoCanceler.isAvailable())
            return AudioAttributes(
                bitRate = bitRate,
                sampleRate = sampleRate,
                stereo = stereo,
                echoCanceler = AcousticEchoCanceler.isAvailable(),
                noiseSuppressor = NoiseSuppressor.isAvailable()
            )
        }
    }
}
