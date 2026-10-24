package com.mygolfswing.app.ui.interaction

import android.content.Context
import androidx.core.content.edit
import com.mygolfswing.app.data.coach.V2CoachConnection
import org.json.JSONArray

private const val INTERACTION_COACH_ORDER_PREFS = "v2_interaction_coach_order"

internal fun applyInteractionCoachOrder(
    connections: List<V2CoachConnection>,
    savedOrder: List<String>,
): List<V2CoachConnection> {
    if (savedOrder.isEmpty()) return connections
    val rank = savedOrder.withIndex().associate { it.value to it.index }
    return connections.sortedWith(
        compareBy<V2CoachConnection> { rank[it.connectionId] ?: Int.MAX_VALUE }
            .thenBy(V2CoachConnection::connectionId),
    )
}

internal fun loadInteractionCoachOrder(context: Context, userId: String?): List<String> {
    val accountId = userId?.takeIf(String::isNotBlank) ?: return emptyList()
    return runCatching {
        val raw = context.getSharedPreferences(INTERACTION_COACH_ORDER_PREFS, Context.MODE_PRIVATE)
            .getString("coach:$accountId", null) ?: return@runCatching emptyList()
        val json = JSONArray(raw)
        List(json.length()) { index -> json.getString(index) }
    }.getOrDefault(emptyList())
}

internal fun saveInteractionCoachOrder(
    context: Context,
    userId: String?,
    connectionIds: List<String>,
) {
    val accountId = userId?.takeIf(String::isNotBlank) ?: return
    val json = JSONArray().apply { connectionIds.forEach(::put) }
    context.getSharedPreferences(INTERACTION_COACH_ORDER_PREFS, Context.MODE_PRIVATE).edit {
        putString("coach:$accountId", json.toString())
    }
}
