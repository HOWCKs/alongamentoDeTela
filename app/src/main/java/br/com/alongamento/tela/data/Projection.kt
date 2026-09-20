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
    val cropEachSide: Int
        get() = ((projW - nativeW) / 2).coerceAtLeast(0)
}

object Projection {
    fun landscapeNative(physW: Int, physH: Int): Pair<Int, Int> {
        val w = maxOf(physW, physH)
        val h = minOf(physW, physH)
        return w to h
    }

    fun plan(
        physW: Int,
        physH: Int,
        multiplier: Float,
        mode: ProjectionMode
    ): ProjectionPlan {
        val (nw, nh) = landscapeNative(physW, physH)
        val m = multiplier.coerceIn(1.01f, 1.99f)
        val pw = even((nw * m).toInt().coerceIn(nw, 7680))
        return ProjectionPlan(nw, nh, pw, nh, m, mode)
    }

    fun multiplierLabel(value: Float): String = String.format("%.2fx", value)

    private fun even(v: Int): Int = v - (v and 1)
}
