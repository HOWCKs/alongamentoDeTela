package br.com.alongamento.tela.hub

object DiagnosticEngine {
    fun evaluate(s: PerformanceSnapshot): DiagResult {
        if (s.fps <= 0f && s.cpuUsage <= 0f) {
            return DiagResult(
                DiagKind.UNKNOWN,
                "Dados insuficientes",
                "FPS ${s.fps} · fonte ${s.source}",
                "baixa",
                "Deixe o Hub aberto no jogo por 10 s. Sem Shizuku o FPS é só da bolha, não do jogo."
            )
        }
        if (s.thermalBand == ThermalBand.HOT || s.thermalBand == ThermalBand.CRITICAL ||
            s.thermalBand == ThermalBand.THROTTLING_SUSPECTED
        ) {
            return DiagResult(
                DiagKind.THERMAL_BOUND,
                "Aquecimento",
                "Banda ${s.thermalBand} · ${"%.1f".format(s.batteryTemperature)} °C",
                "média",
                "Pare otimizações novas. Prefira Equilibrado ou Economia. Não prometemos FPS extra."
            )
        }
        if (s.memoryAvailMb in 1..350) {
            return DiagResult(
                DiagKind.MEMORY_PRESSURE,
                "Pouca RAM livre",
                "${s.memoryAvailMb} MB livres",
                "média",
                "Feche apps em segundo plano manualmente. Não encerramos serviços do sistema."
            )
        }
        if (s.refreshRate <= 61f && s.averageFps in 55f..61f) {
            return DiagResult(
                DiagKind.REFRESH_RATE_LIMIT,
                "Teto da tela",
                "Hz ${s.refreshRate} · média ${"%.0f".format(s.averageFps)}",
                "média",
                "A tela está em ~60 Hz. O jogo não passa disso neste modo."
            )
        }
        if (s.cpuUsage >= 85f && s.fps in 1f..50f) {
            return DiagResult(
                DiagKind.CPU_BOUND,
                "CPU alta com FPS baixo",
                "CPU ${"%.0f".format(s.cpuUsage)}% · FPS ${"%.0f".format(s.fps)}",
                "baixa",
                "Possível gargalo de CPU. Reduza qualidade no jogo. Não forçamos clock."
            )
        }
        if (s.jankPercentage >= 12f) {
            return DiagResult(
                DiagKind.GPU_BOUND,
                "Muitos frames acima do orçamento",
                "jank ${"%.1f".format(s.jankPercentage)}%",
                "baixa",
                "Pode ser GPU ou o próprio jogo limitando. Fonte: ${s.source}."
            )
        }
        if (s.fps > 0f && s.thermalBand == ThermalBand.COOL && s.cpuUsage < 70f) {
            return DiagResult(
                DiagKind.GAME_LIMIT,
                "Possível limite interno do jogo",
                "FPS estável ${"%.0f".format(s.averageFps)} · temp ok",
                "baixa",
                "Se o FPS fica preso num teto com o aparelho frio, o título costuma limitar sozinho."
            )
        }
        return DiagResult(
            DiagKind.UNKNOWN,
            "Sem gargalo claro",
            "FPS ${"%.0f".format(s.fps)} · CPU ${"%.0f".format(s.cpuUsage)}%",
            "baixa",
            "Monitorando. O Hub não aumenta FPS por mágica."
        )
    }
}
