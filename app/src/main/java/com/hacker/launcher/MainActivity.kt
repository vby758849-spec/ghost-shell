package com.hacker.launcher

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private val green = Color.parseColor("#00FF41")
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var root: FrameLayout
    private val prefs by lazy { getSharedPreferences("hl", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        setContentView(root)
        showBoot()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Home pressed again: lock back to the pattern screen
        handler.removeCallbacksAndMessages(null)
        showPattern()
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
                    showPattern()
                }
            }
        }
        handler.post(step)
    }

    // ---------- 2. Pattern lock ----------
    private fun showPattern() {
        val saved = prefs.getString("pattern", null)
        val title = mono(16f).apply {
            gravity = Gravity.CENTER
            setPadding(0, 120, 0, 0)
            text = if (saved == null) "> SET NEW PATTERN (min 4 dots)" else "> ENTER PATTERN"
        }
        val pv = PatternView(this)
        pv.onPattern = { p ->
            val s = p.joinToString(",")
            if (saved == null) {
                prefs.edit().putString("pattern", s).apply()
                title.text = "> PATTERN SAVED. DRAW AGAIN TO LOGIN"
                showPatternDelayed()
            } else if (s == saved) {
                showTerminal()
            } else {
                title.text = "> ACCESS DENIED"
            }
        }
        root.removeAllViews()
        root.addView(pv, FrameLayout.LayoutParams(-1, -1))
        root.addView(title, FrameLayout.LayoutParams(-1, -2))
    }

    private fun showPatternDelayed() {
        handler.postDelayed({ showPattern() }, 800)
    }

    // ---------- 3. Terminal ----------
    private fun showTerminal() {
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
            if (cmd.isNotEmpty()) run(cmd, out)
            out.append("\n")
            scroll.post { scroll.fullScroll(ScrollView.FOCUS_DOWN) }
            true
        }
    }

    private fun run(cmd: String, out: TextView) {
        val parts = cmd.split(" ", limit = 2)
        val arg = parts.getOrNull(1)?.trim().orEmpty()
        when (parts[0].lowercase()) {
            "help" -> out.append(
                "help         show commands\n" +
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
                out.append("pattern cleared. Press Home to set a new one.\n")
            }
            "lock" -> showPattern()
            "clear" -> out.text = ""
            else -> out.append("${parts[0]}: command not found\n")
        }
    }

    private fun launchables(): List<Pair<String, String>> {
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(i, 0)
            .map { it.loadLabel(packageManager).toString() to it.activityInfo.packageName }
            .filter { it.second != packageName }
    }

    private fun mono(size: Float) = TextView(this).apply {
        setTextColor(green)
        typeface = Typeface.MONOSPACE
        textSize = size
        layoutParams = ViewGroup.LayoutParams(-1, -2)
    }
}
