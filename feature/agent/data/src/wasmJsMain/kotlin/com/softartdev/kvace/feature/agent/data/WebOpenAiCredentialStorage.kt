package com.softartdev.kvace.feature.agent.data

import com.softartdev.kvace.feature.agent.domain.ProviderCredentialResult
import com.softartdev.kvace.feature.agent.domain.ProviderCredentialStatus

class WebOpenAiCredentialStorage : OpenAiCredentialStorage {
    private var apiKey: String? = null

    override val initialStatus: ProviderCredentialStatus = ProviderCredentialStatus.Absent
    override val storedStatus: ProviderCredentialStatus = ProviderCredentialStatus.SessionOnly

    override suspend fun read(): String? = apiKey

    override suspend fun save(apiKey: String): ProviderCredentialResult {
        this.apiKey = apiKey
        return ProviderCredentialResult.Success
    }

    override suspend fun delete(): ProviderCredentialResult {
        apiKey = null
        return ProviderCredentialResult.Success
    }

    override suspend fun unlock(masterPassword: String): ProviderCredentialResult = ProviderCredentialResult.Unavailable

    override suspend fun clearLocked(): ProviderCredentialResult = delete()
}
