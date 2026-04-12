package com.mewbook.app.di

import android.content.Context
import androidx.room.Room
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
            .fallbackToDestructiveMigration()
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
