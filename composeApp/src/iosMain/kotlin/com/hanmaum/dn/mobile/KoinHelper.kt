package com.hanmaum.dn.mobile


import com.hanmaum.dn.mobile.di.initKoin
import org.koin.core.context.startKoin

fun initKoinIos() {
    initKoin {
        // Hier kommen keine plattformspezifischen Dinge wie androidContext rein
    }
}