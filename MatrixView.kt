package com.hacker.launcher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.view.View
import kotlin.random.Random

class MatrixView(context: Context) : View(context) {

    private val chars = "01アイウエオカキクケコサシスセソ#@%&".toCharArray()
    private val size = 14f * resources.displayMetrics.density
    private var bmp: Bitmap? = null
    private var bc: Canvas? = null
    private var drops = IntArray(0)
    private val fade = Paint().apply { color = Color.argb(40, 0, 0, 0) }
    private val txt = Paint().apply {
        color = Color.parseColor("#00FF41")
        textSize = size
        typeface = Typeface.MONOSPACE
    }

    private val tick = object : Runnable {
        override fun run() {
            step()
            invalidate()
            postDelayed(this, 60)
        }
    }

    init { setBackgroundColor(Color.BLACK) }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        if (w <= 0 || h <= 0) return
        bmp?.recycle()
        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp = b
        bc = Canvas(b).also { it.drawColor(Color.BLACK) }
        drops = IntArray((w / size).toInt() + 1) { Random.nextInt(-20, 0) }
    }

    private fun step() {
        val c = bc ?: return
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fade)
        for (i in drops.indices) {
            c.drawText(chars.random().toString(), i * size, drops[i] * size, txt)
            if (drops[i] * size > height && Random.nextFloat() > 0.975f) drops[i] = 0
            drops[i]++
        }
    }

    override fun onDraw(c: Canvas) {
        bmp?.let { c.drawBitmap(it, 0f, 0f, null) }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        removeCallbacks(tick)
        post(tick)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(tick)
        super.onDetachedFromWindow()
    }
}
