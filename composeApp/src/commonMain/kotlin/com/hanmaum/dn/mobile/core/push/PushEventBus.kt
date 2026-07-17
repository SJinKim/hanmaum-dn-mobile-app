package com.hanmaum.dn.mobile.core.push

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow

/** Bridge between platform push callbacks (service/AppDelegate) and common code. */
object PushEventBus {
    val tokenRefreshes = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val notificationTaps = MutableSharedFlow<PushTapPayload>(replay = 1, extraBufferCapacity = 1)

    @ExperimentalCoroutinesApi
    fun consumeTap() {
        notificationTaps.resetReplayCache()
    }
}
