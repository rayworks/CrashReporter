package com.balsikandar.crashreporter.upload.internal

import android.util.Log
import com.balsikandar.crashreporter.upload.ReportType
import com.balsikandar.crashreporter.utils.Constants
import java.io.File

/**
 * Reconciles the crash-report files on disk with the local state store.
 *
 * This is the only path that discovers reports for upload. It runs inside
 * [CrashUploadWorker] on a background thread — deliberately NOT at crash time, because:
 *  - a fatal JVM crash is handled on the dying thread, where DB work is unsafe;
 *  - native minidumps are written entirely in C++, so the JVM never observes them until
 *    a later launch anyway.
 *
 * New files become [UploadStatus.PENDING] rows; rows whose backing file is gone are pruned.
 */
internal object CrashReportScanner {

    private const val TAG = "CrashReportScanner"
    private const val CORE_DUMP_DIR = "core_dump"
    private const val MINIDUMP_EXTENSION = ".dmp"

    fun reconcile(dao: CrashReportDao, crashReportRootPath: String) {
        val root = File(crashReportRootPath)
        val discovered = mutableListOf<CrashReportEntity>()

        // Text reports live directly under the root: *_crash.txt and *_exception.txt
        root.listFiles()?.forEach { file ->
            if (file.isFile && file.name.endsWith(Constants.FILE_EXTENSION)) {
                classifyTextReport(file.name)?.let { type ->
                    discovered += newEntity(file, type)
                }
            }
        }

        // Native minidumps live under core_dump/*.dmp
        File(root, CORE_DUMP_DIR).listFiles()?.forEach { file ->
            if (file.isFile && file.name.endsWith(MINIDUMP_EXTENSION)) {
                discovered += newEntity(file, ReportType.MINIDUMP)
            }
        }

        if (discovered.isNotEmpty()) {
            dao.insertIgnore(discovered)
        }

        // Prune rows whose backing file was deleted (e.g. after a successful upload+cleanup,
        // or a manual delete from the viewer UI).
        val onDisk = discovered.mapTo(HashSet()) { it.filePath }
        dao.getAllPaths().forEach { path ->
            if (path !in onDisk && !File(path).exists()) {
                dao.deleteByPath(path)
            }
        }

        Log.d(TAG, "reconciled ${discovered.size} report file(s) under $crashReportRootPath")
    }

    private fun classifyTextReport(name: String): ReportType? = when {
        name.contains(Constants.CRASH_SUFFIX) -> ReportType.CRASH
        name.contains(Constants.EXCEPTION_SUFFIX) -> ReportType.EXCEPTION
        else -> null
    }

    private fun newEntity(file: File, type: ReportType) = CrashReportEntity(
        filePath = file.absolutePath,
        type = type,
        createdAt = file.lastModified()
    )
}
