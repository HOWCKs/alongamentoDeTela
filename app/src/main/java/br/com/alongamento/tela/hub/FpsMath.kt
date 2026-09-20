package br.com.alongamento.tela.hub

object FpsMath {
    fun frameTimeMs(fps: Float): Float = if (fps <= 0.01f) 0f else 1000f / fps

    fun budgetMs(refreshHz: Float): Float =
        if (refreshHz <= 0.01f) 16.67f else 1000f / refreshHz

    fun jankPercentage(framesAboveBudget: Int, totalFrames: Int): Float {
        if (totalFrames <= 0) return 0f
        return framesAboveBudget * 100f / totalFrames
    }

    fun stability(averageFps: Float, refreshHz: Float): Float {
        val hz = refreshHz.coerceAtLeast(1f)
        return (averageFps / hz).coerceIn(0f, 1.5f)
    }

    fun plausibleFps(value: Float): Boolean = value in 1f..240f
}
