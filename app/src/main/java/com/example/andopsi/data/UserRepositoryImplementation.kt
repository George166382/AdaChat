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




    // Add this helper function to decode hex string to ASCII
    private fun hexToString(hex: String): String {
        val result = StringBuilder()
        var i = 0
        while (i < hex.length) {
            val str = hex.substring(i, i + 2)
            result.append(str.toInt(16).toChar())
            i += 2
        }
        return result.toString()
    }

    override suspend fun login(email: String, pass: String): Result<User> {
        return try {
            val users = supabase.from("users")
                .select()
                .decodeList<User>()

            val supabaseUser = users.firstOrNull { it.email == email }

            if (supabaseUser != null) {
                val inputHash = hashPassword(pass)

                val storedHash = if (supabaseUser.passwordHash.startsWith("\\x")) {
                    val hexString = supabaseUser.passwordHash.removePrefix("\\x")
                    hexToString(hexString)
                } else {
                    supabaseUser.passwordHash
                }

                if (storedHash == inputHash) {
                    if (supabaseUser.status != UserStatus.ACTIVE && supabaseUser.status != UserStatus.PENDING) {
                        return Result.failure(Exception("Account status is ${supabaseUser.status}. Access denied."))
                    }

                    // IMPORTANT: Check if user exists locally first
                    val existingLocalUser = userDao.getUserByEmail(email)

                    val user = if (existingLocalUser != null) {
                        // Update existing user
                        existingLocalUser.copy(
                            lastLoginAt = Clock.System.now(),
                            lastLoginIp = "127.0.0.1",
                            updatedAt = Clock.System.now()
                        ).also { userDao.updateUser(it) }
                    } else {
                        // Insert new user with Supabase ID
                        User(
                            id = supabaseUser.id ?: 0, // Use Supabase ID
                            username = supabaseUser.username,
                            email = supabaseUser.email,
                            passwordHash = supabaseUser.passwordHash,
                            displayName = supabaseUser.displayName,
                            avatarUrl = supabaseUser.avatarUrl,
                            role = supabaseUser.role,
                            status = supabaseUser.status,
                            isVerified = supabaseUser.isVerified,
                            lastLoginAt = Clock.System.now(),
                            lastLoginIp = "127.0.0.1",
                            createdAt = supabaseUser.createdAt,
                            updatedAt = Clock.System.now()
                        ).also { userDao.insertUser(it) }
                    }

                    _loggedInUser.value = user

                    try {
                        supabase.from("users").upsert(user)
                        Log.d("Supabase", "Sync successful")
                    } catch (e: Exception) {
                        Log.e("Supabase", "Sync failed, Worker will handle it later", e)
                    }
                    Result.success(user)
                } else {
                    Result.failure(Exception("Invalid password"))
                }
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
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