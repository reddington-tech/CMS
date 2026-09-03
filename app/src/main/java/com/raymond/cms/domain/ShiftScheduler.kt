package com.raymond.cms.domain

import android.content.Context
import androidx.work.*
import java.util.*
import java.util.concurrent.TimeUnit

object ShiftScheduler {
    private const val AUTO_CLOCK_OUT_TAG = "auto_clock_out"
    private const val REMINDER_TAG = "clock_out_reminder"

    fun scheduleTasks(context: Context) {
        val workManager = WorkManager.getInstance(context)

        // 1. Schedule Automatic Clock-Out at 11:59 PM
        scheduleDailyTask(workManager, 23, 59, "AUTO_CLOCK_OUT", AUTO_CLOCK_OUT_TAG)

        // 2. Schedule Reminders from 8:00 PM to 11:00 PM every 15 minutes
        for (hour in 20..23) {
            for (minute in listOf(0, 15, 30, 45)) {
                if (hour == 23 && minute > 0) break // Stop at 11:00 PM
                scheduleDailyTask(workManager, hour, minute, "REMINDER", "${REMINDER_TAG}_${hour}_$minute")
            }
        }
    }

    private fun scheduleDailyTask(workManager: WorkManager, hour: Int, minute: Int, type: String, tag: String) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val delay = calendar.timeInMillis - System.currentTimeMillis()

        val data = Data.Builder()
            .putString("type", type)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<ShiftWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(tag)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()

        workManager.enqueueUniqueWork(tag, ExistingWorkPolicy.REPLACE, workRequest)
    }
}
