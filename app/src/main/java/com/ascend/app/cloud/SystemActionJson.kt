package com.ascend.app.cloud

import com.ascend.app.domain.*
import org.json.JSONObject

object SystemActionJson {
    fun encode(a: SystemAction): String = JSONObject().apply {
        put("type", a.type.name); put("name", a.name); put("target", a.target); put("unit", a.unit)
        put("frequency", a.frequency.name); put("difficulty", a.difficulty.name); put("category", a.category.name); put("reward", a.rewardXp)
        put("entity", a.entityId); put("template", a.templateId); put("from", a.fromDate); put("to", a.toDate)
        put("sets", a.sets); put("min", a.minReps); put("max", a.maxReps)
    }.toString()
    fun decode(value: String): SystemAction = JSONObject(value).let {
        SystemAction(SystemActionType.valueOf(it.getString("type")), it.getString("name"), it.getDouble("target"), it.getString("unit"),
            HabitFrequency.valueOf(it.getString("frequency")), HabitDifficulty.valueOf(it.getString("difficulty")),
            QuestCategory.valueOf(it.getString("category")), it.getInt("reward"), it.getString("entity"), it.getString("template"),
            it.getString("from"), it.getString("to"), it.getInt("sets"), it.getInt("min"), it.getInt("max"))
    }
}
