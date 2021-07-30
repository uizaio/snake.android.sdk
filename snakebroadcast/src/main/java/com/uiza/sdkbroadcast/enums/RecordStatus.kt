package com.uiza.sdkbroadcast.enums

import com.pedro.rtplibrary.util.RecordController
import java.util.*

enum class RecordStatus(
    val status: RecordController.Status
) {
    STARTED(RecordController.Status.STARTED),
    STOPPED(RecordController.Status.STOPPED),
    RECORDING(RecordController.Status.RECORDING),
    PAUSED(RecordController.Status.PAUSED),
    RESUMED(RecordController.Status.RESUMED);

    companion object {
        private val valueMaps: MutableMap<RecordController.Status, RecordStatus> = EnumMap(
            RecordController.Status::class.java
        )

        @JvmStatic
        fun lookup(status: RecordController.Status): RecordStatus? {
            return valueMaps[status]
        }

        init {
            for (s in values()) {
                valueMaps[s.status] = s
            }
        }
    }

}
