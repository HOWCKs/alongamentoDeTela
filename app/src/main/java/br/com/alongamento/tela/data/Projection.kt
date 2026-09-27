package br.com.alongamento.tela.data

enum class ProjectionMode {
    ALONGAR,
    CORTE
}

data class ProjectionPlan(
    val nativeW: Int,
    val nativeH: Int,
    val projW: Int,
    val projH: Int,
    val multiplier: Float,
    val mode: ProjectionMode
) {
    val nativeLabel: String get() = "$nativeW × $nativeH"
    val projLabel: String get() = "$projW × $projH"
}

object Projection {
    /** Real stretch on one axis. UI 1.00–1.20 maps 1:1 to this factor. */
    fun factor(raw: Float): Float = raw.coerceIn(1.00f, 1.20f)

    fun plan(
        physW: Int,
        physH: Int,
        multiplier: Float,
        mode: ProjectionMode
    ): ProjectionPlan {
        val f = factor(multiplier)
        val nw = physW
        val nh = physH
        if (nw <= 0 || nh <= 0) {
            return ProjectionPlan(nw, nh, nw, nh, f, mode)
        }
        val portrait = nh >= nw
        val shortSide = minOf(nw, nh)
        val longSide = maxOf(nw, nh)
        val newLong = when (mode) {
            ProjectionMode.ALONGAR ->
                even((longSide * f).toInt().coerceIn(longSide, 7680))
            ProjectionMode.CORTE ->
                even((longSide / f).toInt().coerceIn(shortSide.coerceAtLeast(720), longSide))
        }
        val projW: Int
        val projH: Int
        if (portrait) {
            projW = shortSide
            projH = newLong
        } else {
            projW = newLong
            projH = shortSide
        }
        return ProjectionPlan(nw, nh, projW, projH, f, mode)
    }

    fun multiplierLabel(value: Float): String = String.format("%.2fx", factor(value))

    private fun even(v: Int): Int = v - (v and 1)
}
