package com.hanmaum.dn.mobile.core.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import platform.Security.SecAccessControlCreateWithFlags
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.errSecUserCanceled
import platform.Security.kSecAttrAccessControl
import platform.Security.kSecAttrAccessibleWhenUnlockedThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecUseAuthenticationContext
import platform.Security.kSecValueData
import platform.darwin.OSStatus
import kotlin.coroutines.resume

/**
 * iOS half of the vault: a Keychain item carrying a `SecAccessControl` built
 * with `kSecAccessControlBiometryCurrentSet`.
 *
 * The flag does two things a bare `LAContext.evaluatePolicy` check cannot. The
 * Keychain refuses to return the bytes at all without a live biometric match,
 * so there is no boolean for a hooked build to answer; and "CurrentSet" binds
 * the item to the faces enrolled when it was sealed, so adding a face later
 * invalidates it rather than granting the newcomer access.
 *
 * Paired with `...WhenUnlockedThisDeviceOnly`, which keeps the item out of
 * iCloud Keychain and device backups — a biometry-bound secret cannot be
 * synchronised anyway, and this says so explicitly.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosBiometricVault : BiometricVault {

    /**
     * The context of the last successful [open], kept so [reseal] can write the
     * rotated token back without a second prompt. Dropped as soon as it is used
     * or the vault is cleared.
     */
    private var authorised: LAContext? = null

    override fun isAvailable(): Boolean =
        LAContext().canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, null)

    override fun hasSecret(): Boolean {
        // Attributes only. Asking for kSecReturnData here would raise the
        // prompt, and merely knowing whether the switch is armed must not.
        val query = Query()
        try {
            query.putIdentity()
            query.put(kSecMatchLimit, kSecMatchLimitOne)
            return SecItemCopyMatching(query.build(), null) == errSecSuccess
        } finally {
            query.release()
        }
    }

    override suspend fun seal(
        secret: String,
        title: String,
        subtitle: String,
        cancelLabel: String,
    ): VaultResult {
        if (!isAvailable()) return VaultResult.Unavailable

        // SecItemAdd does not prompt, so the confirmation is explicit: switching
        // Face ID on should cost exactly one prompt, as it does on Android where
        // the encrypting cipher forces one.
        val context = LAContext().apply { localizedCancelTitle = cancelLabel }
        when (val confirmed = context.evaluate(title)) {
            is EvaluateOutcome.Failure -> return confirmed.result
            EvaluateOutcome.Ok -> Unit
        }

        deleteItem()

        val access = SecAccessControlCreateWithFlags(
            null,
            kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
            BIOMETRY_CURRENT_SET,
            null,
        ) ?: return VaultResult.Failed

        val query = Query()
        return try {
            query.putIdentity()
            query.put(kSecAttrAccessControl, access)
            query.putObject(kSecUseAuthenticationContext, context)
            query.putObject(kSecValueData, secret.toNSData())
            if (SecItemAdd(query.build(), null) == errSecSuccess) VaultResult.Success("")
            else VaultResult.Failed
        } finally {
            query.release()
            CFRelease(access)
        }
    }

    override suspend fun open(title: String, subtitle: String, cancelLabel: String): VaultResult {
        if (!isAvailable()) return VaultResult.Unavailable
        // No hasSecret() gate here on purpose: an attribute query that comes back
        // anything but errSecSuccess would be read as "never set up", and the
        // caller switches Face ID off on that. The data query below answers the
        // same question authoritatively — errSecItemNotFound means not there.

        val context = LAContext().apply {
            localizedCancelTitle = cancelLabel
            localizedReason = title
        }

        val query = Query()
        return try {
            memScoped {
                query.putIdentity()
                query.put(kSecMatchLimit, kSecMatchLimitOne)
                query.put(kSecReturnData, kCFBooleanTrue)
                query.putObject(kSecUseAuthenticationContext, context)

                val out = alloc<CFTypeRefVar>()
                when (SecItemCopyMatching(query.build(), out.ptr)) {
                    errSecSuccess -> {
                        val data = CFBridgingRelease(out.value) as? NSData
                        val text = data?.let {
                            NSString.create(data = it, encoding = NSUTF8StringEncoding)?.toString()
                        }
                        if (text != null) {
                            // The match just happened; hold the context so the
                            // rotated token can go back in without prompting again.
                            authorised = context
                            VaultResult.Success(text)
                        } else {
                            VaultResult.Failed
                        }
                    }
                    errSecUserCanceled -> VaultResult.Cancelled
                    errSecItemNotFound -> VaultResult.Empty
                    // The item is bound to the enrolment that sealed it; once
                    // that changes the Keychain refuses it for good, so drop it.
                    ERR_SEC_AUTH_FAILED -> { deleteItem(); VaultResult.Invalidated }
                    else -> VaultResult.Failed
                }
            }
        } finally {
            query.release()
        }
    }

    /**
     * Re-adds the item under the context that the last [open] authenticated.
     *
     * `SecItemAdd` never prompts by itself — the prompt in [seal] is an explicit
     * `evaluatePolicy` call. Handing it an already-authenticated context is
     * therefore all it takes, and no second prompt appears.
     */
    override suspend fun reseal(secret: String): VaultResult {
        val context = authorised ?: return VaultResult.Failed
        authorised = null

        val access = SecAccessControlCreateWithFlags(
            null,
            kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
            BIOMETRY_CURRENT_SET,
            null,
        ) ?: return VaultResult.Failed

        deleteItem()

        val query = Query()
        return try {
            query.putIdentity()
            query.put(kSecAttrAccessControl, access)
            query.putObject(kSecUseAuthenticationContext, context)
            query.putObject(kSecValueData, secret.toNSData())
            if (SecItemAdd(query.build(), null) == errSecSuccess) VaultResult.Success("")
            else VaultResult.Failed
        } finally {
            query.release()
            CFRelease(access)
        }
    }

    override fun clear() {
        authorised = null
        deleteItem()
    }

    private fun deleteItem() {
        val query = Query()
        try {
            query.putIdentity()
            SecItemDelete(query.build())
        } finally {
            query.release()
        }
    }

    /**
     * A CoreFoundation dictionary plus the bridged objects put into it.
     *
     * Kotlin maps do not convert to `CFDictionaryRef`, and every Kotlin or
     * Obj-C value handed to CoreFoundation has to be retained for as long as
     * the dictionary lives. Collecting the retains here keeps the release in
     * one place instead of scattered across each call site.
     */
    private class Query {
        private val retained = mutableListOf<CPointer<out CPointed>>()
        private val dict: CFMutableDictionaryRef? = CFDictionaryCreateMutable(
            null,
            0,
            kCFTypeDictionaryKeyCallBacks.ptr,
            kCFTypeDictionaryValueCallBacks.ptr,
        )

        fun put(key: CValuesRef<*>?, value: CValuesRef<*>?) {
            CFDictionaryAddValue(dict, key, value)
        }

        fun putObject(key: CValuesRef<*>?, value: Any) {
            val ref = CFBridgingRetain(value)
            if (ref != null) retained += ref
            CFDictionaryAddValue(dict, key, ref)
        }

        fun putIdentity() {
            put(kSecClass, kSecClassGenericPassword)
            putObject(kSecAttrService, NSString.create(string = SERVICE))
            putObject(kSecAttrAccount, NSString.create(string = ACCOUNT))
        }

        fun build(): CFDictionaryRef? = dict

        fun release() {
            retained.forEach { CFRelease(it) }
            retained.clear()
            dict?.let { CFRelease(it) }
        }
    }

    private sealed interface EvaluateOutcome {
        data object Ok : EvaluateOutcome
        data class Failure(val result: VaultResult) : EvaluateOutcome
    }

    private suspend fun LAContext.evaluate(reason: String): EvaluateOutcome =
        suspendCancellableCoroutine { cont ->
            evaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, reason) { ok, error ->
                if (!cont.isActive) return@evaluatePolicy
                cont.resume(
                    when {
                        ok -> EvaluateOutcome.Ok
                        error?.code?.toInt() == LA_ERROR_USER_CANCEL ->
                            EvaluateOutcome.Failure(VaultResult.Cancelled)
                        else -> EvaluateOutcome.Failure(VaultResult.Failed)
                    },
                )
            }
        }

    private companion object {
        const val SERVICE = "com.hanmaum.dn.mobile.vault"
        const val ACCOUNT = "refresh_token"

        /** `kSecAccessControlBiometryCurrentSet` — not exported by the bindings. */
        const val BIOMETRY_CURRENT_SET = 8uL

        const val ERR_SEC_AUTH_FAILED: OSStatus = -25293
        const val LA_ERROR_USER_CANCEL = -2
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun String.toNSData(): NSData {
    val bytes = encodeToByteArray()
    return bytes.usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = bytes.size.convert())
    }
}

@Composable
actual fun rememberBiometricVault(): BiometricVault = remember { IosBiometricVault() }
