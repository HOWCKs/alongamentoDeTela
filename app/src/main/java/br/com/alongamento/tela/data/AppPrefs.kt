package br.com.alongamento.tela.data

import android.content.Context
import android.content.SharedPreferences

object AppPrefs {
    private lateinit var p: SharedPreferences

    fun init(context: Context) {
        p = context.applicationContext.getSharedPreferences("alongamento", Context.MODE_PRIVATE)
    }

    var selectedPackage: String
        get() = p.getString("pkg", "") ?: ""
        set(v) { p.edit().putString("pkg", v).apply() }

    var selectedLabel: String
        get() = p.getString("pkg_label", "") ?: ""
        set(v) { p.edit().putString("pkg_label", v).apply() }

    var lastWidth: Int
        get() = p.getInt("w", 0)
        set(v) { p.edit().putInt("w", v).apply() }

    var lastHeight: Int
        get() = p.getInt("h", 0)
        set(v) { p.edit().putInt("h", v).apply() }

    var lastDpi: Int
        get() = p.getInt("dpi", 0)
        set(v) { p.edit().putInt("dpi", v).apply() }

    var stretched: Boolean
        get() = p.getBoolean("stretched", false)
        set(v) { p.edit().putBoolean("stretched", v).apply() }

    var adbHost: String
        get() = p.getString("adb_host", "127.0.0.1") ?: "127.0.0.1"
        set(v) { p.edit().putString("adb_host", v).apply() }

    var adbPairPort: Int
        get() = p.getInt("adb_pair_port", 0)
        set(v) { p.edit().putInt("adb_pair_port", v).apply() }

    var adbConnectPort: Int
        get() = p.getInt("adb_connect_port", 0)
        set(v) { p.edit().putInt("adb_connect_port", v).apply() }

    var nativeW: Int
        get() = p.getInt("nat_w", 0)
        set(v) { p.edit().putInt("nat_w", v).apply() }

    var nativeH: Int
        get() = p.getInt("nat_h", 0)
        set(v) { p.edit().putInt("nat_h", v).apply() }

    var nativeDpi: Int
        get() = p.getInt("nat_dpi", 0)
        set(v) { p.edit().putInt("nat_dpi", v).apply() }

    var multiplierCents: Int
        get() = p.getInt("mult", 110).coerceIn(100, 120)
        set(v) { p.edit().putInt("mult", v.coerceIn(100, 120)).apply() }

    val multiplier: Float get() = multiplierCents / 100f

    var corteLateral: Boolean
        get() = p.getBoolean("corte", false)
        set(v) { p.edit().putBoolean("corte", v).apply() }

    val mode: ProjectionMode
        get() = if (corteLateral) ProjectionMode.CORTE else ProjectionMode.ALONGAR

    fun flush() {
        p.edit()
            .putString("pkg", selectedPackage)
            .putString("pkg_label", selectedLabel)
            .putInt("mult", multiplierCents)
            .putBoolean("corte", corteLateral)
            .putInt("nat_w", nativeW)
            .putInt("nat_h", nativeH)
            .putInt("nat_dpi", nativeDpi)
            .putBoolean("stretched", stretched)
            .putInt("w", lastWidth)
            .putInt("h", lastHeight)
            .commit()
    }
}
