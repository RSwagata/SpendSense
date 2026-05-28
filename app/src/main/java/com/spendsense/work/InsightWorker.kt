package com.spendsense.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.spendsense.agents.InsightAgent
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that fires every Sunday at 8am (approximately).
 * WorkManager respects battery-saving modes and Doze — it will run as soon
 * as the device is active near the scheduled time.
 */
@HiltWorker
class InsightWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val insightAgent: InsightAgent
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            insightAgent.generateAndNotify()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "weekly_insight"

        fun schedule(workManager: WorkManager) {
            val request = PeriodicWorkRequestBuilder<InsightWorker>(7, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
