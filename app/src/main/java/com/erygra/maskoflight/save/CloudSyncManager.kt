package com.erygra.maskoflight.save

import com.erygra.maskoflight.network.NetworkModule
import com.erygra.maskoflight.network.models.UploadSaveRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * ════════════════════════════════════════════════════════════════════════════════
 * CloudSyncManager.kt - مدير المزامنة السحابية
 * ════════════════════════════════════════════════════════════════════════════════
 * 
 * الوصف:
 * - مزامنة البيانات مع السحابة
 * - حل التضاربات
 * - مزامنة تلقائية
 * - إدارة الحالات المتزامنة
 * 
 * المكونات الرئيسية:
 * - Cloud synchronization
 * - Conflict resolution
 * - Auto-sync
 * - State tracking
 * 
 * @author Erygra Team
 * @since 2.0.0
 * ════════════════════════════════════════════════════════════════════════════════
 */

class CloudSyncManager {
    
    // ════════════════════════════════════════════════════════════════════════════
    // Properties
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * حالة المزامنة
     */
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()
    
    /**
     * آخر وقت مزامنة
     */
    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()
    
    /**
     * هل المزامنة جارية
     */
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()
    
    /**
     * عدد الأخطاء
     */
    private val _syncErrors = MutableStateFlow<List<String>>(emptyList())
    val syncErrors: StateFlow<List<String>> = _syncErrors.asStateFlow()
    
    /**
     * تفعيل المزامنة التلقائية
     */
    private var autoSyncEnabled = true
    
    // ════════════════════════════════════════════════════════════════════════════
    // Synchronization
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * مزامنة البيانات مع السحابة
     *
     * @param saveData بيانات الحفظ
     * @param slot رقم الخانة
     */
    suspend fun syncToCloud(saveData: SaveData, slot: Int): Boolean {
        if (!NetworkModule.isNetworkAvailable()) {
            addError("No internet connection")
            return false
        }
        
        _isSyncing.value = true
        _syncStatus.value = SyncStatus.SYNCING
        
        return try {
            val serialized = SaveSerializer.serialize(saveData)
            val checksum = calculateChecksum(serialized)
            
            val uploadRequest = UploadSaveRequest(
                slotId = slot,
                saveData = serialized,
                checksum = checksum,
                version = SAVE_VERSION,
                playtime = saveData.playtime,
                lastSaveLocation = saveData.lastLocation
            )
            
            // رفع للسحابة
            val result = NetworkModule.cloudSaveApi.uploadSave(
                token = NetworkModule.getAuthToken() ?: "",
                request = uploadRequest
            )
            
            if (result.isSuccessful) {
                _lastSyncTime.value = System.currentTimeMillis()
                _syncStatus.value = SyncStatus.SYNCED
                _syncErrors.value = emptyList()
                
                Timber.d("Cloud sync successful for slot: $slot")
                true
            } else {
                addError("Upload failed: ${result.code()}")
                _syncStatus.value = SyncStatus.SYNC_FAILED
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Cloud sync error")
            addError("Sync error: ${e.message}")
            _syncStatus.value = SyncStatus.SYNC_FAILED
            false
        } finally {
            _isSyncing.value = false
        }
    }
    
    /**
     * تحميل البيانات من السحابة
     *
     * @param slot رقم الخانة
     */
    suspend fun syncFromCloud(slot: Int): SaveData? {
        if (!NetworkModule.isNetworkAvailable()) {
            addError("No internet connection")
            return null
        }
        
        _isSyncing.value = true
        _syncStatus.value = SyncStatus.SYNCING
        
        return try {
            val result = NetworkModule.cloudSaveApi.downloadSave(
                token = NetworkModule.getAuthToken() ?: "",
                slotId = slot
            )
            
            if (result.isSuccessful && result.body() != null) {
                val cloudData = result.body()?.data
                
                if (cloudData == null) {
                    addError("Cloud data is null")
                    return null
                }
                
                // التحقق من الـ checksum
                if (!verifyChecksum(cloudData.saveData, cloudData.checksum)) {
                    addError("Checksum verification failed")
                    return null
                }
                
                val saveData = SaveSerializer.deserialize(cloudData.saveData)
                _lastSyncTime.value = System.currentTimeMillis()
                _syncStatus.value = SyncStatus.SYNCED
                _syncErrors.value = emptyList()
                
                Timber.d("Cloud sync successful for slot: $slot")
                saveData
            } else {
                addError("Download failed: ${result.code()}")
                _syncStatus.value = SyncStatus.SYNC_FAILED
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Cloud sync error")
            addError("Sync error: ${e.message}")
            _syncStatus.value = SyncStatus.SYNC_FAILED
            null
        } finally {
            _isSyncing.value = false
        }
    }
    
    /**
     * مزامنة ثنائية الاتجاه
     *
     * @param localData البيانات المحلية
     * @param slot رقم الخانة
     */
    suspend fun bidirectionalSync(localData: SaveData, slot: Int): SyncResult {
        if (!NetworkModule.isNetworkAvailable()) {
            return SyncResult.ERROR("No internet connection")
        }
        
        _isSyncing.value = true
        
        return try {
            // الحصول على البيانات من السحابة
            val cloudData = syncFromCloud(slot)
            
            if (cloudData == null) {
                // لا توجد بيانات سحابية، رفع البيانات المحلية
                val uploaded = syncToCloud(localData, slot)
                if (uploaded) {
                    SyncResult.SUCCESS("Uploaded local data")
                } else {
                    SyncResult.ERROR("Failed to upload")
                }
            } else {
                // مقارنة الطوابع الزمنية
                val result = when {
                    localData.timestamp > cloudData.timestamp -> {
                        // البيانات المحلية أحدث
                        if (syncToCloud(localData, slot)) {
                            SyncResult.SUCCESS("Local data is newer, uploaded")
                        } else {
                            SyncResult.ERROR("Failed to upload local data")
                        }
                    }
                    cloudData.timestamp > localData.timestamp -> {
                        // البيانات السحابية أحدث
                        SyncResult.SUCCESS("Cloud data is newer, downloaded")
                    }
                    else -> {
                        // متطابقة
                        SyncResult.SUCCESS("Data synchronized")
                    }
                }
                result
            }
        } catch (e: Exception) {
            Timber.e(e, "Bidirectional sync error")
            SyncResult.ERROR("Sync error: ${e.message}")
        } finally {
            _isSyncing.value = false
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Conflict Resolution
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * حل التضارب بين النسختين
     *
     * @param localData البيانات المحلية
     * @param cloudData البيانات السحابية
     * @param strategy استراتيجية الحل
     */
    fun resolveConflict(
        localData: SaveData,
        cloudData: SaveData,
        strategy: ConflictStrategy = ConflictStrategy.KEEP_NEWER
    ): SaveData {
        return when (strategy) {
            ConflictStrategy.KEEP_LOCAL -> localData
            ConflictStrategy.KEEP_CLOUD -> cloudData
            ConflictStrategy.KEEP_NEWER -> {
                if (localData.timestamp > cloudData.timestamp) localData else cloudData
            }
            ConflictStrategy.MERGE -> mergeData(localData, cloudData)
        }
    }
    
    /**
     * دمج البيانات من النسختين
     *
     * @param localData البيانات المحلية
     * @param cloudData البيانات السحابية
     */
    private fun mergeData(localData: SaveData, cloudData: SaveData): SaveData {
        return SaveData(
            name = localData.name,
            timestamp = System.currentTimeMillis(),
            playtime = maxOf(localData.playtime, cloudData.playtime),
            playerData = localData.playerData, // الحفاظ على البيانات المحلية الأساسية
            worldData = worldDataMerge(localData.worldData, cloudData.worldData),
            questData = questDataMerge(localData.questData, cloudData.questData),
            dialogueData = localData.dialogueData,
            gameSettings = localData.gameSettings,
            statistics = statisticsMerge(localData.statistics, cloudData.statistics),
            inventory = localData.inventory,
            achievements = achievementsMerge(localData.achievements, cloudData.achievements),
            lastLocation = localData.lastLocation,
            playerLevel = maxOf(localData.playerLevel, cloudData.playerLevel),
            gameVersion = localData.gameVersion
        )
    }
    
    /**
     * دمج بيانات العالم
     */
    private fun worldDataMerge(local: WorldData, cloud: WorldData): WorldData {
        return WorldData(
            discoveredLocations = local.discoveredLocations + cloud.discoveredLocations,
            visitedLocations = local.visitedLocations + cloud.visitedLocations,
            activatedObjects = local.activatedObjects + cloud.activatedObjects,
            doorStates = (cloud.doorStates + local.doorStates),
            environmentalState = cloud.environmentalState
        )
    }
    
    /**
     * دمج بيانات المهام
     */
    private fun questDataMerge(local: QuestData, cloud: QuestData): QuestData {
        return QuestData(
            completedQuests = local.completedQuests + cloud.completedQuests,
            activeQuests = (local.activeQuests + cloud.activeQuests).distinct(),
            questProgress = (cloud.questProgress + local.questProgress),
            questHistory = (local.questHistory + cloud.questHistory).distinct()
        )
    }
    
    /**
     * دمج الإحصائيات
     */
    private fun statisticsMerge(local: GameStatistics, cloud: GameStatistics): GameStatistics {
        return GameStatistics(
            totalPlaytime = maxOf(local.totalPlaytime, cloud.totalPlaytime),
            totalDeaths = maxOf(local.totalDeaths, cloud.totalDeaths),
            totalKills = maxOf(local.totalKills, cloud.totalKills),
            bossesDefeated = (local.bossesDefeated + cloud.bossesDefeated).distinct(),
            areasExplored = (local.areasExplored + cloud.areasExplored).distinct(),
            secretsFound = maxOf(local.secretsFound, cloud.secretsFound),
            enemiesDefeated = maxOf(local.enemiesDefeated, cloud.enemiesDefeated),
            itemsCollected = maxOf(local.itemsCollected, cloud.itemsCollected)
        )
    }
    
    /**
     * دمج الإنجازات
     */
    private fun achievementsMerge(local: AchievementsData, cloud: AchievementsData): AchievementsData {
        return AchievementsData(
            unlockedAchievements = local.unlockedAchievements + cloud.unlockedAchievements,
            achievementProgress = (cloud.achievementProgress + local.achievementProgress),
            totalPoints = maxOf(local.totalPoints, cloud.totalPoints)
        )
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Checksum & Verification
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * حساب الـ checksum
     *
     * @param data البيانات
     */
    private fun calculateChecksum(data: String): String {
        return data.hashCode().toString()
    }
    
    /**
     * التحقق من الـ checksum
     *
     * @param data البيانات
     * @param checksum الـ checksum
     */
    private fun verifyChecksum(data: String, checksum: String): Boolean {
        return calculateChecksum(data) == checksum
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Error Management
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * إضافة خطأ
     *
     * @param error رسالة الخطأ
     */
    private fun addError(error: String) {
        _syncErrors.value = _syncErrors.value + error
        Timber.e(error)
    }
    
    /**
     * مسح الأخطاء
     */
    fun clearErrors() {
        _syncErrors.value = emptyList()
    }
    
    companion object {
        const val SAVE_VERSION = 1
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// Enums and Data Classes
// ════════════════════════════════════════════════════════════════════════════════

/**
 * حالة المزامنة
 */
enum class SyncStatus {
    IDLE,
    SYNCING,
    SYNCED,
    SYNC_FAILED,
    CONFLICT_DETECTED
}

/**
 * استراتيجية حل التضارب
 */
enum class ConflictStrategy {
    KEEP_LOCAL,
    KEEP_CLOUD,
    KEEP_NEWER,
    MERGE
}

/**
 * نتيجة المزامنة
 */
sealed class SyncResult {
    data class SUCCESS(val message: String) : SyncResult()
    data class ERROR(val message: String) : SyncResult()
    data class CONFLICT(val message: String) : SyncResult()
}