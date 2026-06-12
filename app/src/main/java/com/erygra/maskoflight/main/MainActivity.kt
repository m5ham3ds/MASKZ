package com.erygra.maskoflight.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.erygra.maskoflight.core.GameConfig
import com.erygra.maskoflight.save.SaveManager
import com.erygra.maskoflight.ui.MenuSystem
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MainActivity - النشاط الرئيسي للتطبيق
 * 
 * المسؤولية:
 * - إدارة دورة حياة التطبيق (Lifecycle Management)
 * - التعامل مع الأذن المطلوبة (Permission Handling)
 * - تعيين واجهة المستخدم الرئيسية (UI Setup)
 * - إدارة التوجيه بين الشاشات المختلفة (Navigation)
 * - التعامل مع الأحداث النظام (System Events)
 * 
 * Features:
 * - Android 6+ Permission Management
 * - Jetpack Compose UI Integration
 * - ViewModel Integration
 * - Lifecycle-aware Resource Management
 * - Crash Analytics Integration
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // ====================================================================
    // 🔧 الخصائص والمتغيرات
    // ====================================================================

    /** ViewModel الرئيسي للعبة */
    private val gameViewModel: GameViewModel by viewModels()

    /** SaveManager للتعامل مع حفظ البيانات */
    @Inject
    lateinit var saveManager: SaveManager

    /** حالة تحميل البيانات الأولية */
    private var isInitializing = true

    /** قائمة الأذن المطلوبة */
    private val requiredPermissions = mutableListOf(
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE
    ).apply {
        // إضافة أذن التخزين على Android 12 وما قبله
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            // Android 13+ - أذن محسّنة
            add(Manifest.permission.READ_MEDIA_IMAGES)
            add(Manifest.permission.READ_MEDIA_VIDEO)
            add(Manifest.permission.READ_MEDIA_AUDIO)
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_REQUEST_CODE = 100
        private const val INITIALIZATION_TIMEOUT = 30000L // 30 ثانية
    }

    // ====================================================================
    // 🔄 دورة الحياة (Lifecycle)
    // ====================================================================

    /**
     * onCreate - عند إنشاء النشاط
     * 
     * المسؤولية:
     * - التحقق من الأذن
     * - تهيئة البيانات الأولية
     * - تعيين واجهة المستخدم
     * - بدء المراقبة
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // تعيين محتوى واجهة المستخدم
        setContent {
            MaskOfLightApp(
                gameViewModel = gameViewModel,
                onPermissionsGranted = { initializeGame() },
                onPermissionsDenied = { showPermissionError() }
            )
        }

        // بدء العمليات الأساسية
        initializeApp()
    }

    /**
     * onStart - عند بدء النشاط
     */
    override fun onStart() {
        super.onStart()
        
        // إعادة تحميل حالة اللعبة إذا لزم الأمر
        lifecycleScope.launch {
            gameViewModel.resumeGame()
        }
    }

    /**
     * onResume - عند العودة للنشاط
     */
    override fun onResume() {
        super.onResume()
        
        // إعادة تشغيل الموسيقى والأصوات
        lifecycleScope.launch {
            gameViewModel.resumeAudio()
        }
    }

    /**
     * onPause - عند توقف النشاط مؤقتاً
     */
    override fun onPause() {
        super.onPause()
        
        // إيقاف مؤقت للموسيقى
        lifecycleScope.launch {
            gameViewModel.pauseAudio()
        }
    }

    /**
     * onStop - عند إيقاف النشاط
     */
    override fun onStop() {
        super.onStop()
        
        // حفظ البيانات الحالية
        lifecycleScope.launch {
            saveManager.autoSave()
        }
    }

    /**
     * onDestroy - عند تدمير النشاط
     */
    override fun onDestroy() {
        super.onDestroy()
        
        // تنظيف الموارد
        gameViewModel.cleanup()
    }

    /**
     * onBackPressed - عند الضغط على زر الرجوع
     */
    override fun onBackPressedDispatcher() {
        // يتم التعامل معه من خلال Compose Navigation
        super.onBackPressedDispatcher()
    }

    // ====================================================================
    // 🔐 إدارة الأذن (Permission Management)
    // ====================================================================

    /**
     * initializeApp - تهيئة التطبيق بشكل عام
     * 
     * الخطوات:
     * 1. التحقق من الأذن المطلوبة
     * 2. تحميل البيانات الأولية
     * 3. إعداد الـ ViewModel
     * 4. بدء الاستماع للأحداث
     */
    private fun initializeApp() {
        lifecycleScope.launch {
            try {
                // التحقق من الأذن
                if (hasAllPermissions()) {
                    initializeGame()
                } else {
                    requestMissingPermissions()
                }
            } catch (e: Exception) {
                handleInitializationError(e)
            }
        }
    }

    /**
     * hasAllPermissions - التحقق من وجود جميع الأذن المطلوبة
     * 
     * @return true إذا كانت جميع الأذن موجودة
     */
    private fun hasAllPermissions(): Boolean {
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * requestMissingPermissions - طلب الأذن الناقصة
     */
    private fun requestMissingPermissions() {
        val missingPermissions = requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                missingPermissions,
                PERMISSION_REQUEST_CODE
            )
        }
    }

    /**
     * onRequestPermissionsResult - نتيجة طلب الأذن
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                
                if (allGranted) {
                    initializeGame()
                } else {
                    showPermissionError()
                }
            }
        }
    }

    // ====================================================================
    // ⚙️ التهيئة والإعداد (Initialization)
    // ====================================================================

    /**
     * initializeGame - تهيئة اللعبة بعد التحقق من الأذن
     * 
     * الخطوات:
     * 1. تحميل الإعدادات الأساسية
     * 2. تحميل حالة اللعبة المحفوظة
     * 3. تهيئة جميع المحركات
     * 4. بدء الاستماع للأحداث
     * 5. تحديث حالة الـ ViewModel
     */
    private fun initializeGame() {
        lifecycleScope.launch {
            try {
                isInitializing = true

                // 1. تحميل الإعدادات
                val config = GameConfig.loadConfig(this@MainActivity)
                gameViewModel.setGameConfig(config)

                // 2. تحميل حالة اللعبة المحفوظة
                val savedGameData = saveManager.loadLatestSave()
                if (savedGameData != null) {
                    gameViewModel.restoreGameState(savedGameData)
                } else {
                    gameViewModel.startNewGame()
                }

                // 3. تهيئة المحركات
                gameViewModel.initializeGameEngines()

                // 4. بدء الاستماع للأحداث
                setupEventListeners()

                // 5. تحديث الحالة
                gameViewModel.setGameInitialized(true)
                isInitializing = false

                // تسجيل نجاح التهيئة
                logInitializationSuccess()

            } catch (e: Exception) {
                handleInitializationError(e)
            }
        }
    }

    /**
     * setupEventListeners - إعداد مستمعي الأحداث
     */
    private fun setupEventListeners() {
        lifecycleScope.launch {
            // الاستماع لأحداث اللعبة من خلال Event Bus
            gameViewModel.gameEvents.collect { event ->
                handleGameEvent(event)
            }
        }
    }

    /**
     * handleGameEvent - معالجة أحداث اللعبة
     * 
     * @param event حدث اللعبة
     */
    private fun handleGameEvent(event: String) {
        when {
            event.contains("GAME_OVER") -> handleGameOver()
            event.contains("LEVEL_COMPLETE") -> handleLevelComplete()
            event.contains("PLAYER_DIED") -> handlePlayerDeath()
            event.contains("SAVE_COMPLETE") -> handleSaveComplete()
            event.contains("ERROR") -> handleGameError(event)
        }
    }

    // ====================================================================
    // 📱 معالجات الأحداث (Event Handlers)
    // ====================================================================

    /**
     * handleGameOver - معالجة نهاية اللعبة
     */
    private fun handleGameOver() {
        lifecycleScope.launch {
            gameViewModel.showGameOverScreen()
            saveManager.autoSave()
        }
    }

    /**
     * handleLevelComplete - معالجة اكتمال المرحلة
     */
    private fun handleLevelComplete() {
        lifecycleScope.launch {
            gameViewModel.showLevelCompleteDialog()
            saveManager.autoSave()
        }
    }

    /**
     * handlePlayerDeath - معالجة موت اللاعب
     */
    private fun handlePlayerDeath() {
        lifecycleScope.launch {
            gameViewModel.showDeathScreen()
            saveManager.createAutoSavePoint()
        }
    }

    /**
     * handleSaveComplete - معالجة اكتمال الحفظ
     */
    private fun handleSaveComplete() {
        gameViewModel.showSaveNotification("تم حفظ اللعبة بنجاح")
    }

    /**
     * handleGameError - معالجة أخطاء اللعبة
     * 
     * @param event وصف الخطأ
     */
    private fun handleGameError(event: String) {
        lifecycleScope.launch {
            gameViewModel.showErrorDialog(
                title = "خطأ في اللعبة",
                message = event.replace("ERROR:", "").trim(),
                onDismiss = { gameViewModel.resumeGame() }
            )
        }
    }

    // ====================================================================
    // ❌ معالجات الأخطاء (Error Handling)
    // ====================================================================

    /**
     * handleInitializationError - معالجة خطأ التهيئة
     * 
     * @param exception الاستثناء الذي حدث
     */
    private fun handleInitializationError(exception: Exception) {
        isInitializing = false
        
        gameViewModel.showErrorDialog(
            title = "خطأ في تحميل اللعبة",
            message = exception.message ?: "حدث خطأ غير معروف",
            onDismiss = {
                // إعادة محاولة التهيئة
                initializeApp()
            }
        )
    }

    /**
     * showPermissionError - عرض رسالة خطأ الأذن
     */
    private fun showPermissionError() {
        gameViewModel.showErrorDialog(
            title = "أذن مطلوبة",
            message = "التطبيق يحتاج إلى بعض الأذن للعمل بشكل صحيح",
            onDismiss = {
                // طلب الأذن مرة أخرى
                requestMissingPermissions()
            }
        )
    }

    // ====================================================================
    // 📊 التسجيل والمراقبة (Logging & Monitoring)
    // ====================================================================

    /**
     * logInitializationSuccess - تسجيل نجاح التهيئة
     */
    private fun logInitializationSuccess() {
        android.util.Log.d(
            TAG,
            """
            ✅ تم تهيئة اللعبة بنجاح
            Version: ${GameConfig.VERSION}
            Device: ${Build.DEVICE}
            Android: ${Build.VERSION.SDK_INT}
            """.trimIndent()
        )
    }
}