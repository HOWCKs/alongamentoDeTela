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
        return if (mode == ProjectionMode.CORTE) {
            val longIsHeight = nh >= nw
            if (longIsHeight) {
                ProjectionPlan(nw, nh, nw, even((nh * m).toInt().coerceIn(nh, 7680)), m, mode)
            } else {
                ProjectionPlan(nw, nh, even((nw * m).toInt().coerceIn(nw, 7680)), nh, m, mode)
            }
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
