package com.example.andopsi.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Entity representing user metadata (e.g., language, settings).
 * Linked to the User table via foreign key.
 */
@Serializable
@Entity(
    tableName = "usermeta",
    foreignKeys = [
        ForeignKey(
            entity = User::class, // Assumes you have a 'User' entity class
            parentColumns = ["id"], // The Primary Key in your User table
            childColumns = ["user_id"], // The Foreign Key in this table
            onDelete = ForeignKey.CASCADE // If user is deleted, delete their meta too
        )
    ],
    indices = [Index(value = ["user_id"]), Index(value = ["meta_key"]), Index(value = ["user_id", "meta_key", "client_type"]) ] // Index for faster lookups
)
data class Usermeta(
    @SerialName("usermeta_id")
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "usermeta_id")
    val id: Int = 0,

    @SerialName("user_id")
    @ColumnInfo(name = "user_id")
    val userId: Int,

    @SerialName("meta_key")
    @ColumnInfo(name = "meta_key")
    val metaKey: String, // e.g., "language"

    @SerialName("meta_value")
    @ColumnInfo(name = "meta_value")
    val metaValue: String, // e.g., "en", "ro"

    @SerialName("client_type")
    @ColumnInfo(name = "client_type")
    val clientType: String? = null, // e.g., "mobile", "web"

    @SerialName("created_at")
    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Clock.System.now(),

    @SerialName("updated_at")
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant = Clock.System.now()
)