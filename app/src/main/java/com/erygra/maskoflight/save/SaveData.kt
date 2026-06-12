package com.erygra.maskoflight.save

import java.io.Serializable

/**
 * ════════════════════════════════════════════════════════════════════════════════
 * SaveData.kt - بيانات الحفظ الرئيسية
 * ════════════════════════════════════════════════════════════════════════════════
 * 
 * الوصف:
 * - تمثيل شامل لحالة اللعبة
 * - بيانات اللاعب والعالم
 * - تقدم المهام والإحصائيات
 * - الإعدادات والتفضيلات
 * 
 * @author Erygra Team
 * @since 2.0.0
 * ════════════════════════════════════════════════════════════════════════════════
 */

/**
 * بيانات الحفظ الرئيسية
 *
 * @property name اسم الحفظ
 * @property timestamp وقت الحفظ
 * @property playtime وقت اللعب الإجمالي
 * @property playerData بيانات اللاعب
 * @property worldData بيانات العالم
 * @property questData بيانات المهام
 * @property dialogueData بيانات الحوارات
 * @property gameSettings إعدادات اللعبة
 * @property statistics الإحصائيات
 * @property inventory الحقيبة
 * @property achievements الإنجازات
 * @property lastLocation آخر موقع
 * @property playerLevel مستوى اللاعب
 * @property gameVersion إصدار اللعبة
 */
data class SaveData(
    val name: String = "Save Slot",
    val timestamp: Long = System.currentTimeMillis(),
    val playtime: Long = 0L,
    val playerData: PlayerData = PlayerData(),
    val worldData: WorldData = WorldData(),
    val questData: QuestData = QuestData(),
    val dialogueData: DialogueData = DialogueData(),
    val gameSettings: GameSettings = GameSettings(),
    val statistics: GameStatistics = GameStatistics(),
    val inventory: InventoryData = InventoryData(),
    val achievements: AchievementsData = AchievementsData(),
    val lastLocation: String = "sanctuary",
    val playerLevel: Int = 1,
    val gameVersion: String = "2.0.0"
) : Serializable

/**
 * بيانات اللاعب
 *
 * @property playerId معرف اللاعب
 * @property playerName اسم اللاعب
 * @property health الصحة الحالية
 * @property maxHealth أقصى صحة
 * @property mana المانا الحالي
 * @property maxMana أقصى مانا
 * @property experience الخبرة الحالية
 * @property level المستوى
 * @property position موقع اللاعب
 * @property rotation دوران اللاعب
 * @property abilities القدرات المفتوحة
 * @property equipment المعدات المجهزة
 */
data class PlayerData(
    val playerId: String = "",
    val playerName: String = "Player",
    val health: Float = 100f,
    val maxHealth: Float = 100f,
    val mana: Float = 50f,
    val maxMana: Float = 50f,
    val experience: Long = 0L,
    val level: Int = 1,
    val position: Vector3Data = Vector3Data(),
    val rotation: Vector3Data = Vector3Data(),
    val abilities: List<String> = emptyList(),
    val equipment: EquipmentData = EquipmentData()
) : Serializable

/**
 * بيانات العالم
 *
 * @property discoveredLocations المواقع المكتشفة
 * @property visitedLocations المواقع المزارة
 * @property activateObjects الأجسام المفعلة
 * @property doorStates حالات الأبواب
 * @property environmentalState حالة البيئة
 */
data class WorldData(
    val discoveredLocations: Set<String> = emptySet(),
    val visitedLocations: Set<String> = emptySet(),
    val activatedObjects: Set<String> = emptySet(),
    val doorStates: Map<String, Boolean> = emptyMap(),
    val environmentalState: Map<String, Any> = emptyMap()
) : Serializable

/**
 * بيانات المهام
 *
 * @property completedQuests المهام المكتملة
 * @property activeQuests المهام النشطة
 * @property questProgress تقدم المهام
 * @property questHistory تاريخ المهام
 */
data class QuestData(
    val completedQuests: Set<String> = emptySet(),
    val activeQuests: List<String> = emptyList(),
    val questProgress: Map<String, Int> = emptyMap(),
    val questHistory: List<String> = emptyList()
) : Serializable

/**
 * بيانات الحوارات
 *
 * @property completedDialogues الحوارات المكتملة
 * @property dialogueHistory تاريخ الحوارات
 * @property relationshipScores نقاط العلاقات
 */
data class DialogueData(
    val completedDialogues: Set<String> = emptySet(),
    val dialogueHistory: List<String> = emptyList(),
    val relationshipScores: Map<String, Int> = emptyMap()
) : Serializable

/**
 * إعدادات اللعبة
 *
 * @property difficulty صعوبة اللعبة
 * @property language اللغة
 * @property graphicsQuality جودة الرسوميات
 * @property masterVolume مستوى الصوت الرئيسي
 * @property musicVolume مستوى صوت الموسيقى
 * @property sfxVolume مستوى صوت المؤثرات
 * @property vibrationEnabled تفعيل الاهتزاز
 * @property autoSaveEnabled تفعيل الحفظ التلقائي
 */
data class GameSettings(
    val difficulty: String = "normal",
    val language: String = "en",
    val graphicsQuality: String = "high",
    val masterVolume: Float = 1.0f,
    val musicVolume: Float = 0.8f,
    val sfxVolume: Float = 0.8f,
    val vibrationEnabled: Boolean = true,
    val autoSaveEnabled: Boolean = true
) : Serializable

/**
 * إحصائيات اللعبة
 *
 * @property totalPlaytime إجمالي وقت اللعب
 * @property totalDeaths إجمالي الوفيات
 * @property totalKills إجمالي القتلى
 * @property bossesDefeated الزعماء المهزومون
 * @property areasExplored المناطق المستكشفة
 * @property secretsFound الأسرار المكتشفة
 * @property enemiesDefeated الأعداء المهزومون
 * @property itemsCollected الأشياء المجموعة
 */
data class GameStatistics(
    val totalPlaytime: Long = 0L,
    val totalDeaths: Int = 0,
    val totalKills: Int = 0,
    val bossesDefeated: List<String> = emptyList(),
    val areasExplored: List<String> = emptyList(),
    val secretsFound: Int = 0,
    val enemiesDefeated: Int = 0,
    val itemsCollected: Int = 0
) : Serializable

/**
 * بيانات الحقيبة
 *
 * @property items الأشياء
 * @property equipment المعدات
 * @property materials المواد
 * @property capacity السعة
 */
data class InventoryData(
    val items: List<ItemData> = emptyList(),
    val equipment: List<ItemData> = emptyList(),
    val materials: List<MaterialData> = emptyList(),
    val capacity: Int = 20
) : Serializable

/**
 * بيانات الشيء
 *
 * @property itemId معرف الشيء
 * @property quantity الكمية
 * @property durability المتانة
 * @property enchantments السحر
 */
data class ItemData(
    val itemId: String,
    val quantity: Int = 1,
    val durability: Float = 100f,
    val enchantments: List<String> = emptyList()
) : Serializable

/**
 * بيانات المادة
 *
 * @property materialId معرف المادة
 * @property quantity الكمية
 */
data class MaterialData(
    val materialId: String,
    val quantity: Int = 1
) : Serializable

/**
 * بيانات المعدات
 *
 * @property helmet الخوذة
 * @property chestplate درع الصدر
 * @property gloves القفازات
 * @property leggings السراويل
 * @property boots الحذاء
 * @property weapon السلاح
 * @property shield الدرع
 * @property accessory1 الإكسسوار 1
 * @property accessory2 الإكسسوار 2
 */
data class EquipmentData(
    val helmet: String? = null,
    val chestplate: String? = null,
    val gloves: String? = null,
    val leggings: String? = null,
    val boots: String? = null,
    val weapon: String? = null,
    val shield: String? = null,
    val accessory1: String? = null,
    val accessory2: String? = null
) : Serializable

/**
 * بيانات الإنجازات
 *
 * @property unlockedAchievements الإنجازات المفتوحة
 * @property achievementProgress تقدم الإنجازات
 * @property totalPoints إجمالي النقاط
 */
data class AchievementsData(
    val unlockedAchievements: Set<String> = emptySet(),
    val achievementProgress: Map<String, Int> = emptyMap(),
    val totalPoints: Int = 0
) : Serializable

/**
 * بيانات المتجه (Vector3)
 *
 * @property x الإحداثي X
 * @property y الإحداثي Y
 * @property z الإحداثي Z
 */
data class Vector3Data(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f
) : Serializable