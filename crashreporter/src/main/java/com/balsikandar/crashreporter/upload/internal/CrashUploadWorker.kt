package com.balsikandar.crashreporter.upload.internal

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.balsikandar.crashreporter.upload.CrashUploadManager
import java.io.File

/**
 * Drains pending crash reports through the registered
 * [com.balsikandar.crashreporter.upload.CrashReportUploader].
 *
 * Runs on WorkManager's background executor (blocking [Worker.doWork]), so all DB and
 * network work here is off the main/crashing thread. Each run first reconciles the
 * on-disk files with the state store, then attempts every uploadable report once.
 */
internal class CrashUploadWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val root = CrashUploadManager.crashReportRootPath
        if (root == null) {
            Log.w(TAG, "no crash-report path configured; nothing to upload")
            return Result.success()
        }

        val uploader = CrashUploadManager.uploader
        if (uploader == null) {
            // Process may have been recreated before the host re-registered its uploader.
            // Retry with backoff so we don't drop reports.
            Log.d(TAG, "no uploader registered yet; will retry")
            return Result.retry()
        }

        val dao = CrashReportDatabase.getInstance(applicationContext).crashReportDao()
        CrashReportScanner.reconcile(dao, root)

        val maxAttempts = CrashUploadManager.maxAttempts
        val deleteAfterUpload = CrashUploadManager.deleteAfterUpload
        var hadFailure = false

        for (entity in dao.getUploadable(maxAttempts)) {
            val file = File(entity.filePath)
            if (!file.exists()) {
                dao.deleteByPath(entity.filePath)
                continue
            }

            dao.updateStatus(entity.filePath, UploadStatus.UPLOADING)
            val now = System.currentTimeMillis()
            val nextAttempt = entity.attemptCount + 1

            try {
                if (uploader.upload(file, entity.type)) {
                    dao.updateAttempt(entity.filePath, UploadStatus.UPLOADED, nextAttempt, now, null)
                    if (deleteAfterUpload) {
                        file.delete()
                        dao.deleteByPath(entity.filePath)
                    }
                    Log.d(TAG, "uploaded ${entity.type} report: ${file.name}")
                } else {
                    hadFailure = true
                    dao.updateAttempt(entity.filePath, UploadStatus.FAILED, nextAttempt, now, "uploader returned false")
                }
            } catch (e: Exception) {
                hadFailure = true
                dao.updateAttempt(entity.filePath, UploadStatus.FAILED, nextAttempt, now, e.message)
                Log.w(TAG, "upload failed for ${file.name}", e)
            }
        }

        // Ask WorkManager to retry (with backoff) only if something failed and is still
        // under the attempt limit; otherwise we're done for this run.
        return if (hadFailure && dao.countUploadable(maxAttempts) > 0) Result.retry() else Result.success()
    }

    companion object {
        private const val TAG = "CrashUploadWorker"
    }
}
