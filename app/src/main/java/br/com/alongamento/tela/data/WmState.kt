package br.com.alongamento.tela.data

data class WmState(
    val physicalW: Int,
    val physicalH: Int,
    val overrideW: Int?,
    val overrideH: Int?,
    val physicalDpi: Int,
    val overrideDpi: Int?
) {
    val currentW: Int get() = overrideW ?: physicalW
    val currentH: Int get() = overrideH ?: physicalH
    val currentDpi: Int get() = overrideDpi ?: physicalDpi

    fun prettySize(): String = "${currentW} × ${currentH}"
    fun prettyDpi(): String = "$currentDpi dpi"

    companion object {
        fun parse(sizeOut: String, densityOut: String, fallbackW: Int, fallbackH: Int, fallbackDpi: Int): WmState {
            val physSize = regex("Physical size:\\s*(\\d+)x(\\d+)").find(sizeOut)
            val overSize = regex("Override size:\\s*(\\d+)x(\\d+)").find(sizeOut)
            val physDpi = regex("Physical density:\\s*(\\d+)").find(densityOut)
            val overDpi = regex("Override density:\\s*(\\d+)").find(densityOut)
            return WmState(
                physicalW = physSize?.groupValues?.get(1)?.toIntOrNull() ?: fallbackW,
                physicalH = physSize?.groupValues?.get(2)?.toIntOrNull() ?: fallbackH,
                overrideW = overSize?.groupValues?.get(1)?.toIntOrNull(),
                overrideH = overSize?.groupValues?.get(2)?.toIntOrNull(),
                physicalDpi = physDpi?.groupValues?.get(1)?.toIntOrNull() ?: fallbackDpi,
                overrideDpi = overDpi?.groupValues?.get(1)?.toIntOrNull()
            )
        }

        private fun regex(p: String) = Regex(p)
    }
}
