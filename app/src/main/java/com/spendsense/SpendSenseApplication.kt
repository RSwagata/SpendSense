package com.spendsense

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.spendsense.work.InsightWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * @HiltAndroidApp triggers Hilt's code generation.
 * Configuration.Provider lets Hilt inject dependencies into WorkManager workers.
 */
@HiltAndroidApp
class SpendSenseApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var workManager: androidx.work.WorkManager

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        InsightWorker.schedule(workManager)
    }
}
