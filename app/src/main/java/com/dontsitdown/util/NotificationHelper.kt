package com.dontsitdown.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dontsitdown.MainActivity
import com.dontsitdown.R
import com.dontsitdown.service.FullScreenReminderActivity
import com.dontsitdown.service.ReminderReceiver

object NotificationHelper {
    const val CH_TAKEOVER = "takeover"
    const val CH_TIMER = "timer"
    const val CH_PROGRESS = "progress"
    const val CH_SUMMARY = "weekly"
    const val CH_NUDGE = "nudge"

    const val ID_TAKEOVER = 1
    const val ID_TIMER = 2
    const val ID_PROGRESS = 3
    const val ID_SUMMARY = 4
    const val ID_NUDGE = 5

    private const val AMBER = 0xFFD4920A.toInt()

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val chime = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.chime}")
        val takeover = NotificationChannel(CH_TAKEOVER, "the takeover", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "full-screen reminder when you get home"
            setSound(chime, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 180, 120, 180)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val timer = NotificationChannel(CH_TIMER, "timer", NotificationManager.IMPORTANCE_LOW).apply {
            description = "running task timer"
            setShowBadge(false)
        }
        val progress = NotificationChannel(CH_PROGRESS, "task done", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "\"dishes: done. 6 min.\" when the app isn't open"
        }
        val summary = NotificationChannel(CH_SUMMARY, "weekly summary", NotificationManager.IMPORTANCE_DEFAULT)
        val nudge = NotificationChannel(CH_NUDGE, "one honest nudge", NotificationManager.IMPORTANCE_DEFAULT)
        nm.createNotificationChannels(listOf(takeover, timer, progress, summary, nudge))
    }

    fun canPost(context: Context): Boolean =
        (Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    @SuppressLint("MissingPermission") // checked by canPost
    fun notify(context: Context, id: Int, n: Notification) {
        if (canPost(context)) NotificationManagerCompat.from(context).notify(id, n)
    }

    fun cancel(context: Context, id: Int) = NotificationManagerCompat.from(context).cancel(id)

    fun openAppIntent(context: Context, target: String = MainActivity.OPEN_CHAIN, requestCode: Int = 0): PendingIntent =
        PendingIntent.getActivity(
            context, requestCode,
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN, target)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun broadcast(context: Context, action: String, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context, requestCode,
            Intent(context, ReminderReceiver::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    fun showTakeover(context: Context, taskNames: List<String>, minutes: Float, snoozeAllowed: Boolean) {
        val fullScreen = PendingIntent.getActivity(
            context, 10,
            Intent(context, FullScreenReminderActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val start = PendingIntent.getActivity(
            context, 11,
            Intent(context, FullScreenReminderActivity::class.java)
                .putExtra(FullScreenReminderActivity.EXTRA_START_NOW, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val b = NotificationCompat.Builder(context, CH_TAKEOVER)
            .setSmallIcon(R.drawable.ic_stat_chair)
            .setColor(AMBER)
            .setContentTitle("don't sit down.")
            .setContentText(com.dontsitdown.chain.MessageGenerator.notificationText(taskNames, minutes))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .setDeleteIntent(broadcast(context, ReminderReceiver.ACTION_DISMISSED, 12))
            .setAutoCancel(true)
            .addAction(0, "start", start)
        if (snoozeAllowed) b.addAction(0, "snooze 10 min", broadcast(context, ReminderReceiver.ACTION_SNOOZE, 13))
        notify(context, ID_TAKEOVER, b.build())
    }

    fun simple(context: Context, channel: String, id: Int, title: String, text: String, target: String = MainActivity.OPEN_CHAIN) {
        val n = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_chair)
            .setColor(AMBER)
            .setContentTitle(title)
            .setContentText(text.lineSequence().firstOrNull { it.isNotBlank() } ?: text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openAppIntent(context, target, id))
            .setAutoCancel(true)
            .build()
        notify(context, id, n)
    }
}
