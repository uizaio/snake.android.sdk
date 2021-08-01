package com.uiza.sdkbroadcast.util

import android.os.Build
import java.util.*
import java.util.stream.Collectors

object ListUtils {
    fun <T> filter(list: List<T>, pre: Pre<T, Boolean>): List<T> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            list.stream().filter { item: T -> pre[item] }.collect(Collectors.toList())
        } else {
            val col: MutableList<T> = ArrayList()
            for (i in list.indices) if (pre[list[i]]) col.add(list[i])
            col
        }
    }

    @JvmStatic
    fun <T, R> map(list: List<T>, pre: Pre<T, R>): List<R> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            list.stream().map { item: T -> pre[item] }.collect(Collectors.toList())
        } else {
            val cols: MutableList<R> = ArrayList()
            for (i in list.indices) {
                cols.add(pre[list[i]])
            }
            cols
        }
    }

    interface Pre<T, R> {
        operator fun get(item: T): R
    }
}
