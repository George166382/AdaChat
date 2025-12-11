package com.example.andopsi.data

import com.example.andopsi.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    // Observes the current user. Emits null if logged out.
    // Flow allows the UI to update automatically if the user changes.
    //val currentUser: Flow<User?>
    val currentUserFlow: Flow<User?>
    // Basic CRUD / Auth actions
    suspend fun refreshUserData()
    suspend fun signOut()

    suspend fun login(email: String, password: String): Result<User>



    suspend fun register(username: String, email: String, pass: String, name: String, avatarUrl: String?): Result<User>


    suspend fun updateUserProfile(userId: Int, name: String, avatarUrl: String?): Result<User>
}