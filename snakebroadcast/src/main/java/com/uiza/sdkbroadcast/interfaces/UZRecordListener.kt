package com.uiza.sdkbroadcast.interfaces

import com.uiza.sdkbroadcast.enums.RecordStatus

interface UZRecordListener {
    fun onStatusChange(status: RecordStatus?)
}
