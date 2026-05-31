package com.softartdev.kvace.feature.agent.data

import android.os.Build
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel

class AndroidOnDeviceModelProvider : OnDeviceModelProvider {
    override val modelName: String = ANDROID_ON_DEVICE_MODEL_LABEL
    override val isAvailable: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    private val generativeModel: GenerativeModel by lazy { Generation.getClient() }

    override suspend fun generateContent(prompt: String): String {
        if (!isAvailable) {
            throw OnDeviceModelException.Unavailable(
                "On-device AI requires Android 8.0/API 26 or newer.",
            )
        }

        ensureModelReady()
        val response = generativeModel.generateContent(prompt)
        return response.candidates
            .firstOrNull()
            ?.text
            ?.takeIf { it.isNotBlank() }
            ?: throw OnDeviceModelException.GenerationFailed("$ANDROID_ON_DEVICE_MODEL_LABEL returned no content.")
    }

    private suspend fun ensureModelReady() {
        when (val status = generativeModel.checkStatus()) {
            FeatureStatus.AVAILABLE -> Unit
            FeatureStatus.DOWNLOADABLE -> downloadModel()
            FeatureStatus.DOWNLOADING -> throw OnDeviceModelException.Downloading(
                "$ANDROID_ON_DEVICE_MODEL_LABEL is downloading. Try again when the download completes.",
            )
            FeatureStatus.UNAVAILABLE -> throw OnDeviceModelException.Unavailable(
                "$ANDROID_ON_DEVICE_MODEL_LABEL is unavailable on this device.",
            )
            else -> throw OnDeviceModelException.Unavailable(
                "$ANDROID_ON_DEVICE_MODEL_LABEL returned unsupported status $status.",
            )
        }
    }

    private suspend fun downloadModel() {
        var completed = false
        generativeModel.download().collect { status ->
            when (status) {
                DownloadStatus.DownloadCompleted -> completed = true
                is DownloadStatus.DownloadFailed -> throw OnDeviceModelException.DownloadFailed(
                    status.e.message ?: "$ANDROID_ON_DEVICE_MODEL_LABEL download failed.",
                )
                is DownloadStatus.DownloadProgress,
                is DownloadStatus.DownloadStarted -> Unit
            }
        }

        if (!completed) {
            throw OnDeviceModelException.DownloadFailed("$ANDROID_ON_DEVICE_MODEL_LABEL download did not complete.")
        }
    }
}
