package com.snoozecontrol.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.content.edit

object ManufacturerPermissionHelper {

    private const val PREFS_NAME = "snooze_control_oem_prefs"
    private const val KEY_AUTOSTART_DISMISSED = "autostart_prompt_dismissed"
    private const val KEY_AUTOSTART_DISMISSED_TIMESTAMP = "autostart_prompt_dismissed_timestamp"

    /**
     * Default TTL expiry duration for autostart dismissal prompt: 14 days.
     */
    const val DEFAULT_EXPIRY_MS = 3 * 24 * 60 * 60 * 1000L

    fun isOemDeviceRequiringAutostart(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return manufacturer.contains("xiaomi") ||
                manufacturer.contains("redmi") ||
                manufacturer.contains("poco") ||
                manufacturer.contains("oppo") ||
                manufacturer.contains("vivo") ||
                manufacturer.contains("realme") ||
                manufacturer.contains("huawei") ||
                manufacturer.contains("honor")
    }

    /**
     * Checks if the app is currently exempt from battery optimizations.
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return if (powerManager != null) {
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }
    }

    /**
     * Shows the direct system popup dialog asking the user to allow the app to run in the background without battery restrictions.
     */
    fun requestIgnoreBatteryOptimizations(context: Context) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e("ManufacturerHelper", "Failed to show battery optimization dialog", e)
                openAppInfoSettings(context)
            }
        }
    }

    /**
     * Checks if the user previously dismissed or configured the autostart prompt.
     * Automatically expires the dismissal after [expiryMillis] (default 14 days) so the prompt remains updated.
     */
    fun hasUserDismissedAutostartPrompt(
        context: Context,
        expiryMillis: Long = DEFAULT_EXPIRY_MS
    ): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isDismissed = prefs.getBoolean(KEY_AUTOSTART_DISMISSED, false)
        if (!isDismissed) return false

        val now = System.currentTimeMillis()
        val timestamp = prefs.getLong(KEY_AUTOSTART_DISMISSED_TIMESTAMP, 0L)

        // Handle legacy saved preference without timestamp
        if (timestamp == 0L) {
            prefs.edit { putLong(KEY_AUTOSTART_DISMISSED_TIMESTAMP, now) }
            return true
        }

        // Check if dismissal has expired
        val age = now - timestamp
        if (age > expiryMillis) {
            // Dismissal expired - clear state so prompt can show again if needed
            setAutostartPromptDismissed(context, false)
            return false
        }

        return true
    }

    /**
     * Saves the autostart prompt dismissal status along with the current timestamp.
     */
    fun setAutostartPromptDismissed(context: Context, dismissed: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        prefs.edit {
            putBoolean(KEY_AUTOSTART_DISMISSED, dismissed)
            if (dismissed) {
                putLong(KEY_AUTOSTART_DISMISSED_TIMESTAMP, now)
            } else {
                remove(KEY_AUTOSTART_DISMISSED_TIMESTAMP)
            }
        }
    }

    /**
     * Determines whether the Autostart Guidance Card should be shown to the user.
     * Returns false if non-OEM device, if user dismissed it (and not expired), or if battery optimizations are already ignored.
     */
    fun shouldShowAutostartGuidance(context: Context): Boolean {
        if (!isOemDeviceRequiringAutostart()) return false
        if (hasUserDismissedAutostartPrompt(context)) return false
        if (isIgnoringBatteryOptimizations(context)) return false
        return true
    }

    fun openAutostartSettings(context: Context) {
        // Mark as configured when user opens settings
        setAutostartPromptDismissed(context, true)

        val manufacturer = Build.MANUFACTURER.lowercase()
        val intent = Intent()

        try {
            when {
                manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains(
                    "poco"
                ) -> {
                    intent.component = ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                    )
                }

                manufacturer.contains("oppo") || manufacturer.contains("realme") -> {
                    intent.component = ComponentName(
                        "com.coloros.safecenter",
                        "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                    )
                }

                manufacturer.contains("vivo") -> {
                    intent.component = ComponentName(
                        "com.iqoo.secure",
                        "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
                    )
                }

                manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
                    intent.component = ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                    )
                }

                else -> {
                    openAppInfoSettings(context)
                    return
                }
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(
                "ManufacturerHelper",
                "Failed to launch OEM autostart activity, falling back to App Info settings",
                e
            )
            openAppInfoSettings(context)
        }
    }

    private fun openAppInfoSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("ManufacturerHelper", "Failed to open Application Details Settings", e)
        }
    }
}
