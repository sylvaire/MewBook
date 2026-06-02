package com.mewbook.app.domain.policy

import com.mewbook.app.data.backup.BackupChangeCount
import com.mewbook.app.data.backup.BackupChangeSummary
import com.mewbook.app.data.backup.BackupConflictSummary
import com.mewbook.app.data.backup.BackupRestorePreview
import com.mewbook.app.data.backup.BackupSnapshotSummary
import com.mewbook.app.domain.model.DavConflictStrategy
import org.junit.Assert.assertEquals
import org.junit.Test

class DavConflictPolicyTest {

    @Test
    fun localFirst_keepsLocalWhenPreviewHasOverwriteRisk() {
        val preview = previewWith(changes = BackupChangeSummary(records = BackupChangeCount(modified = 1)))

        val decision = DavConflictPolicy.decide(DavConflictStrategy.LOCAL_FIRST, preview)

        assertEquals(DavImportDecision.KEEP_LOCAL, decision)
    }

    @Test
    fun localFirst_importsWhenRemoteOnlyAddsData() {
        val preview = previewWith(changes = BackupChangeSummary(records = BackupChangeCount(added = 2)))

        val decision = DavConflictPolicy.decide(DavConflictStrategy.LOCAL_FIRST, preview)

        assertEquals(DavImportDecision.IMPORT_REMOTE, decision)
    }

    @Test
    fun remoteFirst_importsEvenWhenPreviewHasOverwriteRisk() {
        val preview = previewWith(changes = BackupChangeSummary(records = BackupChangeCount(deleted = 1)))

        val decision = DavConflictPolicy.decide(DavConflictStrategy.REMOTE_FIRST, preview)

        assertEquals(DavImportDecision.IMPORT_REMOTE, decision)
    }

    @Test
    fun manual_requiresChoiceWhenPreviewHasOverwriteRisk() {
        val preview = previewWith(
            conflicts = BackupConflictSummary(
                records = 1,
                categories = 0,
                accounts = 0,
                budgets = 0,
                templates = 0,
                ledgers = 0
            )
        )

        val decision = DavConflictPolicy.decide(DavConflictStrategy.MANUAL, preview)

        assertEquals(DavImportDecision.REQUIRE_MANUAL_CHOICE, decision)
    }

    private fun previewWith(
        changes: BackupChangeSummary = BackupChangeSummary(),
        conflicts: BackupConflictSummary = BackupConflictSummary(
            records = 0,
            categories = 0,
            accounts = 0,
            budgets = 0,
            templates = 0,
            ledgers = 0
        )
    ): BackupRestorePreview {
        return BackupRestorePreview(
            current = summary(),
            incoming = summary(),
            conflicts = conflicts,
            changes = changes
        )
    }

    private fun summary(): BackupSnapshotSummary {
        return BackupSnapshotSummary(
            records = 0,
            categories = 0,
            accounts = 0,
            budgets = 0,
            templates = 0,
            ledgers = 0,
            hasDavConfig = false,
            themeMode = null
        )
    }
}
