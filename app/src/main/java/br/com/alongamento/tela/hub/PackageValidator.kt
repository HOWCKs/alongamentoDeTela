package br.com.alongamento.tela.hub

object PackageValidator {
    private val re = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$")

    fun isSafe(pkg: String): Boolean {
        if (pkg.length !in 3..180) return false
        if (pkg.contains("..") || pkg.contains("/") || pkg.contains(" ") || pkg.contains(";")) return false
        return re.matches(pkg)
    }
}
