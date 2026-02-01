package com.smartwalkie.voicepingdemo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Autostart foreground service after device reboot.
 *
 * IMPORTANT: Android vendors may restrict background/autostart behavior. Foreground service helps,
 * but on some devices the user still has to allow autostart / disable battery optimizations.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        val userId = MyPrefs.userId.orEmpty()
        val company = MyPrefs.company.orEmpty()
        val serverUrl = MyPrefs.serverUrl.orEmpty()
        if (userId.isBlank() || company.isBlank() || serverUrl.isBlank()) return

        VoicePingForegroundService.start(context)
    }
}
