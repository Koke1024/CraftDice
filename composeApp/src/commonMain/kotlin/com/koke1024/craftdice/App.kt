package com.koke1024.craftdice

import androidx.compose.runtime.Composable
import com.koke1024.craftdice.core.AppLogger
import com.koke1024.craftdice.ui.navigation.AppNavigation
import com.koke1024.craftdice.ui.theme.AppTheme

@Composable
fun App() {
    AppLogger.init()
    AppTheme {
        AppNavigation()
    }
}
