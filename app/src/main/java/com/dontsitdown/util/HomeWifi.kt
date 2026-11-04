package com.dontsitdown.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager

/**
 * "Am I actually home?" by Wi-Fi. On Wi-Fi, and (if we're allowed to read its name) the right one.
 * Reading the name needs location permission; without it, any Wi-Fi counts as home.
 */
object HomeWifi {
    fun onWifi(context: Context): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    /** The current network's name, or null when it can't be read (no permission, location off, not on Wi-Fi). */
    @Suppress("DEPRECATION")
    fun currentSsid(context: Context): String? = runCatching {
        val wm = context.applicationContext.getSystemService(WifiManager::class.java) ?: return null
        val raw = wm.connectionInfo?.ssid ?: return null
        clean(raw)
    }.getOrNull()

    fun clean(raw: String): String? = raw.trim().removeSurrounding("\"").takeIf { it.isNotBlank() && it != "<unknown ssid>" && it != "0x" }

    fun atHome(context: Context, s: SettingsState): Boolean = decide(onWifi(context), currentSsid(context), s.homeSsid)

    /** The rule on its own, for tests: off Wi-Fi is never home; an unreadable name gives you the benefit of the doubt. */
    fun decide(onWifi: Boolean, ssid: String?, home: String?): Boolean = when {
        !onWifi -> false
        home.isNullOrBlank() || ssid == null -> true
        else -> ssid == home
    }
}
