package com.erygra.maskoflight.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erygra.maskoflight.core.GameConfig
import com.erygra.maskoflight.core.GameState
import com.erygra.maskoflight.core.EventBus
import com.erygra.maskoflight.data.repository.*
import com.erygra.maskoflight.engine.*
import com.erygra.maskoflight.player.InventorySystem
import com.erygra.maskoflight.player.SkillTree
import com.erygra.maskoflight.quest.QuestSystem
import com.erygra.maskoflight.dialogue.DialogueSystem
import com.erygra.maskoflight.save.SaveManager
import com.erygra.maskoflight.save.AutoSaveSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * GameViewModel - ViewModel الرئيسي للعبة
 * 
 * يدير:
 * - حالة اللعبة العامة
 * - التواصل بين الـ UI والأنظمة
 * - المحركات المختلفة
 * - الحفظ والتحميل
 * 
 * Scope: Activity (يعيش طول عمر Activity)
 */
@HiltViewModel
class GameViewModel @Inject constructor(
    // Repositories
    private val playerRepository: PlayerRepository,
    private val questRepository: QuestRepository,
    private val inventoryRepository: InventoryRepository,
    private val worldRepository: WorldRepository,

    // Engines
    private val physicsEngine: PhysicsEngine,
    private val combatEngine: CombatEngine,
    private val particleEngine: ParticleEngine,
    private val audioEngine: AudioEngine,
    private val eventBus: EventBus,

    // Game Systems
    private val inventorySystem: InventorySystem,
    private val skillTree: SkillTree,
    private val questSystem: QuestSystem,
    private val dialogueSystem: DialogueSystem,

    // Save Systems
    private val saveManager: SaveManager,
    private val autoSaveSystem: AutoSaveSystem
) : ViewModel() {

    companion object {
        private const val TAG = "GameViewModel"
        private const val AUTO_SAVE_INTERVAL = 300000L // 5 دقائق
    }

    // ====================================================================
    // 🎮 حالة اللعبة (Game State)
    // ====================================================================

    /** حالة اللعبة الحالية */
    private val _gameState = MutableStateFlow(GameState.MENU)
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    /** هل تم تهيئة اللعبة */
    private val _isGameInitialized = MutableStateFlow(false)
    val isGameInitialized: StateFlow<Boolean> = _isGameInitialized.asStateFlow()

    /** الشاشة الحالية */
    private val _currentScreen = MutableStateFlow("main_menu")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    /** حالة التطبيق */
    private val _appState = MutableStateFlow("initialized")
    val appState: StateFlow<String> = _appState.asStateFlow()

    /** أحداث اللعبة */
    private val _gameEvents = MutableStateFlow<String>("")
    val gameEvents = _gameEvents

    // ====================================================================
    // 👤 بيانات اللاعب (Player Data)
    // ====================================================================

    /** صحة اللاعب */
    private val _playerHealth = MutableStateFlow(100)
    val playerHealth: StateFlow<Int> = _playerHealth.asStateFlow()

    /** طاقة اللاعب */
    private val _playerMana = MutableStateFlow(100)
    val playerMana: StateFlow<Int> = _playerMana.asStateFlow()

    /** مستوى اللاعب */
    private val _playerLevel = MutableStateFlow(1)
    val playerLevel: StateFlow<Int> = _playerLevel.asStateFlow()

    /** تجربة اللاعب */
    private val _playerExperience = MutableStateFlow(0)
    val playerExperience: StateFlow<Int> = _playerExperience.asStateFlow()

    // ====================================================================
    // 🎯 الأهداف والمهام (Quests & Objectives)
    // ====================================================================

    /** قائمة المهام النشطة */
    private val _activeQuests = MutableStateFlow<List<String>>(emptyList())
    val activeQuests: StateFlow<List<String>> = _activeQuests.asStateFlow()

    /** المهام المكتملة */
    private val _completedQuests = MutableStateFlow<List<String>>(emptyList())
    val completedQuests: StateFlow<List<String>> = _completedQuests.asStateFlow()

    // ====================================================================
    // 🗺️ بيانات العالم (World Data)
    // ====================================================================

    /** المنطقة الحالية */
    private val _currentRegion = MutableStateFlow("starting_region")
    val currentRegion: StateFlow<String> = _currentRegion.asStateFlow()

    /** مستوى العالم الحالي */
    private val _currentLevel = MutableStateFlow(1)
    val currentLevel: StateFlow<Int> = _currentLevel.asStateFlow()

    /** موقع اللاعب */
    private val _playerPosition = MutableStateFlow(Pair(0f, 0f))
    val playerPosition: StateFlow<Pair<Float, Float>> = _playerPosition.asStateFlow()

    // ====================================================================
    // 🎬 الإشعارات والحوارات (Notifications & Dialogs)
    // ====================================================================

    /** نص الإشعار */
    private val _notificationText = MutableStateFlow("")
    val notificationText: StateFlow<String> = _notificationText.asStateFlow()

    /** هل يوجد حوار مفتوح */
    private val _hasActiveDialog = MutableStateFlow(false)
    val hasActiveDialog: StateFlow<Boolean> = _hasActiveDialog.asStateFlow()

    // ====================================================================
    // ⚙️ التهيئة (Initialization)
    // ====================================================================

    /**
     * setGameConfig - تعيين إعدادات اللعبة
     * 
     * @param config إعدادات اللعبة
     */
    fun setGameConfig(config: GameConfig) {
        // تطبيق الإعدادات على جميع الأنظمة
        audioEngine.setVolume(config.masterVolume)
        // إعدادات أخرى...
    }

    /**
     * initializeGameEngines - تهيئة محركات اللعبة
     */
    fun initializeGameEngines() {
        viewModelScope.launch {
            try {
                physicsEngine.initialize()
                combatEngine.initialize()
                particleEngine.initialize()
                audioEngine.initialize()
                questSystem.initialize()
                dialogueSystem.initialize()
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    /**
     * setGameInitialized - تحديد أن اللعبة تمت تهيئتها
     * 
     * @param initialized هل تمت التهيئة
     */
    fun setGameInitialized(initialized: Boolean) {
        _isGameInitialized.value = initialized
    }

    /**
     * restoreGameState - استعادة حالة اللعبة من الحفظ
     * 
     * @param gameData بيانات اللعبة المحفوظة
     */
    fun restoreGameState(gameData: Map<String, Any>) {
        viewModelScope.launch {
            try {
                // استعادة بيانات اللاعب
                _playerHealth.value = (gameData["health"] as? Int) ?: 100
                _playerMana.value = (gameData["mana"] as? Int) ?: 100
                _playerLevel.value = (gameData["level"] as? Int) ?: 1
                _playerExperience.value = (gameData["experience"] as? Int) ?: 0

                // استعادة بيانات العالم
                _currentRegion.value = (gameData["region"] as? String) ?: "starting_region"
                _currentLevel.value = (gameData["levelNumber"] as? Int) ?: 1

                // استعادة المهام
                @Suppress("UNCHECKED_CAST")
                val quests = (gameData["activeQuests"] as? List<String>) ?: emptyList()
                _activeQuests.value = quests

                _gameState.value = GameState.GAMEPLAY
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    // ====================================================================
    // 🎮 التحكم الأساسي (Game Control)
    // ====================================================================

    /**
     * startNewGame - بدء لعبة جديدة
     */
    fun startNewGame() {
        viewModelScope.launch {
            try {
                // إعادة تعيين جميع البيانات
                _playerHealth.value = 100
                _playerMana.value = 100
                _playerLevel.value = 1
                _playerExperience.value = 0
                _currentRegion.value = "starting_region"
                _currentLevel.value = 1
                _activeQuests.value = emptyList()
                _completedQuests.value = emptyList()

                // بدء المستوى الأول
                _gameState.value = GameState.GAMEPLAY
                _currentScreen.value = "game_screen"

                emitGameEvent("NEW_GAME_STARTED")
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    /**
     * resumeGame - استئناف لعبة محفوظة
     */
    fun resumeGame() {
        viewModelScope.launch {
            _gameState.value = GameState.GAMEPLAY
            audioEngine.resume()
            emitGameEvent("GAME_RESUMED")
        }
    }

    /**
     * pauseGame - إيقاف اللعبة مؤقتاً
     */
    fun pauseGame() {
        viewModelScope.launch {
            _gameState.value = GameState.PAUSED
            audioEngine.pause()
            _currentScreen.value = "pause_menu"
            emitGameEvent("GAME_PAUSED")
        }
    }

    /**
     * quitToMainMenu - الخروج للقائمة الرئيسية
     */
    fun quitToMainMenu() {
        viewModelScope.launch {
            saveManager.autoSave()
            _gameState.value = GameState.MENU
            _currentScreen.value = "main_menu"
            emitGameEvent("QUIT_TO_MENU")
        }
    }

    /**
     * exitGame - الخروج من اللعبة
     */
    fun exitGame() {
        viewModelScope.launch {
            try {
                saveManager.autoSave()
                cleanup()
                emitGameEvent("GAME_EXITED")
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    // ====================================================================
    // 💥 معالجات الأحداث الخاصة (Special Event Handlers)
    // ====================================================================

    /**
     * handleGameOver - معالجة نهاية اللعبة
     */
    fun handleGameOver() {
        viewModelScope.launch {
            _gameState.value = GameState.GAME_OVER
            _currentScreen.value = "game_over_screen"
            saveManager.autoSave()
            emitGameEvent("GAME_OVER_EVENT")
        }
    }

    /**
     * handleLevelComplete - معالجة اكتمال المستوى
     */
    fun handleLevelComplete() {
        viewModelScope.launch {
            _gameState.value = GameState.LEVEL_COMPLETE
            _currentScreen.value = "level_complete_screen"
            
            // إضافة مكافآت
            _playerExperience.value += 1000
            
            saveManager.autoSave()
            emitGameEvent("LEVEL_COMPLETE_EVENT")
        }
    }

    /**
     * handlePlayerDeath - معالجة موت اللاعب
     */
    fun handlePlayerDeath() {
        viewModelScope.launch {
            _playerHealth.value = 0
            saveManager.createAutoSavePoint()
            emitGameEvent("PLAYER_DIED_EVENT")
        }
    }

    /**
     * restartLevel - إعادة تشغيل المستوى الحالي
     */
    fun restartLevel() {
        viewModelScope.launch {
            _gameState.value = GameState.GAMEPLAY
            // إعادة تحميل المستوى
            emitGameEvent("LEVEL_RESTARTED")
        }
    }

    /**
     * loadNextLevel - تحميل المستوى التالي
     */
    fun loadNextLevel() {
        viewModelScope.launch {
            _currentLevel.value += 1
            _gameState.value = GameState.GAMEPLAY
            emitGameEvent("NEXT_LEVEL_LOADED")
        }
    }

    // ====================================================================
    // 🔊 إدارة الصوت والموسيقى (Audio Management)
    // ====================================================================

    /**
     * resumeAudio - استئناف الصوت والموسيقى
     */
    fun resumeAudio() {
        audioEngine.resume()
    }

    /**
     * pauseAudio - إيقاف الصوت والموسيقى مؤقتاً
     */
    fun pauseAudio() {
        audioEngine.pause()
    }

    // ====================================================================
    // 📢 الإشعارات والرسائل (Notifications & Messages)
    // ====================================================================

    /**
     * showSaveNotification - عرض إشعار الحفظ
     * 
     * @param message الرسالة
     */
    fun showSaveNotification(message: String) {
        _notificationText.value = message
        // سيتم إخفاء الإشعار تلقائياً بعد وقت
    }

    /**
     * showErrorDialog - عرض حوار الخطأ
     * 
     * @param title عنوان الخطأ
     * @param message رسالة الخطأ
     * @param onDismiss callback عند الإغلاق
     */
    fun showErrorDialog(
        title: String,
        message: String,
        onDismiss: () -> Unit = {}
    ) {
        _hasActiveDialog.value = true
        _notificationText.value = "$title: $message"
    }

    /**
     * showGameOverScreen - عرض شاشة نهاية اللعبة
     */
    fun showGameOverScreen() {
        _currentScreen.value = "game_over_screen"
    }

    /**
     * showLevelCompleteDialog - عرض حوار اكتمال المستوى
     */
    fun showLevelCompleteDialog() {
        _currentScreen.value = "level_complete_screen"
    }

    /**
     * showDeathScreen - عرض شاشة الموت
     */
    fun showDeathScreen() {
        _currentScreen.value = "death_screen"
    }

    // ====================================================================
    // 🧹 التنظيف (Cleanup)
    // ====================================================================

    /**
     * cleanup - تنظيف جميع الموارد
     */
    fun cleanup() {
        try {
            physicsEngine.cleanup()
            combatEngine.cleanup()
            particleEngine.cleanup()
            audioEngine.cleanup()
            questSystem.cleanup()
            dialogueSystem.cleanup()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error during cleanup", e)
        }
    }

    // ====================================================================
    // 🔧 دوال مساعدة (Helper Functions)
    // ====================================================================

    /**
     * emitGameEvent - بث حدث لعبة
     * 
     * @param event وصف الحدث
     */
    private fun emitGameEvent(event: String) {
        _gameEvents.value = event
    }

    /**
     * handleError - معالجة الأخطاء العامة
     * 
     * @param exception الاستثناء
     */
    private fun handleError(exception: Exception) {
        android.util.Log.e(TAG, "Error in GameViewModel", exception)
        showErrorDialog(
            title = "حدث خطأ",
            message = exception.message ?: "خطأ غير معروف"
        )
    }

    override fun onCleared() {
        super.onCleared()
        cleanup()
    }
}