package com.balsikandar.crashreporter.upload.internal

/**
 * Upload lifecycle of a single crash report, persisted in the local [CrashReportEntity].
 *
 * PENDING → UPLOADING → UPLOADED           (happy path)
 *              ↓
 *            FAILED → (retried while attemptCount < max) → UPLOADED
 */
internal enum class UploadStatus {
    PENDING,
    UPLOADING,
    UPLOADED,
    FAILED
}
