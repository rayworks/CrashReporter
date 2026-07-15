package com.balsikandar.crashreporter.upload.internal

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Blocking DAO. All methods are invoked from a WorkManager background thread
 * ([CrashUploadWorker]) — never from the main thread or the crashing thread — so
 * synchronous queries are safe.
 */
@Dao
internal interface CrashReportDao {

    /**
     * Insert newly discovered reports. [OnConflictStrategy.IGNORE] keeps the existing row
     * (and therefore its status/attempt history) for files already tracked.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIgnore(reports: List<CrashReportEntity>)

    @Query("SELECT filePath FROM crash_reports")
    fun getAllPaths(): List<String>

    /** Reports still worth attempting: never-sent or previously failed but under the limit. */
    @Query(
        "SELECT * FROM crash_reports " +
            "WHERE status IN ('PENDING', 'FAILED') AND attemptCount < :maxAttempts " +
            "ORDER BY createdAt ASC"
    )
    fun getUploadable(maxAttempts: Int): List<CrashReportEntity>

    @Query("SELECT COUNT(*) FROM crash_reports WHERE status IN ('PENDING', 'FAILED') AND attemptCount < :maxAttempts")
    fun countUploadable(maxAttempts: Int): Int

    @Query("UPDATE crash_reports SET status = :status WHERE filePath = :filePath")
    fun updateStatus(filePath: String, status: UploadStatus)

    @Query(
        "UPDATE crash_reports SET status = :status, attemptCount = :attemptCount, " +
            "lastAttemptAt = :lastAttemptAt, lastError = :lastError WHERE filePath = :filePath"
    )
    fun updateAttempt(
        filePath: String,
        status: UploadStatus,
        attemptCount: Int,
        lastAttemptAt: Long,
        lastError: String?
    )

    @Query("DELETE FROM crash_reports WHERE filePath = :filePath")
    fun deleteByPath(filePath: String)
}
