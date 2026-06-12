package com.erygra.maskoflight.core

/**
 * ══════════════════════════════════════════════════════════════════════════
 *  GameState.kt — حالات اللعبة الرئيسية
 *  Erygra Universe 2.0 | Mask of Light
 * ══════════════════════════════════════════════════════════════════════════
 *
 *  يحتوي هذا الملف على كافة حالات اللعبة (State Machine) التي تتنقل
 *  بينها اللعبة بشكل كامل. كل حالة تمثل شاشة أو وضع لعب محدد.
 *
 *  التدفق العام:
 *  SPLASH → MAIN_MENU → [PLAYING ↔ PAUSED] → [GAME_OVER | VICTORY]
 *                     ↘ SETTINGS
 *                     ↘ CHRONICLES
 *                     ↘ SAVE_LOAD
 *
 *  حالات فرعية داخل PLAYING:
 *  PLAYING → [MAP | INVENTORY | DIALOGUE | ORACLE | SANCTUARY | FAST_TRAVEL]
 * ══════════════════════════════════════════════════════════════════════════
 */

// ─────────────────────────────────────────────────────────────────────────────
// حالات اللعبة الرئيسية
// ─────────────────────────────────────────────────────────────────────────────

/**
 * GameState — يمثل الحالة الكاملة للعبة في أي لحظة.
 *
 * @property isPlayingState هل اللعبة نشطة (يُحتاج لتحديث الفيزياء والرسم)
 * @property showsGameWorld هل يظهر عالم اللعبة خلف الواجهة
 * @property blocksGameInput هل تُوقف هذه الحالة الإدخال من اللاعب
 */
enum class GameState(
    val isPlayingState: Boolean = false,
    val showsGameWorld: Boolean = false,
    val blocksGameInput: Boolean = true
) {

    // ─── شاشات الإطلاق والقائمة ───────────────────────────────────────────

    /** شاشة Splash الأولية مع الشعار */
    SPLASH(
        isPlayingState = false,
        showsGameWorld = false,
        blocksGameInput = true
    ),

    /** القائمة الرئيسية */
    MAIN_MENU(
        isPlayingState = false,
        showsGameWorld = false,
        blocksGameInput = true
    ),

    /** شاشة اختيار/تحميل اللعبة المحفوظة */
    SAVE_LOAD(
        isPlayingState = false,
        showsGameWorld = false,
        blocksGameInput = true
    ),

    /** شاشة الإعدادات */
    SETTINGS(
        isPlayingState = false,
        showsGameWorld = false,
        blocksGameInput = true
    ),

    /** شاشة السجلات (الأحداث والقصة السابقة) */
    CHRONICLES(
        isPlayingState = false,
        showsGameWorld = false,
        blocksGameInput = true
    ),

    /** شاشة الإنجازات */
    ACHIEVEMENTS(
        isPlayingState = false,
        showsGameWorld = false,
        blocksGameInput = true
    ),

    /** شاشة الاعتمادات */
    CREDITS(
        isPlayingState = false,
        showsGameWorld = false,
        blocksGameInput = true
    ),

    // ─── حالات اللعب الفعلية ──────────────────────────────────────────────

    /** اللعب النشط — الحالة الأساسية */
    PLAYING(
        isPlayingState = true,
        showsGameWorld = true,
        blocksGameInput = false
    ),

    /** الإيقاف المؤقت */
    PAUSED(
        isPlayingState = false,
        showsGameWorld = true,
        blocksGameInput = true
    ),

    // ─── واجهات تُفتح أثناء اللعب ────────────────────────────────────────

    /** خريطة العالم الكاملة */
    MAP(
        isPlayingState = false,
        showsGameWorld = false,
        blocksGameInput = true
    ),

    /** حقيبة اللاعب والمخزون */
    INVENTORY(
        isPlayingState = false,
        showsGameWorld = true,
        blocksGameInput = true
    ),

    /** شجرة المهارات */
    SKILL_TREE(
        isPlayingState = false,
        showsGameWorld = true,
        blocksGameInput = true
    ),

    // ─── حالات التفاعل ───────────────────────────────────────────────────

    /** حوار NPC */
    DIALOGUE(
        isPlayingState = false,
        showsGameWorld = true,
        blocksGameInput = true
    ),

    /** حوار العراف (Oracle / Gemini AI) */
    ORACLE_CONVERSATION(
        isPlayingState = false,
        showsGameWorld = true,
        blocksGameInput = true
    ),

    /** تفعيل نقطة التفتيش (Sanctuary) */
    SANCTUARY(
        isPlayingState = false,
        showsGameWorld = true,
        blocksGameInput = true
    ),

    /** واجهة السفر السريع */
    FAST_TRAVEL(
        isPlayingState = false,
        showsGameWorld = true,
        blocksGameInput = true
    ),

    /** واجهة التاجر */
    MERCHANT(
        isPlayingState = false,
        showsGameWorld = true,
        blocksGameInput = true
    ),

    /** حل الألغاز */
    PUZZLE(
        isPlayingState = false,
        showsGameWorld = true,
        blocksGameInput = true
    ),

    // ─── حالات نهاية اللعبة ──────────────────────────────────────────────

    /** انتهاء اللعبة بالفشل */
    GAME_OVER(
        isPlayingState = false,
        showsGameWorld = true,
        blocksGameInput = true
    ),

    /** الانتصار / إتمام المنطقة */
    VICTORY(
        isPlayingState = false,
        showsGameWorld = true,
        blocksGameInput = true
    ),

    /** الانتقال بين المناطق (Loading) */
    REGION_TRANSITION(
        isPlayingState = false,
        showsGameWorld = false,
        blocksGameInput = true
    ),

    /** قتال الزعيم (Boss Fight - يبدّل الموسيقى والكاميرا) */
    BOSS_FIGHT(
        isPlayingState = true,
        showsGameWorld = true,
        blocksGameInput = false
    ),

    /** حدث عالمي نشط */
    WORLD_EVENT(
        isPlayingState = true,
        showsGameWorld = true,
        blocksGameInput = false
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// حالات الانتقال المسموح بها
// ─────────────────────────────────────────────────────────────────────────────

/**
 * StateTransition — يحدد الانتقالات المسموح بها بين الحالات.
 *
 * يمنع الانتقالات غير المنطقية مثل:
 * - الانتقال من GAME_OVER إلى MAP مباشرةً
 * - الانتقال من SPLASH إلى BOSS_FIGHT
 */
object StateTransitions {

    /** قائمة الانتقالات المسموح بها لكل حالة */
    private val allowedTransitions: Map<GameState, Set<GameState>> = mapOf(

        GameState.SPLASH to setOf(
            GameState.MAIN_MENU
        ),

        GameState.MAIN_MENU to setOf(
            GameState.SAVE_LOAD,
            GameState.SETTINGS,
            GameState.CHRONICLES,
            GameState.ACHIEVEMENTS,
            GameState.CREDITS,
            GameState.PLAYING
        ),

        GameState.SAVE_LOAD to setOf(
            GameState.MAIN_MENU,
            GameState.PLAYING,
            GameState.REGION_TRANSITION
        ),

        GameState.SETTINGS to setOf(
            GameState.MAIN_MENU,
            GameState.PAUSED
        ),

        GameState.CHRONICLES to setOf(
            GameState.MAIN_MENU,
            GameState.PAUSED
        ),

        GameState.ACHIEVEMENTS to setOf(
            GameState.MAIN_MENU,
            GameState.PAUSED
        ),

        GameState.CREDITS to setOf(
            GameState.MAIN_MENU
        ),

        GameState.PLAYING to setOf(
            GameState.PAUSED,
            GameState.MAP,
            GameState.INVENTORY,
            GameState.SKILL_TREE,
            GameState.DIALOGUE,
            GameState.ORACLE_CONVERSATION,
            GameState.SANCTUARY,
            GameState.FAST_TRAVEL,
            GameState.MERCHANT,
            GameState.PUZZLE,
            GameState.GAME_OVER,
            GameState.VICTORY,
            GameState.REGION_TRANSITION,
            GameState.BOSS_FIGHT,
            GameState.WORLD_EVENT
        ),

        GameState.PAUSED to setOf(
            GameState.PLAYING,
            GameState.SETTINGS,
            GameState.CHRONICLES,
            GameState.ACHIEVEMENTS,
            GameState.SAVE_LOAD,
            GameState.MAIN_MENU
        ),

        GameState.MAP to setOf(
            GameState.PLAYING,
            GameState.FAST_TRAVEL
        ),

        GameState.INVENTORY to setOf(
            GameState.PLAYING,
            GameState.SKILL_TREE
        ),

        GameState.SKILL_TREE to setOf(
            GameState.PLAYING,
            GameState.INVENTORY
        ),

        GameState.DIALOGUE to setOf(
            GameState.PLAYING,
            GameState.ORACLE_CONVERSATION,
            GameState.MERCHANT,
            GameState.SANCTUARY
        ),

        GameState.ORACLE_CONVERSATION to setOf(
            GameState.PLAYING,
            GameState.DIALOGUE
        ),

        GameState.SANCTUARY to setOf(
            GameState.PLAYING,
            GameState.FAST_TRAVEL,
            GameState.SAVE_LOAD,
            GameState.MERCHANT
        ),

        GameState.FAST_TRAVEL to setOf(
            GameState.PLAYING,
            GameState.REGION_TRANSITION,
            GameState.MAP,
            GameState.SANCTUARY
        ),

        GameState.MERCHANT to setOf(
            GameState.PLAYING,
            GameState.DIALOGUE,
            GameState.SANCTUARY
        ),

        GameState.PUZZLE to setOf(
            GameState.PLAYING,
            GameState.GAME_OVER
        ),

        GameState.GAME_OVER to setOf(
            GameState.PLAYING,
            GameState.SAVE_LOAD,
            GameState.MAIN_MENU
        ),

        GameState.VICTORY to setOf(
            GameState.PLAYING,
            GameState.MAIN_MENU,
            GameState.CHRONICLES
        ),

        GameState.REGION_TRANSITION to setOf(
            GameState.PLAYING,
            GameState.BOSS_FIGHT
        ),

        GameState.BOSS_FIGHT to setOf(
            GameState.PLAYING,
            GameState.GAME_OVER,
            GameState.VICTORY
        ),

        GameState.WORLD_EVENT to setOf(
            GameState.PLAYING,
            GameState.GAME_OVER
        )
    )

    /**
     * هل الانتقال من [from] إلى [to] مسموح؟
     */
    fun isAllowed(from: GameState, to: GameState): Boolean {
        return allowedTransitions[from]?.contains(to) ?: false
    }

    /**
     * قائمة الحالات المسموح بالانتقال إليها من [state]
     */
    fun getAllowedFrom(state: GameState): Set<GameState> {
        return allowedTransitions[state] ?: emptySet()
    }
}