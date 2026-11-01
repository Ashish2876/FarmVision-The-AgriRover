package com.farmvision.agrirover.data.device.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.util.Base64
import com.farmvision.agrirover.domain.device.model.KeystoreHardwareLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.KeyStore
import java.security.ProviderException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec

/**
 * Hardware-backed AES-256-GCM secret storage for device authentication tokens.
 *
 * SAFETY & SECURITY INVARIANTS:
 * 1. Plaintext persistence to disk/SharedPreferences is STRICTLY PROHIBITED.
 * 2. StrongBox fallback is permitted ONLY for hardware-unavailability exceptions.
 * 3. Partially initialized key aliases are explicitly purged before retrying keygen.
 * 4. In case of fatal keystore failure, falls back strictly to volatile RAM memory.
 */
class KeystoreSecretStorage(
    private val context: Context,
    private val keyAlias: String = "FarmVision_DeviceSecret_v1"
) {
    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val AES_GCM_NOPADDING = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val PREFS_NAME = "farmvision_device_keystore_prefs"
        private const val KEY_ENCRYPTED_TOKEN = "encrypted_bearer_token"
        private const val KEY_IV = "token_iv"
    }

    private val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    // Ephemeral RAM storage when hardware keystore fails closed
    private val _ramFallbackToken = MutableStateFlow<String?>(null)

    private var strongBoxRequestedAndSucceeded = false
    private var isUsingRamFallback = false

    init {
        initializeOrLoadKey()
    }

    @Synchronized
    private fun initializeOrLoadKey() {
        try {
            if (!keyStore.containsAlias(keyAlias)) {
                generateKey()
            }
        } catch (e: Exception) {
            // Fatal Keystore provider failure: Fail closed to RAM-only volatile storage
            isUsingRamFallback = true
        }
    }

    @Synchronized
    private fun generateKey() {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)

        // Attempt 1: StrongBox (API 28+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val strongBoxSpec = KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setIsStrongBoxBacked(true)
                    .build()

                keyGenerator.init(strongBoxSpec)
                keyGenerator.generateKey()
                strongBoxRequestedAndSucceeded = true
                return
            } catch (e: Exception) {
                val isStrongBoxUnavailable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    e is android.security.keystore.StrongBoxUnavailableException ||
                        (e is ProviderException && (e.message?.contains("StrongBox", ignoreCase = true) == true ||
                            e.message?.contains("SECURE_HW_UNAVAILABLE", ignoreCase = true) == true))
                } else {
                    false
                }

                if (!isStrongBoxUnavailable) {
                    // Algorithmic, padding, parameter or security exceptions must NOT downgrade silently
                    throw e
                }

                // Explicitly purge partially generated or corrupt key alias
                try {
                    keyStore.deleteEntry(keyAlias)
                } catch (_: Exception) {
                    // Best effort cleanup
                }
                strongBoxRequestedAndSucceeded = false
            }
        }

        // Attempt 2: Standard TEE / Hardware Keystore
        val standardSpec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(standardSpec)
        keyGenerator.generateKey()
    }

    fun getHardwareLevel(): KeystoreHardwareLevel {
        if (isUsingRamFallback) {
            return KeystoreHardwareLevel.RamOnlyFallback
        }
        return try {
            val key = keyStore.getKey(keyAlias, null) as? SecretKey
                ?: return KeystoreHardwareLevel.Software

            val factory = SecretKeyFactory.getInstance(key.algorithm, ANDROID_KEYSTORE)
            val keyInfo = factory.getKeySpec(key, KeyInfo::class.java) as KeyInfo

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                when (keyInfo.securityLevel) {
                    KeyProperties.SECURITY_LEVEL_STRONGBOX -> KeystoreHardwareLevel.StrongBoxHardware
                    KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT -> KeystoreHardwareLevel.TeeHardware
                    else -> KeystoreHardwareLevel.Software
                }
            } else {
                if (!keyInfo.isInsideSecureHardware) {
                    KeystoreHardwareLevel.Software
                } else if (strongBoxRequestedAndSucceeded) {
                    KeystoreHardwareLevel.StrongBoxHardware
                } else {
                    // Strictly TEE on API 26-30 without over-claiming StrongBox
                    KeystoreHardwareLevel.TeeHardware
                }
            }
        } catch (_: Exception) {
            KeystoreHardwareLevel.Software
        }
    }

    @Synchronized
    fun saveBearerToken(token: String): Boolean {
        if (isUsingRamFallback) {
            _ramFallbackToken.value = token
            return true
        }

        return try {
            val key = keyStore.getKey(keyAlias, null) as SecretKey
            val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
            cipher.init(Cipher.ENCRYPT_MODE, key)

            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(token.toByteArray(Charsets.UTF_8))

            val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val cipherBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)

            sharedPrefs.edit()
                .putString(KEY_IV, ivBase64)
                .putString(KEY_ENCRYPTED_TOKEN, cipherBase64)
                .commit()
        } catch (e: Exception) {
            // If Keystore encryption fails, fall back to RAM
            _ramFallbackToken.value = token
            true
        }
    }

    @Synchronized
    fun getBearerToken(): String? {
        if (isUsingRamFallback || _ramFallbackToken.value != null) {
            return _ramFallbackToken.value
        }

        val ivBase64 = sharedPrefs.getString(KEY_IV, null) ?: return null
        val cipherBase64 = sharedPrefs.getString(KEY_ENCRYPTED_TOKEN, null) ?: return null

        return try {
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            val cipherBytes = Base64.decode(cipherBase64, Base64.NO_WRAP)

            val key = keyStore.getKey(keyAlias, null) as SecretKey
            val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)

            val decryptedBytes = cipher.doFinal(cipherBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    @Synchronized
    fun clearBearerToken(): Boolean {
        _ramFallbackToken.value = null
        sharedPrefs.edit()
            .remove(KEY_IV)
            .remove(KEY_ENCRYPTED_TOKEN)
            .commit()
        return true
    }
}

