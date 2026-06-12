package com.erygra.maskoflight.player

import androidx.compose.ui.graphics.Color

enum class Rarity(
    val nameEn: String,
    val nameAr: String,
    val color: Color,
    val glowIntensity: Float
) {
    COMMON("Common", "عادي", Color.Gray, 0.3f),
    UNCOMMON("Uncommon", "غير شائع", Color(0xFF2ECC71), 0.5f),
    RARE("Rare", "نادر", Color(0xFF3498DB), 0.7f),
    EPIC("Epic", "ملحمي", Color(0xFF9B59B6), 0.85f),
    LEGENDARY("Legendary", "أسطوري", Color(0xFFFFD700), 1.0f),
    UNIQUE("Unique", "فريد", Color(0xFFFF4500), 1.0f)
}
