package com.koke1024.craftdice.di

import com.koke1024.craftdice.domain.physics.DicePhysicsEngine
import com.koke1024.craftdice.domain.physics.PhysicsEngine
import com.koke1024.craftdice.ui.battle.BattleViewModel
import com.koke1024.craftdice.ui.home.HomeViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.bind
import org.koin.dsl.module

expect fun platformModule(): Module

val appModule =
    module {
        singleOf(::DicePhysicsEngine) { bind<PhysicsEngine>() }
        viewModelOf(::HomeViewModel)
        viewModelOf(::BattleViewModel)
    }

fun allModules(): List<Module> = listOf(appModule, platformModule())
