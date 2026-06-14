package com.koke1024.craftdice.di

import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    // SQLDelight driver will be configured in Phase 1
}
