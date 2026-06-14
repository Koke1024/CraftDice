package com.koke1024.craftdice.di

import com.koke1024.craftdice.ui.home.HomeViewModel
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.core.module.dsl.viewModelOf

expect fun platformModule(): Module

val appModule =
    module {
        viewModelOf(::HomeViewModel)
    }

fun allModules(): List<Module> = listOf(appModule, platformModule())
