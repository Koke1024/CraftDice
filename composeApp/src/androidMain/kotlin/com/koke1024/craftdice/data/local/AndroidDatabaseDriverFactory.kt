package com.koke1024.craftdice.data.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

/**
 * Android [DatabaseDriverFactory] backed by [AndroidSqliteDriver].
 *
 * The application [Context] is supplied by the platform DI module (which has
 * access to Koin's androidContext). The database file lives in the app's
 * private storage, so no extra permissions are required.
 */
class AndroidDatabaseDriverFactory(
    private val context: Context,
) : DatabaseDriverFactory {

    override fun createDriver(): SqlDriver =
        AndroidSqliteDriver(CraftDiceDatabase.Schema, context, DB_NAME)

    private companion object {
        const val DB_NAME = "craftdice.db"
    }
}
