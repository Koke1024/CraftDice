package com.koke1024.craftdice.di

import com.koke1024.craftdice.domain.battle.BattleEngine
import com.koke1024.craftdice.domain.battle.ai.EnemyAI
import com.koke1024.craftdice.domain.physics.PhysicsEngine
import com.koke1024.craftdice.ui.session.BattleSessionHolder
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Regression guard for the battle-screen entry crash.
 *
 * `DicePhysicsEngine`, `BattleEngine`, and `EnemyAI` all take only primitive /
 * functional constructor params with Kotlin defaults. Registering them with
 * `singleOf(::X)` made Koin try to resolve `Double`/`Int`/function types from
 * the graph and threw `NoDefinitionFoundException` the first time a
 * `BattleViewModel` was built (i.e. the first time the battle route was
 * opened). These engines must be registered as plain `single { X() }`.
 */
class BattleEngineDiTest {

    private fun app() = koinApplication { modules(appModule) }

    @Test
    fun resolvesPhysicsEngineSingleton() {
        val koin = app()
        assertNotNull(koin.koin.get<PhysicsEngine>())
        koin.close()
    }

    @Test
    fun resolvesBattleEngineSingleton() {
        val koin = app()
        assertNotNull(koin.koin.get<BattleEngine>())
        koin.close()
    }

    @Test
    fun resolvesEnemyAiSingleton() {
        val koin = app()
        assertNotNull(koin.koin.get<EnemyAI>())
        koin.close()
    }

    @Test
    fun resolvesBattleSessionHolderSingleton() {
        val koin = app()
        assertNotNull(koin.koin.get<BattleSessionHolder>())
        koin.close()
    }
}
