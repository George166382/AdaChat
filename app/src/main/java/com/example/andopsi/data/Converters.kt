package com.example.andopsi.data

import androidx.room.TypeConverter
import com.example.andopsi.model.UserRole
import com.example.andopsi.model.UserStatus
import kotlinx.datetime.Instant

class Converters {

    // Convert Instant to Long (milliseconds) for Room
    @TypeConverter
    fun fromInstant(value: Instant?): Long? {
        return value?.toEpochMilliseconds()
    }

    // Convert Long (milliseconds) back to Instant
    @TypeConverter
    fun toInstant(value: Long?): Instant? {
        return value?.let { Instant.fromEpochMilliseconds(it) }
    }
    @TypeConverter
    fun fromRole(role: UserRole): String = role.name

    @TypeConverter
    fun toRole(value: String): UserRole = enumValueOf(value)

    @TypeConverter
    fun fromStatus(status: UserStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): UserStatus = enumValueOf(value)
}