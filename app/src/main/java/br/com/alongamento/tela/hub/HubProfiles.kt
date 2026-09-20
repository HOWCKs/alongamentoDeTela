package br.com.alongamento.tela.hub

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import br.com.alongamento.tela.data.AppPrefs
import br.com.alongamento.tela.shell.ShellExecutor

object HubProfiles {
    var lastGameMode: Int? = null

    suspend fun apply(context: Context, profile: HubProfile): List<ActionResult> {
        val snap = PerformanceSampler.snapshot(context)
        if (snap.thermalBand == ThermalBand.HOT || snap.thermalBand == ThermalBand.CRITICAL) {
            return listOf(
                ActionResult(
                    "thermal_guard",
                    SupportState.TEMPORARILY_UNAVAILABLE,
                    "Aparelho quente (${snap.thermalBand}). Nenhuma otimização nova."
                )
            )
        }
        val out = mutableListOf<ActionResult>()
        val pkg = AppPrefs.selectedPackage
        if (pkg.isNotBlank() && PackageValidator.isSafe(pkg) && ShellExecutor.isReady()) {
            val mode = when (profile) {
                HubProfile.PERFORMANCE -> 2
                HubProfile.BATTERY -> 3
                HubProfile.BALANCED -> 1
            }
            lastGameMode = lastGameMode ?: 1
            out += PrivilegedOps.gameModeSet(pkg, mode)
        } else if (!ShellExecutor.isReady()) {
            out += ActionResult("game_mode", SupportState.SHIZUKU_REQUIRED, "Shizuku inativo — Game Mode não aplicado")
        } else {
            out += ActionResult("game_mode", SupportState.NOT_SUPPORTED, "Escolha um jogo na aba Jogos")
        }
        out += ActionResult(
            "gpu_note",
            SupportState.SUPPORTED,
            GpuInfo.note(runCatching { GpuInfo.glesRenderer() }.getOrDefault(""))
        )
        out += ActionResult(
            "disclaimer",
            SupportState.SUPPORTED,
            "Não prometemos ganho de FPS. Não alteramos clock, voltagem nem o APK do jogo."
        )
        AppPrefs.hubProfile = profile.name
        return out
    }

    suspend fun rollback(pkg: String): ActionResult {
        if (!PackageValidator.isSafe(pkg)) {
            return ActionResult("rollback", SupportState.NOT_SUPPORTED, "Pacote inválido")
        }
        return PrivilegedOps.gameModeSet(pkg, lastGameMode ?: 1)
    }

    fun openBatteryUnrestricted(context: Context) {
        val pkg = context.packageName
        if (Build.VERSION.SDK_INT >= 23) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(pkg)) {
                val i = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$pkg"))
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(i) }
            } else {
                val i = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(i) }
            }
        }
    }
}
