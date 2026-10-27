package com.benz.shortcutlauncher

import android.app.Activity
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Bundle
import android.os.Process
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class HomeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(0xFF101218.toInt())
        }
        layout.addView(TextView(this).apply {
            text = "BENZ"
            textSize = 32f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
        })
        layout.addView(TextView(this).apply {
            text = "Мини-экран Benz нужен только для доступа к shortcuts Android. Иконка «Benz» запускает выбранный ярлык напрямую."
            textSize = 15f
            setTextColor(0xFFCDD2DF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 20)
        })
        layout.addView(Button(this).apply {
            text = "Запустить выбранный ярлык"
            isAllCaps = false
            setOnClickListener { launchSelected() }
        })
        layout.addView(Button(this).apply {
            text = "Benz Setup"
            isAllCaps = false
            setOnClickListener { startActivity(Intent(this@HomeActivity, SetupActivity::class.java)) }
        })
        layout.addView(Button(this).apply {
            text = "Открыть настройки главного экрана"
            isAllCaps = false
            setOnClickListener {
                try {
                    startActivity(Intent(android.provider.Settings.ACTION_HOME_SETTINGS))
                } catch (_: Exception) {
                    Toast.makeText(this@HomeActivity, "Измени приложение главного экрана в настройках Android.", Toast.LENGTH_LONG).show()
                }
            }
        })
        setContentView(layout)
    }

    private fun launchSelected() {
        val prefs = getSharedPreferences(SetupActivity.PREFS, MODE_PRIVATE)
        val pkg = prefs.getString(SetupActivity.KEY_PACKAGE, null)
        val id = prefs.getString(SetupActivity.KEY_SHORTCUT_ID, null)
        if (pkg.isNullOrBlank() || id.isNullOrBlank()) {
            startActivity(Intent(this, SetupActivity::class.java))
            return
        }
        val apps = getSystemService(LauncherApps::class.java)
        if (!apps.hasShortcutHostPermission()) {
            Toast.makeText(this, "Сначала выбери Benz как главный экран.", Toast.LENGTH_LONG).show()
            return
        }
        try {
            apps.startShortcut(pkg, id, null, null, Process.myUserHandle())
        } catch (_: Exception) {
            Toast.makeText(this, "Ярлык недоступен. Открой Benz Setup и выбери его заново.", Toast.LENGTH_LONG).show()
        }
    }
}
