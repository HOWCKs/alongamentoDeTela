package br.com.alongamento.tela.hub

import br.com.alongamento.tela.shell.ShellExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

object PrivilegedOps {
    suspend fun gfxInfo(pkg: String): String {
        require(PackageValidator.isSafe(pkg))
        return run("dumpsys gfxinfo $pkg")
    }

    suspend fun surfaceFlinger(): String = run("dumpsys SurfaceFlinger")

    suspend fun gameModeGet(pkg: String): String {
        require(PackageValidator.isSafe(pkg))
        return run("cmd game mode $pkg")
    }

    suspend fun gameModeSet(pkg: String, mode: Int): ActionResult {
        if (mode !in 1..3) {
            return ActionResult("game_mode", SupportState.NOT_SUPPORTED, "Modo inválido")
        }
        if (!PackageValidator.isSafe(pkg)) {
            return ActionResult("game_mode", SupportState.NOT_SUPPORTED, "Pacote inválido")
        }
        if (!ShellExecutor.isReady()) {
            return ActionResult("game_mode", SupportState.SHIZUKU_REQUIRED, "Shizuku necessário", false)
        }
        return try {
            val out = run("cmd game mode $mode $pkg")
            val low = out.lowercase()
            if (low.contains("unknown") || low.contains("not found") || low.contains("exception")) {
                ActionResult("game_mode", SupportState.OEM_RESTRICTED, "Game Mode indisponível neste aparelho: $out")
            } else {
                ActionResult("game_mode", SupportState.SUPPORTED, "Game Mode $mode aplicado em $pkg", reversible = true)
            }
        } catch (e: Exception) {
            ActionResult("game_mode", SupportState.NOT_SUPPORTED, e.message ?: "falha")
        }
    }

    private suspend fun run(command: String): String = withContext(Dispatchers.IO) {
        withTimeout(4000) { ShellExecutor.exec(command) }
    }
}
