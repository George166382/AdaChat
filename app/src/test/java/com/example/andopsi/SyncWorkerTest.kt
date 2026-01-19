package com.example.andopsi.workers

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.andopsi.data.AppDatabase
import com.example.andopsi.model.User
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.hamcrest.CoreMatchers.`is`
import kotlinx.datetime.Clock

@RunWith(AndroidJUnit4::class)
class SyncWorkerTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Initialize an in-memory database for isolation
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @Test
    fun testSyncWorker_sendsData() {
        // 1. SETUP: Seed the local DB with a "fresh" user
        val testUser = User(
            username = "TestUser_${System.currentTimeMillis()}",
            email = "test@example.com",
            displayName = "Automated Test User",
            passwordHash = "dummyhash",
            updatedAt = Clock.System.now(), // Timestamp ensures inclusion
            avatarUrl = "https://example.com/avatar.png"
        )

        runBlocking { db.userDao().insertUser(testUser) }

        // 2. Build Worker
        val worker = TestListenableWorkerBuilder<SyncWorker>(context).build()

        // 3. CRITICAL: Inject the InMemory DB into the Worker
        worker.testDatabase = db

        runBlocking {
            val result = worker.doWork()

            // 3. VERIFY: Assert the worker completed successfully
            assertThat(result, `is`(ListenableWorker.Result.success()))
        }
    }



}