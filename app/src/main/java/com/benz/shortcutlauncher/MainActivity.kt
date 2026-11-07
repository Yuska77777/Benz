package com.benz.shortcutlauncher

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {
    private data class Media(val uri: String, val name: String, val kind: String, val ratio: String)
    private val items = mutableListOf<Media>()
    private var tab = "wide"
    private var index = 0
    private lateinit var stage: FrameLayout
    private lateinit var counter: TextView
    private lateinit var search: EditText
    private lateinit var detector: GestureDetector
    private var filtered = listOf<Media>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(12, 12, 14)
        window.navigationBarColor = Color.rgb(12, 12, 14)
        loadItems()
        buildUi()
        refresh()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(12, 12, 14))
            setPadding(dp(14), dp(8), dp(14), dp(8))
        }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val brand = TextView(this).apply {
            text = "benz"; textSize = 27f; setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        header.addView(brand, LinearLayout.LayoutParams(0, dp(48), 1f))
        header.addView(Button(this).apply {
            text = "+ Добавить"; isAllCaps = false
            setOnClickListener { pickMedia() }
        })
        root.addView(header)

        search = EditText(this).apply {
            hint = "Поиск по названию"
            setSingleLine(true); textSize = 15f
            setTextColor(Color.WHITE); setHintTextColor(Color.GRAY)
            setPadding(dp(14), 0, dp(14), 0)
            setBackgroundColor(Color.rgb(35, 35, 39))
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { refresh() }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })
        }
        root.addView(search, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(6) })

        stage = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        root.addView(stage, LinearLayout.LayoutParams(-1, 0, 1f).apply {
            topMargin = dp(12); bottomMargin = dp(8)
        })

        counter = TextView(this).apply {
            setTextColor(Color.LTGRAY); textSize = 12f; gravity = Gravity.CENTER
            setPadding(0, dp(2), 0, dp(8))
        }
        root.addView(counter)
        val tabs = LinearLayout(this).apply { gravity = Gravity.CENTER }
        tabs.addView(tabButton("16:9 видео", "wide"), LinearLayout.LayoutParams(0, dp(50), 1f))
        tabs.addView(tabButton("9:16 видео", "vertical"), LinearLayout.LayoutParams(0, dp(50), 1f))
        tabs.addView(tabButton("Фото", "photos"), LinearLayout.LayoutParams(0, dp(50), 1f))
        root.addView(tabs)
        setContentView(root)

        detector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent) = true
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null || kotlin.math.abs(e1.y - e2.y) < dp(45)) return false
                move(if (e1.y - e2.y > 0) 1 else -1)
                return true
            }
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                val current = filtered.getOrNull(index) ?: return false
                val view = stage.getChildAt(0)
                if (current.kind == "video" && view is VideoView) {
                    if (view.isPlaying) view.pause() else view.start()
                }
                return true
            }
        })
        stage.setOnTouchListener { _, event -> detector.onTouchEvent(event); true }
    }

    private fun tabButton(label: String, value: String) = Button(this).apply {
        text = label; textSize = 12f; isAllCaps = false
        setOnClickListener { tab = value; index = 0; refresh() }
    }

    private fun pickMedia() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("video/*", "image/*"))
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(intent, 41)
    }

    @Deprecated("Deprecated API retained for compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 41 || resultCode != RESULT_OK || data == null) return
        val picked = mutableListOf<Uri>()
        data.data?.let { picked.add(it) }
        data.clipData?.let { clip -> for (i in 0 until clip.itemCount) picked.add(clip.getItemAt(i).uri) }
        picked.distinct().forEach { uri ->
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val mime = contentResolver.getType(uri) ?: ""
                val kind = when {
                    mime.startsWith("image/") -> "photo"
                    mime.startsWith("video/") -> "video"
                    else -> return@forEach
                }
                val name = queryName(uri)
                val ratio = if (kind == "photo") "photo" else getRatio(uri)
                if (items.none { it.uri == uri.toString() }) items.add(Media(uri.toString(), name, kind, ratio))
            } catch (_: Exception) { }
        }
        saveItems()
        refresh()
    }

    private fun queryName(uri: Uri): String {
        var result = uri.lastPathSegment ?: "Без названия"
        val cursor: Cursor? = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        cursor?.use { if (it.moveToFirst()) result = it.getString(0) ?: result }
        return result
    }

    private fun getRatio(uri: Uri): String {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(this, uri)
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toFloatOrNull() ?: 1f
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toFloatOrNull() ?: 1f
            if (width / height >= 1f) "wide" else "vertical"
        } catch (_: Exception) { "wide" }
        finally { try { retriever.release() } catch (_: Exception) { } }
    }

    private fun currentItems(): List<Media> {
        val query = search.text?.toString()?.trim()?.lowercase() ?: ""
        return items.filter { item ->
            val inTab = when (tab) {
                "photos" -> item.kind == "photo"
                "vertical" -> item.kind == "video" && item.ratio == "vertical"
                else -> item.kind == "video" && item.ratio == "wide"
            }
            inTab && item.name.lowercase().contains(query)
        }
    }

    private fun refresh() {
        if (!::stage.isInitialized) return
        filtered = currentItems()
        if (index >= filtered.size) index = (filtered.size - 1).coerceAtLeast(0)
        renderCurrent()
        counter.text = if (filtered.isEmpty()) "0 материалов" else "${index + 1} / ${filtered.size}"
    }

    private fun renderCurrent() {
        stage.removeAllViews()
        val item = filtered.getOrNull(index)
        if (item == null) {
            val empty = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
            empty.addView(TextView(this).apply {
                text = "▣"; textSize = 54f; setTextColor(Color.DKGRAY); gravity = Gravity.CENTER
            })
            empty.addView(TextView(this).apply {
                text = if (items.isEmpty()) "Твоя личная медиатека\n\nНажми «+ Добавить» и выбери фото или видео из галереи."
                else "Здесь пока пусто\nДобавь файлы или выбери другую вкладку."
                setTextColor(Color.LTGRAY); gravity = Gravity.CENTER; textSize = 15f
            })
            stage.addView(empty, FrameLayout.LayoutParams(-1, -1))
            return
        }
        val uri = Uri.parse(item.uri)
        if (item.kind == "photo") {
            val image = ImageView(this).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                setImageURI(uri)
            }
            stage.addView(image, FrameLayout.LayoutParams(-1, -1))
            image.setOnTouchListener { _, event -> detector.onTouchEvent(event); true }
        } else {
            val video = VideoView(this)
            stage.addView(video, FrameLayout.LayoutParams(-1, -1))
            video.setOnTouchListener { _, event -> detector.onTouchEvent(event); true }
            video.setVideoURI(uri)
            video.setOnPreparedListener { player -> player.isLooping = true; video.start() }
            video.setOnErrorListener { _, _, _ ->
                Toast.makeText(this, "Не удалось открыть это видео", Toast.LENGTH_SHORT).show()
                true
            }
        }
        val caption = TextView(this).apply {
            text = item.name; textSize = 14f; setTextColor(Color.WHITE)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setBackgroundColor(0x99000000.toInt()); maxLines = 2
        }
        stage.addView(caption, FrameLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))
    }

    private fun move(delta: Int) {
        if (filtered.isEmpty()) return
        index = (index + delta).coerceIn(0, filtered.lastIndex)
        renderCurrent()
        counter.text = "${index + 1} / ${filtered.size}"
    }

    private fun saveItems() {
        val array = JSONArray()
        items.forEach { array.put(JSONObject().put("uri", it.uri).put("name", it.name).put("kind", it.kind).put("ratio", it.ratio)) }
        getSharedPreferences("benz_media", MODE_PRIVATE).edit().putString("items", array.toString()).apply()
    }

    private fun loadItems() {
        val raw = getSharedPreferences("benz_media", MODE_PRIVATE).getString("items", "[]") ?: "[]"
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                items.add(Media(item.getString("uri"), item.getString("name"), item.getString("kind"), item.getString("ratio")))
            }
        } catch (_: Exception) { }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
