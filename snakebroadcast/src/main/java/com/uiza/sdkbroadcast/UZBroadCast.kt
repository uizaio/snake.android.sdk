package com.uiza.sdkbroadcast

import androidx.annotation.DrawableRes
import org.greenrobot.eventbus.EventBus

class UZBroadCast {
    companion object {
        @get:DrawableRes
        @DrawableRes
        var iconNotify = R.mipmap.ic_launcher_round

        @JvmOverloads
        fun init(@DrawableRes iconNotify: Int = R.drawable.ic_start_live) {
            UZBroadCast.iconNotify = iconNotify
            EventBus.builder().installDefaultEventBus()
        }
    }
}
