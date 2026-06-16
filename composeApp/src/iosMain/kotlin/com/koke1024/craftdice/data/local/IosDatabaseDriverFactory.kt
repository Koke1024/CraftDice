package com.koke1024.craftdice.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

/**
 * iOS [DatabaseDriverFactory] backed by [NativeSqliteDriver].
 *
 * The database file is placed in the default NSDocumentDirectory, matching the
 * convention the native driver uses when only a name is given.
 */
class IosDatabaseDriverFactory : DatabaseDriverFactory {

    override fun createDriver(): SqlDriver =
        NativeSqliteDriver(CraftDiceDatabase.Schema, DB_NAME)

    private companion object {
        const val DB_NAME = "craftdice.db"
    }
}
