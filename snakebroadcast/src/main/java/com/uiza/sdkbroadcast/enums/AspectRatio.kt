package com.uiza.sdkbroadcast.enums

enum class AspectRatio {
    /**
     * 19:9 standard aspect ratio.
     * Ex: note 10
     */
    RATIO_19_9,

    /**
     * 18:9 standard aspect ratio.
     */
    RATIO_18_9,

    /**
     * default
     * 16:9 standard aspect ratio.
     */
    RATIO_16_9,

    /**
     * 4:3 standard aspect ratio.
     */
    RATIO_4_3;

    val aspectRatio: Double
        get() = when (this) {
            RATIO_4_3 -> 4.0 / 3
            RATIO_19_9 -> 19.0 / 9
            RATIO_18_9 -> 18.0 / 9
            RATIO_16_9 -> 16.0 / 9
            else -> 16.0 / 9
        }
}
