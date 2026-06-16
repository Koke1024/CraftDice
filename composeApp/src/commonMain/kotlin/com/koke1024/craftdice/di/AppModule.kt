package com.koke1024.craftdice.di

import com.koke1024.craftdice.domain.battle.BattleEngine
import com.koke1024.craftdice.domain.battle.ai.EnemyAI
import com.koke1024.craftdice.domain.physics.DicePhysicsEngine
import com.koke1024.craftdice.domain.physics.PhysicsEngine
import com.koke1024.craftdice.domain.roguelike.RunEngine
import com.koke1024.craftdice.domain.roguelike.event.EventResolver
import com.koke1024.craftdice.domain.roguelike.generation.DefaultEnemyCatalog
import com.koke1024.craftdice.domain.roguelike.generation.DungeonGenerator
import com.koke1024.craftdice.domain.roguelike.generation.EnemyCatalog
import com.koke1024.craftdice.domain.roguelike.reward.RewardRoller
import com.koke1024.craftdice.ui.battle.BattleViewModel
import com.koke1024.craftdice.ui.dungeon.DungeonViewModel
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
        singleOf(::BattleEngine)
        singleOf(::EnemyAI)
        single<EnemyCatalog> { DefaultEnemyCatalog.instance }
        single { DungeonGenerator(get()) }
        singleOf(::RewardRoller)
        singleOf(::EventResolver)
        single { RunEngine(get(), get(), get()) }
        viewModelOf(::HomeViewModel)
        viewModelOf(::BattleViewModel)
        viewModelOf(::DungeonViewModel)
    }

fun allModules(): List<Module> = listOf(appModule, platformModule())
