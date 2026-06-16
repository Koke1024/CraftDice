package com.koke1024.craftdice.data.local

import app.cash.sqldelight.db.SqlDriver

/**
 * Platform-aware factory for the [CraftDiceDatabase] SQLDelight driver.
 *
 * Defined as an interface so the common layer depends only on the abstraction;
 * each platform source set provides a concrete implementation that knows how
 * to build the native driver (Android: [app.cash.sqldelight.driver.android.AndroidSqliteDriver],
 * iOS: NativeSqliteDriver) and the platform DI module wires the right one.
 */
interface DatabaseDriverFactory {

    fun createDriver(): SqlDriver
}
