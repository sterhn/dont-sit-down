package com.dontsitdown

import android.app.Application
import android.content.Context
import com.dontsitdown.chain.ChainGenerator
import com.dontsitdown.chain.ChainSession
import com.dontsitdown.chain.MessageGenerator
import com.dontsitdown.data.db.AppDatabase
import com.dontsitdown.data.model.Task
import com.dontsitdown.data.repository.TaskRepository
import com.dontsitdown.service.DailyWorker
import com.dontsitdown.service.WeeklySummaryWorker
import com.dontsitdown.util.NotificationHelper
import com.dontsitdown.util.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

open class DontSitDownApp : Application() {
    val db by lazy { AppDatabase.create(this) }
    val repository by lazy { TaskRepository(db) }
    val settings by lazy { Settings(this) }
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val session by lazy { ChainSession(this, repository, settings, scope) }

    override fun onCreate() {
        super.onCreate()
        MessageGenerator.init(settings)
        NotificationHelper.createChannels(this)
        DailyWorker.schedule(this)
        WeeklySummaryWorker.schedule(this)
    }

    suspend fun generateChain(exclude: Set<String> = emptySet(), shuffle: Boolean = false): List<Task> =
        ChainGenerator.generate(
            tasks = repository.activeTasks(),
            now = System.currentTimeMillis(),
            maxMinutes = settings.current.chainMaxMinutes,
            exclude = exclude,
            shuffle = shuffle,
        )
}

val Context.app: DontSitDownApp get() = applicationContext as DontSitDownApp
