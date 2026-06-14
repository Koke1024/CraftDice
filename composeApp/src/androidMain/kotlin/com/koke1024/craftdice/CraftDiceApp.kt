package com.koke1024.craftdice

import android.app.Application
import com.koke1024.craftdice.di.androidModules
import com.koke1024.craftdice.di.appModule
import com.koke1024.craftdice.di.initKoin
import org.koin.android.ext.koin.androidContext

class CraftDiceApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@CraftDiceApp)
            modules(androidModules())
        }
    }
}
