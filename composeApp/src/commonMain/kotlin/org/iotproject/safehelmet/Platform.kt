package org.iotproject.safehelmet

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform