package com.koke1024.craftdice.di

import com.koke1024.craftdice.core.AppLogger
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.logger.Level
import org.koin.dsl.KoinAppDeclaration

fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    stopKoin()
    startKoin {
        printLogger(Level.DEBUG)
        modules(allModules())
        appDeclaration()
    }
    AppLogger.init()
}
