package com.sonora.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class WaveView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.primario)
        strokeWidth = 6f
        style = Paint.Style.STROKE
    }
    private val amplitudes = FloatArray(40) { (Math.random() * 20).toFloat() }
    private var phase = 0f

    fun update(amplitude: Float) {
        phase += 0.3f
        for (i in amplitudes.indices) {
            amplitudes[i] = (Math.sin((i + phase) * 0.5).toFloat() * amplitude * 30 + 10)
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width.toFloat()
        val height = height.toFloat()
        val step = width / (amplitudes.size - 1)
        for (i in 0 until amplitudes.size - 1) {
            canvas.drawLine(
                i * step, height / 2 - amplitudes[i],
                (i + 1) * step, height / 2 - amplitudes[i + 1],
                paint
            )
        }
    }
}
