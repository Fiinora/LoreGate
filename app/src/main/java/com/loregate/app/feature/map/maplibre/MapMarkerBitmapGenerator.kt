package com.loregate.app.feature.map.maplibre

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.loregate.app.core.designsystem.OrangePrimary
import com.loregate.app.domain.model.SiteType

object MapMarkerBitmapGenerator {

    private var cachedIdleCharacterBitmap: Bitmap? = null
    private var cachedWalkingCharacterBitmap: Bitmap? = null
    private var cachedSpearBitmap: Bitmap? = null

    fun createStaticCharacterBitmap(isWalking: Boolean): Bitmap {
        val cached = if (isWalking) cachedWalkingCharacterBitmap else cachedIdleCharacterBitmap
        if (cached != null) return cached

        val size = 280
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint().apply {
            isAntiAlias = true
            isDither = true
            isFilterBitmap = true
        }

        // Outer glow (blue)
        paint.color = android.graphics.Color.parseColor("#4A90E2")
        paint.alpha = 51
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        // Middle ring
        paint.alpha = 102
        canvas.drawCircle(size / 2f, size / 2f, size / 2.5f, paint)

        // Inner circle
        paint.alpha = 255
        canvas.drawCircle(size / 2f, size / 2f, size / 3.5f, paint)

        // White border
        paint.color = android.graphics.Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 8f
        canvas.drawCircle(size / 2f, size / 2f, size / 3.5f, paint)
        paint.style = Paint.Style.FILL

        // Center white circle
        canvas.drawCircle(size / 2f, size / 2f, size / 4.5f, paint)

        // Direction arrow pointing North
        paint.color = android.graphics.Color.parseColor("#EC9213")
        paint.style = Paint.Style.FILL

        val path = Path()
        val centerX = size / 2f
        val topY = size / 2f - size / 3f
        val arrowSize = 22f

        path.moveTo(centerX, topY - arrowSize)
        path.lineTo(centerX - arrowSize / 2, topY)
        path.lineTo(centerX + arrowSize / 2, topY)
        path.close()
        canvas.drawPath(path, paint)

        // Arrow base circle
        paint.alpha = 204
        canvas.drawCircle(centerX, topY, 4f, paint)

        // Character emoji (stays upright)
        paint.color = android.graphics.Color.parseColor("#4A90E2")
        paint.textSize = 50f
        paint.textAlign = Paint.Align.CENTER
        paint.style = Paint.Style.FILL
        paint.alpha = 255

        val emoji = if (isWalking) "🚶" else "🧍"
        canvas.drawText(emoji, size / 2f, size / 2f + 20f, paint)

        if (isWalking) {
            cachedWalkingCharacterBitmap = bitmap
        } else {
            cachedIdleCharacterBitmap = bitmap
        }
        return bitmap
    }

    fun createSiteMarkerBitmap(type: SiteType, isSelected: Boolean): Bitmap {
        val size = 120
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { isAntiAlias = true }

        if (isSelected) {
            paint.color = OrangePrimary.copy(alpha = 0.5f).toArgb()
            canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

            paint.color = OrangePrimary.copy(alpha = 0.7f).toArgb()
            canvas.drawCircle(size / 2f, size / 2f, size / 2.3f, paint)

            paint.color = OrangePrimary.toArgb()
            canvas.drawCircle(size / 2f, size / 2f, size / 2.5f, paint)

            paint.color = Color.White.toArgb()
            canvas.drawCircle(size / 2f, size / 2f, size / 3.2f, paint)

            paint.color = OrangePrimary.toArgb()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 6f
            canvas.drawCircle(size / 2f, size / 2f, size / 3.2f, paint)
            paint.style = Paint.Style.FILL
        } else {
            paint.color = Color.Gray.copy(alpha = 0.2f).toArgb()
            canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

            paint.color = Color.DarkGray.toArgb()
            canvas.drawCircle(size / 2f, size / 2f, size / 2.5f, paint)

            paint.color = Color.LightGray.toArgb()
            canvas.drawCircle(size / 2f, size / 2f, size / 3f, paint)
        }

        paint.color = if (isSelected) OrangePrimary.toArgb() else Color.DarkGray.toArgb()
        paint.textSize = 40f
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.DEFAULT_BOLD

        val textY = size / 2f + paint.textSize / 3
        canvas.drawText(type.emoji, size / 2f, textY, paint)

        return bitmap
    }

    fun createSpearBitmap(): Bitmap {
        cachedSpearBitmap?.let { return it }

        val size = 96
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val centerX = size / 2f
        val centerY = size / 2f

        val shaftPaint = Paint().apply {
            isAntiAlias = true
            color = Color(0xFFD4AF37).toArgb()
            strokeWidth = size / 14f
            strokeCap = Paint.Cap.ROUND
        }
        val tipPaint = Paint().apply {
            isAntiAlias = true
            color = Color(0xFFFAFAFA).toArgb()
            style = Paint.Style.FILL
            setShadowLayer(6f, 0f, 2f, Color(0x66000000).toArgb())
        }
        val accentPaint = Paint().apply {
            isAntiAlias = true
            color = Color(0xFF4CAF50).toArgb()
            strokeWidth = size / 12f
            strokeCap = Paint.Cap.ROUND
        }
        val glowPaint = Paint().apply {
            isAntiAlias = true
            color = Color(0x334CAF50).toArgb()
        }

        canvas.drawCircle(centerX, centerY, size * 0.35f, glowPaint)
        canvas.drawLine(centerX, centerY + size * 0.25f, centerX, centerY - size * 0.1f, shaftPaint)
        canvas.drawLine(centerX, centerY + size * 0.1f, centerX, centerY + size * 0.16f, accentPaint)

        val path = Path().apply {
            moveTo(centerX, centerY - size * 0.4f)
            lineTo(centerX - size * 0.08f, centerY - size * 0.18f)
            lineTo(centerX + size * 0.08f, centerY - size * 0.18f)
            close()
        }
        canvas.drawPath(path, tipPaint)
        canvas.drawCircle(centerX, centerY + size * 0.28f, size * 0.05f, tipPaint)

        cachedSpearBitmap = bmp
        return bmp
    }
}
