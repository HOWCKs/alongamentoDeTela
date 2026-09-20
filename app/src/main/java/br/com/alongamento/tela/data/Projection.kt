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
    val cropLong: Int
        get() = ((projH - nativeH) / 2).coerceAtLeast(0)
}

object Projection {
    fun portraitNative(physW: Int, physH: Int): Pair<Int, Int> {
        val short = minOf(physW, physH)
        val long = maxOf(physW, physH)
        return short to long
    }

    fun plan(
        physW: Int,
        physH: Int,
        multiplier: Float,
        mode: ProjectionMode
    ): ProjectionPlan {
        val (nw, nh) = portraitNative(physW, physH)
        val m = multiplier.coerceIn(1.01f, 1.99f)
        return if (mode == ProjectionMode.CORTE) {
            val ph = even((nh * m).toInt().coerceIn(nh, 7680))
            ProjectionPlan(nw, nh, nw, ph, m, mode)
        } else {
            val pw = even((nw * m).toInt().coerceIn(nw, 7680))
            val ph = even((nh * m).toInt().coerceIn(nh, 7680))
            ProjectionPlan(nw, nh, pw, ph, m, mode)
        }
    }

    fun multiplierLabel(value: Float): String = String.format("%.2fx", value)

    private fun even(v: Int): Int = v - (v and 1)
}
