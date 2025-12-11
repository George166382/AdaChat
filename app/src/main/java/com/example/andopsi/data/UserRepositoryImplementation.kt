package com.example.andopsi.data

import android.util.Log
import com.example.andopsi.model.User
import com.example.andopsi.model.UserRole
import com.example.andopsi.model.UserStatus
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.security.MessageDigest
import java.util.UUID

class UserRepositoryImplementation(private val userDao: UserDao, private val supabase: SupabaseClient) : UserRepository {

    // Simplified approach: We will just emit the user object directly for now
    private val _loggedInUser = MutableStateFlow<User?>(null)
    override val currentUserFlow: Flow<User?> = _loggedInUser.asStateFlow()

    override suspend fun register(
        username: String,
        email: String,
        pass: String,
        name: String,
        avatarUrl: String? // <--- NEW PARAMETER
    ): Result<User> {
        return try {
            // Check if email already exists
            val existing = userDao.getUserByEmail(email)
            if (existing != null) {
                return Result.failure(Exception("Email already exists"))
            }

            // Treat empty string as null for cleaner DB
            val finalAvatarUrl = if (avatarUrl.isNullOrBlank()) null else avatarUrl

            val newUser = User(
                id = 0,
                username = username,
                email = email,
                displayName = name,
                avatarUrl = finalAvatarUrl, // <--- PASS IT HERE
                passwordHash = hashPassword(pass),

                // Defaults
                role = UserRole.USER,
                status = UserStatus.PENDING,
                isVerified = false
            )

            userDao.insertUser(newUser)
            _loggedInUser.value = newUser

            try {
                supabase.from("users").insert(newUser)

                // If we get here, it worked!
                Log.d("Supabase", "User registration sync successful")

            } catch (e: Exception) {
                Log.e("Supabase", "User registration sync failed, Worker will handle it later", e)
                // Worker logic (checking 'created_at') will pick this up later!
            }
            Result.success(newUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun login(email: String, pass: String): Result<User> {
        val user = userDao.getUserByEmail(email)

        return if (user != null) {
            if (user.passwordHash == hashPassword(pass)) {

                // 1. Security Check: Is account active?
                if (user.status != UserStatus.ACTIVE && user.status != UserStatus.PENDING) {
                    return Result.failure(Exception("Account status is ${user.status}. Access denied."))
                }

                // 2. Update Tracking Fields (Last Login & IP)
                // Note: "127.0.0.1" is a placeholder. In a real app, you might fetch the actual IP.
                val updatedUser = user.copy(
                    lastLoginAt =  Clock.System.now(),

                    lastLoginIp = "127.0.0.1",
                    updatedAt = Clock.System.now() // Update the modification time too
                )

                // 3. Persist these updates to the database
                userDao.updateUser(updatedUser)

                // 4. Update UI State
                _loggedInUser.value = updatedUser

                // 3. Remote Update (Supabase)
                try {
                    // "upsert" = Insert if new, Update if exists (matches by Primary Key 'id')
                    supabase.from("users").upsert(updatedUser)

                    // If we get here, it worked!
                    Log.d("Supabase", "Sync successful")

                } catch (e: Exception) {
                    Log.e("Supabase", "Sync failed, Worker will handle it later", e)
                    // Worker logic (checking 'updated_at') will pick this up later!
                }
                Result.success(updatedUser)
            } else {
                Result.failure(Exception("Invalid password"))
            }
        } else {
            Result.failure(Exception("User not found"))
        }
    }


    override suspend fun updateUserProfile(userId: Int, name: String, avatarUrl: String?): Result<User> {
        return try {
            val currentUser = _loggedInUser.value ?: return Result.failure(Exception("No user"))

            // 1. Prepare Updated Object
            val updatedUser = currentUser.copy(
                displayName = name,
                avatarUrl = if (avatarUrl.isNullOrBlank()) null else avatarUrl,
                updatedAt = Clock.System.now()
            )

            // 2. Local Update (Optimistic)
            userDao.updateUser(updatedUser)
            _loggedInUser.value = updatedUser

            // 3. Remote Update (Supabase)
            try {
                // "upsert" = Insert if new, Update if exists (matches by Primary Key 'id')
                supabase.from("users").upsert(updatedUser)

                // If we get here, it worked!
                Log.d("Supabase", "Sync successful")

            } catch (e: Exception) {
                Log.e("Supabase", "Sync failed, Worker will handle it later", e)
                // Worker logic (checking 'updated_at') will pick this up later!
            }

            Result.success(updatedUser)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    override suspend fun signOut() {
        _loggedInUser.value = null
    }

    // Helper function to hash passwords
    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    override suspend fun refreshUserData() {
        // Not needed for local offline auth logic
    }
}