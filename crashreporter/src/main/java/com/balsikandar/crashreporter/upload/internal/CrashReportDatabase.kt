package com.balsikandar.crashreporter.upload.internal

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [CrashReportEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
internal abstract class CrashReportDatabase : RoomDatabase() {

    abstract fun crashReportDao(): CrashReportDao

    companion object {
        @Volatile
        private var instance: CrashReportDatabase? = null

        /** Lives in internal storage, separate from the (external) crash-report files. */
        fun getInstance(context: Context): CrashReportDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CrashReportDatabase::class.java,
                    "crashreporter_uploads.db"
                ).build().also { instance = it }
            }
    }
}
