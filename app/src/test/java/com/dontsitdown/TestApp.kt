package com.dontsitdown

import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper

/** WorkManager has no auto-init under Robolectric; set up the test one before the app schedules work. */
class TestApp : DontSitDownApp() {
    override fun onCreate() {
        WorkManagerTestInitHelper.initializeTestWorkManager(this, Configuration.Builder().setExecutor(SynchronousExecutor()).build())
        super.onCreate()
    }
}
