package com.hanmaum.dn.mobile.core.security

/**
 * Stands in for the OS-backed vault. The prompt itself cannot be exercised
 * here — [nextResult] is how a test says what the member and the system did.
 */
class FakeBiometricVault(
    var available: Boolean = true,
    var nextResult: VaultResult? = null,
) : BiometricVault {

    var sealed: String? = null
        private set
    var cleared = false
        private set

    /** How often the rotated secret was written back — no prompt of its own. */
    var resealCount = 0
        private set

    private var opened = false

    override fun isAvailable(): Boolean = available

    override fun hasSecret(): Boolean = sealed != null

    override suspend fun seal(
        secret: String,
        title: String,
        subtitle: String,
        cancelLabel: String,
    ): VaultResult {
        if (!available) return VaultResult.Unavailable
        nextResult?.let { return it }
        sealed = secret
        return VaultResult.Success("")
    }

    override suspend fun open(title: String, subtitle: String, cancelLabel: String): VaultResult {
        if (!available) return VaultResult.Unavailable
        nextResult?.let { return it }
        return sealed?.let { opened = true; VaultResult.Success(it) } ?: VaultResult.Empty
    }

    /** Mirrors the real vaults: only an [open] that just succeeded authorises this. */
    override suspend fun reseal(secret: String): VaultResult {
        if (!opened) return VaultResult.Failed
        opened = false
        sealed = secret
        resealCount++
        return VaultResult.Success("")
    }

    override fun clear() {
        sealed = null
        opened = false
        cleared = true
    }
}
