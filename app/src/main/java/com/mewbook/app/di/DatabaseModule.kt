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
            .addMigrations(MIGRATION_2_3)
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
}
