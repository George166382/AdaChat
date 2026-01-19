package com.example.andopsi.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.andopsi.data.AppDatabase
import com.example.andopsi.data.PostgresDataSource
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    // Dependency Injection for Testability
    var testDatabase: AppDatabase? = null

    companion object {
        private const val PREFS_NAME = "sync_prefs"
        private const val KEY_LAST_SYNC = "last_sync_timestamp"
    }

    override suspend fun doWork(): Result {
        val db = testDatabase ?: AppDatabase.getDatabase(applicationContext)
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // 1. Get the "Watermark" (Last successful sync time)
        // Default to 30 days ago if this is the first ever run
        val lastSyncEpoch = prefs.getLong(KEY_LAST_SYNC,
            Clock.System.now().minus(30.days).toEpochMilliseconds()
        )
        val lastSyncTime = Instant.fromEpochMilliseconds(lastSyncEpoch)

        // 2. Fetch users updated AFTER the last success
        val recentUsers = db.userDao().getRecentUpdates(lastSyncTime)
        println("SyncWorker: Checking changes since $lastSyncTime. Found: ${recentUsers.size}")

        if (recentUsers.isNotEmpty()) {
            val remoteDataSource = PostgresDataSource()
            val result = remoteDataSource.syncUsers(recentUsers)

            if (result.success) {
                // 3. SUCCESS: Save the NEW "Watermark" (Current Time)
                // We save "Now" so next time we only fetch changes that happen after this moment.
                prefs.edit().putLong(KEY_LAST_SYNC, Clock.System.now().toEpochMilliseconds()).apply()
                return Result.success()
            } else {
                // 4. FAILURE: Do NOT save the timestamp.
                // We return retry(). WorkManager will try again later.
                // Because we didn't save, the next attempt will use the OLD 'lastSyncTime'
                // and fetch these users again.
                result.error?.printStackTrace()
                return Result.retry()
            }
        }

        // No data to sync, but we should still update the timestamp to now
        // to keep the window short for next time.
        prefs.edit().putLong(KEY_LAST_SYNC, Clock.System.now().toEpochMilliseconds()).apply()
        return Result.success()
    }
}