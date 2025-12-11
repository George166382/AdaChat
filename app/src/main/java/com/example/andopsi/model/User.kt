package com.example.andopsi.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Define Enums for stricter control
enum class UserRole { USER, ADMIN, MODERATOR }
enum class UserStatus { ACTIVE, BANNED, PENDING }



@Serializable
@Entity(tableName = "users")
data class User(
    @SerialName("user_id")
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @SerialName("user_name")
    @ColumnInfo(name = "user_name")
    val username: String,

    @SerialName("user_email")
    @ColumnInfo(name = "user_email")
    val email: String,

    @SerialName("passwd_hash")
    @ColumnInfo(name = "passwd_hash")
    val passwordHash: String = "",

    @SerialName("display_name")
    @ColumnInfo(name = "display_name")
    val displayName: String?,

    @SerialName("avatar_url")
    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String?,

    val role: UserRole = UserRole.USER,
    val status: UserStatus = UserStatus.ACTIVE,

    @SerialName("is_verified")
    @ColumnInfo(name = "is_verified")
    val isVerified: Boolean = false,

    @SerialName("last_login_at")
    @ColumnInfo(name = "last_login_at")
    val lastLoginAt: Instant? = null,  // Changed from Long to Instant

    @SerialName("last_login_ip")
    @ColumnInfo(name = "last_login_ip")
    val lastLoginIp: String? = null,

    @SerialName("created_at")
    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Clock.System.now(),  // Changed from Long to Instant

    @SerialName("updated_at")
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant = Clock.System.now()  // Changed from Long to Instant
)



