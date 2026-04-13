package com.phuocpham.pumiah

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.phuocpham.pumiah.data.preferences.ThemeManager
import com.phuocpham.pumiah.ui.navigation.AppNavigation
import com.phuocpham.pumiah.ui.theme.PumiahTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var themeManager: ThemeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val darkMode by themeManager.darkMode.collectAsState()
            PumiahTheme(darkTheme = darkMode) {
                AppNavigation()
            }
        }
    }
}
