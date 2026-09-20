package br.com.alongamento.tela.hub

import android.content.Context

object HistoryStore {
    private const val PREF = "hub_history"
    private const val KEY = "rows"
    private const val MAX = 20

    fun add(context: Context, e: HistoryEntry) {
        val p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val cur = p.getString(KEY, "") ?: ""
        val row = listOf(
            e.timestamp, e.packageName, e.avgFps, e.minFps, e.maxFps, e.tempC, e.profile, e.durationSec
        ).joinToString("|")
        val lines = (listOf(row) + cur.split('\n').filter { it.isNotBlank() }).take(MAX)
        p.edit().putString(KEY, lines.joinToString("\n")).apply()
    }

    fun list(context: Context): List<HistoryEntry> {
        val p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        return (p.getString(KEY, "") ?: "").split('\n').mapNotNull { parse(it) }
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().remove(KEY).apply()
    }

    private fun parse(line: String): HistoryEntry? {
        val p = line.split('|')
        if (p.size < 8) return null
        return runCatching {
            HistoryEntry(
                timestamp = p[0].toLong(),
                packageName = p[1],
                avgFps = p[2].toFloat(),
                minFps = p[3].toFloat(),
                maxFps = p[4].toFloat(),
                tempC = p[5].toFloat(),
                profile = p[6],
                durationSec = p[7].toInt()
            )
        }.getOrNull()
    }
}
