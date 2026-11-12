package com.hacker.launcher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot

class PatternView(context: Context) : View(context) {

    private val green = Color.parseColor("#00FF41")
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = green
        style = Paint.Style.FILL
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = green
        strokeWidth = 8f
        setShadowLayer(20f, 0f, 0f, green)
    }
    private val pts = mutableListOf<PointF>()
    private val sel = mutableListOf<Int>()
    private var cur: PointF? = null

    /** Called when the finger lifts with the sequence of selected dots. */
    var onPattern: (List<Int>) -> Unit = {}

    init {
        setBackgroundColor(Color.BLACK)
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        pts.clear()
        for (r in 0..2) for (c in 0..2) {
            pts.add(PointF(w * (c + 1) / 4f, h * (r + 2) / 6f))
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        cur = PointF(e.x, e.y)
        pts.forEachIndexed { i, p ->
            if (hypot(e.x - p.x, e.y - p.y) < 70 && i !in sel) sel.add(i)
        }
        if (e.action == MotionEvent.ACTION_UP || e.action == MotionEvent.ACTION_CANCEL) {
            val result = sel.toList()
            sel.clear()
            cur = null
            invalidate()
            if (result.size >= 4) onPattern(result)
            return true
        }
        invalidate()
        return true
    }

    override fun onDraw(c: Canvas) {
        for (i in 0 until sel.size - 1) {
            c.drawLine(pts[sel[i]].x, pts[sel[i]].y, pts[sel[i + 1]].x, pts[sel[i + 1]].y, linePaint)
        }
        val last = sel.lastOrNull()
        val p = cur
        if (last != null && p != null) c.drawLine(pts[last].x, pts[last].y, p.x, p.y, linePaint)
        pts.forEachIndexed { i, pt ->
            c.drawCircle(pt.x, pt.y, if (i in sel) 28f else 20f, dotPaint)
        }
    }
}
