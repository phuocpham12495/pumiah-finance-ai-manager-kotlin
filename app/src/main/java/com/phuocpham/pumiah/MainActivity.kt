package com.phuocpham.pumiah

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.phuocpham.pumiah.ui.navigation.AppNavigation
import com.phuocpham.pumiah.ui.theme.PumiahTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PumiahTheme {
                AppNavigation()
            }
        }
    }
}
