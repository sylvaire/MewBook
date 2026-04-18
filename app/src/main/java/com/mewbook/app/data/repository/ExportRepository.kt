package com.mewbook.app.data.repository

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.mewbook.app.domain.repository.CategoryRepository
import com.mewbook.app.domain.repository.RecordRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recordRepository: RecordRepository,
    private val categoryRepository: CategoryRepository,
    private val backupRepository: BackupRepository
) {

    suspend fun exportToCsv(ledgerId: Long): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val records = recordRepository.getAllRecordsOnce()
                .filter { it.ledgerId == ledgerId }
            val categoriesMap = categoryRepository.getAllCategories().first()
                .associateBy { it.id }

            val fileName = "mewbook_export_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)

            FileWriter(file).use { writer ->
                // CSV header with BOM for Excel UTF-8 compatibility
                writer.append("\uFEFF")
                writer.append("日期,类型,分类,子分类,金额,备注\n")

                // Data rows
                records.forEach { record ->
                    val category = categoriesMap[record.categoryId]
                    val parentCategory = category?.parentId?.let { categoriesMap[it] }
                    val date = record.date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    val type = if (record.type.name == "EXPENSE") "支出" else "收入"
                    val categoryName = parentCategory?.name ?: category?.name ?: "未知"
                    val subCategoryName = if (parentCategory != null) category.name else ""
                    val amount = String.format("%.2f", record.amount)
                    val note = record.note?.replace(",", "，")?.replace("\n", " ") ?: ""

                    writer.append("$date,$type,$categoryName,$subCategoryName,$amount,$note\n")
                }
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportToJson(): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val jsonString = backupRepository.exportToJsonString()

            val fileName = "mewbook_export_${System.currentTimeMillis()}.json"
            val file = File(context.cacheDir, fileName)

            FileWriter(file).use { writer ->
                writer.append(jsonString)
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
