package com.hanmaum.dn.mobile.core.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

actual class RefreshTokenUnlocker(private val store: BiometricRefreshStore) {
    actual suspend fun unlock(reason: String): UnlockResult = withContext(Dispatchers.Default) {
        if (!store.hasStored()) return@withContext UnlockResult.Empty
        // SecItemCopyMatching with the access-control item shows the system prompt.
        when (val token = store.read(reason)) {
            null -> UnlockResult.Failed // includes user cancel; treated as failed-to-unlock
            else -> UnlockResult.Success(token)
        }
    }
}

@Composable
actual fun rememberRefreshTokenUnlocker(): RefreshTokenUnlocker {
    val store = koinInject<BiometricRefreshStore>()
    return remember { RefreshTokenUnlocker(store) }
}
