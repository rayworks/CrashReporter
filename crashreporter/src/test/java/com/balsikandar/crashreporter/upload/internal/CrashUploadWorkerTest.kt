package com.balsikandar.crashreporter.upload.internal

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.balsikandar.crashreporter.upload.CrashReportUploader
import com.balsikandar.crashreporter.upload.CrashUploadManager
import com.balsikandar.crashreporter.upload.ReportType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.concurrent.Executors

@RunWith(RobolectricTestRunner::class)
class CrashUploadWorkerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var context: Context
    private lateinit var root: File

    /** Room forbids main-thread queries; the WorkManager worker (and our assertions) run off it. */
    private fun <T> onBackground(block: () -> T): T = executor.submit(block).get()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        root = tempFolder.root
        File(root, "2026-07-14 10-00-00_crash.txt").writeText("stack")
        CrashUploadManager.setup(context, root.absolutePath)
        CrashUploadManager.setDeleteAfterUpload(true)
        onBackground { CrashReportDatabase.getInstance(context).clearAllTables() }
    }

    @After
    fun tearDown() {
        CrashUploadManager.setUploader(null)
        executor.shutdown()
    }

    private fun runWorker(): ListenableWorker.Result {
        // doWork() is synchronous; run it off the main thread so Room queries are allowed.
        val worker = TestListenableWorkerBuilder<CrashUploadWorker>(context).build()
        return onBackground { worker.doWork() }
    }

    private fun crashFile() = File(root, "2026-07-14 10-00-00_crash.txt")

    @Test
    fun givenNoUploader_whenRun_thenRetry() {
        CrashUploadManager.setUploader(null)

        assertEquals(ListenableWorker.Result.retry(), runWorker())
    }

    @Test
    fun givenUploaderSucceeds_whenRun_thenSuccessAndFileDeleted() {
        val uploaded = mutableListOf<ReportType>()
        CrashUploadManager.setUploader(CrashReportUploader { _, type -> uploaded += type; true })

        val result = runWorker()

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(listOf(ReportType.CRASH), uploaded)
        assertFalse(crashFile().exists())
    }

    @Test
    fun givenUploaderReturnsFalse_whenRun_thenRetryAndFileKept() {
        CrashUploadManager.setUploader(CrashReportUploader { _, _ -> false })

        val result = runWorker()

        assertEquals(ListenableWorker.Result.retry(), result)
        assertTrue(crashFile().exists())

        val failed = onBackground {
            CrashReportDatabase.getInstance(context).crashReportDao().getUploadable(5).single()
        }
        assertEquals(UploadStatus.FAILED, failed.status)
        assertEquals(1, failed.attemptCount)
    }

    @Test
    fun givenUploaderThrows_whenRun_thenRetryAndFileKept() {
        CrashUploadManager.setUploader(CrashReportUploader { _, _ -> throw RuntimeException("network down") })

        val result = runWorker()

        assertEquals(ListenableWorker.Result.retry(), result)
        assertTrue(crashFile().exists())
    }
}
