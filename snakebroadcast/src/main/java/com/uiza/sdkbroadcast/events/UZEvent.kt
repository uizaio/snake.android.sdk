package com.uiza.sdkbroadcast.events

class UZEvent(
    val signal: EventSignal,
    val message: String?
) {

    constructor(message: String?) : this(EventSignal.UPDATE, message)
}
