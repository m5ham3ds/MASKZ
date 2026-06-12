package com.erygra.maskoflight.save

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * ════════════════════════════════════════════════════════════════════════════════
 * SaveManager.kt - مدير الحفظ الرئيسي
 * ════════════════════════════════════════════════════════════════════════════════
 * 
 * الوصف:
 * - إدارة حفظ وتحميل البيانات
 * - دعم عدة خانات حفظ
 * - نظام النسخ الاحتياطية
 * - مزامنة البيانات
 * 
 * المكونات الرئيسية:
 * - Save/Load operations
 * - Multiple save slots
 * - Backup system
 * - Data synchronization
 * - File management
 * 
 * @author Erygra Team
 * @since 2.0.0
 * ════════════════════════════════════════════════════════════════════════════════
 */

class SaveManager(
    private val context: Context
) {
    
    // ════════════════════════════════════════════════════════════════════════════
    // Properties
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * مجلد الحفظ
     */
    private val saveDirectory = File(context.filesDir, "saves")
    
    /**
     * مجلد النسخ الاحتياطية
     */
    private val backupDirectory = File(context.filesDir, "backups")
    
    /**
     * البيانات المحملة حالياً
     */
    private val _currentSaveData = MutableStateFlow<SaveData?>(null)
    val currentSaveData: StateFlow<SaveData?> = _currentSaveData.asStateFlow()
    
    /**
     * الخانة الحالية
     */
    private val _currentSlot = MutableStateFlow(-1)
    val currentSlot: StateFlow<Int> = _currentSlot.asStateFlow()
    
    /**
     * قائمة الحفوظات المتاحة
     */
    private val _availableSaves = MutableStateFlow<List<SaveInfo>>(emptyList())
    val availableSaves: StateFlow<List<SaveInfo>> = _availableSaves.asStateFlow()
    
    /**
     * حالة الحفظ
     */
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()
    
    /**
     * حالة التحميل
     */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    /**
     * آخر رسالة خطأ
     */
    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()
    
    // ════════════════════════════════════════════════════════════════════════════
    // Initialization
    // ════════════════════════════════════════════════════════════════════════════
    
    init {
        initializeDirectories()
        refreshAvailableSaves()
    }
    
    /**
     * تهيئة المجلدات
     */
    private fun initializeDirectories() {
        if (!saveDirectory.exists()) {
            saveDirectory.mkdirs()
            Timber.d("Save directory created")
        }
        
        if (!backupDirectory.exists()) {
            backupDirectory.mkdirs()
            Timber.d("Backup directory created")
        }
    }
    
    /**
     * تحديث قائمة الحفوظات المتاحة
     */
    fun refreshAvailableSaves() {
        try {
            val saves = mutableListOf<SaveInfo>()
            
            for (slot in 0 until MAX_SAVE_SLOTS) {
                val slotFile = getSaveFile(slot)
                if (slotFile.exists()) {
                    val metadata = readSaveMetadata(slot)
                    if (metadata != null) {
                        saves.add(metadata)
                    }
                }
            }
            
            _availableSaves.value = saves
            Timber.d("Available saves refreshed: ${saves.size}")
        } catch (e: Exception) {
            Timber.e(e, "Error refreshing available saves")
            _lastError.value = "Failed to refresh saves"
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Save Operations
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * حفظ البيانات في خانة محددة
     *
     * @param slot رقم الخانة
     * @param data البيانات
     */
    suspend fun saveGame(slot: Int, data: SaveData): Boolean {
        if (slot !in 0 until MAX_SAVE_SLOTS) {
            _lastError.value = "Invalid save slot: $slot"
            return false
        }
        
        _isSaving.value = true
        
        return try {
            // إنشاء نسخة احتياطية من الحفظ القديم
            createBackup(slot)
            
            // حفظ البيانات الجديدة
            val saveFile = getSaveFile(slot)
            FileOutputStream(saveFile).use { fos ->
                ObjectOutputStream(fos).use { oos ->
                    oos.writeObject(data)
                }
            }
            
            _currentSlot.value = slot
            _currentSaveData.value = data
            
            refreshAvailableSaves()
            _lastError.value = null
            
            Timber.d("Game saved to slot: $slot")
            true
        } catch (e: Exception) {
            Timber.e(e, "Error saving game")
            _lastError.value = "Failed to save game: ${e.message}"
            false
        } finally {
            _isSaving.value = false
        }
    }
    
    /**
     * تحميل البيانات من خانة محددة
     *
     * @param slot رقم الخانة
     */
    suspend fun loadGame(slot: Int): SaveData? {
        if (slot !in 0 until MAX_SAVE_SLOTS) {
            _lastError.value = "Invalid save slot: $slot"
            return null
        }
        
        _isLoading.value = true
        
        return try {
            val saveFile = getSaveFile(slot)
            if (!saveFile.exists()) {
                _lastError.value = "Save file not found"
                return null
            }
            
            val data: SaveData = FileInputStream(saveFile).use { fis ->
                ObjectInputStream(fis).use { ois ->
                    ois.readObject() as SaveData
                }
            }
            
            _currentSlot.value = slot
            _currentSaveData.value = data
            _lastError.value = null
            
            Timber.d("Game loaded from slot: $slot")
            data
        } catch (e: Exception) {
            Timber.e(e, "Error loading game")
            _lastError.value = "Failed to load game: ${e.message}"
            null
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * الحصول على أحدث حفظ
     */
    suspend fun loadLatestSave(): SaveData? {
        val latest = _availableSaves.value.maxByOrNull { it.timestamp }
        return if (latest != null) {
            loadGame(latest.slot)
        } else {
            null
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Save Slot Management
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * حذف حفظ من خانة محددة
     *
     * @param slot رقم الخانة
     */
    fun deleteSave(slot: Int): Boolean {
        return try {
            val saveFile = getSaveFile(slot)
            if (saveFile.exists()) {
                saveFile.delete()
                refreshAvailableSaves()
                _lastError.value = null
                Timber.d("Save slot deleted: $slot")
                true
            } else {
                _lastError.value = "Save slot is empty"
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Error deleting save")
            _lastError.value = "Failed to delete save"
            false
        }
    }
    
    /**
     * نسخ حفظ من خانة لأخرى
     *
     * @param fromSlot الخانة المصدر
     * @param toSlot الخانة الهدف
     */
    fun copySave(fromSlot: Int, toSlot: Int): Boolean {
        if (fromSlot !in 0 until MAX_SAVE_SLOTS || toSlot !in 0 until MAX_SAVE_SLOTS) {
            _lastError.value = "Invalid save slots"
            return false
        }
        
        return try {
            val fromFile = getSaveFile(fromSlot)
            val toFile = getSaveFile(toSlot)
            
            if (!fromFile.exists()) {
                _lastError.value = "Source save not found"
                return false
            }
            
            fromFile.copyTo(toFile, overwrite = true)
            refreshAvailableSaves()
            _lastError.value = null
            
            Timber.d("Save copied from slot $fromSlot to $toSlot")
            true
        } catch (e: Exception) {
            Timber.e(e, "Error copying save")
            _lastError.value = "Failed to copy save"
            false
        }
    }
    
    /**
     * الحصول على معلومات حفظ محدد
     *
     * @param slot رقم الخانة
     */
    fun getSaveInfo(slot: Int): SaveInfo? {
        return _availableSaves.value.find { it.slot == slot }
    }
    
    /**
     * هل الخانة تحتوي على حفظ
     *
     * @param slot رقم الخانة
     */
    fun hasSave(slot: Int): Boolean {
        return getSaveInfo(slot) != null
    }
    
    /**
     * إعادة تسمية حفظ
     *
     * @param slot رقم الخانة
     * @param newName الاسم الجديد
     */
    fun renameSave(slot: Int, newName: String): Boolean {
        return try {
            val saveFile = getSaveFile(slot)
            if (!saveFile.exists()) {
                _lastError.value = "Save not found"
                return false
            }
            
            // قراءة البيانات وإعادة الكتابة مع الاسم الجديد
            val data = FileInputStream(saveFile).use { fis ->
                ObjectInputStream(fis).use { ois ->
                    ois.readObject() as SaveData
                }
            }
            
            val updatedData = data.copy(name = newName)
            
            FileOutputStream(saveFile).use { fos ->
                ObjectOutputStream(fos).use { oos ->
                    oos.writeObject(updatedData)
                }
            }
            
            refreshAvailableSaves()
            _lastError.value = null
            
            Timber.d("Save renamed to: $newName")
            true
        } catch (e: Exception) {
            Timber.e(e, "Error renaming save")
            _lastError.value = "Failed to rename save"
            false
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Backup Operations
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * إنشاء نسخة احتياطية
     *
     * @param slot رقم الخانة
     */
    private fun createBackup(slot: Int) {
        try {
            val saveFile = getSaveFile(slot)
            if (saveFile.exists()) {
                val timestamp = System.currentTimeMillis()
                val backupFile = File(backupDirectory, "save_${slot}_${timestamp}.bak")
                
                saveFile.copyTo(backupFile)
                
                // حذف النسخ القديمة جداً
                cleanOldBackups()
                
                Timber.d("Backup created: ${backupFile.name}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error creating backup")
        }
    }
    
    /**
     * حذف النسخ الاحتياطية القديمة
     */
    private fun cleanOldBackups() {
        try {
            val backups = backupDirectory.listFiles() ?: return
            
            // احتفظ بآخر MAX_BACKUPS_PER_SLOT نسخة لكل خانة
            val groupedBackups = backups.groupBy { file ->
                file.name.substringAfter("save_").substringBefore("_")
            }
            
            groupedBackups.forEach { (_, backupFiles) ->
                if (backupFiles.size > MAX_BACKUPS_PER_SLOT) {
                    backupFiles
                        .sortedBy { it.lastModified() }
                        .take(backupFiles.size - MAX_BACKUPS_PER_SLOT)
                        .forEach { it.delete() }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error cleaning old backups")
        }
    }
    
    /**
     * الحصول على قائمة النسخ الاحتياطية
     *
     * @param slot رقم الخانة
     */
    fun getBackups(slot: Int): List<File> {
        return try {
            val backups = backupDirectory.listFiles() ?: emptyArray()
            backups
                .filter { it.name.startsWith("save_${slot}_") }
                .sortedByDescending { it.lastModified() }
        } catch (e: Exception) {
            Timber.e(e, "Error getting backups")
            emptyList()
        }
    }
    
    /**
     * استعادة من نسخة احتياطية
     *
     * @param slot رقم الخانة
     * @param backupFile ملف النسخة الاحتياطية
     */
    fun restoreBackup(slot: Int, backupFile: File): Boolean {
        return try {
            val saveFile = getSaveFile(slot)
            
            backupFile.copyTo(saveFile, overwrite = true)
            refreshAvailableSaves()
            _lastError.value = null
            
            Timber.d("Backup restored for slot: $slot")
            true
        } catch (e: Exception) {
            Timber.e(e, "Error restoring backup")
            _lastError.value = "Failed to restore backup"
            false
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // File Management
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * الحصول على ملف الحفظ
     *
     * @param slot رقم الخانة
     */
    private fun getSaveFile(slot: Int): File {
        return File(saveDirectory, "save_$slot.dat")
    }
    
    /**
     * قراءة بيانات الحفظ (Metadata فقط)
     *
     * @param slot رقم الخانة
     */
    private fun readSaveMetadata(slot: Int): SaveInfo? {
        return try {
            val saveFile = getSaveFile(slot)
            if (!saveFile.exists()) return null
            
            val data: SaveData = FileInputStream(saveFile).use { fis ->
                ObjectInputStream(fis).use { ois ->
                    ois.readObject() as SaveData
                }
            }
            
            SaveInfo(
                slot = slot,
                name = data.name,
                level = data.playerLevel,
                location = data.lastLocation,
                playtime = data.playtime,
                timestamp = data.timestamp
            )
        } catch (e: Exception) {
            Timber.e(e, "Error reading save metadata")
            null
        }
    }
    
    /**
     * الحصول على حجم الحفظ
     *
     * @param slot رقم الخانة
     */
    fun getSaveSize(slot: Int): Long {
        return try {
            val saveFile = getSaveFile(slot)
            if (saveFile.exists()) saveFile.length() else 0L
        } catch (e: Exception) {
            Timber.e(e, "Error getting save size")
            0L
        }
    }
    
    /**
     * الحصول على إجمالي حجم الحفوظات
     */
    fun getTotalSaveSize(): Long {
        return (0 until MAX_SAVE_SLOTS).sumOf { getSaveSize(it) }
    }
    
    /**
     * حذف جميع الحفوظات
     */
    fun deleteAllSaves(): Boolean {
        return try {
            val files = saveDirectory.listFiles() ?: emptyArray()
            files.forEach { it.delete() }
            
            refreshAvailableSaves()
            _lastError.value = null
            
            Timber.d("All saves deleted")
            true
        } catch (e: Exception) {
            Timber.e(e, "Error deleting all saves")
            _lastError.value = "Failed to delete saves"
            false
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Cleanup
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * تنظيف الموارد
     */
    fun cleanup() {
        _currentSaveData.value = null
        _currentSlot.value = -1
        Timber.d("SaveManager cleaned up")
    }
    
    companion object {
        /**
         * الحد الأقصى لخانات الحفظ
         */
        const val MAX_SAVE_SLOTS = 3
        
        /**
         * الحد الأقصى للنسخ الاحتياطية لكل خانة
         */
        const val MAX_BACKUPS_PER_SLOT = 5
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// Data Classes
// ════════════════════════════════════════════════════════════════════════════════

/**
 * معلومات الحفظ
 *
 * @property slot رقم الخانة
 * @property name اسم الحفظ
 * @property level مستوى اللاعب
 * @property location آخر موقع
 * @property playtime وقت اللعب
 * @property timestamp وقت الحفظ
 */
data class SaveInfo(
    val slot: Int,
    val name: String,
    val level: Int,
    val location: String,
    val playtime: Long,
    val timestamp: Long
)