package com.softartdev.kvace.feature.agent.data

import com.softartdev.kvace.feature.agent.domain.ProviderCredentialResult
import com.softartdev.kvace.feature.agent.domain.ProviderCredentialStatus
import net.harawata.appdirs.AppDirsFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.security.SecureRandom
import java.security.spec.KeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class JvmOpenAiCredentialStorage : OpenAiCredentialStorage {
    private val credentialFile = File(
        AppDirsFactory.getInstance().getUserDataDir("Kvace", null, null),
        CREDENTIAL_FILE_NAME,
    )
    private var sessionKey: ByteArray? = null
    private var pendingPassword: CharArray? = null

    override val initialStatus: ProviderCredentialStatus
        get() = when {
            macOsKeychainRead() != null -> ProviderCredentialStatus.Stored
            credentialFile.exists() -> ProviderCredentialStatus.Locked
            isMacOs() -> ProviderCredentialStatus.Absent
            else -> ProviderCredentialStatus.Locked
        }

    override val storedStatus: ProviderCredentialStatus = ProviderCredentialStatus.Stored

    override suspend fun read(): String? = macOsKeychainRead() ?: readFallback()

    override suspend fun save(apiKey: String): ProviderCredentialResult {
        if (macOsKeychainWrite(apiKey)) return ProviderCredentialResult.Success
        return runCatching {
            credentialFile.parentFile?.mkdirs()
            val salt = ByteArray(SALT_SIZE_BYTES).also(SecureRandom()::nextBytes)
            val iv = ByteArray(IV_SIZE_BYTES).also(SecureRandom()::nextBytes)
            val key = sessionKey ?: pendingPassword?.let { deriveKey(it, salt) } ?: return ProviderCredentialResult.Locked
            val encrypted = cipher(Cipher.ENCRYPT_MODE, key, iv).doFinal(apiKey.encodeToByteArray())
            val payload = listOf(salt, iv, encrypted).joinToString(":") { Base64.getEncoder().encodeToString(it) }
            Files.writeString(
                credentialFile.toPath(),
                payload,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE,
            )
            sessionKey = key
            pendingPassword?.fill('\u0000')
            pendingPassword = null
            ProviderCredentialResult.Success
        }.getOrElse { ProviderCredentialResult.Failure() }
    }

    override suspend fun delete(): ProviderCredentialResult = runCatching {
        macOsKeychainDelete()
        Files.deleteIfExists(credentialFile.toPath())
        sessionKey = null
        pendingPassword?.fill('\u0000')
        pendingPassword = null
        ProviderCredentialResult.Success
    }.getOrElse { ProviderCredentialResult.Failure() }

    override suspend fun unlock(masterPassword: String): ProviderCredentialResult {
        if (masterPassword.isBlank()) return ProviderCredentialResult.Invalid
        if (!credentialFile.exists()) {
            pendingPassword?.fill('\u0000')
            pendingPassword = masterPassword.toCharArray()
            return ProviderCredentialResult.Success
        }
        return runCatching {
            val salt = decodePayload().first()
            val derived = deriveKey(masterPassword.toCharArray(), salt)
            val value = decryptFallback(derived)
            if (value.isBlank()) error("Invalid credential.")
            sessionKey = derived
            pendingPassword?.fill('\u0000')
            pendingPassword = null
            ProviderCredentialResult.Success
        }.getOrElse { ProviderCredentialResult.Locked }
    }

    override suspend fun clearLocked(): ProviderCredentialResult = runCatching {
        Files.deleteIfExists(credentialFile.toPath())
        sessionKey = null
        pendingPassword?.fill('\u0000')
        pendingPassword = null
        ProviderCredentialResult.Success
    }.getOrElse { ProviderCredentialResult.Failure() }

    private fun readFallback(): String? {
        val key = sessionKey ?: return null
        return runCatching { decryptFallback(key) }.getOrNull()
    }

    private fun decryptFallback(key: ByteArray): String {
        val (_, iv, encrypted) = decodePayload()
        return cipher(Cipher.DECRYPT_MODE, key, iv).doFinal(encrypted).decodeToString()
    }

    private fun decodePayload(): List<ByteArray> = Files.readString(credentialFile.toPath())
        .split(':')
        .also { require(it.size == PAYLOAD_PARTS) { "Invalid encrypted credential." } }
        .map(Base64.getDecoder()::decode)

    private fun deriveKey(password: CharArray, salt: ByteArray): ByteArray {
        val spec: KeySpec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_SIZE_BITS)
        return SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(spec).encoded
    }

    private fun cipher(mode: Int, key: ByteArray, iv: ByteArray): Cipher = Cipher.getInstance(TRANSFORMATION).apply {
        init(mode, SecretKeySpec(key, KEY_ALGORITHM), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
    }

    private fun macOsKeychainRead(): String? {
        if (!isMacOs()) return null
        return runCatching {
            ProcessBuilder("security", "find-generic-password", "-s", KEYCHAIN_SERVICE, "-a", KEYCHAIN_ACCOUNT, "-w")
                .start()
                .inputStream
                .bufferedReader()
                .readText()
                .trim()
                .takeIf(String::isNotBlank)
        }.getOrNull()
    }

    private fun macOsKeychainWrite(apiKey: String): Boolean {
        if (!isMacOs()) return false
        return runCatching {
            ProcessBuilder(
                "security", "add-generic-password", "-U", "-s", KEYCHAIN_SERVICE, "-a", KEYCHAIN_ACCOUNT, "-w", apiKey,
            ).start().waitFor() == 0
        }.getOrDefault(false)
    }

    private fun macOsKeychainDelete() {
        if (!isMacOs()) return
        runCatching {
            ProcessBuilder("security", "delete-generic-password", "-s", KEYCHAIN_SERVICE, "-a", KEYCHAIN_ACCOUNT).start().waitFor()
        }
    }

    private fun isMacOs(): Boolean = System.getProperty("os.name").contains("mac", ignoreCase = true)

    private companion object {
        const val CREDENTIAL_FILE_NAME = "openai-credential.enc"
        const val KEYCHAIN_SERVICE = "com.softartdev.kvace"
        const val KEYCHAIN_ACCOUNT = "openai-api-key"
        const val KEY_ALGORITHM = "AES"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
        const val PBKDF2_ITERATIONS = 600_000
        const val KEY_SIZE_BITS = 256
        const val SALT_SIZE_BYTES = 16
        const val IV_SIZE_BYTES = 12
        const val GCM_TAG_LENGTH_BITS = 128
        const val PAYLOAD_PARTS = 3
    }
}
