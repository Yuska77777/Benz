package com.benz.shortcutlauncher

import android.app.Activity
import android.app.AlertDialog
import android.app.role.RoleManager
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.graphics.Typeface
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
    private lateinit var launcherApps: LauncherApps
    private val shortcutItems = mutableListOf<ShortcutInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launcherApps = getSystemService(LauncherApps::class.java)
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
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val title = TextView(this).apply {
            text = "BENZ"
            textSize = 30f
            setTextColor(0xFFFFFFFF.toInt())
            typeface = Typeface.DEFAULT_BOLD
        }
        val subtitle = TextView(this).apply {
            text = "Выбери ярлык GameHub. После настройки нажатие на Benz будет запускать выбранный ярлык напрямую."
            textSize = 16f
            setTextColor(0xFFCDD2DF.toInt())
            setPadding(0, dp(10), 0, dp(20))
        }
        content.addView(title)
        content.addView(subtitle)
        content.addView(actionButton("1. Разрешить доступ к ярлыкам") { requestHomeRole() })
        content.addView(actionButton("2. Найти ярлыки GameHub") { chooseProvider() })
        content.addView(actionButton("3. Проверить выбранный ярлык") { showCurrentSelection() })
        val note = TextView(this).apply {
            text = "\nВажно: Android обычно разрешает читать и запускать shortcuts других приложений только текущему главному экрану. Поэтому для настоящих ярлыков GameHub может потребоваться выбрать Benz как приложение главного экрана."
            textSize = 13f
            setTextColor(0xFF9BA3B7.toInt())
            setPadding(0, dp(18), 0, dp(12))
        }
        content.addView(note)
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
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val selected = prefs.getString(KEY_SHORTCUT_LABEL, null)
        status.text = (if (launcherApps.hasShortcutHostPermission()) "Доступ к ярлыкам: есть" else "Доступ к ярлыкам: пока нет") +
            "\n" + (if (selected.isNullOrBlank()) "Ярлык ещё не выбран" else "Выбран: " + selected)
    }

    private fun requestHomeRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager.isRoleAvailable(RoleManager.ROLE_HOME) && !roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                startActivityForResult(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME), 4101)
            } else {
                Toast.makeText(this, "Benz уже выбран главным экраном либо роль недоступна.", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "Открой Настройки → Приложения по умолчанию → Главный экран и выбери Benz.", Toast.LENGTH_LONG).show()
        }
    }

    private fun chooseProvider() {
        if (!launcherApps.hasShortcutHostPermission()) {
            AlertDialog.Builder(this).setTitle("Нужен доступ лаунчера")
                .setMessage("Сначала выбери Benz как приложение главного экрана. После этого Android позволит перечислять shortcuts других приложений.")
                .setPositiveButton("Понятно", null).show()
            return
        }
        val providers = launcherApps.getActivityList(null, Process.myUserHandle())
            .groupBy { it.applicationInfo.packageName }
            .map { entry ->
                Provider(entry.key, entry.value.firstOrNull()?.label?.toString() ?: entry.key)
            }.sortedBy { it.label.lowercase() }
        val labels = providers.map { it.label + " (" + it.packageName + ")" }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Выбери приложение, создавшее ярлык")
            .setItems(labels) { _, which -> loadShortcuts(providers[which].packageName, providers[which].label) }
            .setNegativeButton("Отмена", null).show()
    }

    private fun loadShortcuts(packageName: String, providerLabel: String) {
        try {
            val query = LauncherApps.ShortcutQuery()
                .setPackage(packageName)
                .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
            val found = launcherApps.getShortcuts(query, Process.myUserHandle()).orEmpty()
            shortcutItems.clear()
            shortcutItems.addAll(found.distinctBy { it.id })
            if (shortcutItems.isEmpty()) {
                AlertDialog.Builder(this).setTitle("Ярлыки не найдены")
                    .setMessage("Android не показывает shortcuts для «" + providerLabel + "». Возможно, GameHub создаёт иконку не через ShortcutManager или этот ярлык не раскрывается через публичный API.")
                    .setPositiveButton("OK", null).show()
                return
            }
            val labels = shortcutItems.map {
                (it.shortLabel?.toString() ?: it.id) + "\nID: " + it.id
            }.toTypedArray()
            AlertDialog.Builder(this).setTitle("Выбери ярлык из " + providerLabel)
                .setItems(labels) { _, which -> saveShortcut(packageName, shortcutItems[which]) }
                .setNegativeButton("Отмена", null).show()
        } catch (_: SecurityException) {
            Toast.makeText(this, "Android не разрешил читать ярлыки. Проверь, что Benz выбран главным экраном.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Не удалось получить ярлыки: " + e.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun saveShortcut(packageName: String, shortcut: ShortcutInfo) {
        val label = shortcut.shortLabel?.toString() ?: shortcut.id
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
            .putString(KEY_PACKAGE, packageName)
            .putString(KEY_SHORTCUT_ID, shortcut.id)
            .putString(KEY_SHORTCUT_LABEL, label)
            .apply()
        refreshStatus()
        AlertDialog.Builder(this).setTitle("Ярлык сохранён")
            .setMessage("Теперь нажимай на иконку Benz, чтобы запустить «" + label + "». Для смены выбора открой Benz Setup.")
            .setPositiveButton("Готово", null).show()
    }

    private fun showCurrentSelection() {
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val pkg = prefs.getString(KEY_PACKAGE, null)
        val id = prefs.getString(KEY_SHORTCUT_ID, null)
        val label = prefs.getString(KEY_SHORTCUT_LABEL, null)
        val message = if (pkg == null || id == null) "Сначала выбери ярлык." else
            "Название: " + label + "\nПриложение-источник: " + pkg + "\nShortcut ID: " + id +
                "\n\nДоступ к shortcuts: " + launcherApps.hasShortcutHostPermission()
        AlertDialog.Builder(this).setTitle("Текущий выбор").setMessage(message).setPositiveButton("OK", null).show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private data class Provider(val packageName: String, val label: String)
}
