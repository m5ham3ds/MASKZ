package com.erygra.maskoflight.quest

/**
 * ════════════════════════════════════════════════════════════════════════════════
 * QuestObjective.kt - أهداف المهام
 * ════════════════════════════════════════════════════════════════════════════════
 * 
 * الوصف:
 * - تمثيل أهداف المهام المختلفة
 * - أنواع مختلفة من الأهداف
 * - نظام تقدم قابل للتتبع
 * 
 * المكونات الرئيسية:
 * - Objective types
 * - Progress tracking
 * - Completion conditions
 * 
 * @author Erygra Team
 * @since 2.0.0
 * ════════════════════════════════════════════════════════════════════════════════
 */

/**
 * الهدف
 *
 * @property id معرف الهدف الفريد
 * @property title عنوان الهدف
 * @property description وصف الهدف
 * @property type نوع الهدف
 * @property targetValue القيمة المستهدفة
 * @property currentValue القيمة الحالية (يتم تحديثها أثناء اللعب)
 * @property optional هل الهدف اختياري
 * @property hidden هل الهدف مخفي حتى الإكمال
 * @property data بيانات إضافية حسب نوع الهدف
 */
data class QuestObjective(
    val id: String,
    val title: String,
    val description: String,
    val type: ObjectiveType,
    val targetValue: Int,
    val currentValue: Int = 0,
    val optional: Boolean = false,
    val hidden: Boolean = false,
    val data: Map<String, Any> = emptyMap()
) {
    /**
     * هل الهدف مكتمل
     */
    val isCompleted: Boolean
        get() = currentValue >= targetValue
    
    /**
     * نسبة الإكمال
     */
    val completionPercentage: Float
        get() = if (targetValue > 0) {
            (currentValue.toFloat() / targetValue) * 100f
        } else {
            100f
        }
    
    /**
     * النص المتبقي
     */
    val remaining: Int
        get() = (targetValue - currentValue).coerceAtLeast(0)
}

/**
 * أنواع الأهداف
 */
enum class ObjectiveType {
    // أهداف تجميع
    COLLECT_ITEMS,           // جمع أشياء
    COLLECT_RESOURCES,       // جمع موارد
    
    // أهداف القتال
    DEFEAT_ENEMIES,          // هزيمة أعداء
    DEFEAT_BOSS,             // هزيمة زعيم
    KILL_SPECIFIC_ENEMY,     // قتل عدو محدد
    
    // أهداف الاستكشاف
    EXPLORE_LOCATION,        // استكشاف موقع
    REACH_LOCATION,          // الوصول لموقع
    DISCOVER_SECRET,         // اكتشاف سر
    UNLOCK_DOOR,             // فتح باب
    
    // أهداف التفاعل
    TALK_TO_NPC,             // التحدث مع شخصية
    INTERACT_WITH_OBJECT,    // التفاعل مع جسم
    DELIVER_ITEM,            // تسليم شيء
    ESCORT_NPC,              // مرافقة شخصية
    
    // أهداف البقاء
    SURVIVE_TIME,            // البقاء بحي لوقت محدد
    AVOID_DAMAGE,            // تجنب الضرر
    PROTECT_NPC,             // حماية شخصية
    
    // أهداف الألعاب الصغيرة
    MINIGAME,                // لعبة صغيرة
    PUZZLE,                  // لغز
    
    // أهداف مخصصة
    CUSTOM                   // مخصص
}

/**
 * Builder للأهداف
 */
class ObjectiveBuilder {
    private var id: String = ""
    private var title: String = ""
    private var description: String = ""
    private var type: ObjectiveType = ObjectiveType.CUSTOM
    private var targetValue: Int = 1
    private var optional: Boolean = false
    private var hidden: Boolean = false
    private val data = mutableMapOf<String, Any>()
    
    fun id(value: String) = apply { id = value }
    fun title(value: String) = apply { title = value }
    fun description(value: String) = apply { description = value }
    fun type(value: ObjectiveType) = apply { type = value }
    fun targetValue(value: Int) = apply { targetValue = value }
    fun optional(value: Boolean) = apply { optional = value }
    fun hidden(value: Boolean) = apply { hidden = value }
    fun data(key: String, value: Any) = apply { data[key] = value }
    fun data(values: Map<String, Any>) = apply { data.putAll(values) }
    
    fun build(): QuestObjective {
        require(id.isNotEmpty()) { "Objective ID is required" }
        require(title.isNotEmpty()) { "Objective title is required" }
        require(targetValue > 0) { "Target value must be greater than 0" }
        
        return QuestObjective(
            id = id,
            title = title,
            description = description,
            type = type,
            targetValue = targetValue,
            optional = optional,
            hidden = hidden,
            data = data
        )
    }
}

/**
 * Helper function للإنشاء
 */
fun objective(block: ObjectiveBuilder.() -> Unit): QuestObjective {
    return ObjectiveBuilder().apply(block).build()
}

/**
 * مصنع الأهداف الشائعة
 */
object ObjectiveFactory {
    
    fun collectItems(id: String, title: String, itemId: String, amount: Int): QuestObjective {
        return objective {
            this.id(id)
            this.title(title)
            description("Collect $amount items")
            type(ObjectiveType.COLLECT_ITEMS)
            targetValue(amount)
            data("itemId", itemId)
        }
    }
    
    fun defeatEnemies(id: String, title: String, enemyId: String, amount: Int): QuestObjective {
        return objective {
            this.id(id)
            this.title(title)
            description("Defeat $amount enemies")
            type(ObjectiveType.DEFEAT_ENEMIES)
            targetValue(amount)
            data("enemyId", enemyId)
        }
    }
    
    fun defeatBoss(id: String, title: String, bossId: String): QuestObjective {
        return objective {
            this.id(id)
            this.title(title)
            description("Defeat $bossId")
            type(ObjectiveType.DEFEAT_BOSS)
            targetValue(1)
            data("bossId", bossId)
        }
    }
    
    fun exploreLocation(id: String, title: String, locationId: String): QuestObjective {
        return objective {
            this.id(id)
            this.title(title)
            description("Explore $locationId")
            type(ObjectiveType.EXPLORE_LOCATION)
            targetValue(1)
            data("locationId", locationId)
        }
    }
    
    fun talkToNpc(id: String, title: String, npcId: String): QuestObjective {
        return objective {
            this.id(id)
            this.title(title)
            description("Talk to $npcId")
            type(ObjectiveType.TALK_TO_NPC)
            targetValue(1)
            data("npcId", npcId)
        }
    }
    
    fun deliverItem(id: String, title: String, itemId: String, npcId: String): QuestObjective {
        return objective {
            this.id(id)
            this.title(title)
            description("Deliver item to $npcId")
            type(ObjectiveType.DELIVER_ITEM)
            targetValue(1)
            data("itemId", itemId)
            data("npcId", npcId)
        }
    }
    
    fun surviveTime(id: String, title: String, seconds: Int): QuestObjective {
        return objective {
            this.id(id)
            this.title(title)
            description("Survive for $seconds seconds")
            type(ObjectiveType.SURVIVE_TIME)
            targetValue(seconds)
        }
    }
}