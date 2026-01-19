package com.example.andopsi.data

import androidx.room.*
import com.example.andopsi.model.User
import com.example.andopsi.model.Usermeta
import kotlinx.coroutines.flow.Flow

@Dao
interface UsermetaDao {
    // Insert or Update the preference
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(usermeta: Usermeta)

    @Update
    suspend fun updateUsermeta(usermeta: Usermeta)

    @Query("SELECT * FROM usermeta WHERE user_id = :userId AND meta_key = :metaKey LIMIT 1")
    suspend fun getMetaForKey(userId: Long, metaKey: String): Usermeta?
}