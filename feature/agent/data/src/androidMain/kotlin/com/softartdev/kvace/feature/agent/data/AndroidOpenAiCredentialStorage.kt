package com.softartdev.kvace.feature.agent.data

import android.content.Context
import android.util.Base64
import com.softartdev.kvace.feature.agent.domain.ProviderCredentialResult
import com.softartdev.kvace.feature.agent.domain.ProviderCredentialStatus
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AndroidOpenAiCredentialStorage(context: Context) : OpenAiCredentialStorage {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override val initialStatus: ProviderCredentialStatus
        get() = if (preferences.contains(KEY_CREDENTIAL)) ProviderCredentialStatus.Stored else ProviderCredentialStatus.Absent

    override val storedStatus: ProviderCredentialStatus = ProviderCredentialStatus.Stored

    override suspend fun read(): String? = runCatching {
        preferences.getString(KEY_CREDENTIAL, null)?.let(::decrypt)
    }.getOrNull()

    override suspend fun save(apiKey: String): ProviderCredentialResult = runCatching {
        preferences.edit().putString(KEY_CREDENTIAL, encrypt(apiKey)).commit()
        ProviderCredentialResult.Success
    }.getOrElse { ProviderCredentialResult.Failure() }

    override suspend fun delete(): ProviderCredentialResult = runCatching {
        preferences.edit().remove(KEY_CREDENTIAL).commit()
        ProviderCredentialResult.Success
    }.getOrElse { ProviderCredentialResult.Failure() }

    override suspend fun unlock(masterPassword: String): ProviderCredentialResult = ProviderCredentialResult.Unavailable

    override suspend fun clearLocked(): ProviderCredentialResult = delete()

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(value.encodeToByteArray())
        return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String {
        val encoded = Base64.decode(value, Base64.NO_WRAP)
        require(encoded.size > GCM_IV_SIZE_BYTES) { "Invalid encrypted credential." }
        val iv = encoded.copyOfRange(0, GCM_IV_SIZE_BYTES)
        val encrypted = encoded.copyOfRange(GCM_IV_SIZE_BYTES, encoded.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        return cipher.doFinal(encrypted).decodeToString()
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        return keyStore.getKey(KEY_ALIAS, null) as? SecretKey ?: KeyGenerator
            .getInstance(KEY_ALGORITHM, ANDROID_KEY_STORE)
            .apply { init(android.security.keystore.KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                    android.security.keystore.KeyProperties.PURPOSE_DECRYPT,
            ).setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()) }
            .generateKey()
    }

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "kvace.openai.api-key"
        const val KEY_ALGORITHM = "AES"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val PREFERENCES_NAME = "kvace_openai_credentials"
        const val KEY_CREDENTIAL = "encrypted_api_key"
        const val GCM_IV_SIZE_BYTES = 12
        const val GCM_TAG_LENGTH_BITS = 128
    }
}
