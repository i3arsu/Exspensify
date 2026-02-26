package com.example.exspensify.core.util

import androidx.compose.ui.graphics.Color
import android.util.Log

/**
 * Parse hex color string to Compose Color
 * Supports formats: #RRGGBB, #AARRGGBB, RRGGBB, AARRGGBB
 */
fun parseColor(colorString: String): Color {
    return try {
        // Remove # prefix if present
        val cleanColor = colorString.trim().removePrefix("#")

        // Validate hex string
        if (cleanColor.isEmpty() || !cleanColor.all { it.isDigit() || it in 'A'..'F' || it in 'a'..'f' }) {
            Log.w("ColorUtils", "Invalid color format: $colorString")
            return Color.Gray
        }

        // Ensure we have alpha channel (8 characters)
        val colorWithAlpha = when (cleanColor.length) {
            6 -> "FF$cleanColor"  // RGB -> ARGB
            8 -> cleanColor       // Already ARGB
            else -> {
                Log.w("ColorUtils", "Unexpected color length: ${cleanColor.length} for $colorString")
                return Color.Gray
            }
        }

        // Parse using Android Color class which is more robust
        val androidColor = android.graphics.Color.parseColor("#$colorWithAlpha")
        Color(androidColor)

    } catch (e: Exception) {
        Log.e("ColorUtils", "Failed to parse color: $colorString", e)
        Color.Gray
    }
}

/**
 * Convert Long ARGB to Compose Color
 */
fun Long.toComposeColor(): Color {
    return try {
        val androidColor = this.toInt()
        Color(androidColor)
    } catch (e: Exception) {
        Log.e("ColorUtils", "Failed to convert Long to Color: $this", e)
        Color.Gray
    }
}

/**
 * Convert Compose Color to hex string
 */
fun Color.toHexString(): String {
    return try {
        String.format("#%08X", this.value.toLong())
    } catch (e: Exception) {
        "#FF999999"
    }
}