package com.mewbook.app.di

import com.mewbook.app.data.remote.DavClient
import com.mewbook.app.data.remote.DavRemoteDataSource
import com.mewbook.app.data.repository.AccountRepositoryImpl
import com.mewbook.app.data.repository.BackupRepository
import com.mewbook.app.data.repository.BackupSnapshotDataSource
import com.mewbook.app.data.repository.BudgetRepositoryImpl
import com.mewbook.app.data.repository.CategoryRepositoryImpl
import com.mewbook.app.data.repository.DavRepositoryImpl
import com.mewbook.app.data.repository.LedgerRepositoryImpl
import com.mewbook.app.data.repository.RecurringTemplateRepositoryImpl
import com.mewbook.app.data.repository.RecordRepositoryImpl
import com.mewbook.app.domain.repository.AccountRepository
import com.mewbook.app.domain.repository.BudgetRepository
import com.mewbook.app.domain.repository.CategoryRepository
import com.mewbook.app.domain.repository.DavRepository
import com.mewbook.app.domain.repository.LedgerRepository
import com.mewbook.app.domain.repository.RecurringTemplateRepository
import com.mewbook.app.domain.repository.RecordRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRecordRepository(
        recordRepositoryImpl: RecordRepositoryImpl
    ): RecordRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(
        categoryRepositoryImpl: CategoryRepositoryImpl
    ): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindDavRepository(
        davRepositoryImpl: DavRepositoryImpl
    ): DavRepository

    @Binds
    @Singleton
    abstract fun bindDavRemoteDataSource(
        davClient: DavClient
    ): DavRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindBackupSnapshotDataSource(
        backupRepository: BackupRepository
    ): BackupSnapshotDataSource

    @Binds
    @Singleton
    abstract fun bindAccountRepository(
        accountRepositoryImpl: AccountRepositoryImpl
    ): AccountRepository

    @Binds
    @Singleton
    abstract fun bindBudgetRepository(
        budgetRepositoryImpl: BudgetRepositoryImpl
    ): BudgetRepository

    @Binds
    @Singleton
    abstract fun bindLedgerRepository(
        ledgerRepositoryImpl: LedgerRepositoryImpl
    ): LedgerRepository

    @Binds
    @Singleton
    abstract fun bindRecurringTemplateRepository(
        recurringTemplateRepositoryImpl: RecurringTemplateRepositoryImpl
    ): RecurringTemplateRepository
}
