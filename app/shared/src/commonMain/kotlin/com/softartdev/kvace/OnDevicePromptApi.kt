package com.softartdev.kvace

interface OnDevicePromptApi {
    suspend fun generateContent(prompt: String): String?
}
