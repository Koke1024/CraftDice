package com.koke1024.craftdice.di

import com.koke1024.craftdice.data.local.CraftDiceDatabase
import com.koke1024.craftdice.data.local.DatabaseDriverFactory
import com.koke1024.craftdice.data.repository.MetaProgressRepository
import com.koke1024.craftdice.data.repository.SqlDelightMetaProgressRepository
import com.koke1024.craftdice.domain.battle.BattleEngine
import com.koke1024.craftdice.domain.battle.ai.EnemyAI
import com.koke1024.craftdice.domain.meta.MetaProgressionService
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
import com.koke1024.craftdice.ui.session.BattleSessionHolder
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.bind
import org.koin.dsl.module

expect fun platformModule(): Module

val appModule =
    module {
        // These engines take only primitive/functional constructor params with
        // defaults, so singleOf(::X) would try (and fail) to resolve Double/Int
        // from the Koin graph. Construct them with their Kotlin defaults instead.
        single<PhysicsEngine> { DicePhysicsEngine() }
        single { BattleEngine() }
        single { EnemyAI() }
        single<EnemyCatalog> { DefaultEnemyCatalog.instance }
        single { DungeonGenerator(get()) }
        singleOf(::RewardRoller)
        singleOf(::EventResolver)
        single { RunEngine(get(), get(), get()) }

        single { CraftDiceDatabase(get<DatabaseDriverFactory>().createDriver()) }
        singleOf(::SqlDelightMetaProgressRepository) { bind<MetaProgressRepository>() }
        singleOf(::MetaProgressionService)
        singleOf(::BattleSessionHolder)

        viewModelOf(::HomeViewModel)
        viewModelOf(::BattleViewModel)
        viewModelOf(::DungeonViewModel)
    }

fun allModules(): List<Module> = listOf(appModule, platformModule())
