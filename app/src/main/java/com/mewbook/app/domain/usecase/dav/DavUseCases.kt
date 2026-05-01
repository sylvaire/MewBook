package com.mewbook.app.domain.usecase.dav

import com.mewbook.app.domain.model.DavBackupFile
import com.mewbook.app.domain.model.DavConfig
import com.mewbook.app.domain.repository.DavRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDavConfigUseCase @Inject constructor(
    private val davRepository: DavRepository
) {
    operator fun invoke(): Flow<DavConfig?> {
        return davRepository.getDavConfig()
    }

    suspend fun getOnce(): DavConfig? {
        return davRepository.getDavConfigOnce()
    }
}

class SaveDavConfigUseCase @Inject constructor(
    private val davRepository: DavRepository
) {
    suspend operator fun invoke(config: DavConfig) {
        davRepository.saveDavConfig(config)
    }
}

class TestConnectionUseCase @Inject constructor(
    private val davRepository: DavRepository
) {
    suspend operator fun invoke(config: DavConfig): Result<Boolean> {
        return davRepository.testConnection(config)
    }
}

class ExportDataUseCase @Inject constructor(
    private val davRepository: DavRepository
) {
    suspend operator fun invoke(config: DavConfig, fileName: String? = null): Result<Boolean> {
        return davRepository.exportData(config, fileName)
    }

    suspend fun autoBackup(config: DavConfig): Result<Boolean> {
        return davRepository.exportAutoBackupData(config)
    }
}

class PreviewImportDataUseCase @Inject constructor(
    private val davRepository: DavRepository
) {
    suspend operator fun invoke(config: DavConfig): Result<com.mewbook.app.data.backup.BackupRestorePreview> {
        return davRepository.previewImportData(config)
    }

    suspend operator fun invoke(
        config: DavConfig,
        backupFile: DavBackupFile
    ): Result<com.mewbook.app.data.backup.BackupRestorePreview> {
        return davRepository.previewImportData(config, backupFile)
    }
}

class ImportDataUseCase @Inject constructor(
    private val davRepository: DavRepository
) {
    suspend operator fun invoke(config: DavConfig): Result<Boolean> {
        return davRepository.importData(config)
    }

    suspend operator fun invoke(config: DavConfig, backupFile: DavBackupFile): Result<Boolean> {
        return davRepository.importData(config, backupFile)
    }
}

class ListBackupFilesUseCase @Inject constructor(
    private val davRepository: DavRepository
) {
    suspend operator fun invoke(config: DavConfig): Result<List<DavBackupFile>> {
        return davRepository.listBackupFiles(config)
    }
}
