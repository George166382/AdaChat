package com.example.andopsi.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.andopsi.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

@Dao
interface UserDao {


    // FIX: Change 'email' to 'user_email' to match your @ColumnInfo
    @Query("SELECT * FROM users WHERE user_email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    // "Extract data from last 6 hours"
    // Room maps the query result to your User class automatically
    @Query("SELECT * FROM users WHERE updated_at >= :cutoffTime")
    suspend fun getRecentUpdates(cutoffTime: Instant): List<User>

    // Return Long (the new Row ID) instead of Unit, helpful for Int keys
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    // Update ID type
    @Query("SELECT * FROM users WHERE id = :userId")
    fun observeUserById(userId: Int): Flow<User?>

    // Update: Change profile pic or name
    @Update
    suspend fun updateUser(user: User)
}