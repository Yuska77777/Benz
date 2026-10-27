package com.benz.shortcutlauncher

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class SetupActivity : Activity() {
    companion object {
        const val PREFS = "benz_settings"
        const val KEY_PACKAGE = "target_package"
        const val KEY_SHORTCUT_ID = "target_shortcut_id"
        const val KEY_SHORTCUT_LABEL = "target_shortcut_label"
    }

    private lateinit var root: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
    }

    override fun onResume() {
        super.onResume()
        if (::root.isInitialized) refreshStatus()
    }

    private fun buildScreen() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(28), dp(24), dp(24))
            setBackgroundColor(0xFF101218.toInt())
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(TextView(this).apply {
            text = "BENZ"
            textSize = 30f
            setTextColor(0xFFFFFFFF.toInt())
            typeface = Typeface.DEFAULT_BOLD
        })
        content.addView(TextView(this).apply {
            text = "Benz должен запускать конкретный ярлык GameHub, а не просто само приложение."
            textSize = 16f
            setTextColor(0xFFCDD2DF.toInt())
            setPadding(0, dp(10), 0, dp(16))
        })
        content.addView(TextView(this).apply {
            text = "Почему поиск не сработал\n\nGameHub создаёт ярлык на рабочем столе, но Android не предоставляет обычным приложениям общий список ярлыков, созданных другими приложениями. Предыдущая версия просила сделать Benz главным экраном — это было неудобно и всё равно не гарантировало доступ к ярлыку. Мы убрали это требование.\n\nСейчас Benz не может выбрать существующий ярлык GameHub через публичный API. Чтобы запускать именно Bendy, GameHub должен предоставить внешний способ запуска игры или нужно получить данные самого ярлыка."
            textSize = 15f
            setTextColor(0xFFCDD2DF.toInt())
            setPadding(0, 0, 0, dp(20))
        })
        content.addView(actionButton("Открыть GameHub") { openGameHub() })
        content.addView(actionButton("Проверить поддержку ярлыков Android") { checkShortcutAccess() })
        content.addView(actionButton("Как вернуть прежний главный экран") { openHomeSettings() })
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(TextView(this).apply {
            tag = "status"
            textSize = 13f
            setTextColor(0xFF77D9A3.toInt())
            setPadding(0, dp(12), 0, dp(8))
        })
        setContentView(root)
        refreshStatus()
    }

    private fun actionButton(label: String, onClick: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 15f
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(10)
            }
        }

    private fun refreshStatus() {
        val status = root.findViewWithTag<TextView>("status") ?: return
        val selected = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_SHORTCUT_LABEL, null)
        status.text = if (selected.isNullOrBlank()) "Сохранённый ярлык: не выбран" else "Сохранённый ярлык: $selected"
    }

    private fun openGameHub() {
        val pm = packageManager
        val candidates = listOf("com.xiaoji.egggame", "gamehub.lite")
        val intent = candidates.firstNotNullOfOrNull { pkg ->
            pm.getLaunchIntentForPackage(pkg)
        }
        if (intent != null) {
            startActivity(intent)
            return
        }
        Toast.makeText(this, "Не удалось определить пакет GameHub. Открой его из списка приложений.", Toast.LENGTH_LONG).show()
    }

    private fun checkShortcutAccess() {
        val launcherApps = getSystemService(LauncherApps::class.java)
        val message = "Android API доступен, но список shortcuts других приложений требует разрешения приложения главного экрана. Даже с этим разрешением GameHub может не публиковать свой ярлык через ShortcutManager. Benz больше не будет просить менять главный экран автоматически."
        AlertDialog.Builder(this).setTitle("Ограничение Android").setMessage(message).setPositiveButton("OK", null).show()
    }

    private fun openHomeSettings() {
        try {
            startActivity(Intent(android.provider.Settings.ACTION_HOME_SETTINGS))
        } catch (_: Exception) {
            Toast.makeText(this, "Открой настройки Android → Приложения по умолчанию → Главное приложение.", Toast.LENGTH_LONG).show()
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
