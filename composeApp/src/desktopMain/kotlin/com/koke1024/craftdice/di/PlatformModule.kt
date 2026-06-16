package com.koke1024.craftdice.di

import com.koke1024.craftdice.data.local.DatabaseDriverFactory
import com.koke1024.craftdice.data.local.DesktopDatabaseDriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<DatabaseDriverFactory> { DesktopDatabaseDriverFactory() }
}
