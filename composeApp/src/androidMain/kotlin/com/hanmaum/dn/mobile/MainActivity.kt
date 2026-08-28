package com.hanmaum.dn.mobile

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity

/**
 * FragmentActivity rather than ComponentActivity: BiometricPrompt requires a
 * FragmentActivity host, and the Face ID / fingerprint setting needs it.
 */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App()
        }
    }
}
