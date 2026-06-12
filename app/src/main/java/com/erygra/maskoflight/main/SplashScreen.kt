package com.erygra.maskoflight.main

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * SplashScreenContent - محتوى شاشة البداية
 * 
 * يعرض:
 * - شعار اللعبة
 * - نص التحميل
 * - شريط التقدم
 * 
 * @param onInitialized callback عند اكتمال التحميل
 */
@Composable
fun SplashScreenContent(
    onInitialized: () -> Unit = {}
) {
    var isLoading by remember { mutableStateOf(true) }
    var progress by remember { mutableStateOf(0f) }

    // محاكاة عملية التحميل
    LaunchedEffect(Unit) {
        repeat(101) {
            progress = it / 100f
            delay(30)
        }
        delay(500)
        isLoading = false
        delay(500)
        onInitialized()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                color = Color(0xFF0A0E27), // لون خلفية اللعبة الداكن
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ================================================================
            // 🎭 شعار اللعبة
            // ================================================================
            SplashLogo()

            Spacer(modifier = Modifier.height(48.dp))

            // ================================================================
            // 📝 عنوان اللعبة
            // ================================================================
            Text(
                text = "Mask of Light",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00D4FF),
                textAlign = TextAlign.Center,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "قناع النور",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF00A8CC),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(64.dp))

            // ================================================================
            // ⏳ مؤشر التحميل
            // ================================================================
            if (isLoading) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(200.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(60.dp),
                        color = Color(0xFF00D4FF),
                        strokeWidth = 4.dp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // نص التحميل
                    Text(
                        text = "جاري التحميل...",
                        fontSize = 16.sp,
                        color = Color(0xFF00D4FF),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // نسبة التقدم
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontSize = 14.sp,
                        color = Color(0xFF00A8CC),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // ================================================================
            // ℹ️ معلومات اللعبة
            // ================================================================
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Metroidvania 2D Adventure",
                    fontSize = 12.sp,
                    color = Color(0xFF00A8CC),
                    fontWeight = FontWeight.Light
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "مغامرة الألعاب ثنائية الأبعاد",
                    fontSize = 12.sp,
                    color = Color(0xFF00A8CC),
                    fontWeight = FontWeight.Light
                )
            }
        }
    }
}

/**
 * SplashLogo - شعار اللعبة المحرك
 */
@Composable
fun SplashLogo() {
    val infinite = rememberInfiniteTransition(label = "splash_animation")
    
    val scale by infinite.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_animation"
    )

    val alpha by infinite.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha_animation"
    )

    Box(
        modifier = Modifier
            .size(120.dp)
            .scale(scale)
            .alpha(alpha)
            .background(
                color = Color(0xFF00D4FF).copy(alpha = 0.2f),
                shape = androidx.compose.foundation.shape.CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "◈",
            fontSize = 80.sp,
            color = Color(0xFF00D4FF),
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * SplashScreenPreview - عرض معاينة شاشة البداية
 */
@Composable
fun SplashScreenPreview() {
    SplashScreenContent()
}