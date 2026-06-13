package com.hanmaum.dn.mobile.core.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPairGenerator
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

actual class BiometricRefreshStore(private val context: Context) : RefreshTokenPersistence {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    actual override fun isDeviceSecured(): Boolean {
        val km = context.getSystemService(android.app.KeyguardManager::class.java)
        return km?.isDeviceSecure == true
    }

    actual override fun hasStored(): Boolean = prefs.contains(KEY_CIPHERTEXT)

    actual override fun store(token: String) {
        ensureKeyPair()
        // 1. Random AES-256 key encrypts the token (GCM).
        val aesKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val aesCipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, aesKey) }
        val ciphertext = aesCipher.doFinal(token.encodeToByteArray())
        val iv = aesCipher.iv
        // 2. RSA public key (no auth) wraps the AES key.
        val publicKey = keyStore.getCertificate(KEY_ALIAS).publicKey
        val rsaCipher = Cipher.getInstance(RSA_TRANSFORM).apply { init(Cipher.ENCRYPT_MODE, publicKey) }
        val wrappedKey = rsaCipher.doFinal(aesKey.encoded)
        prefs.edit()
            .putString(KEY_CIPHERTEXT, ciphertext.b64())
            .putString(KEY_IV, iv.b64())
            .putString(KEY_WRAPPED, wrappedKey.b64())
            .apply()
    }

    actual override fun delete() {
        prefs.edit().clear().apply()
        if (keyStore.containsAlias(KEY_ALIAS)) keyStore.deleteEntry(KEY_ALIAS)
    }

    /** Cipher (RSA DECRYPT) that BiometricPrompt must authorize before [decryptAfterAuth]. */
    fun cipherForUnlock(): Cipher {
        val privateKey = keyStore.getKey(KEY_ALIAS, null) as java.security.PrivateKey
        return Cipher.getInstance(RSA_TRANSFORM).apply { init(Cipher.DECRYPT_MODE, privateKey) }
    }

    /** After BiometricPrompt authorizes [authorizedCipher], unwrap the AES key and decrypt. */
    fun decryptAfterAuth(authorizedCipher: Cipher): String? {
        val wrapped = prefs.getString(KEY_WRAPPED, null)?.unb64() ?: return null
        val iv = prefs.getString(KEY_IV, null)?.unb64() ?: return null
        val ciphertext = prefs.getString(KEY_CIPHERTEXT, null)?.unb64() ?: return null
        val aesKeyBytes = authorizedCipher.doFinal(wrapped)
        val aesKey = SecretKeySpec(aesKeyBytes, "AES")
        val aesCipher = Cipher.getInstance("AES/GCM/NoPadding")
            .apply { init(Cipher.DECRYPT_MODE, aesKey, GCMParameterSpec(128, iv)) }
        return aesCipher.doFinal(ciphertext).decodeToString()
    }

    private fun ensureKeyPair() {
        if (keyStore.containsAlias(KEY_ALIAS)) return
        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
            .setKeySize(2048)
            .setUserAuthenticationRequired(true) // gates PRIVATE-key (decrypt) use only
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(
                0,
                KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL,
            )
        } else {
            @Suppress("DEPRECATION")
            builder.setUserAuthenticationValidityDurationSeconds(-1) // -1 = require auth for every use
        }
        val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE)
        generator.initialize(builder.build())
        generator.generateKeyPair()
    }

    private fun ByteArray.b64() = Base64.encodeToString(this, Base64.NO_WRAP)
    private fun String.unb64() = Base64.decode(this, Base64.NO_WRAP)

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "dn_refresh_token_key"
        const val RSA_TRANSFORM = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
        const val PREFS = "dn_refresh_secure"
        const val KEY_CIPHERTEXT = "ct"
        const val KEY_IV = "iv"
        const val KEY_WRAPPED = "wk"
    }
}
