package com.softartdev.kvace.feature.agent.data

import com.softartdev.kvace.feature.agent.domain.ProviderCredentialRepository
import com.softartdev.kvace.feature.agent.domain.ProviderCredentialResult
import com.softartdev.kvace.feature.agent.domain.ProviderCredentialStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface OpenAiCredentialStorage {
    val initialStatus: ProviderCredentialStatus
    val storedStatus: ProviderCredentialStatus
    suspend fun read(): String?
    suspend fun save(apiKey: String): ProviderCredentialResult
    suspend fun delete(): ProviderCredentialResult
    suspend fun unlock(masterPassword: String): ProviderCredentialResult
    suspend fun clearLocked(): ProviderCredentialResult
}

class OpenAiCredentialRepository(
    private val storage: OpenAiCredentialStorage,
) : ProviderCredentialRepository {

    override val openAiStatus: StateFlow<ProviderCredentialStatus>
        field = MutableStateFlow(storage.initialStatus)

    override suspend fun readOpenAiApiKey(): String? = storage.read()

    override suspend fun saveOpenAiApiKey(apiKey: String): ProviderCredentialResult {
        if (apiKey.isBlank()) return ProviderCredentialResult.Invalid
        return storage.save(apiKey.trim()).also { result -> updateStatus(result, storage.storedStatus) }
    }

    override suspend fun deleteOpenAiApiKey(): ProviderCredentialResult =
        storage.delete().also { result -> updateStatus(result, ProviderCredentialStatus.Absent) }

    override suspend fun unlockOpenAiApiKey(masterPassword: String): ProviderCredentialResult =
        storage.unlock(masterPassword).also { result -> updateStatus(result, ProviderCredentialStatus.Stored) }

    override suspend fun clearLockedOpenAiApiKey(): ProviderCredentialResult =
        storage.clearLocked().also { result -> updateStatus(result, ProviderCredentialStatus.Absent) }

    private fun updateStatus(result: ProviderCredentialResult, successStatus: ProviderCredentialStatus) {
        when (result) {
            ProviderCredentialResult.Success -> openAiStatus.value = successStatus
            ProviderCredentialResult.Invalid -> Unit
            ProviderCredentialResult.Locked -> openAiStatus.value = ProviderCredentialStatus.Locked
            ProviderCredentialResult.Unavailable -> openAiStatus.value = ProviderCredentialStatus.Unavailable
            is ProviderCredentialResult.Failure -> Unit
        }
    }
}
