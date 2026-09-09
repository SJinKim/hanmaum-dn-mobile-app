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
        return sealed?.let { VaultResult.Success(it) } ?: VaultResult.Empty
    }

    override fun clear() {
        sealed = null
        cleared = true
    }
}
