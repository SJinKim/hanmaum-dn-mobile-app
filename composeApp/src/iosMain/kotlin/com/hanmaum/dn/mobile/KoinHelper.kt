package com.hanmaum.dn.mobile

import com.hanmaum.dn.mobile.di.initKoin

/**
 * Swift entry point for Koin start-up.
 *
 * [initKoin] takes a [org.koin.dsl.KoinAppDeclaration] with a default value,
 * but Kotlin default arguments do not survive the Objective-C bridge — Swift
 * sees `doInitKoin(appDeclaration:)` and refuses a call without it. This
 * no-argument wrapper is what iOSApp.swift calls instead.
 *
 * Nothing platform-specific goes in the declaration; iOS has no equivalent of
 * Android's `androidContext()`.
 */
fun initKoinIos() {
    initKoin { }
}
