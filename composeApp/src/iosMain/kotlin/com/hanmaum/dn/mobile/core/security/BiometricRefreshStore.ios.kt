package com.hanmaum.dn.mobile.core.security

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryCreate
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanFalse
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecAccessControlCreateWithFlags
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.kSecAccessControlUserPresence
import platform.Security.kSecAttrAccessControl
import platform.Security.kSecAttrAccessibleWhenUnlockedThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecUseOperationPrompt
import platform.Security.kSecValueData
import platform.Security.errSecSuccess
import platform.darwin.OSStatus

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class BiometricRefreshStore {

    actual fun isDeviceSecured(): Boolean = true // userPresence falls back to passcode; assume securable

    actual fun hasStored(): Boolean = memScoped {
        // Do NOT request data (no prompt) — only check existence.
        val query = cfDictionaryOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to SERVICE.toCF(),
            kSecAttrAccount to ACCOUNT.toCF(),
            kSecMatchLimit to kSecMatchLimitOne,
            kSecReturnData to kCFBooleanFalse,
        )
        val status = SecItemCopyMatching(query, null)
        status == errSecSuccess
    }

    actual fun store(token: String) {
        delete()
        memScoped {
            val access = SecAccessControlCreateWithFlags(
                kCFAllocatorDefault,
                kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
                kSecAccessControlUserPresence, // biometric OR device passcode
                null,
            )
            val data = (token.toNSString()).dataUsingEncoding(NSUTF8StringEncoding)
                ?: return@memScoped
            val attrs = cfDictionaryOf(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to SERVICE.toCF(),
                kSecAttrAccount to ACCOUNT.toCF(),
                kSecValueData to CFBridgingRetain(data),
                kSecAttrAccessControl to access,
            )
            SecItemAdd(attrs, null)
        }
    }

    /** Gated read — triggers the system biometric/passcode prompt with [reason]. */
    fun read(reason: String): String? = memScoped {
        val result = alloc<CFTypeRefVar>()
        val query = cfDictionaryOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to SERVICE.toCF(),
            kSecAttrAccount to ACCOUNT.toCF(),
            kSecMatchLimit to kSecMatchLimitOne,
            kSecReturnData to kCFBooleanTrue,
            kSecUseOperationPrompt to reason.toCF(),
        )
        val status: OSStatus = SecItemCopyMatching(query, result.ptr)
        if (status != errSecSuccess) return@memScoped null
        val nsData = CFBridgingRelease(result.value) as? NSData ?: return@memScoped null
        NSString.create(nsData, NSUTF8StringEncoding) as String?
    }

    actual fun delete() {
        memScoped {
            val query = cfDictionaryOf(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to SERVICE.toCF(),
                kSecAttrAccount to ACCOUNT.toCF(),
            )
            SecItemDelete(query)
        }
    }

    private companion object {
        const val SERVICE = "com.hanmaum.dn.mobile.refresh"
        const val ACCOUNT = "offline_refresh_token"
    }
}

/** Bridge a Kotlin String to a retained CoreFoundation ref (for use as a CF value). */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun String.toCF(): COpaquePointer? = CFBridgingRetain(this.toNSString())

@OptIn(BetaInteropApi::class)
private fun String.toNSString(): NSString = NSString.create(string = this)

/**
 * Build a `CFDictionaryRef` from CoreFoundation key/value pairs.
 *
 * Mirrors the toll-free-bridge idiom used by `multiplatform-settings`' `KeychainSettings`
 * on this Kotlin/Native toolchain: lay the keys and values into parallel C arrays and call
 * `CFDictionaryCreate`. Keys are the immortal `kSec*` `CFStringRef` constants; values are
 * either those constants/booleans or `CFBridgingRetain`-ed objects. The Security APIs accept
 * the resulting `CFDictionaryRef` directly.
 */
@OptIn(ExperimentalForeignApi::class)
private fun MemScope.cfDictionaryOf(
    vararg pairs: Pair<COpaquePointer?, COpaquePointer?>,
): CFDictionaryRef? {
    val keys: List<COpaquePointer?> = pairs.map { it.first }
    val values: List<COpaquePointer?> = pairs.map { it.second }
    return CFDictionaryCreate(
        kCFAllocatorDefault,
        allocArrayOf(keys),
        allocArrayOf(values),
        pairs.size.toLong(),
        kCFTypeDictionaryKeyCallBacks.ptr,
        kCFTypeDictionaryValueCallBacks.ptr,
    )
}
