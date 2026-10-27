package com.benz.shortcutlauncher

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.widget.Toast
import android.content.pm.LauncherApps

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences(SetupActivity.PREFS, MODE_PRIVATE)
        val targetPackage = prefs.getString(SetupActivity.KEY_PACKAGE, null)
        val shortcutId = prefs.getString(SetupActivity.KEY_SHORTCUT_ID, null)
        if (targetPackage.isNullOrBlank() || shortcutId.isNullOrBlank()) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }
        val launcherApps = getSystemService(LauncherApps::class.java)
        if (!launcherApps.hasShortcutHostPermission()) {
            Toast.makeText(this, "Для запуска ярлыков GameHub открой Benz Setup и предоставь доступ лаунчера.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }
        try {
            launcherApps.startShortcut(targetPackage, shortcutId, null, null, Process.myUserHandle())
        } catch (_: Exception) {
            Toast.makeText(this, "Ярлык недоступен. Выбери его заново в Benz Setup.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, SetupActivity::class.java))
        }
        finish()
    }
}
