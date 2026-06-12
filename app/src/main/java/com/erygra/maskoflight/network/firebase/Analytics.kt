package com.erygra.maskoflight.network.firebase

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import timber.log.Timber

/**
 * ════════════════════════════════════════════════════════════════════════════════
 * Analytics.kt - مدير Firebase Analytics
 * ════════════════════════════════════════════════════════════════════════════════
 * 
 * الوصف:
 * - إدارة جميع أحداث التحليلات
 * - تتبع سلوك المستخدم
 * - إحصائيات اللعب والتفاعل
 * 
 * المكونات الرئيسية:
 * - User events tracking
 * - Game events tracking
 * - Custom events
 * - User properties
 * 
 * @author Erygra Team
 * @since 2.0.0
 * ════════════════════════════════════════════════════════════════════════════════
 */

class Analytics(
    private val context: Context
) {
    
    // ════════════════════════════════════════════════════════════════════════════
    // Properties
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Firebase Analytics instance
     */
    private val analytics: FirebaseAnalytics by lazy {
        Firebase.analytics
    }
    
    /**
     * تفعيل/تعطيل جمع البيانات
     */
    var isEnabled: Boolean
        get() = analytics.appInstanceId != null
        set(value) {
            analytics.setAnalyticsCollectionEnabled(value)
        }
    
    // ════════════════════════════════════════════════════════════════════════════
    // User Properties
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * تعيين معرف المستخدم
     *
     * @param userId معرف المستخدم
     */
    fun setUserId(userId: String) {
        analytics.setUserId(userId)
        Timber.d("User ID set: $userId")
    }
    
    /**
     * تعيين خاصية مستخدم
     *
     * @param name اسم الخاصية
     * @param value القيمة
     */
    fun setUserProperty(name: String, value: String) {
        analytics.setUserProperty(name, value)
        Timber.d("User property set: $name = $value")
    }
    
    /**
     * تعيين مستوى اللاعب
     *
     * @param level المستوى
     */
    fun setPlayerLevel(level: Int) {
        setUserProperty(PROPERTY_PLAYER_LEVEL, level.toString())
    }
    
    /**
     * تعيين نوع اللاعب
     *
     * @param type النوع (casual, hardcore, speedrunner, etc.)
     */
    fun setPlayerType(type: String) {
        setUserProperty(PROPERTY_PLAYER_TYPE, type)
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // General Events
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * تسجيل حدث مخصص
     *
     * @param eventName اسم الحدث
     * @param params معاملات الحدث
     */
    fun logEvent(eventName: String, params: Map<String, Any> = emptyMap()) {
        analytics.logEvent(eventName) {
            params.forEach { (key, value) ->
                when (value) {
                    is String -> param(key, value)
                    is Long -> param(key, value)
                    is Int -> param(key, value.toLong())
                    is Double -> param(key, value)
                    is Boolean -> param(key, if (value) 1L else 0L)
                    else -> param(key, value.toString())
                }
            }
        }
        Timber.d("Event logged: $eventName")
    }
    
    /**
     * تسجيل شاشة
     *
     * @param screenName اسم الشاشة
     * @param screenClass فئة الشاشة
     */
    fun logScreenView(screenName: String, screenClass: String) {
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            param(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
        }
        Timber.d("Screen view: $screenName")
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Game Events
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * بدء جلسة لعب
     *
     * @param saveSlot رقم الخانة
     */
    fun logGameStart(saveSlot: Int) {
        logEvent(EVENT_GAME_START, mapOf(
            PARAM_SAVE_SLOT to saveSlot
        ))
    }
    
    /**
     * انتهاء جلسة لعب
     *
     * @param duration مدة الجلسة (ثواني)
     * @param saveSlot رقم الخانة
     */
    fun logGameEnd(duration: Long, saveSlot: Int) {
        logEvent(EVENT_GAME_END, mapOf(
            PARAM_DURATION to duration,
            PARAM_SAVE_SLOT to saveSlot
        ))
    }
    
    /**
     * رفع مستوى
     *
     * @param newLevel المستوى الجديد
     * @param character الشخصية
     */
    fun logLevelUp(newLevel: Int, character: String) {
        analytics.logEvent(FirebaseAnalytics.Event.LEVEL_UP) {
            param(FirebaseAnalytics.Param.LEVEL, newLevel.toLong())
            param(FirebaseAnalytics.Param.CHARACTER, character)
        }
        Timber.d("Level up: $newLevel")
    }
    
    /**
     * إتمام مستوى/منطقة
     *
     * @param levelName اسم المستوى
     * @param success هل نجح
     * @param score النقاط
     */
    fun logLevelComplete(levelName: String, success: Boolean, score: Long = 0) {
        analytics.logEvent(FirebaseAnalytics.Event.LEVEL_END) {
            param(FirebaseAnalytics.Param.LEVEL_NAME, levelName)
            param(FirebaseAnalytics.Param.SUCCESS, if (success) 1L else 0L)
            if (score > 0) param(FirebaseAnalytics.Param.SCORE, score)
        }
        Timber.d("Level complete: $levelName (success: $success)")
    }
    
    /**
     * هزيمة زعيم
     *
     * @param bossName اسم الزعيم
     * @param attempts عدد المحاولات
     * @param duration مدة المعركة (ثواني)
     */
    fun logBossDefeat(bossName: String, attempts: Int, duration: Long) {
        logEvent(EVENT_BOSS_DEFEAT, mapOf(
            PARAM_BOSS_NAME to bossName,
            PARAM_ATTEMPTS to attempts,
            PARAM_DURATION to duration
        ))
    }
    
    /**
     * موت اللاعب
     *
     * @param location الموقع
     * @param cause السبب
     */
    fun logPlayerDeath(location: String, cause: String) {
        logEvent(EVENT_PLAYER_DEATH, mapOf(
            PARAM_LOCATION to location,
            PARAM_CAUSE to cause
        ))
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Item Events
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * الحصول على شيء
     *
     * @param itemId معرف الشيء
     * @param itemName اسم الشيء
     * @param itemCategory الفئة
     * @param location موقع الحصول عليه
     */
    fun logItemObtained(
        itemId: String,
        itemName: String,
        itemCategory: String,
        location: String
    ) {
        analytics.logEvent(FirebaseAnalytics.Event.OBTAIN_ITEM) {
            param(FirebaseAnalytics.Param.ITEM_ID, itemId)
            param(FirebaseAnalytics.Param.ITEM_NAME, itemName)
            param(FirebaseAnalytics.Param.ITEM_CATEGORY, itemCategory)
            param(PARAM_LOCATION, location)
        }
        Timber.d("Item obtained: $itemName")
    }
    
    /**
     * استخدام شيء
     *
     * @param itemId معرف الشيء
     * @param itemName اسم الشيء
     */
    fun logItemUsed(itemId: String, itemName: String) {
        logEvent(EVENT_ITEM_USED, mapOf(
            FirebaseAnalytics.Param.ITEM_ID to itemId,
            FirebaseAnalytics.Param.ITEM_NAME to itemName
        ))
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Achievement Events
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * فتح إنجاز
     *
     * @param achievementId معرف الإنجاز
     * @param achievementName اسم الإنجاز
     */
    fun logAchievementUnlocked(achievementId: String, achievementName: String) {
        analytics.logEvent(FirebaseAnalytics.Event.UNLOCK_ACHIEVEMENT) {
            param(FirebaseAnalytics.Param.ACHIEVEMENT_ID, achievementId)
            param(PARAM_ACHIEVEMENT_NAME, achievementName)
        }
        Timber.d("Achievement unlocked: $achievementName")
    }
    
    /**
     * إتمام مهمة
     *
     * @param questId معرف المهمة
     * @param questName اسم المهمة
     * @param reward المكافأة
     */
    fun logQuestComplete(questId: String, questName: String, reward: String) {
        logEvent(EVENT_QUEST_COMPLETE, mapOf(
            PARAM_QUEST_ID to questId,
            PARAM_QUEST_NAME to questName,
            PARAM_REWARD to reward
        ))
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Social Events
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * رفع نتيجة للوحة المتصدرين
     *
     * @param leaderboardType نوع اللوحة
     * @param score النقاط
     * @param rank الترتيب
     */
    fun logLeaderboardSubmit(leaderboardType: String, score: Long, rank: Int) {
        analytics.logEvent(FirebaseAnalytics.Event.POST_SCORE) {
            param(PARAM_LEADERBOARD_TYPE, leaderboardType)
            param(FirebaseAnalytics.Param.SCORE, score)
            param(PARAM_RANK, rank.toLong())
        }
        Timber.d("Leaderboard submit: $leaderboardType = $score")
    }
    
    /**
     * مشاركة إنجاز
     *
     * @param contentType نوع المحتوى
     * @param method طريقة المشاركة
     */
    fun logShare(contentType: String, method: String) {
        analytics.logEvent(FirebaseAnalytics.Event.SHARE) {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, contentType)
            param(FirebaseAnalytics.Param.METHOD, method)
        }
        Timber.d("Share: $contentType via $method")
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Economy Events
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * كسب عملة
     *
     * @param virtualCurrencyName اسم العملة
     * @param value القيمة
     */
    fun logEarnVirtualCurrency(virtualCurrencyName: String, value: Long) {
        analytics.logEvent(FirebaseAnalytics.Event.EARN_VIRTUAL_CURRENCY) {
            param(FirebaseAnalytics.Param.VIRTUAL_CURRENCY_NAME, virtualCurrencyName)
            param(FirebaseAnalytics.Param.VALUE, value.toDouble())
        }
        Timber.d("Earned currency: $virtualCurrencyName = $value")
    }
    
    /**
     * إنفاق عملة
     *
     * @param itemName اسم الشيء
     * @param virtualCurrencyName اسم العملة
     * @param value القيمة
     */
    fun logSpendVirtualCurrency(
        itemName: String,
        virtualCurrencyName: String,
        value: Long
    ) {
        analytics.logEvent(FirebaseAnalytics.Event.SPEND_VIRTUAL_CURRENCY) {
            param(FirebaseAnalytics.Param.ITEM_NAME, itemName)
            param(FirebaseAnalytics.Param.VIRTUAL_CURRENCY_NAME, virtualCurrencyName)
            param(FirebaseAnalytics.Param.VALUE, value.toDouble())
        }
        Timber.d("Spent currency: $virtualCurrencyName = $value on $itemName")
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Tutorial Events
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * بدء التعليم
     */
    fun logTutorialBegin() {
        analytics.logEvent(FirebaseAnalytics.Event.TUTORIAL_BEGIN, null)
        Timber.d("Tutorial begin")
    }
    
    /**
     * إتمام التعليم
     */
    fun logTutorialComplete() {
        analytics.logEvent(FirebaseAnalytics.Event.TUTORIAL_COMPLETE, null)
        Timber.d("Tutorial complete")
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Error Events
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * تسجيل خطأ
     *
     * @param errorType نوع الخطأ
     * @param errorMessage رسالة الخطأ
     * @param fatal هل الخطأ قاتل
     */
    fun logError(errorType: String, errorMessage: String, fatal: Boolean = false) {
        logEvent(EVENT_ERROR, mapOf(
            PARAM_ERROR_TYPE to errorType,
            PARAM_ERROR_MESSAGE to errorMessage,
            PARAM_FATAL to fatal
        ))
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Session Events
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * بدء جلسة
     */
    fun logSessionStart() {
        analytics.logEvent(FirebaseAnalytics.Event.SESSION_START, null)
        Timber.d("Session start")
    }
    
    /**
     * اختيار محتوى
     *
     * @param contentType نوع المحتوى
     * @param itemId معرف العنصر
     */
    fun logSelectContent(contentType: String, itemId: String) {
        analytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT) {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, contentType)
            param(FirebaseAnalytics.Param.ITEM_ID, itemId)
        }
        Timber.d("Content selected: $contentType - $itemId")
    }
    
    companion object {
        // Custom Events
        const val EVENT_GAME_START = "game_start"
        const val EVENT_GAME_END = "game_end"
        const val EVENT_BOSS_DEFEAT = "boss_defeat"
        const val EVENT_PLAYER_DEATH = "player_death"
        const val EVENT_ITEM_USED = "item_used"
        const val EVENT_QUEST_COMPLETE = "quest_complete"
        const val EVENT_ERROR = "app_error"
        
        // Custom Parameters
        const val PARAM_SAVE_SLOT = "save_slot"
        const val PARAM_DURATION = "duration"
        const val PARAM_BOSS_NAME = "boss_name"
        const val PARAM_ATTEMPTS = "attempts"
        const val PARAM_LOCATION = "location"
        const val PARAM_CAUSE = "cause"
        const val PARAM_ACHIEVEMENT_NAME = "achievement_name"
        const val PARAM_QUEST_ID = "quest_id"
        const val PARAM_QUEST_NAME = "quest_name"
        const val PARAM_REWARD = "reward"
        const val PARAM_LEADERBOARD_TYPE = "leaderboard_type"
        const val PARAM_RANK = "rank"
        const val PARAM_ERROR_TYPE = "error_type"
        const val PARAM_ERROR_MESSAGE = "error_message"
        const val PARAM_FATAL = "fatal"
        
        // User Properties
        const val PROPERTY_PLAYER_LEVEL = "player_level"
        const val PROPERTY_PLAYER_TYPE = "player_type"
    }
}