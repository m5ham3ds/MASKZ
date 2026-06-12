package com.erygra.maskoflight.main

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.erygra.maskoflight.core.EventBus
import com.erygra.maskoflight.core.GameConfig
import com.erygra.maskoflight.data.database.GameDatabase
import com.erygra.maskoflight.data.dao.*
import com.erygra.maskoflight.data.repository.*
import com.erygra.maskoflight.engine.*
import com.erygra.maskoflight.network.api.GameApiService
import com.erygra.maskoflight.network.api.LeaderboardApi
import com.erygra.maskoflight.network.firebase.FirebaseManager
import com.erygra.maskoflight.network.NetworkModule
import com.erygra.maskoflight.player.InventorySystem
import com.erygra.maskoflight.player.SkillTree
import com.erygra.maskoflight.quest.QuestSystem
import com.erygra.maskoflight.dialogue.DialogueSystem
import com.erygra.maskoflight.save.SaveManager
import com.erygra.maskoflight.save.CloudSyncManager
import com.erygra.maskoflight.save.AutoSaveSystem
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * AppModule - وحدة Hilt الرئيسية
 * 
 * توفر:
 * - Database و DAOs
 * - Repositories
 * - Game Engines
 * - Network Services
 * - System Services
 * 
 * Scope: Singleton (يتم إنشاء instance واحد فقط للتطبيق)
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    companion object {
        private const val TAG = "AppModule"
        private const val DATABASE_NAME = "mask_of_light.db"
        private const val PREFERENCES_NAME = "mask_of_light_preferences"
    }

    // ====================================================================
    // 📱 Context & Application
    // ====================================================================

    /**
     * provideContext - توفير Context التطبيق
     * 
     * @param application التطبيق الحالي
     * @return Context التطبيق
     */
    @Provides
    @Singleton
    fun provideContext(application: Application): Context {
        return application.applicationContext
    }

    // ====================================================================
    // 💾 Database و DAOs
    // ====================================================================

    /**
     * provideGameDatabase - إنشاء قاعدة البيانات
     * 
     * إعدادات:
     * - Fallback على Migration إذا فشلت
     * - مسح البيانات عند فشل المتطلب
     * - فعّال للقراءة والكتابة
     * 
     * @param context Application Context
     * @return GameDatabase instance
     */
    @Provides
    @Singleton
    fun provideGameDatabase(
        @ApplicationContext context: Context
    ): GameDatabase {
        return Room.databaseBuilder(
            context,
            GameDatabase::class.java,
            DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .enableMultiInstanceInvalidation()
            .setQueryExecutor(Dispatchers.IO.asExecutor())
            .build()
    }

    /**
     * providePlayerDao - توفير Player DAO
     * 
     * @param database GameDatabase
     * @return PlayerDao
     */
    @Provides
    @Singleton
    fun providePlayerDao(database: GameDatabase): PlayerDao {
        return database.playerDao()
    }

    /**
     * provideQuestDao - توفير Quest DAO
     * 
     * @param database GameDatabase
     * @return QuestDao
     */
    @Provides
    @Singleton
    fun provideQuestDao(database: GameDatabase): QuestDao {
        return database.questDao()
    }

    /**
     * provideInventoryDao - توفير Inventory DAO
     * 
     * @param database GameDatabase
     * @return InventoryDao
     */
    @Provides
    @Singleton
    fun provideInventoryDao(database: GameDatabase): InventoryDao {
        return database.inventoryDao()
    }

    /**
     * provideWorldStateDao - توفير World State DAO
     * 
     * @param database GameDatabase
     * @return WorldStateDao
     */
    @Provides
    @Singleton
    fun provideWorldStateDao(database: GameDatabase): WorldStateDao {
        return database.worldStateDao()
    }

    // ====================================================================
    // 🗂️ Repositories
    // ====================================================================

    /**
     * providePlayerRepository - توفير Player Repository
     * 
     * مسؤول عن:
     * - حفظ وتحميل بيانات اللاعب
     * - إدارة الحالة
     * - التزامن مع السحابة
     * 
     * @param playerDao Player DAO
     * @param cloudSync Cloud Sync Manager
     * @return PlayerRepository
     */
    @Provides
    @Singleton
    fun providePlayerRepository(
        playerDao: PlayerDao,
        cloudSync: CloudSyncManager
    ): PlayerRepository {
        return PlayerRepository(playerDao, cloudSync)
    }

    /**
     * provideQuestRepository - توفير Quest Repository
     * 
     * @param questDao Quest DAO
     * @return QuestRepository
     */
    @Provides
    @Singleton
    fun provideQuestRepository(questDao: QuestDao): QuestRepository {
        return QuestRepository(questDao)
    }

    /**
     * provideInventoryRepository - توفير Inventory Repository
     * 
     * @param inventoryDao Inventory DAO
     * @return InventoryRepository
     */
    @Provides
    @Singleton
    fun provideInventoryRepository(
        inventoryDao: InventoryDao
    ): InventoryRepository {
        return InventoryRepository(inventoryDao)
    }

    /**
     * provideWorldRepository - توفير World Repository
     * 
     * @param worldStateDao World State DAO
     * @return WorldRepository
     */
    @Provides
    @Singleton
    fun provideWorldRepository(
        worldStateDao: WorldStateDao
    ): WorldRepository {
        return WorldRepository(worldStateDao)
    }

    // ====================================================================
    // ⚙️ Game Engines
    // ====================================================================

    /**
     * provideEventBus - توفير Event Bus
     * 
     * مركز توزيع الأحداث في اللعبة
     * 
     * @return EventBus instance
     */
    @Provides
    @Singleton
    fun provideEventBus(): EventBus {
        return EventBus()
    }

    /**
     * providePhysicsEngine - توفير Physics Engine
     * 
     * يدير:
     * - الجاذبية والحركة
     * - التصادمات
     * - الفيزياء العامة
     * 
     * @return PhysicsEngine
     */
    @Provides
    @Singleton
    fun providePhysicsEngine(): PhysicsEngine {
        return PhysicsEngine()
    }

    /**
     * provideCombatEngine - توفير Combat Engine
     * 
     * يدير:
     * - حسابات الضرر
     * - نظام المهارات القتالية
     * - الحالات الخاصة
     * 
     * @param eventBus Event Bus
     * @return CombatEngine
     */
    @Provides
    @Singleton
    fun provideCombatEngine(eventBus: EventBus): CombatEngine {
        return CombatEngine(eventBus)
    }

    /**
     * provideParticleEngine - توفير Particle Engine
     * 
     * يدير:
     * - التأثيرات البصرية
     * - الجزيئات والمتفجرات
     * - الإضاءة والظلال
     * 
     * @return ParticleEngine
     */
    @Provides
    @Singleton
    fun provideParticleEngine(): ParticleEngine {
        return ParticleEngine()
    }

    /**
     * provideAudioEngine - توفير Audio Engine
     * 
     * يدير:
     * - الموسيقى الخلفية
     * - المؤثرات الصوتية
     * - مستويات الصوت
     * 
     * @param context Application Context
     * @return AudioEngine
     */
    @Provides
    @Singleton
    fun provideAudioEngine(
        @ApplicationContext context: Context
    ): AudioEngine {
        return AudioEngine(context)
    }

    // ====================================================================
    // 🎮 Game Systems
    // ====================================================================

    /**
     * provideInventorySystem - توفير Inventory System
     * 
     * يدير:
     * - الأشياء والعناصر
     * - السعة والتنظيم
     * - استخدام الأشياء
     * 
     * @param repository Inventory Repository
     * @return InventorySystem
     */
    @Provides
    @Singleton
    fun provideInventorySystem(
        repository: InventoryRepository
    ): InventorySystem {
        return InventorySystem(repository)
    }

    /**
     * provideSkillTree - توفير Skill Tree
     * 
     * يدير:
     * - شجرة المهارات
     * - نقاط المهارات
     * - التحسينات
     * 
     * @return SkillTree
     */
    @Provides
    @Singleton
    fun provideSkillTree(): SkillTree {
        return SkillTree()
    }

    /**
     * provideQuestSystem - توفير Quest System
     * 
     * يدير:
     * - المهام والسلاسل
     * - الأهداف والمكافآت
     * - التتبع
     * 
     * @param repository Quest Repository
     * @param eventBus Event Bus
     * @return QuestSystem
     */
    @Provides
    @Singleton
    fun provideQuestSystem(
        repository: QuestRepository,
        eventBus: EventBus
    ): QuestSystem {
        return QuestSystem(repository, eventBus)
    }

    /**
     * provideDialogueSystem - توفير Dialogue System
     * 
     * يدير:
     * - الحوارات والنصوص
     * - اختيارات الحوار
     * - شروط الحوار
     * 
     * @return DialogueSystem
     */
    @Provides
    @Singleton
    fun provideDialogueSystem(): DialogueSystem {
        return DialogueSystem()
    }

    // ====================================================================
    // 💾 Save Systems
    // ====================================================================

    /**
     * provideSaveManager - توفير Save Manager
     * 
     * يدير:
     * - حفظ البيانات
     * - تحميل البيانات
     * - معالجة الأخطاء
     * 
     * @param playerRepository Player Repository
     * @param questRepository Quest Repository
     * @param inventoryRepository Inventory Repository
     * @param worldRepository World Repository
     * @param context Application Context
     * @return SaveManager
     */
    @Provides
    @Singleton
    fun provideSaveManager(
        playerRepository: PlayerRepository,
        questRepository: QuestRepository,
        inventoryRepository: InventoryRepository,
        worldRepository: WorldRepository,
        @ApplicationContext context: Context
    ): SaveManager {
        return SaveManager(
            playerRepository,
            questRepository,
            inventoryRepository,
            worldRepository,
            context
        )
    }

    /**
     * provideAutoSaveSystem - توفير Auto Save System
     * 
     * يقوم بـ:
     * - الحفظ التلقائي الدوري
     * - حفظ نقاط التحقق
     * - استعادة البيانات
     * 
     * @param saveManager Save Manager
     * @return AutoSaveSystem
     */
    @Provides
    @Singleton
    fun provideAutoSaveSystem(
        saveManager: SaveManager
    ): AutoSaveSystem {
        return AutoSaveSystem(saveManager)
    }

    /**
     * provideCloudSyncManager - توفير Cloud Sync Manager
     * 
     * يقوم بـ:
     * - مزامنة السحابة
     * - تحميل البيانات
     * - التعامل مع التضارب
     * 
     * @param firebaseManager Firebase Manager
     * @return CloudSyncManager
     */
    @Provides
    @Singleton
    fun provideCloudSyncManager(
        firebaseManager: FirebaseManager
    ): CloudSyncManager {
        return CloudSyncManager(firebaseManager)
    }

    // ====================================================================
    // 🌐 Network Services
    // ====================================================================

    /**
     * provideGameApiService - توفير Game API Service
     * 
     * الخوادم:
     * - بيانات اللعبة
     * - التحديثات
     * - الإحصائيات
     * 
     * @param retrofit Retrofit instance
     * @return GameApiService
     */
    @Provides
    @Singleton
    fun provideGameApiService(retrofit: Retrofit): GameApiService {
        return retrofit.create(GameApiService::class.java)
    }

    /**
     * provideLeaderboardApi - توفير Leaderboard API
     * 
     * @param retrofit Retrofit instance
     * @return LeaderboardApi
     */
    @Provides
    @Singleton
    fun provideLeaderboardApi(retrofit: Retrofit): LeaderboardApi {
        return retrofit.create(LeaderboardApi::class.java)
    }

    /**
     * provideFirebaseManager - توفير Firebase Manager
     * 
     * @param context Application Context
     * @return FirebaseManager
     */
    @Provides
    @Singleton
    fun provideFirebaseManager(
        @ApplicationContext context: Context
    ): FirebaseManager {
        return FirebaseManager(context)
    }

    /**
     * provideRetrofit - توفير Retrofit instance
     * 
     * @return Retrofit
     */
    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        return NetworkModule.provideRetrofit()
    }

    // ====================================================================
    // ⚙️ Preferences و Configuration
    // ====================================================================

    /**
     * provideSharedPreferences - توفير SharedPreferences
     * 
     * لتخزين:
     * - الإعدادات
     * - البيانات الخفيفة
     * - تفضيلات المستخدم
     * 
     * @param context Application Context
     * @return SharedPreferences
     */
    @Provides
    @Singleton
    fun provideSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences {
        return context.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )
    }

    /**
     * provideGameConfig - توفير Game Configuration
     * 
     * @return GameConfig
     */
    @Provides
    @Singleton
    fun provideGameConfig(): GameConfig {
        return GameConfig
    }
}