package com.softartdev.kvace

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform