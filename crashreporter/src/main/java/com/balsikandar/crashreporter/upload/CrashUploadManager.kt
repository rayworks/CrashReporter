package com.balsikandar.crashreporter.upload

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.balsikandar.crashreporter.upload.internal.CrashUploadWorker
import java.util.concurrent.TimeUnit

/**
 * Coordinates crash-report upload: holds the consumer configuration and schedules the
 * background [CrashUploadWorker].
 *
 * Wiring:
 *  - [setup] is called once by `CrashReporter.initialize(...)` at app startup (via the
 *    auto-init ContentProvider), giving us the app context and the crash-report directory.
 *  - The host app then opts in with [setUploader] + [setUploadEnabled] (e.g. in
 *    `Application.onCreate`). Enabling with an uploader present schedules an upload pass.
 *
 * The registered uploader is held only in memory: after a process restart the host must
 * register it again before reports can be sent, hence the worker retries when none is set.
 */
object CrashUploadManager {

    private const val UNIQUE_WORK_NAME = "crashreporter_upload"
    private const val BACKOFF_SECONDS = 30L

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var uploadEnabled: Boolean = false

    @Volatile
    internal var crashReportRootPath: String? = null
        private set

    @Volatile
    internal var uploader: CrashReportUploader? = null
        private set

    @Volatile
    internal var deleteAfterUpload: Boolean = true
        private set

    @Volatile
    internal var maxAttempts: Int = 5
        private set

    /** Called by the library at initialize time. Not part of the consumer-facing API. */
    @JvmStatic
    fun setup(context: Context, crashReportRootPath: String) {
        this.appContext = context.applicationContext
        this.crashReportRootPath = crashReportRootPath
        maybeEnqueue()
    }

    /** Register (or clear with `null`) the strategy that ships reports to a backend. */
    @JvmStatic
    fun setUploader(uploader: CrashReportUploader?) {
        this.uploader = uploader
        maybeEnqueue()
    }

    /** Master switch for uploading. Disabled by default so behaviour is unchanged until opted in. */
    @JvmStatic
    fun setUploadEnabled(enabled: Boolean) {
        this.uploadEnabled = enabled
        if (enabled) maybeEnqueue()
    }

    /** Delete a report file once it has been uploaded successfully. Defaults to `true`. */
    @JvmStatic
    fun setDeleteAfterUpload(delete: Boolean) {
        this.deleteAfterUpload = delete
    }

    /** Max upload attempts before a report is left as permanently `FAILED`. Defaults to 5. */
    @JvmStatic
    fun setMaxAttempts(max: Int) {
        this.maxAttempts = max.coerceAtLeast(1)
    }

    /** Force an upload pass now (subject to the network constraint), replacing any pending run. */
    @JvmStatic
    fun uploadNow() {
        enqueue(ExistingWorkPolicy.REPLACE)
    }

    private fun maybeEnqueue() {
        if (uploadEnabled && uploader != null && appContext != null && crashReportRootPath != null) {
            enqueue(ExistingWorkPolicy.KEEP)
        }
    }

    private fun enqueue(policy: ExistingWorkPolicy) {
        val context = appContext ?: return
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequest.Builder(CrashUploadWorker::class.java)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_WORK_NAME, policy, request)
    }
}
