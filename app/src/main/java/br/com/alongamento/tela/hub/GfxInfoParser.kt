package br.com.alongamento.tela.hub

data class GfxInfoStats(
    val totalFrames: Int,
    val jankyFrames: Int,
    val jankPercent: Float,
    val ok: Boolean
)

object GfxInfoParser {
    fun parse(text: String): GfxInfoStats {
        if (text.isBlank()) return GfxInfoStats(0, 0, 0f, false)
        val total = numberAfter(text, "Total frames rendered:")
            ?: numberAfter(text, "Total frames:")
        val janky = numberAfter(text, "Janky frames:")
        if (total == null || total <= 0) return GfxInfoStats(0, 0, 0f, false)
        val j = (janky ?: 0).coerceIn(0, total)
        return GfxInfoStats(total, j, FpsMath.jankPercentage(j, total), true)
    }

    private fun numberAfter(text: String, label: String): Int? {
        val i = text.indexOf(label, ignoreCase = true)
        if (i < 0) return null
        val rest = text.substring(i + label.length)
        val m = Regex("(\\d+)").find(rest) ?: return null
        return m.groupValues[1].toIntOrNull()
    }
}
