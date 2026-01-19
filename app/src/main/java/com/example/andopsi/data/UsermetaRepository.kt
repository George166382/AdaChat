package com.example.andopsi.data

import android.util.Log
import com.example.andopsi.model.Usermeta
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock



interface UsermetaRepository {
    suspend fun saveUserLanguage(userId: Int, languageCode: String) : Result<Usermeta>
    suspend fun getUserLanguage(userId: Long): String?
}

class OfflineUsermetaRepository(private val usermetaDao: UsermetaDao, private val supabase: SupabaseClient) : UsermetaRepository {

    override suspend fun saveUserLanguage(userId: Int, languageCode: String): Result<Usermeta> {
        return try {
            // Check if meta exists
            val existingMeta = usermetaDao.getMetaForKey(userId.toLong(), "language")

            val metaToSave = if (existingMeta != null) {
                Log.d("UsermetaRepository", "Updating existing meta: $existingMeta")
                val updated = existingMeta.copy(
                    metaValue = languageCode,
                    updatedAt = Clock.System.now()
                )
                usermetaDao.updateUsermeta(updated)
                updated
            } else {
                Log.d("UsermetaRepository", "Creating new meta for user $userId")
                val newMeta = Usermeta(
                    userId = userId,
                    metaKey = "language",
                    metaValue = languageCode
                )
                usermetaDao.insertOrUpdate(newMeta)
                newMeta
            }

            // Sync to Supabase
            try {
                supabase.from("usermeta").upsert(metaToSave)
                Log.d("Supabase", "Sync successful")
            } catch (e: Exception) {
                Log.e("Supabase", "Sync failed, Worker will handle it later", e)
            }

            Result.success(metaToSave) // Changed from 'meta' to 'metaToSave'

        } catch (e: Exception) {
            Log.e("UsermetaRepository", "Failed to save user language", e)
            Result.failure(e)
        }
    }

    override suspend fun getUserLanguage(userId: Long): String? {
        // Map the Usermeta object directly to the String value
        // return usermetaDao.getMetaForKey(userId, "language")

        return usermetaDao.getMetaForKey(userId, "language")?.metaValue




    }
}