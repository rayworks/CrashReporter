package com.balsikandar.crashreporter.upload.internal

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.balsikandar.crashreporter.upload.ReportType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CrashReportDaoTest {

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

    private fun entity(path: String, type: ReportType = ReportType.CRASH) =
        CrashReportEntity(filePath = path, type = type, createdAt = 1L)

    @Test
    fun givenNewReports_whenInsertIgnore_thenAllTracked() {
        dao.insertIgnore(listOf(entity("/a_crash.txt"), entity("/b.dmp", ReportType.MINIDUMP)))

        assertEquals(2, dao.getUploadable(5).size)
    }

    @Test
    fun givenExistingRow_whenInsertIgnoreSamePath_thenStatusPreserved() {
        dao.insertIgnore(listOf(entity("/a_crash.txt")))
        dao.updateStatus("/a_crash.txt", UploadStatus.UPLOADED)

        // Re-discovering the same file must not reset it back to PENDING.
        dao.insertIgnore(listOf(entity("/a_crash.txt")))

        assertTrue(dao.getUploadable(5).isEmpty())
    }

    @Test
    fun givenAttemptsAtLimit_whenGetUploadable_thenExcluded() {
        dao.insertIgnore(listOf(entity("/a_crash.txt")))
        dao.updateAttempt("/a_crash.txt", UploadStatus.FAILED, 5, 10L, "boom")

        assertTrue(dao.getUploadable(5).isEmpty())
        assertEquals(0, dao.countUploadable(5))
    }

    @Test
    fun givenFailedUnderLimit_whenGetUploadable_thenRetryable() {
        dao.insertIgnore(listOf(entity("/a_crash.txt")))
        dao.updateAttempt("/a_crash.txt", UploadStatus.FAILED, 2, 10L, "boom")

        assertEquals(1, dao.getUploadable(5).size)
    }
}
