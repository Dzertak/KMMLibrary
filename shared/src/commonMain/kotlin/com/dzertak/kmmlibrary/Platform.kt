package com.dzertak.kmmlibrary

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform