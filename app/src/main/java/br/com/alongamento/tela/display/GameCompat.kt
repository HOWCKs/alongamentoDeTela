package br.com.alongamento.tela.display

import br.com.alongamento.tela.data.AppPrefs
import br.com.alongamento.tela.hub.PackageValidator
import br.com.alongamento.tela.shell.ShellExecutor

/**
 * Per-app letterbox bypass. Pixel squash (Sx ≠ Sy) is not in AOSP;
 * FORCE_RESIZE_APP + NEVER_SANDBOX_DISPLAY_APIS is what fills Unity/UNITE.
 * https://developer.android.com/guide/practices/device-compatibility-mode
 */
object GameCompat {
    private val enableIds = listOf(
        "FORCE_RESIZE_APP",
        "OVERRIDE_MIN_ASPECT_RATIO",
        "OVERRIDE_MIN_ASPECT_RATIO_TO_ALIGN_WITH_SPLIT_SCREEN",
        "OVERRIDE_ANY_ORIENTATION_TO_USER",
        "NEVER_SANDBOX_DISPLAY_APIS"
    )

    private val disableIds = listOf(
        "OVERRIDE_MIN_ASPECT_RATIO_PORTRAIT_ONLY",
        "FORCE_NON_RESIZE_APP"
    )

    suspend fun applyForSelected(): List<String> {
        val pkg = AppPrefs.selectedPackage
        if (!PackageValidator.isSafe(pkg)) return listOf("sem pacote")
        val log = mutableListOf<String>()
        enableIds.forEach { id ->
            log += runCompat("enable", id, pkg)
        }
        disableIds.forEach { id ->
            log += runCompat("disable", id, pkg)
        }
        runCatching {
            ShellExecutor.exec("wm set-letterbox-style --cornerRadius 0")
        }
        AppPrefs.compatPackage = pkg
        return log
    }

    suspend fun rollback(): List<String> {
        val pkg = AppPrefs.compatPackage.ifBlank { AppPrefs.selectedPackage }
        if (!PackageValidator.isSafe(pkg)) return emptyList()
        val log = mutableListOf<String>()
        (enableIds + disableIds).forEach { id ->
            log += runCompat("reset", id, pkg)
        }
        runCatching { ShellExecutor.exec("wm reset-letterbox-style") }
        AppPrefs.compatPackage = ""
        return log
    }

    private suspend fun runCompat(op: String, id: String, pkg: String): String {
        if (id !in enableIds && id !in disableIds) return "$id bloqueado"
        if (op !in setOf("enable", "disable", "reset")) return "op inválida"
        return runCatching {
            val out = ShellExecutor.exec("am compat $op $id $pkg")
            "$op $id: ${out.ifBlank { "ok" }.take(80)}"
        }.getOrElse { "$op $id: ${it.message}" }
    }
}
