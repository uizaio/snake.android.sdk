package com.uiza.sdkbroadcast.interfaces

interface UZBroadCastListener {
    fun onInit(success: Boolean)
    fun onConnectionSuccess()
    fun onConnectionFailed(reason: String?)
    fun onRetryConnection(delay: Long)
    fun onDisconnect()
    fun onAuthError()
    fun onAuthSuccess()
    fun surfaceCreated()
    fun surfaceChanged(
        format: Int,
        width: Int,
        height: Int,
    )

    fun surfaceDestroyed()
    fun onBackgroundTooLong()
}
