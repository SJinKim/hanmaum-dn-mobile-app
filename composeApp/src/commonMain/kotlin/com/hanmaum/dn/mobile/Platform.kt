package com.hanmaum.dn.mobile

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform