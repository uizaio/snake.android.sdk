package com.uiza.sdkbroadcast.util

import android.app.ActivityManager
import android.app.Service
import android.content.Context

object ValidValues {
    @JvmStatic
    fun check(value: Int, min: Int, max: Int) {
        require(!(value > max || value < min)) {
            String.format(
                "You must set value in [%d, %d]",
                min,
                max
            )
        }
        //        else pass
    }

    @JvmStatic
    fun <T : Service?> isMyServiceRunning(context: Context, tClass: Class<T>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (tClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}
