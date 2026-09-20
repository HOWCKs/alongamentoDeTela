package br.com.alongamento.tela.data

data class AspectPreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val w: Int,
    val h: Int
)

object AspectMath {
    fun physicalPortrait(width: Int, height: Int): Pair<Int, Int> {
        val shortSide = minOf(width, height)
        val longSide = maxOf(width, height)
        return shortSide to longSide
    }

    /**
     * Alongamento em jogos em paisagem: mantém o lado curto (ex.: 1080)
     * e recalcula o lado longo para a proporção pedida (ex.: 21:9 → 2520x1080).
     */
    fun sizeFor(ratioW: Int, ratioH: Int, physW: Int, physH: Int): Pair<Int, Int> {
        val shortSide = minOf(physW, physH)
        val longSide = ((shortSide.toLong() * ratioW) / ratioH).toInt().coerceAtLeast(shortSide)
        return longSide to shortSide
    }

    fun presets(physW: Int, physH: Int): List<AspectPreset> {
        val shortSide = minOf(physW, physH)
        val longSide = maxOf(physW, physH)
        val nativeRatio = if (shortSide == 0) "nativo" else String.format("%.2f:1", longSide.toFloat() / shortSide)
        fun item(id: String, title: String, rw: Int, rh: Int, note: String): AspectPreset {
            val (w, h) = sizeFor(rw, rh, physW, physH)
            return AspectPreset(id, title, "$w × $h · $note", w, h)
        }
        return listOf(
            AspectPreset("nativo", "Original", "$longSide × $shortSide · $nativeRatio", longSide, shortSide),
            item("16:9", "16:9 cinema", 16, 9, "mais quadrado"),
            item("18:9", "18:9", 18, 9, "alongado leve"),
            item("20:9", "20:9", 20, 9, "padrão de muitos celulares"),
            item("21:9", "21:9 ultrawide", 21, 9, "FOV extra"),
            item("32:9", "32:9 super ultrawide", 32, 9, "máximo alongamento")
        )
    }
}
