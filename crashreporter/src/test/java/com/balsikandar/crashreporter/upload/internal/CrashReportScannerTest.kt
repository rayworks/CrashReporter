package com.balsikandar.crashreporter.upload.internal

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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

@RunWith(RobolectricTestRunner::class)
class CrashReportScannerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var db: CrashReportDatabase
    private lateinit var dao: CrashReportDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            CrashReportDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.crashReportDao()
    }

    @After
    fun tearDown() = db.close()

    private fun write(dir: File, name: String): File =
        File(dir, name).apply { parentFile?.mkdirs(); writeText("stack") }

    @Test
    fun givenMixedFiles_whenReconcile_thenClassifiedByLocationAndSuffix() {
        val root = tempFolder.root
        write(root, "2026-07-14 10-00-00_crash.txt")
        write(root, "2026-07-14 10-01-00_exception.txt")
        write(root, "notes.log") // ignored: wrong extension
        val coreDump = File(root, "core_dump").apply { mkdirs() }
        write(coreDump, "abc123.dmp")

        CrashReportScanner.reconcile(dao, root.absolutePath)

        val byType = dao.getUploadable(5).groupBy { it.type }
        assertEquals(1, byType[ReportType.CRASH]?.size)
        assertEquals(1, byType[ReportType.EXCEPTION]?.size)
        assertEquals(1, byType[ReportType.MINIDUMP]?.size)
    }

    @Test
    fun givenTrackedFileDeleted_whenReconcile_thenRowPruned() {
        val root = tempFolder.root
        val crash = write(root, "2026-07-14 10-00-00_crash.txt")
        CrashReportScanner.reconcile(dao, root.absolutePath)
        assertEquals(1, dao.getAllPaths().size)

        assertTrue(crash.delete())
        CrashReportScanner.reconcile(dao, root.absolutePath)

        assertTrue(dao.getAllPaths().isEmpty())
    }

    @Test
    fun givenAlreadyUploaded_whenReconcile_thenNotResurrected() {
        val root = tempFolder.root
        val crash = write(root, "2026-07-14 10-00-00_crash.txt")
        CrashReportScanner.reconcile(dao, root.absolutePath)
        dao.updateStatus(crash.absolutePath, UploadStatus.UPLOADED)

        CrashReportScanner.reconcile(dao, root.absolutePath)

        assertFalse(dao.getUploadable(5).any { it.filePath == crash.absolutePath })
    }
}
