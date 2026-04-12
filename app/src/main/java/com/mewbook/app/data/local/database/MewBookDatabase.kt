package com.mewbook.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mewbook.app.data.local.dao.AccountDao
import com.mewbook.app.data.local.dao.BudgetDao
import com.mewbook.app.data.local.dao.CategoryDao
import com.mewbook.app.data.local.dao.DavConfigDao
import com.mewbook.app.data.local.dao.LedgerDao
import com.mewbook.app.data.local.dao.RecordDao
import com.mewbook.app.data.local.entity.AccountEntity
import com.mewbook.app.data.local.entity.BudgetEntity
import com.mewbook.app.data.local.entity.CategoryEntity
import com.mewbook.app.data.local.entity.DavConfigEntity
import com.mewbook.app.data.local.entity.LedgerEntity
import com.mewbook.app.data.local.entity.RecordEntity

@Database(
    entities = [
        RecordEntity::class,
        CategoryEntity::class,
        DavConfigEntity::class,
        AccountEntity::class,
        BudgetEntity::class,
        LedgerEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MewBookDatabase : RoomDatabase() {
    abstract fun recordDao(): RecordDao
    abstract fun categoryDao(): CategoryDao
    abstract fun davConfigDao(): DavConfigDao
    abstract fun accountDao(): AccountDao
    abstract fun budgetDao(): BudgetDao
    abstract fun ledgerDao(): LedgerDao

    companion object {
        const val DATABASE_NAME = "mewbook.db"
    }
}
