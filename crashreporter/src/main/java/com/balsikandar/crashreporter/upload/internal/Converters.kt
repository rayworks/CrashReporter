package com.balsikandar.crashreporter.upload.internal

import androidx.room.TypeConverter
import com.balsikandar.crashreporter.upload.ReportType

/** Room converters for the enums stored on [CrashReportEntity]. */
internal class Converters {

    @TypeConverter
    fun reportTypeToString(type: ReportType): String = type.name

    @TypeConverter
    fun stringToReportType(value: String): ReportType = ReportType.valueOf(value)

    @TypeConverter
    fun uploadStatusToString(status: UploadStatus): String = status.name

    @TypeConverter
    fun stringToUploadStatus(value: String): UploadStatus = UploadStatus.valueOf(value)
}
