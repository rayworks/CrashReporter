package com.balsikandar.crashreporter.upload.internal

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.balsikandar.crashreporter.upload.ReportType

/**
 * Local record tracking the upload state of one crash report file.
 *
 * The absolute file path is the primary key: the file itself is the source of truth for
 * "does this report exist", and the DB row is the source of truth for "has it been sent".
 * Rows are (re)populated by [CrashReportScanner] on each upload run, never on the crashing
 * thread.
 */
@Entity(tableName = "crash_reports")
internal data class CrashReportEntity(
    @PrimaryKey val filePath: String,
    val type: ReportType,
    val status: UploadStatus = UploadStatus.PENDING,
    val attemptCount: Int = 0,
    val createdAt: Long,
    val lastAttemptAt: Long = 0L,
    val lastError: String? = null
)
