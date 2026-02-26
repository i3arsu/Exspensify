package com.example.exspensify.data.mapper

import com.example.exspensify.data.local.entity.CategoryEntity
import com.example.exspensify.domain.model.Category
import com.example.exspensify.domain.model.TransactionType

fun CategoryEntity.toCategory(): Category {
    return Category(
        id = this.id.toString(),
        name = this.name,
        icon = this.icon,
        color = try {
            // Ensure alpha channel and proper format
            val colorValue = 0xFF000000 or (this.color and 0x00FFFFFF)
            String.format("#%08X", colorValue)
        } catch (e: Exception) {
            android.util.Log.e("CategoryMapper", "Error formatting color for ${this.name}: ${this.color}", e)
            "#FF999999"  // Default gray
        },
        isDefault = this.isDefault,
        type = this.type?.let {
            try {
                TransactionType.valueOf(it)
            } catch (e: Exception) {
                null
            }
        }
    )
}

fun Category.toCategoryEntity(): CategoryEntity {
    return CategoryEntity(
        id = this.id.toLongOrNull() ?: 0L,
        name = this.name,
        icon = this.icon,
        color = try {
            val hex = this.color.removePrefix("#")
            val colorWithAlpha = if (hex.length == 6) "FF$hex" else hex
            colorWithAlpha.toLong(16)
        } catch (e: Exception) {
            0xFF9E9E9EL  // Default gray
        },
        isDefault = this.isDefault,
        type = this.type?.name
    )
}