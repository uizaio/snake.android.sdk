package com.uiza.samplebroadcast

import androidx.multidex.MultiDexApplication
import com.uiza.sdkbroadcast.UZBroadCast

class SampleLiveApplication : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()

        UZBroadCast.init(R.drawable.snake)
    }
}