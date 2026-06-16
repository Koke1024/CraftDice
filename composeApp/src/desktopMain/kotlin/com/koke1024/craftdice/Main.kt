package com.koke1024.craftdice

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.koke1024.craftdice.di.initKoin

fun main() = application {
    initKoin()
    Window(
        onCloseRequest = ::exitApplication,
        title = "CraftDice",
    ) {
        App()
    }
}
