package com.hacker.launcher

import android.app.Activity
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private val green = Color.parseColor("#00FF41")
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var root: FrameLayout
    private val prefs by lazy { getSharedPreferences("hl", Context.MODE_PRIVATE) }
    private var unlocked = false

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) {
            showPattern()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        setContentView(root)
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
        showBoot()
    }

    override fun onDestroy() {
        try { unregisterReceiver(screenOffReceiver) } catch (e: Exception) { }
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (unlocked) showHome() else showPattern()
    }

    override fun onBackPressed() {
        // Launcher: ignore back
    }

    // ---------- 1. Boot animation ----------
    private fun showBoot() {
        val tv = mono(14f).apply { setPadding(32, 80, 32, 32) }
        root.removeAllViews()
        root.addView(tv)
        val lines = listOf(
            "[ OK ] Booting kernel...",
            "[ OK ] Mounting /dev/phone",
            "[ OK ] Loading modules",
            "[ OK ] Bypassing firewall",
            "[ OK ] Starting shell",
            "",
            "GHOST SHELL v1.0\nACCESS REQUESTED..."
        )
        var i = 0
        val step = object : Runnable {
            override fun run() {
                if (i < lines.size) {
                    tv.append(lines[i++] + "\n")
                    handler.postDelayed(this, 350)
                } else {
                    if (prefs.getBoolean("themed", false)) showPattern() else showThemeScreen()
                }
            }
        }
        handler.post(step)
    }

    // ---------- 2. Theme screen (apply wallpaper + launcher) ----------
    private fun showThemeScreen() {
        handler.removeCallbacksAndMessages(null)
        val d = resources.displayMetrics.density
        val rain = MatrixView(this).apply { alpha = 0.4f }

        val title = mono(22f).apply {
            text = "> GHOST SHELL THEME"
            gravity = Gravity.CENTER
        }
        val sub = mono(13f).apply {
            text = "Phone ko hacker device bana do.\nStep 1 aur 2 dabao."
            gravity = Gravity.CENTER
            setPadding(0, (8 * d).toInt(), 0, (24 * d).toInt())
        }

        val b1 = button("[ 1 ]  SET MATRIX WALLPAPER") {
            try {
                val i = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                i.putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(this, MatrixWallpaperService::class.java)
                )
                startActivity(i)
            } catch (e: Exception) {
                try {
                    startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
                } catch (e2: Exception) {
                    Toast.makeText(this, "Settings > Wallpaper > Live wallpapers", Toast.LENGTH_LONG).show()
                }
            }
        }
        val b2 = button("[ 2 ]  USE GREEN ICONS (SET LAUNCHER)") {
            try {
                startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
            } catch (e: Exception) {
                Toast.makeText(this, "Settings > Apps > Default apps > Home app", Toast.LENGTH_LONG).show()
            }
        }
        val b3 = button("[ 3 ]  DONE >") {
            prefs.edit().putBoolean("themed", true).apply()
            showPattern()
        }

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding((24 * d).toInt(), 0, (24 * d).toInt(), 0)
            addView(title, LinearLayout.LayoutParams(-1, -2))
            addView(sub, LinearLayout.LayoutParams(-1, -2))
            addView(b1, btnParams())
            addView(b2, btnParams())
            addView(b3, btnParams())
        }
        root.removeAllViews()
        root.addView(rain, FrameLayout.LayoutParams(-1, -1))
        root.addView(col, FrameLayout.LayoutParams(-1, -1))
    }

    private fun btnParams(): LinearLayout.LayoutParams {
        val d = resources.displayMetrics.density
        return LinearLayout.LayoutParams(-1, -2).apply { topMargin = (12 * d).toInt() }
    }

    private fun button(label: String, onClick: () -> Unit): TextView {
        val d = resources.displayMetrics.density
        return TextView(this).apply {
            text = label
            setTextColor(green)
            typeface = Typeface.MONOSPACE
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(0, (16 * d).toInt(), 0, (16 * d).toInt())
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#CC000000"))
                setStroke((2 * d).toInt(), green)
                cornerRadius = 8 * d
            }
            setOnClickListener { onClick() }
        }
    }

    // ---------- 3. Pattern lock (rain behind) ----------
    private fun showPattern() {
        unlocked = false
        handler.removeCallbacksAndMessages(null)
        val saved = prefs.getString("pattern", null)
        val rain = MatrixView(this).apply { alpha = 0.5f }
        val title = mono(16f).apply {
            gravity = Gravity.CENTER
            setPadding(0, 120, 0, 0)
            text = if (saved == null) "> SET NEW PATTERN (min 4 dots)" else "> ENTER PATTERN"
        }
        val pv = PatternView(this)
        pv.setBackgroundColor(Color.TRANSPARENT)
        pv.onPattern = { p ->
            val s = p.joinToString(",")
            if (saved == null) {
                prefs.edit().putString("pattern", s).apply()
                title.text = "> PATTERN SAVED. DRAW AGAIN TO LOGIN"
                handler.postDelayed({ showPattern() }, 800)
            } else if (s == saved) {
                unlocked = true
                showHome()
            } else {
                title.text = "> ACCESS DENIED"
            }
        }
        root.removeAllViews()
        root.addView(rain, FrameLayout.LayoutParams(-1, -1))
        root.addView(pv, FrameLayout.LayoutParams(-1, -1))
        root.addView(title, FrameLayout.LayoutParams(-1, -2))
    }

    // ---------- 4. Home: rain + green icons ----------
    private fun showHome() {
        handler.removeCallbacksAndMessages(null)
        val d = resources.displayMetrics.density
        val rain = MatrixView(this).apply { alpha = 0.5f }

        val clock = mono(44f).apply { setPadding(0, 0, 0, (8 * d).toInt()) }
        val tick = object : Runnable {
            override fun run() {
                val now = Date()
                clock.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now) +
                    "\n" + SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(now)
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(tick)

        val greenFilter = ColorMatrixColorFilter(
            ColorMatrix(
                floatArrayOf(
                    0f, 0f, 0f, 0f, 0f,
                    0.3f, 0.59f, 0.11f, 0f, 40f,
                    0f, 0f, 0f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )

        val cols = 4
        val cellW = (resources.displayMetrics.widthPixels - (16 * d * 2).toInt()) / cols
        val grid = GridLayout(this).apply { columnCount = cols }
        for (ri in apps().sortedBy { it.loadLabel(packageManager).toString().lowercase() }) {
            val pkg = ri.activityInfo.packageName
            val icon = ImageView(this).apply {
                setImageDrawable(ri.loadIcon(packageManager))
                colorFilter = greenFilter
            }
            val label = TextView(this).apply {
                text = ri.loadLabel(packageManager).toString()
                setTextColor(green)
                typeface = Typeface.MONOSPACE
                textSize = 11f
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                gravity = Gravity.CENTER
                setShadowLayer(8f, 0f, 0f, Color.BLACK)
            }
            val cell = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(4, (10 * d).toInt(), 4, (10 * d).toInt())
                addView(icon, LinearLayout.LayoutParams((52 * d).toInt(), (52 * d).toInt()))
                addView(label, LinearLayout.LayoutParams(-1, -2))
                setOnClickListener {
                    packageManager.getLaunchIntentForPackage(pkg)?.let { startActivity(it) }
                }
            }
            val lp = GridLayout.LayoutParams().apply { width = cellW }
            grid.addView(cell, lp)
        }
        val scroll = ScrollView(this).apply {
            setPadding((16 * d).toInt(), 0, (16 * d).toInt(), 0)
            addView(grid)
        }

        val termBtn = mono(16f).apply {
            text = "> terminal"
            gravity = Gravity.CENTER
            setPadding(0, (16 * d).toInt(), 0, (24 * d).toInt())
            setBackgroundColor(Color.parseColor("#CC000000"))
            setOnClickListener { showTerminal() }
        }

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, (60 * d).toInt(), 0, 0)
            addView(clock, LinearLayout.LayoutParams(-2, -2))
            addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
            addView(termBtn, LinearLayout.LayoutParams(-1, -2))
        }

        root.removeAllViews()
        root.addView(rain, FrameLayout.LayoutParams(-1, -1))
        root.addView(col, FrameLayout.LayoutParams(-1, -1))
    }

    // ---------- 5. Terminal ----------
    private fun showTerminal() {
        handler.removeCallbacksAndMessages(null)
        val out = mono(14f).apply { setPadding(24, 24, 24, 8) }
        val scroll = ScrollView(this).apply { addView(out) }
        val input = EditText(this).apply {
            setTextColor(green)
            setHintTextColor(Color.parseColor("#006B1B"))
            setBackgroundColor(Color.BLACK)
            typeface = Typeface.MONOSPACE
            hint = "root@phone:~$ "
            setSingleLine()
            imeOptions = EditorInfo.IME_ACTION_DONE
            setPadding(24, 24, 24, 24)
        }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 60, 0, 0)
            addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
            addView(input, LinearLayout.LayoutParams(-1, -2))
        }
        root.removeAllViews()
        root.addView(col, FrameLayout.LayoutParams(-1, -1))
        out.text = "ACCESS GRANTED\nType 'help' for commands.\n\n"

        input.setOnEditorActionListener { _, _, _ ->
            val cmd = input.text.toString().trim()
            input.setText("")
            out.append("root@phone:~$ $cmd\n")
            if (cmd.isNotEmpty()) runCmd(cmd, out)
            out.append("\n")
            scroll.post { scroll.fullScroll(ScrollView.FOCUS_DOWN) }
            true
        }
    }

    private fun runCmd(cmd: String, out: TextView) {
        val parts = cmd.split(" ", limit = 2)
        val arg = parts.getOrNull(1)?.trim().orEmpty()
        when (parts[0].lowercase()) {
            "help" -> out.append(
                "help         show commands\n" +
                "home         go to home screen\n" +
                "theme        apply wallpaper/launcher\n" +
                "apps         list installed apps\n" +
                "open <name>  launch app\n" +
                "battery      battery level\n" +
                "time         current time\n" +
                "whoami       who are you\n" +
                "hack         fake hacking\n" +
                "reset        reset pattern\n" +
                "lock         lock screen\n" +
                "clear        clear screen\n"
            )
            "home" -> showHome()
            "theme" -> showThemeScreen()
            "apps" -> launchables().sortedBy { it.first }.forEach { out.append("- ${it.first}\n") }
            "open" -> {
                val match = launchables().firstOrNull { it.first.contains(arg, true) }
                if (arg.isEmpty() || match == null) {
                    out.append("open: '$arg' not found\n")
                } else {
                    packageManager.getLaunchIntentForPackage(match.second)?.let { startActivity(it) }
                }
            }
            "battery" -> {
                val i = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val lvl = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
                out.append("battery: ${lvl * 100 / scale}%\n")
            }
            "time" -> out.append(SimpleDateFormat("HH:mm:ss dd-MM-yyyy", Locale.getDefault()).format(Date()) + "\n")
            "whoami" -> out.append("root\n")
            "hack" -> {
                val steps = listOf("Scanning ports...", "Injecting payload...", "Cracking hash...", "ACCESS GRANTED (just kidding)")
                steps.forEachIndexed { n, s -> handler.postDelayed({ out.append("$s\n") }, 600L * n) }
            }
            "reset" -> {
                prefs.edit().remove("pattern").apply()
                out.append("pattern cleared. Type 'lock' to set a new one.\n")
            }
            "lock" -> showPattern()
            "clear" -> out.text = ""
            else -> out.append("${parts[0]}: command not found\n")
        }
    }

    private fun apps(): List<ResolveInfo> {
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(i, 0)
            .filter { it.activityInfo.packageName != packageName }
    }

    private fun launchables(): List<Pair<String, String>> =
        apps().map { it.loadLabel(packageManager).toString() to it.activityInfo.packageName }

    private fun mono(size: Float) = TextView(this).apply {
        setTextColor(green)
        typeface = Typeface.MONOSPACE
        textSize = size
        layoutParams = ViewGroup.LayoutParams(-1, -2)
    }
}
