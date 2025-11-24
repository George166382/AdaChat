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
import com.example.andopsi.model.YouTubeVideosResponse
import retrofit2.Response

class HomeViewModel(private val videoRepository: VideoRepository) : ViewModel() {

    // create function getVideo which takes videoId as parameter and returns video details
    fun getVideo(videoId: String, onResult: (Result<Response<YouTubeVideosResponse>>) -> Unit) {
        viewModelScope.launch {
            try {
                val video = videoRepository.getVideo(videoId)
                onResult(Result.success(video))
            } catch (e: IOException) {
                onResult(Result.failure(e))
            } catch (e: HttpException) {
                onResult(Result.failure(e))
            }
        }
    }
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as VideoApplication)
                val videoRepository = application.container.videoRepository
                HomeViewModel(videoRepository = videoRepository)
            }
        }
    }
}