package br.com.alongamento.tela.hub

data class SfFps(
    val fps: Float,
    val ok: Boolean
)

object SurfaceFlingerParser {
    fun parse(text: String): SfFps {
        if (text.isBlank()) return SfFps(0f, false)
        Regex("""(\d+(?:\.\d+)?)\s*fps""", RegexOption.IGNORE_CASE).find(text)?.let {
            val v = it.groupValues[1].toFloatOrNull()
            if (v != null && FpsMath.plausibleFps(v)) return SfFps(v, true)
        }
        Regex("""refresh[- ]rate[^0-9]*(\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE).find(text)?.let {
            val v = it.groupValues[1].toFloatOrNull()
            if (v != null && FpsMath.plausibleFps(v)) return SfFps(v, true)
        }
        return SfFps(0f, false)
    }
}
