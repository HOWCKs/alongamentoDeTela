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
    fun plan(
        physW: Int,
        physH: Int,
        multiplier: Float,
        mode: ProjectionMode
    ): ProjectionPlan {
        val m = multiplier.coerceIn(1.01f, 1.99f)
        val nw = physW
        val nh = physH
        if (nw <= 0 || nh <= 0) {
            return ProjectionPlan(nw, nh, nw, nh, m, mode)
        }
        return if (mode == ProjectionMode.CORTE) {
            // Same aspect as Physical size so the game still fills the panel.
            // Smaller logical size = zoom (corta laterais do mundo), sem faixa.
            val short = minOf(nw, nh)
            val zoomedShort = even((short / m).toInt().coerceIn(720, short))
            val w: Int
            val h: Int
            if (nw <= nh) {
                w = zoomedShort
                h = even(((w.toLong() * nh) / nw).toInt().coerceIn(w, nh))
            } else {
                h = zoomedShort
                w = even(((h.toLong() * nw) / nh).toInt().coerceIn(h, nw))
            }
            ProjectionPlan(nw, nh, w, h, m, mode)
        } else {
            ProjectionPlan(
                nw, nh,
                even((nw * m).toInt().coerceIn(nw, 7680)),
                even((nh * m).toInt().coerceIn(nh, 7680)),
                m, mode
            )
        }
    }

    fun multiplierLabel(value: Float): String = String.format("%.2fx", value)

    private fun even(v: Int): Int = v - (v and 1)
}
