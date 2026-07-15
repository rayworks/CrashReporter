package com.balsikandar.crashreporter.upload

/**
 * The kind of crash report a [CrashReportUploader] is asked to upload.
 *
 * - [CRASH]     : a fatal JVM crash stack-trace (`*_crash.txt`).
 * - [EXCEPTION] : a manually logged non-fatal exception (`*_exception.txt`).
 * - [MINIDUMP]  : a native Breakpad minidump (`*.dmp` under `core_dump/`).
 */
enum class ReportType {
    CRASH,
    EXCEPTION,
    MINIDUMP
}
