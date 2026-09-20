package br.com.alongamento.tela.display

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import br.com.alongamento.tela.data.AppPrefs
import br.com.alongamento.tela.data.WmState
import br.com.alongamento.tela.shell.ShellExecutor

object DisplayController {
    fun metrics(context: Context): Triple<Int, Int, Int> {
        val dm = DisplayMetrics()
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(dm)
        return Triple(dm.widthPixels, dm.heightPixels, dm.densityDpi)
    }

    suspend fun snapshot(context: Context): WmState {
        val (mw, mh, md) = metrics(context)
        if (!ShellExecutor.isReady()) {
            rememberNative(mw, mh, md)
            return WmState(mw, mh, null, null, md, null)
        }
        val size = ShellExecutor.exec("wm size")
        val density = ShellExecutor.exec("wm density")
        val state = WmState.parse(size, density, mw, mh, md)
        rememberNative(state.physicalW, state.physicalH, state.physicalDpi)
        return state
    }

    private fun rememberNative(w: Int, h: Int, dpi: Int) {
        if (AppPrefs.nativeW == 0 && w > 0) {
            AppPrefs.nativeW = w
            AppPrefs.nativeH = h
            AppPrefs.nativeDpi = dpi
        }
    }

    suspend fun apply(width: Int, height: Int, dpi: Int?, safetyNote: Boolean = true) {
        require(width in 240..7680 && height in 240..7680) { "Resolução fora do intervalo seguro." }
        ShellExecutor.exec("wm size ${width}x${height}")
        if (dpi != null && dpi > 0) {
            require(dpi in 80..640) { "DPI fora do intervalo seguro." }
            ShellExecutor.exec("wm density $dpi")
        }
        AppPrefs.lastWidth = width
        AppPrefs.lastHeight = height
        AppPrefs.lastDpi = dpi ?: 0
        AppPrefs.stretched = true
        if (safetyNote) {
            // caller opens countdown
        }
    }

    suspend fun restore() {
        ShellExecutor.exec("wm size reset")
        ShellExecutor.exec("wm density reset")
        AppPrefs.stretched = false
    }

    fun scaledDpi(nativeDpi: Int, nativeShort: Int, newShort: Int): Int {
        if (nativeShort <= 0) return nativeDpi
        return (nativeDpi.toFloat() * newShort / nativeShort).toInt().coerceIn(120, 560)
    }
}
