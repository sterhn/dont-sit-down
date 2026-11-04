package com.dontsitdown

import android.app.Application
import android.content.Context
import com.dontsitdown.chain.ChainGenerator
import com.dontsitdown.checkin.Checkin
import com.dontsitdown.checkin.CheckinStore
import com.dontsitdown.checkin.Energy
import com.dontsitdown.util.TimeUtils
import com.dontsitdown.chain.ChainSession
import com.dontsitdown.chain.MessageGenerator
import com.dontsitdown.data.db.AppDatabase
import com.dontsitdown.data.model.Task
import com.dontsitdown.data.repository.TaskRepository
import com.dontsitdown.service.CheckinReminders
import com.dontsitdown.service.DailyWorker
import com.dontsitdown.service.WeeklySummaryWorker
import com.dontsitdown.util.NotificationHelper
import com.dontsitdown.util.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

open class DontSitDownApp : Application() {
    val db by lazy { AppDatabase.create(this) }
    val repository by lazy { TaskRepository(db, onCompletion = { com.dontsitdown.widget.CheckinWidget.refresh(this) }) }
    val settings by lazy { Settings(this) }
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val session by lazy { ChainSession(this, repository, settings, scope, spiteNow = ::spiteToday, energy = ::energyToday) }
    val checkin by lazy { CheckinStore(this, scope) }

    override fun onCreate() {
        super.onCreate()
        MessageGenerator.init(settings)
        NotificationHelper.createChannels(this)
        DailyWorker.schedule(this)
        WeeklySummaryWorker.schedule(this)
        CheckinReminders.scheduleAll(this)
    }

    /** What this morning's check-in says. The chain is smaller on rough days. */
    fun energyToday(): Energy = Checkin.energy(checkin.current, TimeUtils.today())

    /** The spite voice, unless today's a rough day: harsh, not cruel. */
    fun spiteToday(): Boolean = settings.current.spite && energyToday() != Energy.ROUGH

    suspend fun generateChain(exclude: Set<String> = emptySet(), shuffle: Boolean = false): List<Task> =
        ChainGenerator.generate(
            tasks = repository.activeTasks(),
            now = System.currentTimeMillis(),
            maxMinutes = Checkin.chainCap(energyToday(), settings.current.chainMaxMinutes),
            exclude = exclude,
            shuffle = shuffle,
        )
}

val Context.app: DontSitDownApp get() = applicationContext as DontSitDownApp
