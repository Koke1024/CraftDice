package com.koke1024.craftdice.di

import com.koke1024.craftdice.data.local.AndroidDatabaseDriverFactory
import com.koke1024.craftdice.data.local.DatabaseDriverFactory
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<DatabaseDriverFactory> { AndroidDatabaseDriverFactory(androidContext()) }
}

fun androidModules() = listOf(platformModule())
