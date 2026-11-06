package com.mangaauthority.tvai.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

enum class SaveResult { Encrypted, SessionOnly }

/**
 * Keeps the Gemini API key encrypted with an AES-256-GCM key that lives in the Android Keystore.
 * The Keystore key cannot be exported, but anyone with root access, or code running inside this
 * app, can still ask the device to decrypt it. This is a personal-use (bring your own key) design.
 * If the Keystore is broken on the device (seen on some cheap TV boxes) the key is held in memory
 * for the current run only and is never written to disk in plain text.
 */
class KeyVault(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("vault", Context.MODE_PRIVATE)

    fun hasKey(): Boolean = sessionKey != null || prefs.contains(DATA)

    fun save(rawKey: String): SaveResult {
        val key = rawKey.trim().trim('"', '\'')
        clear()
        return try {
            val cipher = Cipher.getInstance(TRANSFORM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey())
            val ct = cipher.doFinal(key.toByteArray(Charsets.UTF_8))
            prefs.edit()
                .putString(IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                .putString(DATA, Base64.encodeToString(ct, Base64.NO_WRAP))
                .commit()
            SaveResult.Encrypted
        } catch (e: Exception) {
            sessionKey = key
            SaveResult.SessionOnly
        }
    }

    fun load(): String? {
        sessionKey?.let { return it }
        val iv = prefs.getString(IV, null) ?: return null
        val data = prefs.getString(DATA, null) ?: return null
        return try {
            val cipher = Cipher.getInstance(TRANSFORM)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
            String(cipher.doFinal(Base64.decode(data, Base64.NO_WRAP)), Charsets.UTF_8)
        } catch (e: Exception) {
            // Keystore key was lost (for example after a system restore). The saved key is unusable.
            clear()
            null
        }
    }

    fun clear() {
        sessionKey = null
        prefs.edit().clear().commit()
    }

    private fun secretKey(): SecretKey {
        val ks = KeyStore.getInstance(PROVIDER).apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    private companion object {
        const val PROVIDER = "AndroidKeyStore"
        const val ALIAS = "tvai_gemini_key"
        const val TRANSFORM = "AES/GCM/NoPadding"
        const val IV = "iv"
        const val DATA = "data"

        @Volatile
        var sessionKey: String? = null
    }
}
