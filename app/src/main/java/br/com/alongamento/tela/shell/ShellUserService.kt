package br.com.alongamento.tela.shell

import android.content.Context
import android.os.RemoteException
import androidx.annotation.Keep
import br.com.alongamento.tela.IShellService
import java.io.BufferedReader
import java.io.InputStreamReader

class ShellUserService : IShellService.Stub {
    constructor()

    @Keep
    constructor(@Suppress("UNUSED_PARAMETER") context: Context)

    override fun destroy() {
        System.exit(0)
    }

    @Throws(RemoteException::class)
    override fun exec(command: String): String {
        val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
        val stdout = BufferedReader(InputStreamReader(process.inputStream)).readText()
        val stderr = BufferedReader(InputStreamReader(process.errorStream)).readText()
        process.waitFor()
        return (stdout + stderr).trim()
    }
}
