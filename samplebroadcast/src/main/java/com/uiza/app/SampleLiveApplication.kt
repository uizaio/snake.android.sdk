package com.uiza.app

import androidx.multidex.MultiDexApplication
import com.uiza.activity.R
import com.uiza.sdkbroadcast.UZBroadCast

class SampleLiveApplication : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()

        UZBroadCast.init(R.drawable.snake)
    }
}
