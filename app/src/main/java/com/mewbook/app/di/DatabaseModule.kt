package com.mewbook.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mewbook.app.data.local.dao.AccountDao
import com.mewbook.app.data.local.dao.BudgetDao
import com.mewbook.app.data.local.dao.CategoryDao
import com.mewbook.app.data.local.dao.DavConfigDao
import com.mewbook.app.data.local.dao.LedgerDao
import com.mewbook.app.data.local.dao.RecurringTemplateDao
import com.mewbook.app.data.local.dao.RecordDao
import com.mewbook.app.data.local.database.MewBookDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE budgets ADD COLUMN periodType TEXT NOT NULL DEFAULT 'MONTH'")
            db.execSQL("DROP INDEX IF EXISTS index_budgets_categoryId_month")
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_budgets_ledgerId_categoryId_periodType_month " +
                    "ON budgets(ledgerId, categoryId, periodType, month)"
            )
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                    CREATE TABLE IF NOT EXISTS recurring_templates (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        amount REAL NOT NULL,
                        type TEXT NOT NULL,
                        categoryId INTEGER NOT NULL,
                        noteTemplate TEXT,
                        ledgerId INTEGER NOT NULL,
                        accountId INTEGER,
                        scheduleType TEXT NOT NULL,
                        intervalCount INTEGER NOT NULL,
                        startDate INTEGER NOT NULL,
                        nextDueDate INTEGER NOT NULL,
                        endDate INTEGER,
                        isEnabled INTEGER NOT NULL DEFAULT 1,
                        reminderEnabled INTEGER NOT NULL DEFAULT 0,
                        lastGeneratedDate INTEGER,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_recurring_templates_ledgerId ON recurring_templates(ledgerId)")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_recurring_templates_ledgerId_isEnabled_nextDueDate " +
                    "ON recurring_templates(ledgerId, isEnabled, nextDueDate)"
            )
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): MewBookDatabase {
        return Room.databaseBuilder(
            context,
            MewBookDatabase::class.java,
            MewBookDatabase.DATABASE_NAME
        )
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
            .build()
    }

    @Provides
    @Singleton
    fun provideRecordDao(database: MewBookDatabase): RecordDao {
        return database.recordDao()
    }

    @Provides
    @Singleton
    fun provideCategoryDao(database: MewBookDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    @Singleton
    fun provideDavConfigDao(database: MewBookDatabase): DavConfigDao {
        return database.davConfigDao()
    }

    @Provides
    @Singleton
    fun provideAccountDao(database: MewBookDatabase): AccountDao {
        return database.accountDao()
    }

    @Provides
    @Singleton
    fun provideBudgetDao(database: MewBookDatabase): BudgetDao {
        return database.budgetDao()
    }

    @Provides
    @Singleton
    fun provideLedgerDao(database: MewBookDatabase): LedgerDao {
        return database.ledgerDao()
    }

    @Provides
    @Singleton
    fun provideRecurringTemplateDao(database: MewBookDatabase): RecurringTemplateDao {
        return database.recurringTemplateDao()
    }
}
