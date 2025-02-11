package com.safehelmet.safehelmet_mobile.polling

import kotlinx.coroutines.*

class PollingScheduler {
    fun CoroutineScope.startInfiniteScheduler(interval: Long = 1000L, task: suspend () -> Unit): Job {
        return launch {
            while (isActive) {
                task()
                delay(interval)
            }
        }
    }
}