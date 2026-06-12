package com.erygra.maskoflight.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * Migrations - ترقيات قاعدة البيانات
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * إدارة ترقيات قاعدة البيانات بين الإصدارات المختلفة
 * Managing database migrations between different versions
 * 
 * الترقيات المتاحة / Available Migrations:
 * - MIGRATION_1_2: من الإصدار 1 إلى 2
 * - MIGRATION_2_3: من الإصدار 2 إلى 3
 * - إلخ...
 * 
 * ملاحظات / Notes:
 * - يتم تنفيذ الترقيات بشكل تلقائي
 * - الحفاظ على البيانات الموجودة
 * - إضافة أعمدة وجداول جديدة
 * 
 * @author Erygra Studio
 * @since 1.0.0
 * ═══════════════════════════════════════════════════════════════════════════
 */
object Migrations {

    /**
     * الترقية من الإصدار 1 إلى 2
     * Migration from version 1 to 2
     * 
     * التغييرات / Changes:
     * - إضافة عمود "achievements" لجدول اللاعب
     * - إضافة جدول "DailyRewards"
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // إضافة عمود الإنجازات
            // Add achievements column
            database.execSQL(
                """
                ALTER TABLE PlayerEntity 
                ADD COLUMN achievements TEXT NOT NULL DEFAULT '[]'
                """.trimIndent()
            )

            // إنشاء جدول المكافآت اليومية
            // Create daily rewards table
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS DailyRewardEntity (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    playerId TEXT NOT NULL,
                    day INTEGER NOT NULL,
                    claimed INTEGER NOT NULL DEFAULT 0,
                    claimDate INTEGER,
                    rewards TEXT NOT NULL,
                    FOREIGN KEY(playerId) REFERENCES PlayerEntity(playerId) 
                        ON DELETE CASCADE
                )
                """.trimIndent()
            )

            // إضافة فهرس
            // Add index
            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_DailyRewardEntity_playerId 
                ON DailyRewardEntity(playerId)
                """.trimIndent()
            )
        }
    }

    /**
     * الترقية من الإصدار 2 إلى 3
     * Migration from version 2 to 3
     * 
     * التغييرات / Changes:
     * - إضافة نظام الصداقة
     * - إضافة جدول الأصدقاء
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // إنشاء جدول الأصدقاء
            // Create friends table
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS FriendEntity (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    playerId TEXT NOT NULL,
                    friendId TEXT NOT NULL,
                    friendName TEXT NOT NULL,
                    status TEXT NOT NULL,
                    addedDate INTEGER NOT NULL,
                    lastInteraction INTEGER,
                    FOREIGN KEY(playerId) REFERENCES PlayerEntity(playerId) 
                        ON DELETE CASCADE,
                    UNIQUE(playerId, friendId)
                )
                """.trimIndent()
            )

            // إضافة أعمدة اجتماعية للاعب
            // Add social columns to player
            database.execSQL(
                """
                ALTER TABLE PlayerEntity 
                ADD COLUMN friendsList TEXT NOT NULL DEFAULT '[]'
                """.trimIndent()
            )

            database.execSQL(
                """
                ALTER TABLE PlayerEntity 
                ADD COLUMN guildId TEXT DEFAULT NULL
                """.trimIndent()
            )
        }
    }

    /**
     * الترقية من الإصدار 3 إلى 4
     * Migration from version 3 to 4
     * 
     * التغييرات / Changes:
     * - تحسين نظام المهام
     * - إضافة مهام فرعية
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // إضافة عمود المهام الفرعية
            // Add sub-quests column
            database.execSQL(
                """
                ALTER TABLE QuestEntity 
                ADD COLUMN subQuests TEXT NOT NULL DEFAULT '[]'
                """.trimIndent()
            )

            // إضافة عمود المهام الشرطية
            // Add conditional quests column
            database.execSQL(
                """
                ALTER TABLE QuestEntity 
                ADD COLUMN conditions TEXT NOT NULL DEFAULT '{}'
                """.trimIndent()
            )

            // إضافة عمود المكافآت الاختيارية
            // Add optional rewards column
            database.execSQL(
                """
                ALTER TABLE QuestEntity 
                ADD COLUMN optionalRewards TEXT NOT NULL DEFAULT '[]'
                """.trimIndent()
            )
        }
    }

    /**
     * الحصول على جميع الترقيات
     * Get all migrations
     * 
     * @return مصفوفة الترقيات / Migrations array
     */
    fun getAllMigrations(): Array<Migration> {
        return arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4
        )
    }

    /**
     * الحصول على ترقيات محددة
     * Get specific migrations
     * 
     * @param fromVersion من الإصدار / From version
     * @param toVersion إلى الإصدار / To version
     * @return قائمة الترقيات / Migrations list
     */
    fun getMigrationsForVersion(fromVersion: Int, toVersion: Int): List<Migration> {
        return getAllMigrations().filter { migration ->
            migration.startVersion >= fromVersion && migration.endVersion <= toVersion
        }
    }
}