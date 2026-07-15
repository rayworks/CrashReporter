package com.balsikandar.crashreporter.upload

import java.io.File

/**
 * Consumer-supplied strategy for shipping a single crash report file to a backend.
 *
 * CrashReporter owns discovery, state persistence and retry scheduling; the host app
 * owns *where* the report goes. Register an implementation with
 * [com.balsikandar.crashreporter.CrashReporter.setUploader].
 *
 * Implementations:
 *  - are invoked on a WorkManager background thread (never on the crashing thread), so
 *    blocking network calls are fine here;
 *  - must return `true` only when the report has been durably accepted by the backend;
 *  - may throw — a thrown exception is treated exactly like returning `false` (the report
 *    is retried later with backoff until the attempt limit is reached).
 *
 * Because the app process can be recreated between a crash and the upload, register the
 * uploader early and unconditionally (e.g. in `Application.onCreate`), not only right after
 * a crash.
 */
fun interface CrashReportUploader {
    /**
     * @param report the report file on disk (a `.txt` stack trace or a `.dmp` minidump).
     * @param type   which kind of report [report] is.
     * @return `true` if the backend durably accepted the report; `false` to retry later.
     */
    @Throws(Exception::class)
    fun upload(report: File, type: ReportType): Boolean
}
