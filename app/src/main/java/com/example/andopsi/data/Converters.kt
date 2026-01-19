package com.example.andopsi.data

import androidx.room.TypeConverter
import com.example.andopsi.model.UserRole
import com.example.andopsi.model.UserStatus
import kotlinx.datetime.Instant

class Converters {

    // --- Instant Converters (Keep only this one pair) ---

    @TypeConverter
    fun fromTimestamp(value: Long?): Instant? {
        return value?.let { Instant.fromEpochMilliseconds(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Instant?): Long? {
        return date?.toEpochMilliseconds()
    }

    // --- Enum Converters ---

    @TypeConverter
    fun fromRole(role: UserRole): String = role.name

    @TypeConverter
    fun toRole(value: String): UserRole = enumValueOf(value)

    @TypeConverter
    fun fromStatus(status: UserStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): UserStatus = enumValueOf(value)
}