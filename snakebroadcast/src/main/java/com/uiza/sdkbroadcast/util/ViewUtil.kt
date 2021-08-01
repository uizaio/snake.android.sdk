package com.uiza.sdkbroadcast.util

import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation

object ViewUtil {
    @JvmStatic
    fun blinking(v: View?) {
        val anim: Animation = AlphaAnimation(0.0f, 1.0f)
        anim.duration = 1000
        anim.startOffset = 20
        anim.repeatMode = Animation.REVERSE
        anim.repeatCount = Animation.INFINITE
        v?.startAnimation(anim)
    }
}
