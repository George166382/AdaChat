package org.example.upsy.ui.home

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.andopsi.ui.home.HomeViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer

//import org.example.upsy.ui.components.ChatbotSelection
import org.example.upsy.ui.components.UrlInputField

import org.json.JSONObject

@Composable
fun HomeScreen() {


    var textUrl by remember { mutableStateOf("") }
    var videoEmbedding by remember { mutableStateOf<JSONObject?>(null) }
    var triggerFetch by remember { mutableStateOf(false) }

    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)

    //val uiState by vm.state.collectAsState()

    // Handle the fetch operation when savedRequest is available
    /*LaunchedEffect(uiState.savedRequest) {
        val request = uiState.savedRequest
        if (request != null) {
            Log.i("OPSI","Request sent at: ${request.sentAt}")
            Log.i("OPSI","Req id: ${uiState.requestID}")

            try {
                val embedding = fetchYouTubeEmbedding(textUrl, vm, uiState)
                videoEmbedding = embedding

                // Mark operation as complete
                vm.completeFetchOperation()
            } catch (e: Exception) {
                println("Error fetching embedding: ${e.message}")
                vm.setError("Failed to fetch video: ${e.message}")
                vm.completeFetchOperation()
            }
        }
    }*/

    // Handle response updates
    /*LaunchedEffect(uiState.savedResponse) {
        val response = uiState.savedResponse
        if (response != null) {
            Log.i("OPSI","Response delivered at: ${response.deliveredAt}")
            vm.resetSavedResponse()
        }
    }*/

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primaryContainer)
            .safeContentPadding()
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        UrlInputField(
            value = textUrl,
            onValueChange = { textUrl = it }
        )


        Button(
            onClick = {
                if (textUrl.isNotBlank()) {

                    triggerFetch = true
                }
            }
        ) {
            Text("Fetch Info")
        }
        if (triggerFetch) {
            LaunchedEffect(textUrl) {
                val videoId = when {
                    "youtu.be/" in textUrl -> textUrl.substringAfterLast("/").substringBefore("?")
                    "youtube.com/shorts/" in textUrl -> textUrl.substringAfterLast("/").substringBefore("?")
                    "v=" in textUrl -> textUrl.substringAfter("v=").substringBefore("&").substringBefore("?")
                    else -> ""
                }
                homeViewModel.getVideo(videoId) { result ->
                    result.onSuccess { response ->
                        if (response.isSuccessful) {
                            val responseBody = response.body()
                            if (responseBody != null && responseBody.items.isNotEmpty()) {
                                val videoItem = responseBody.items[0]
                                val embeddingJson = JSONObject().apply {
                                    put("title", videoItem.snippet.title)
                                    put("url", "https://www.youtube.com/watch?v=${videoItem.id}")
                                    put("thumbnail_url", videoItem.snippet.thumbnails.high.url)
                                }
                                videoEmbedding = embeddingJson
                            } else {
                                // Handle case where video is not found
                                Log.i("OPSI","Video not found")
                            }
                        } else {
                            // Handle unsuccessful response
                            Log.i("OPSI","Error: ${response.code()} - ${response.message()}")
                        }
                    }.onFailure { exception ->
                        // Handle failure
                        Log.e("OPSI","Failed to fetch video: ${exception.message}")
                    }
                }
                triggerFetch = false
            }
        }

        // Show loading state
        /*if (uiState.isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
            Text("Processing request...")
        }*/

        /*// Show error if any
        uiState.error?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Error: $error",
                color = MaterialTheme.colorScheme.error
            )
        }

        // Show request info
        uiState.savedRequest?.let { request ->
            Spacer(modifier = Modifier.height(16.dp))
            Text("Request sent at: ${request.sentAt}")
        }

        // Show response info
        uiState.savedResponse?.let { response ->
            Spacer(modifier = Modifier.height(8.dp))
            Text("Response delivered at: ${response.deliveredAt}")
        }*/

        // Display video embedding result
        videoEmbedding?.let { embedding ->
            Spacer(modifier = Modifier.height(16.dp))

            // Save the video to database
            LaunchedEffect(embedding) {
                //vm.saveFromEmbedding(embedding)
            }

            val thumbnail = embedding.optString("thumbnail_url", "")
            val title = embedding.optString("title", "No Title")

            /*if (thumbnail.isNotEmpty()) {
                AsyncImage(
                    model = thumbnail,
                    contentDescription = "Video Thumbnail",
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(200.dp)
                )
            }*/

            Spacer(modifier = Modifier.height(8.dp))
            Text("Video: $title")
            val videoUrl = embedding.optString("url", "")
            val videoId = when {
                "youtu.be/" in videoUrl -> videoUrl.substringAfterLast("/").substringBefore("?")
                "youtube.com/shorts/" in videoUrl -> videoUrl.substringAfterLast("/").substringBefore("?")
                "v=" in videoUrl -> videoUrl.substringAfter("v=").substringBefore("&").substringBefore("?")
                else -> ""
            }
            if (videoId.isNotEmpty()) {
                YoutubeScreen(
                    videoId = videoId,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(200.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        //ChatbotSelection()
    }
}