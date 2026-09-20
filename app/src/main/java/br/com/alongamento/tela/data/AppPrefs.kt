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

    var autoDpi: Boolean
        get() = p.getBoolean("auto_dpi", false)
        set(v) { p.edit().putBoolean("auto_dpi", v).apply() }

    var autoApply: Boolean
        get() = p.getBoolean("auto_apply", false)
        set(v) { p.edit().putBoolean("auto_apply", v).apply() }

    var launchAfter: Boolean
        get() = p.getBoolean("launch_after", true)
        set(v) { p.edit().putBoolean("launch_after", v).apply() }

    var safetySeconds: Int
        get() = p.getInt("safety", 12)
        set(v) { p.edit().putInt("safety", v).apply() }

    var safetyEnabled: Boolean
        get() = p.getBoolean("safety_on", true)
        set(v) { p.edit().putBoolean("safety_on", v).apply() }

    var stretched: Boolean
        get() = p.getBoolean("stretched", false)
        set(v) { p.edit().putBoolean("stretched", v).apply() }

    var lastRatioId: String
        get() = p.getString("ratio", "21:9") ?: "21:9"
        set(v) { p.edit().putString("ratio", v).apply() }

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
}
