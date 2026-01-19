// kotlin
package com.example.andopsi.data

import android.content.Context
import com.example.andopsi.network.YouTubeApiService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import retrofit2.Retrofit
import okhttp3.MediaType.Companion.toMediaType
import com.example.andopsi.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer

interface AppContainer {
    val videoRepository: VideoRepository
    val userRepository: UserRepository
    val usermetaRepository: UsermetaRepository
    val audioExtractor: AudioExtractor // Add this
}


class DefaultAppContainer(private val context: Context) : AppContainer {
    private val baseUrl = "https://www.googleapis.com/youtube/v3/"
    //private val transcriptBaseUrl = "https://56272df70a30.ngrok-free.app"

    // Configure Json to ignore unknown keys in responses
    private val json = Json { ignoreUnknownKeys = true }


    private val retrofit1: Retrofit = Retrofit.Builder()
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .baseUrl(baseUrl)
        .build()
    /*private val retrofit2: Retrofit = Retrofit.Builder()
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .baseUrl(transcriptBaseUrl)
        .build()*/

    private val retrofitService: YouTubeApiService by lazy {
        retrofit1.create(YouTubeApiService::class.java)
    }
    // ... inside DefaultAppContainer class ...

    private val database by lazy {
        AppDatabase.getDatabase(context)
    }


    private val supabase = createSupabaseClient(
        supabaseUrl = "https://luabaqioqdhasfmqfudb.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imx1YWJhcWlvcWRoYXNmbXFmdWRiIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc2NTM2OTI0NywiZXhwIjoyMDgwOTQ1MjQ3fQ.YwJpF7h6GOzmdW8sH7R2kHtjTgOI3BCN4fKZUG61NBo" // Found in Supabase Dashboard -> Settings -> API
    ) {
        install(Postgrest) // Enable Database module

        // Configure JSON logic (ignore unknown keys from server)
        defaultSerializer = KotlinXSerializer(Json {
            ignoreUnknownKeys = true
        })
    }
    override val userRepository: UserRepository by lazy {
        UserRepositoryImplementation(database.userDao(), supabase)
    }
    override val usermetaRepository: UsermetaRepository by lazy {
        OfflineUsermetaRepository(database.usermetaDao(), supabase)
    }
    //... Database setup ...

    override val audioExtractor: AudioExtractor by lazy {
        AndroidAudioExtractor()
    }
    /*private val transcriptRetrofitService: com.example.andopsi.network.TranscriptApiService by lazy {
        retrofit2.create(com.example.andopsi.network.TranscriptApiService::class.java)
    }*/

    override val videoRepository: VideoRepository by lazy {
        NetworkVideoRepository(retrofitService, BuildConfig.YOUTUBE_API_KEY)
    }
}