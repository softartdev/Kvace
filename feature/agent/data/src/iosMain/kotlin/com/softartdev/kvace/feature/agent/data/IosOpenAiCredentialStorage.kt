package com.softartdev.kvace.feature.agent.data

import com.softartdev.kvace.feature.agent.domain.ProviderCredentialResult
import com.softartdev.kvace.feature.agent.domain.ProviderCredentialStatus
import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings

@OptIn(ExperimentalSettingsImplementation::class)
class IosOpenAiCredentialStorage : OpenAiCredentialStorage {
    private val settings = KeychainSettings(SERVICE_NAME)

    override val initialStatus: ProviderCredentialStatus
        get() = if (settings.getStringOrNull(KEY_API_KEY) == null) ProviderCredentialStatus.Absent else ProviderCredentialStatus.Stored

    override val storedStatus: ProviderCredentialStatus = ProviderCredentialStatus.Stored

    override suspend fun read(): String? = settings.getStringOrNull(KEY_API_KEY)

    override suspend fun save(apiKey: String): ProviderCredentialResult = runCatching {
        settings.putString(KEY_API_KEY, apiKey)
        ProviderCredentialResult.Success
    }.getOrElse { ProviderCredentialResult.Failure() }

    override suspend fun delete(): ProviderCredentialResult = runCatching {
        settings.remove(KEY_API_KEY)
        ProviderCredentialResult.Success
    }.getOrElse { ProviderCredentialResult.Failure() }

    override suspend fun unlock(masterPassword: String): ProviderCredentialResult = ProviderCredentialResult.Unavailable

    override suspend fun clearLocked(): ProviderCredentialResult = delete()

    private companion object {
        const val SERVICE_NAME = "com.softartdev.kvace"
        const val KEY_API_KEY = "openai-api-key"
    }
}
