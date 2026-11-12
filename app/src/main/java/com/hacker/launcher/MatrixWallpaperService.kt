package com.hacker.launcher

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import kotlin.random.Random

class MatrixWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = MatrixEngine()

    inner class MatrixEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private val chars = "01アイウエオカキクケコサシスセソ#@%&".toCharArray()
        private val size = 36f
        private var drops = IntArray(0)
        private var visible = false

        private val fade = Paint().apply { color = Color.argb(40, 0, 0, 0) }
        private val txt = Paint().apply {
            color = Color.parseColor("#00FF41")
            textSize = size
            typeface = Typeface.MONOSPACE
        }

        private val drawRunnable = object : Runnable {
            override fun run() {
                val holder = surfaceHolder
                val c = holder.lockCanvas()
                if (c != null) {
                    try {
                        if (drops.isEmpty()) {
                            drops = IntArray((c.width / size).toInt() + 1) { Random.nextInt(-20, 0) }
                            c.drawColor(Color.BLACK)
                        }
                        c.drawRect(0f, 0f, c.width.toFloat(), c.height.toFloat(), fade)
                        for (i in drops.indices) {
                            c.drawText(chars.random().toString(), i * size, drops[i] * size, txt)
                            if (drops[i] * size > c.height && Random.nextFloat() > 0.975f) drops[i] = 0
                            drops[i]++
                        }
                    } finally {
                        holder.unlockCanvasAndPost(c)
                    }
                }
                if (visible) handler.postDelayed(this, 50)
            }
        }

        override fun onVisibilityChanged(v: Boolean) {
            visible = v
            handler.removeCallbacks(drawRunnable)
            if (v) handler.post(drawRunnable)
        }

        override fun onDestroy() {
            visible = false
            handler.removeCallbacks(drawRunnable)
        }
    }
}
