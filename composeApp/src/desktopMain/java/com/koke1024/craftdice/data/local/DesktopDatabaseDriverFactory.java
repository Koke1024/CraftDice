package com.koke1024.craftdice.data.local;

import app.cash.sqldelight.driver.jdbc.JdbcDrivers;
import java.io.File;
import javax.sql.DataSource;
import org.sqlite.SQLiteDataSource;

public class DesktopDatabaseDriverFactory implements DatabaseDriverFactory {

    private static final String DB_NAME = "craftdice.db";
    private static final String JDBC_URL = "jdbc:sqlite:craftdice.db";

    @Override
    public app.cash.sqldelight.db.SqlDriver createDriver() {
        File dbFile = new File(DB_NAME);
        boolean isNewDatabase = !dbFile.exists() || dbFile.length() == 0L;

        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(JDBC_URL);

        app.cash.sqldelight.db.SqlDriver driver = JdbcDrivers.fromDataSource(dataSource);
        if (isNewDatabase) {
            CraftDiceDatabase.Companion.getSchema().create(driver);
        }
        return driver;
    }
}
