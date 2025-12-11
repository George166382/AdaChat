package com.example.andopsi.ui.home

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.andopsi.data.VideoRepository
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import com.example.andopsi.VideoApplication
import com.example.andopsi.data.UserRepository
import com.example.andopsi.model.Video
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import retrofit2.Response

class HomeViewModel(private val videoRepository: VideoRepository, private val userRepository: UserRepository) : ViewModel() {

    private val _video = MutableStateFlow<Result<Response<Video>>?>(null)
    private val _videoDebugScreen = MutableStateFlow<Result<Response<Video>>?>(null)
    //private val _transcript = MutableStateFlow<Result<Response<com.example.andopsi.model.TranscriptResponse>>?>(null)
    val video = _video.asStateFlow()
    val videoDebugScreen = _videoDebugScreen.asStateFlow()
   // val transcript = _transcript.asStateFlow()

    // --- USER STATE ---
    // Automatically updates when the user logs in/out in the Repository
    val currentUser = userRepository.currentUserFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private fun extractVideoIdFromUrl(url: String): String {
        return when {
            "youtu.be/" in url -> url.substringAfterLast("/").substringBefore("?")
            "youtube.com/shorts/" in url -> url.substringAfterLast("/").substringBefore("?")
            "v=" in url -> url.substringAfter("v=").substringBefore("&").substringBefore("?")
            else -> ""
        }
    }

    fun fetchVideoDetailsForDebugScreen(url: String) {
        val videoId = extractVideoIdFromUrl(url)
        if (videoId.isNotEmpty()) {
            getVideoForDebugScreen(videoId)
        }
    }
    fun getVideoForDebugScreen(videoId: String) {
        viewModelScope.launch {
            try {
                val v = videoRepository.getVideo(videoId)   // Response<Video>
                _videoDebugScreen.value = Result.success(v)
            } catch (e: Exception) {
                _videoDebugScreen.value = Result.failure(e)
            }
        }
    }
    fun fetchVideoDetailsFromUrl(url: String) {
        val videoId = extractVideoIdFromUrl(url)
        if (videoId.isNotEmpty()) {
            getVideo(videoId)
        }
    }

    fun getVideo(videoId: String) {
        viewModelScope.launch {
            try {
                val v = videoRepository.getVideo(videoId)   // Response<Video>
                _video.value = Result.success(v)
            } catch (e: Exception) {
                _video.value = Result.failure(e)
            }
        }
    }
    // Inside HomeViewModel class

    /*fun analyzeVideo(videoId: String) {

        viewModelScope.launch {

                try {
                    val transcriptResult = videoRepository.getTranscript(videoId)
                    _transcript.value = Result.success(transcriptResult)

                } catch (e: Exception) {
                    // Handle network error
                    _transcript.value = Result.failure(e)
                }
            }
    }*/
    // ... inside HomeViewModel ...

    fun updateUserProfile(name: String, avatarUrl: String?, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val user = currentUser.value
            if (user != null) {
                val result = userRepository.updateUserProfile(user.id, name, avatarUrl)
                result.onSuccess { onResult(true, null) }
                    .onFailure { onResult(false, it.message) }
            } else {
                onResult(false, "Not logged in")
            }
        }
    }
    // --- AUTH ACTIONS ---
    fun login(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = userRepository.login(email, pass)
            result.onSuccess { onResult(true, null) }
                .onFailure { onResult(false, it.message) }
        }
    }


    fun signUp(
        username: String,
        email: String,
        pass: String,
        name: String,
        avatarUrl: String?, // <--- NEW
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            // Pass avatarUrl to repository
            val result = userRepository.register(username, email, pass, name, avatarUrl)
            result.onSuccess { onResult(true, null) }
                .onFailure { onResult(false, it.message) }
        }
    }

    fun logout() {
        viewModelScope.launch {
            userRepository.signOut()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as VideoApplication)
                val videoRepo = application.container.videoRepository
                val userRepo = application.container.userRepository // <--- Get from Container
                HomeViewModel(videoRepo, userRepo)
            }
        }
    }
}
