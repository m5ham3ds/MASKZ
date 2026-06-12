package com.erygra.maskoflight.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import com.erygra.maskoflight.core.GameConfig.AudioConfig
import com.erygra.maskoflight.core.EventBus
import com.erygra.maskoflight.core.GameEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

/**
 * ══════════════════════════════════════════════════════════════════════════
 *  AudioEngine.kt — محرك الصوت والموسيقى
 *  Erygra Universe 2.0 | Mask of Light
 * ══════════════════════════════════════════════════════════════════════════
 *
 *  يدير:
 *  - الموسيقى التكيفية (Adaptive Music Layers)
 *  - مؤثرات الصوت (SFX)
 *  - الأصوات البيئية (Ambient)
 *  - التلاشي والتقاطع (Crossfade)
 *  - تخفيض الصوت (Ducking)
 *
 *  التصميم:
 *  - MediaPlayer للموسيقى (OGG)
 *  - SoundPool للـ SFX (WAV قصيرة)
 *  - Coroutines للـ crossfade والـ ducking
 *  - EventBus listener للتحكم التلقائي
 * ══════════════════════════════════════════════════════════════════════════
 */

// ─────────────────────────────────────────────────────────────────────────────
// معرفات الصوت
// ─────────────────────────────────────────────────────────────────────────────

/**
 * RegionMusicId — معرفات موسيقى المناطق.
 */
enum class RegionMusicId(val rawResName: String) {
    ASHEN_SPRAWL("music_ashen_sprawl"),
    VEILED_ARCHIVES("music_veiled_archives"),
    HOLLOWED_ARCHIPELAGO("music_hollowed_archipelago"),
    GLASSFJORD_CLIFFS("music_glassfjord_cliffs"),
    SUNKEN_CLOCKWORKS("music_sunken_clockworks"),
    BLACKROOT_MOORLANDS("music_blackroot_moorlands"),
    LUMINOUS_CHASM("music_luminous_chasm"),
    MAIN_MENU("music_main_menu"),
    BOSS_FIGHT("music_boss_fight"),
    VICTORY("music_victory"),
    GAME_OVER("music_game_over")
}

/**
 * SfxId — معرفات مؤثرات الصوت.
 */
object SfxId {
    // ─── قتال ─────────────────────────────────────────────────────────────
    const val LIGHT_ATTACK = "sfx_light_attack"
    const val HEAVY_ATTACK = "sfx_heavy_attack"
    const val COMBO_FINISHER = "sfx_combo_finisher"
    const val PARRY_SUCCESS = "sfx_parry_success"
    const val PARRY_FAIL = "sfx_parry_fail"
    const val BLOCK = "sfx_block"
    const val HIT_RECEIVE = "sfx_hit_receive"
    const val CRITICAL_HIT = "sfx_critical_hit"
    const val ENEMY_DEATH = "sfx_enemy_death"
    const val BOSS_PHASE_CHANGE = "sfx_boss_phase_change"

    // ─── حركة ─────────────────────────────────────────────────────────────
    const val JUMP = "sfx_jump"
    const val DOUBLE_JUMP = "sfx_double_jump"
    const val LAND = "sfx_land"
    const val DASH = "sfx_dash"
    const val DODGE = "sfx_dodge"
    const val FOOTSTEP_STONE = "sfx_footstep_stone"
    const val FOOTSTEP_WOOD = "sfx_footstep_wood"
    const val FOOTSTEP_WATER = "sfx_footstep_water"

    // ─── قدرات الذاكرة ────────────────────────────────────────────────────
    const val MEMORY_PULSE = "sfx_memory_pulse"
    const val ECHO_RECALL = "sfx_echo_recall"
    const val MASK_SHARD_BLAST = "sfx_mask_shard_blast"
    const val BORROWED_NAMES = "sfx_borrowed_names"
    const val FM_WARNING = "sfx_fm_warning"

    // ─── UI ───────────────────────────────────────────────────────────────
    const val MENU_SELECT = "sfx_menu_select"
    const val MENU_BACK = "sfx_menu_back"
    const val ITEM_PICKUP = "sfx_item_pickup"
    const val ITEM_USE = "sfx_item_use"
    const val MAP_OPEN = "sfx_map_open"
    const val LEVEL_UP = "sfx_level_up"
    const val ACHIEVEMENT = "sfx_achievement"
    const val SAVE_GAME = "sfx_save_game"

    // ─── بيئة ──────────────────────────────────────────────────────────────
    const val SANCTUARY_ACTIVATE = "sfx_sanctuary_activate"
    const val FAST_TRAVEL = "sfx_fast_travel"
    const val DOOR_OPEN = "sfx_door_open"
    const val SECRET_FOUND = "sfx_secret_found"
    const val PUZZLE_SOLVE = "sfx_puzzle_solve"
    const val MERCHANT_OPEN = "sfx_merchant_open"
}

// ─────────────────────────────────────────────────────────────────────────────
// حالة الصوت
// ─────────────────────────────────────────────────────────────────────────────

/**
 * AudioState — حالة محرك الصوت.
 */
data class AudioState(
    val masterVolume: Float = GameConfig.AudioConfig.MASTER_VOLUME_DEFAULT,
    val musicVolume: Float = GameConfig.AudioConfig.MUSIC_VOLUME_DEFAULT,
    val sfxVolume: Float = GameConfig.AudioConfig.SFX_VOLUME_DEFAULT,
    val ambientVolume: Float = GameConfig.AudioConfig.AMBIENT_VOLUME_DEFAULT,
    val currentRegionMusic: RegionMusicId? = null,
    val isMusicPlaying: Boolean = false,
    val isInCombat: Boolean = false,
    val isInBossFight: Boolean = false,
    val isDucking: Boolean = false
)

// ─────────────────────────────────────────────────────────────────────────────
// محرك الصوت الرئيسي
// ─────────────────────────────────────────────────────────────────────────────

/**
 * AudioEngine — يدير كل صوت في اللعبة.
 *
 * @param context Android Context
 */
class AudioEngine(private val context: Context) {

    companion object {
        var instance: AudioEngine? = null
        fun playSFX(sfxId: String, volume: Float = 1f) {
            instance?.playSfx(sfxId, volume)
        }
    }

    init {
        instance = this
    }

    private val scope = CoroutineScope(Dispatchers.Main)

    // ─── حالة الصوت ──────────────────────────────────────────────────────
    var state = AudioState()
        private set

    // ─── MediaPlayer للموسيقى ─────────────────────────────────────────────
    private var currentMusicPlayer: MediaPlayer? = null
    private var nextMusicPlayer: MediaPlayer? = null

    // ─── SoundPool للـ SFX ────────────────────────────────────────────────
    private val soundPool: SoundPool by lazy { createSoundPool() }
    private val loadedSounds = mutableMapOf<String, Int>()

    // ─── Ambient Player ───────────────────────────────────────────────────
    private var ambientPlayer: MediaPlayer? = null

    // ─── Crossfade Job ────────────────────────────────────────────────────
    private var crossfadeJob: Job? = null
    private var duckingJob: Job? = null

    // ─── EventBus listener ───────────────────────────────────────────────
    private var eventListenerJob: Job? = null

    // ─────────────────────────────────────────────────────────────────────
    // التهيئة
    // ─────────────────────────────────────────────────────────────────────

    /**
     * initialize — يهيئ محرك الصوت ويبدأ الاستماع للأحداث.
     */
    fun initialize() {
        startEventListener()
        preloadCommonSfx()
    }

    /**
     * createSoundPool — ينشئ SoundPool مع إعدادات محسّنة.
     */
    private fun createSoundPool(): SoundPool {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        return SoundPool.Builder()
            .setMaxStreams(GameConfig.AudioConfig.MAX_SIMULTANEOUS_SFX)
            .setAudioAttributes(attributes)
            .build()
    }

    /**
     * preloadCommonSfx — يُحمِّل مسبقاً الأصوات الأكثر استخداماً.
     */
    private fun preloadCommonSfx() {
        val commonSounds = listOf(
            SfxId.LIGHT_ATTACK,
            SfxId.HEAVY_ATTACK,
            SfxId.JUMP,
            SfxId.LAND,
            SfxId.DASH,
            SfxId.HIT_RECEIVE,
            SfxId.MENU_SELECT,
            SfxId.ITEM_PICKUP
        )
        commonSounds.forEach { sfxId -> loadSfx(sfxId) }
    }

    // ─────────────────────────────────────────────────────────────────────
    // الاستماع للأحداث
    // ─────────────────────────────────────────────────────────────────────

    /**
     * startEventListener — يبدأ الاستماع لأحداث EventBus.
     */
    private fun startEventListener() {
        eventListenerJob?.cancel()
        eventListenerJob = scope.launch {
            EventBus.events.collect { event ->
                when (event) {
                    is GameEvent.PlaySFX -> playSfx(event.sfxId, event.volume)
                    is GameEvent.RegionMusicChanged -> playRegionMusic(event.regionId)
                    is GameEvent.BossMusicStarted -> playBossMusic()
                    is GameEvent.BossMusicEnded -> returnToRegionMusic()
                    is GameEvent.DialogueStarted -> duckMusic(GameConfig.AudioConfig.DIALOGUE_MUSIC_DUCK_FACTOR)
                    is GameEvent.DialogueEnded -> unduckMusic()
                    is GameEvent.PlayerDied -> fadeOutMusic(1000L)
                    is GameEvent.PlayerForgetfulnessChanged -> {
                        if (event.newValue >= 20) {
                            playSfx(SfxId.FM_WARNING)
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // التحكم في الموسيقى
    // ─────────────────────────────────────────────────────────────────────

    /**
     * playRegionMusic — يشغّل موسيقى المنطقة مع Crossfade.
     *
     * @param regionId معرف المنطقة
     */
    fun playRegionMusic(regionId: String) {
        val musicId = when (regionId) {
            "ashen_sprawl" -> RegionMusicId.ASHEN_SPRAWL
            "veiled_archives" -> RegionMusicId.VEILED_ARCHIVES
            "hollowed_archipelago" -> RegionMusicId.HOLLOWED_ARCHIPELAGO
            "glassfjord_cliffs" -> RegionMusicId.GLASSFJORD_CLIFFS
            "sunken_clockworks" -> RegionMusicId.SUNKEN_CLOCKWORKS
            "blackroot_moorlands" -> RegionMusicId.BLACKROOT_MOORLANDS
            "luminous_chasm" -> RegionMusicId.LUMINOUS_CHASM
            else -> return
        }
        crossfadeTo(musicId)
    }

    /**
     * playBossMusic — يشغّل موسيقى الزعيم.
     */
    fun playBossMusic() {
        crossfadeTo(RegionMusicId.BOSS_FIGHT)
        state = state.copy(isInBossFight = true)
    }

    /**
     * returnToRegionMusic — يعود لموسيقى المنطقة بعد الزعيم.
     */
    fun returnToRegionMusic() {
        state = state.copy(isInBossFight = false)
        state.currentRegionMusic?.let { crossfadeTo(it) }
    }

    /**
     * playMainMenuMusic — يشغّل موسيقى القائمة الرئيسية.
     */
    fun playMainMenuMusic() {
        crossfadeTo(RegionMusicId.MAIN_MENU)
    }

    /**
     * crossfadeTo — ينتقل من الموسيقى الحالية إلى موسيقى جديدة بـ Crossfade.
     *
     * @param targetMusic الموسيقى الهدف
     * @param durationMs مدة التلاشي بالميلي ثانية
     */
    private fun crossfadeTo(
        targetMusic: RegionMusicId,
        durationMs: Long = GameConfig.AudioConfig.REGION_MUSIC_CROSSFADE_MS
    ) {
        // إذا كانت نفس الموسيقى، لا تفعل شيئاً
        if (state.currentRegionMusic == targetMusic && currentMusicPlayer?.isPlaying == true) return

        crossfadeJob?.cancel()
        crossfadeJob = scope.launch {
            val resId = getMusicResId(targetMusic)
            if (resId == 0) return@launch

            // أنشئ مشغّل جديد للموسيقى القادمة
            val newPlayer = createMusicPlayer(resId, 0f)
            nextMusicPlayer = newPlayer
            newPlayer.start()

            val steps = 50
            val stepDelay = durationMs / steps
            val currentVolume = calculateMusicVolume()

            // Crossfade تدريجي
            for (i in 0..steps) {
                val progress = i.toFloat() / steps
                val oldVol = currentVolume * (1f - progress)
                val newVol = currentVolume * progress

                currentMusicPlayer?.setVolume(oldVol, oldVol)
                newPlayer.setVolume(newVol, newVol)

                delay(stepDelay)
            }

            // أوقف المشغّل القديم وحرّر موارده
            currentMusicPlayer?.stop()
            currentMusicPlayer?.release()
            currentMusicPlayer = newPlayer
            nextMusicPlayer = null

            state = state.copy(
                currentRegionMusic = targetMusic,
                isMusicPlaying = true
            )
        }
    }

    /**
     * fadeOutMusic — يُخفت الموسيقى تدريجياً.
     */
    fun fadeOutMusic(durationMs: Long = 1500L) {
        scope.launch {
            val steps = 30
            val stepDelay = durationMs / steps
            val startVolume = calculateMusicVolume()

            for (i in 0..steps) {
                val progress = i.toFloat() / steps
                val volume = startVolume * (1f - progress)
                currentMusicPlayer?.setVolume(volume, volume)
                delay(stepDelay)
            }

            currentMusicPlayer?.pause()
            state = state.copy(isMusicPlaying = false)
        }
    }

    /**
     * pauseMusic — يوقف مؤقتاً الموسيقى.
     */
    fun pauseMusic() {
        currentMusicPlayer?.pause()
        state = state.copy(isMusicPlaying = false)
    }

    /**
     * resumeMusic — يستأنف تشغيل الموسيقى.
     */
    fun resumeMusic() {
        currentMusicPlayer?.start()
        state = state.copy(isMusicPlaying = true)
    }

    // ─────────────────────────────────────────────────────────────────────
    // تخفيض الصوت (Ducking)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * duckMusic — يُخفّف الموسيقى مؤقتاً.
     *
     * @param duckFactor نسبة التخفيف (0 = صمت تام، 1 = بدون تخفيف)
     */
    fun duckMusic(duckFactor: Float) {
        duckingJob?.cancel()
        val targetVolume = calculateMusicVolume() * duckFactor
        currentMusicPlayer?.setVolume(targetVolume, targetVolume)
        state = state.copy(isDucking = true)
    }

    /**
     * unduckMusic — يُعيد الموسيقى لمستواها الطبيعي.
     */
    fun unduckMusic() {
        duckingJob?.cancel()
        val normalVolume = calculateMusicVolume()
        currentMusicPlayer?.setVolume(normalVolume, normalVolume)
        state = state.copy(isDucking = false)
    }

    // ─────────────────────────────────────────────────────────────────────
    // مؤثرات الصوت (SFX)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * playSfx — يُشغّل مؤثراً صوتياً.
     *
     * @param sfxId معرف الصوت
     * @param volume مستوى الصوت (0–1)
     * @param pitch درجة الصوت (0.5–2.0)
     */
    fun playSfx(
        sfxId: String,
        volume: Float = 1f,
        pitch: Float = 1f
    ) {
        val soundId = loadedSounds[sfxId] ?: run {
            loadSfx(sfxId)
            loadedSounds[sfxId] ?: return
        }

        val finalVolume = volume * state.sfxVolume * state.masterVolume

        soundPool.play(
            soundId,
            finalVolume, finalVolume,
            1,  // priority
            0,  // loop (0 = لا تكرار)
            pitch
        )
    }

    /**
     * loadSfx — يُحمِّل صوتاً من الموارد.
     */
    private fun loadSfx(sfxId: String) {
        val resId = getSfxResId(sfxId)
        if (resId == 0) return

        val soundId = soundPool.load(context, resId, 1)
        loadedSounds[sfxId] = soundId
    }

    // ─────────────────────────────────────────────────────────────────────
    // التحكم في مستويات الصوت
    // ─────────────────────────────────────────────────────────────────────

    /**
     * setMasterVolume — يُعدّل مستوى الصوت الرئيسي.
     */
    fun setMasterVolume(volume: Float) {
        state = state.copy(masterVolume = volume.coerceIn(0f, 1f))
        updateMusicVolume()
    }

    /**
     * setMusicVolume — يُعدّل مستوى الموسيقى.
     */
    fun setMusicVolume(volume: Float) {
        state = state.copy(musicVolume = volume.coerceIn(0f, 1f))
        updateMusicVolume()
    }

    /**
     * setSfxVolume — يُعدّل مستوى المؤثرات الصوتية.
     */
    fun setSfxVolume(volume: Float) {
        state = state.copy(sfxVolume = volume.coerceIn(0f, 1f))
    }

    /**
     * setAmbientVolume — يُعدّل مستوى الأصوات البيئية.
     */
    fun setAmbientVolume(volume: Float) {
        state = state.copy(ambientVolume = volume.coerceIn(0f, 1f))
        val ambientVol = volume * state.masterVolume
        ambientPlayer?.setVolume(ambientVol, ambientVol)
    }

    /**
     * updateMusicVolume — يُحدّث مستوى الموسيقى بعد التغييرات.
     */
    private fun updateMusicVolume() {
        if (!state.isDucking) {
            val vol = calculateMusicVolume()
            currentMusicPlayer?.setVolume(vol, vol)
        }
    }

    /**
     * calculateMusicVolume — يحسب مستوى الموسيقى النهائي.
     */
    private fun calculateMusicVolume(): Float {
        return state.musicVolume * state.masterVolume
    }

    // ─────────────────────────────────────────────────────────────────────
    // الأصوات البيئية (Ambient)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * playAmbient — يُشغّل صوتاً بيئياً في حلقة.
     *
     * @param regionId معرف المنطقة
     */
    fun playAmbient(regionId: String) {
        val resId = getAmbientResId(regionId)
        if (resId == 0) return

        ambientPlayer?.stop()
        ambientPlayer?.release()

        ambientPlayer = MediaPlayer.create(context, resId)?.apply {
            isLooping = true
            val vol = state.ambientVolume * state.masterVolume
            setVolume(vol, vol)
            start()
        }
    }

    /**
     * stopAmbient — يوقف الصوت البيئي.
     */
    fun stopAmbient() {
        ambientPlayer?.stop()
        ambientPlayer?.release()
        ambientPlayer = null
    }

    // ─────────────────────────────────────────────────────────────────────
    // مساعدات
    // ─────────────────────────────────────────────────────────────────────

    /**
     * createMusicPlayer — ينشئ MediaPlayer لملف موسيقى.
     */
    private fun createMusicPlayer(resId: Int, startVolume: Float): MediaPlayer {
        return MediaPlayer.create(context, resId)?.apply {
            isLooping = true
            setVolume(startVolume, startVolume)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
        } ?: MediaPlayer()
    }

    /**
     * getMusicResId — يُعيد معرف مورد الموسيقى.
     *
     * في التطبيق الحقيقي يُستبدل بـ ResourceManager.
     */
    private fun getMusicResId(musicId: RegionMusicId): Int {
        return context.resources.getIdentifier(
            musicId.rawResName,
            "raw",
            context.packageName
        )
    }

    /**
     * getSfxResId — يُعيد معرف مورد المؤثر الصوتي.
     */
    private fun getSfxResId(sfxId: String): Int {
        return context.resources.getIdentifier(
            sfxId,
            "raw",
            context.packageName
        )
    }

    /**
     * getAmbientResId — يُعيد معرف مورد الصوت البيئي.
     */
    private fun getAmbientResId(regionId: String): Int {
        val name = "ambient_$regionId"
        return context.resources.getIdentifier(name, "raw", context.packageName)
    }

    // ─────────────────────────────────────────────────────────────────────
    // التنظيف
    // ─────────────────────────────────────────────────────────────────────

    /**
     * release — يُحرر جميع الموارد.
     * يُستدعى عند إنهاء التطبيق.
     */
    fun release() {
        eventListenerJob?.cancel()
        crossfadeJob?.cancel()
        duckingJob?.cancel()

        currentMusicPlayer?.stop()
        currentMusicPlayer?.release()
        currentMusicPlayer = null

        nextMusicPlayer?.stop()
        nextMusicPlayer?.release()
        nextMusicPlayer = null

        ambientPlayer?.stop()
        ambientPlayer?.release()
        ambientPlayer = null

        soundPool.release()
        loadedSounds.clear()
    }
}