package com.erygra.maskoflight.data.database

import android.content.Context
import android.database.Cursor
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.erygra.maskoflight.data.dao.InventoryDao
import com.erygra.maskoflight.data.dao.PlayerDao
import com.erygra.maskoflight.data.dao.QuestDao
import com.erygra.maskoflight.data.dao.WorldStateDao
import com.erygra.maskoflight.data.entities.ItemEntity
import com.erygra.maskoflight.data.entities.PlayerEntity
import com.erygra.maskoflight.data.entities.QuestEntity
import com.erygra.maskoflight.data.entities.WorldProgressEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * GameDatabase - قاعدة البيانات الرئيسية للعبة
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * قاعدة بيانات Room الرئيسية التي تدير جميع بيانات اللعبة المحلية
 * 
 * Main Room database managing all local game data
 * 
 * المميزات / Features:
 * - حفظ بيانات اللاعب (Player data persistence)
 * - نظام المهام (Quest system)
 * - إدارة المخزون (Inventory management)
 * - تقدم العالم (World progress tracking)
 * - Type converters للأنواع المعقدة
 * - دعم الـ Migrations
 * - Thread-safe operations
 * 
 * الإصدار / Version: 1
 * 
 * @author Erygra Studio
 * @since 1.0.0
 * ═══════════════════════════════════════════════════════════════════════════
 */

@Database(
    entities = [
        PlayerEntity::class,
        QuestEntity::class,
        ItemEntity::class,
        WorldProgressEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class GameDatabase : RoomDatabase() {

    // ═══════════════════════════════════════════════════════════════════════
    // DAOs - واجهات الوصول للبيانات
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * واجهة الوصول لبيانات اللاعب
     * Player data access object
     */
    abstract fun playerDao(): PlayerDao

    /**
     * واجهة الوصول لبيانات المهام
     * Quest data access object
     */
    abstract fun questDao(): QuestDao

    /**
     * واجهة الوصول لبيانات المخزون
     * Inventory data access object
     */
    abstract fun inventoryDao(): InventoryDao

    /**
     * واجهة الوصول لحالة العالم
     * World state data access object
     */
    abstract fun worldStateDao(): WorldStateDao

    // ═══════════════════════════════════════════════════════════════════════
    // Companion Object - Singleton Pattern
    // ═══════════════════════════════════════════════════════════════════════

    companion object {
        /**
         * اسم قاعدة البيانات
         * Database name
         */
        private const val DATABASE_NAME = "mask_of_light_database"

        /**
         * نسخة Singleton من قاعدة البيانات
         * Singleton instance
         */
        @Volatile
        private var INSTANCE: GameDatabase? = null

        /**
         * Coroutine scope لعمليات قاعدة البيانات
         * Coroutine scope for database operations
         */
        private val databaseScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /**
         * الحصول على نسخة من قاعدة البيانات (Thread-safe)
         * Get database instance (thread-safe)
         * 
         * @param context سياق التطبيق / Application context
         * @return نسخة من قاعدة البيانات / Database instance
         */
        fun getDatabase(context: Context): GameDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GameDatabase::class.java,
                    DATABASE_NAME
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration() // للتطوير فقط / Development only
                    .build()
                
                INSTANCE = instance
                instance
            }
        }

        /**
         * إغلاق قاعدة البيانات وتحرير الموارد
         * Close database and release resources
         */
        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }

        /**
         * حذف قاعدة البيانات (للاختبار)
         * Delete database (for testing)
         * 
         * @param context سياق التطبيق / Application context
         */
        fun deleteDatabase(context: Context) {
            closeDatabase()
            context.deleteDatabase(DATABASE_NAME)
        }

        /**
         * فحص وجود قاعدة البيانات
         * Check if database exists
         * 
         * @param context سياق التطبيق / Application context
         * @return true إذا كانت موجودة / true if exists
         */
        fun databaseExists(context: Context): Boolean {
            return context.getDatabasePath(DATABASE_NAME).exists()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Database Callback - معالج أحداث قاعدة البيانات
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * معالج أحداث إنشاء وفتح قاعدة البيانات
     * Database creation and open callback handler
     */
    private class DatabaseCallback : Callback() {

        /**
         * يتم تنفيذه عند إنشاء قاعدة البيانات لأول مرة
         * Called when database is created for the first time
         * 
         * @param db قاعدة البيانات / Database instance
         */
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            
            // تهيئة قاعدة البيانات ببيانات أولية
            // Initialize database with default data
            INSTANCE?.let { database ->
                databaseScope.launch {
                    populateDatabase(database)
                }
            }
        }

        /**
         * يتم تنفيذه عند فتح قاعدة البيانات
         * Called when database is opened
         * 
         * @param db قاعدة البيانات / Database instance
         */
        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            
            // تفعيل Foreign Keys
            // Enable foreign keys
            db.execSQL("PRAGMA foreign_keys=ON")
            
            // تحسين الأداء
            // Performance optimizations
            db.execSQL("PRAGMA journal_mode=WAL")
            db.execSQL("PRAGMA synchronous=NORMAL")
        }

        /**
         * ملء قاعدة البيانات بالبيانات الأولية
         * Populate database with initial data
         * 
         * @param database قاعدة البيانات / Database instance
         */
        private suspend fun populateDatabase(database: GameDatabase) {
            // حذف جميع البيانات (للتطوير)
            // Delete all data (development)
            database.clearAllTables()

            // يمكن إضافة بيانات أولية هنا
            // Can add initial data here
            
            // مثال: إضافة مهمة تعليمية
            // Example: Add tutorial quest
            val tutorialQuest = QuestEntity(
                questId = "quest_tutorial_001",
                nameAr = "بداية المغامرة",
                nameEn = "Beginning of Adventure",
                descriptionAr = "تعلم أساسيات اللعبة",
                descriptionEn = "Learn the game basics",
                type = "tutorial",
                status = "available",
                requirements = emptyMap(),
                rewards = mapOf(
                    "xp" to 100,
                    "gold" to 50
                ),
                objectives = listOf(
                    mapOf(
                        "type" to "move",
                        "target" to "sanctuary",
                        "progress" to 0,
                        "required" to 1
                    )
                ),
                region = "forgotten_valley",
                minLevel = 1,
                isRepeatable = false,
                priority = 1
            )

            database.questDao().insertQuest(tutorialQuest)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Database Operations - عمليات قاعدة البيانات
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * تصدير قاعدة البيانات إلى ملف
     * Export database to file
     * 
     * @param outputPath مسار الملف / File path
     * @return true إذا نجحت / true if successful
     */
    suspend fun exportDatabase(outputPath: String): Boolean {
        return try {
            // إغلاق قاعدة البيانات مؤقتاً
            // Close database temporarily
            close()
            
            // نسخ ملف قاعدة البيانات
            // Copy database file
            val dbFile = openHelper.writableDatabase.path?.let {
                java.io.File(it)
            }
            
            dbFile?.copyTo(
                target = java.io.File(outputPath),
                overwrite = true
            )
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * استيراد قاعدة البيانات من ملف
     * Import database from file
     * 
     * @param inputPath مسار الملف / File path
     * @return true إذا نجحت / true if successful
     */
    suspend fun importDatabase(inputPath: String): Boolean {
        return try {
            // إغلاق قاعدة البيانات
            // Close database
            close()
            
            // نسخ الملف المستورد
            // Copy imported file
            val dbFile = openHelper.writableDatabase.path?.let {
                java.io.File(it)
            }
            
            java.io.File(inputPath).copyTo(
                target = dbFile!!,
                overwrite = true
            )
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * الحصول على حجم قاعدة البيانات بالبايت
     * Get database size in bytes
     * 
     * @return حجم قاعدة البيانات / Database size
     */
    fun getDatabaseSize(): Long {
        return try {
            val dbFile = openHelper.writableDatabase.path?.let {
                java.io.File(it)
            }
            dbFile?.length() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * تحسين قاعدة البيانات (Vacuum)
     * Optimize database (vacuum)
     */
    suspend fun optimizeDatabase() {
        try {
            openHelper.writableDatabase.execSQL("VACUUM")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * فحص سلامة قاعدة البيانات
     * Check database integrity
     * 
     * @return true إذا كانت سليمة / true if integrity is ok
     */
    suspend fun checkIntegrity(): Boolean {
        return try {
            val db = openHelper.writableDatabase
            val cursor: Cursor = db.query("PRAGMA integrity_check", emptyArray())
            
            cursor.use {
                if (it.moveToFirst()) {
                    val result = it.getString(0)
                    result == "ok"
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            false
        }
    }
}