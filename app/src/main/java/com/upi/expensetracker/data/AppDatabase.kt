package com.upi.expensetracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Transaction::class, CustomCategory::class, MonthlySetting::class, Budget::class, CategoryIcon::class],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // v1 -> v2: add the custom_categories table.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS custom_categories (" +
                        "name TEXT NOT NULL, type TEXT NOT NULL, " +
                        "PRIMARY KEY(name, type))"
                )
            }
        }

        // v2 -> v3: add the monthly_settings table (opening balance).
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS monthly_settings (" +
                        "monthKey TEXT NOT NULL, openingBalance REAL NOT NULL, " +
                        "PRIMARY KEY(monthKey))"
                )
            }
        }

        // v3 -> v4: add the budgets table (monthly category targets).
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS budgets (" +
                        "category TEXT NOT NULL, amount REAL NOT NULL, " +
                        "PRIMARY KEY(category))"
                )
            }
        }

        // v4 -> v5: budgets become per-month. Recreate with composite key and
        // move any existing recurring budgets into the current month.
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS budgets_new (" +
                        "monthKey TEXT NOT NULL, category TEXT NOT NULL, amount REAL NOT NULL, " +
                        "PRIMARY KEY(monthKey, category))"
                )
                db.execSQL(
                    "INSERT OR IGNORE INTO budgets_new (monthKey, category, amount) " +
                        "SELECT strftime('%Y-%m','now','localtime'), category, amount FROM budgets"
                )
                db.execSQL("DROP TABLE budgets")
                db.execSQL("ALTER TABLE budgets_new RENAME TO budgets")
            }
        }

        // v5 -> v6: add the category_icons table (user-chosen icons).
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS category_icons (" +
                        "category TEXT NOT NULL, iconKey TEXT NOT NULL, " +
                        "PRIMARY KEY(category))"
                )
            }
        }

        // v6 -> v7: allow a color too, and make iconKey optional.
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS category_icons_new (" +
                        "category TEXT NOT NULL, iconKey TEXT, colorHex TEXT, " +
                        "PRIMARY KEY(category))"
                )
                db.execSQL(
                    "INSERT OR IGNORE INTO category_icons_new (category, iconKey, colorHex) " +
                        "SELECT category, iconKey, NULL FROM category_icons"
                )
                db.execSQL("DROP TABLE category_icons")
                db.execSQL("ALTER TABLE category_icons_new RENAME TO category_icons")
            }
        }

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expenses.db"
                )
                    .addMigrations(
                        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
                        MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7
                    )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
