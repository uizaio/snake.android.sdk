package com.uiza.samplebroadcast;

import androidx.multidex.MultiDexApplication;

import com.uiza.sdkbroadcast.UZBroadCast;

public class SampleLiveApplication extends MultiDexApplication {

    public static final String EXTRA_STREAM_ENDPOINT = "uiza_live_extra_stream_endpoint";

    @Override
    public void onCreate() {
        super.onCreate();

        UZBroadCast.Companion.init(R.mipmap.ic_launcher);
    }
}
