package com.mewbook.app.domain.policy

import com.mewbook.app.data.backup.BackupRestorePreview
import com.mewbook.app.domain.model.DavConflictStrategy

enum class DavImportDecision {
    KEEP_LOCAL,
    IMPORT_REMOTE,
    REQUIRE_MANUAL_CHOICE
}

object DavConflictPolicy {
    fun decide(
        strategy: DavConflictStrategy,
        preview: BackupRestorePreview
    ): DavImportDecision {
        val hasOverwriteRisk = preview.conflicts.totalConflicts > 0 ||
            preview.changes.modified > 0 ||
            preview.changes.deleted > 0

        return when (strategy) {
            DavConflictStrategy.LOCAL_FIRST -> {
                if (hasOverwriteRisk) DavImportDecision.KEEP_LOCAL else DavImportDecision.IMPORT_REMOTE
            }
            DavConflictStrategy.REMOTE_FIRST -> DavImportDecision.IMPORT_REMOTE
            DavConflictStrategy.MANUAL -> {
                if (hasOverwriteRisk) DavImportDecision.REQUIRE_MANUAL_CHOICE else DavImportDecision.IMPORT_REMOTE
            }
        }
    }
}
