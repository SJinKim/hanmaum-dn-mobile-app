package com.hanmaum.dn.mobile

import android.app.Application
import com.hanmaum.dn.mobile.di.initKoin
import org.koin.android.ext.koin.androidContext

class DnChurchApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Koin starten
        initKoin {
            androidContext(this@DnChurchApp)
        }
    }
}

//        startKoin {
//            androidLogger()
//            androidContext(this@MainApplication)
//            // Hier lädst du deine Module (Shared + Android spezifisch)
//            modules(appModule)
//        }