package br.com.alongamento.tela.shell

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import br.com.alongamento.tela.BuildConfig
import br.com.alongamento.tela.IShellService
import br.com.alongamento.tela.adb.AlongamentoAdb
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import rikka.shizuku.Shizuku
import java.io.InputStreamReader

enum class ShellBackend { SHIZUKU, ADB, NONE }

object ShellExecutor {
    private lateinit var appContext: Context
    private val mutex = Mutex()
    @Volatile private var userService: IShellService? = null
    private var bound = false
    private var connection: ServiceConnection? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun backend(): ShellBackend {
        if (isShizukuReady()) return ShellBackend.SHIZUKU
        if (AlongamentoAdb.isConnected()) return ShellBackend.ADB
        return ShellBackend.NONE
    }

    fun isReady(): Boolean = backend() != ShellBackend.NONE

    fun isShizukuReady(): Boolean {
        return try {
            Shizuku.pingBinder() &&
                !Shizuku.isPreV11() &&
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }
    }

    fun requestShizukuPermission(code: Int) {
        try {
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(code)
            }
        } catch (_: Throwable) {
        }
    }

    suspend fun exec(command: String): String = mutex.withLock {
        when {
            isShizukuReady() -> execShizuku(command)
            AlongamentoAdb.isConnected() -> AlongamentoAdb.shell(command)
            else -> error("Nenhuma permissão de shell. Ative o Shizuku ou a depuração Wi‑Fi.")
        }
    }

    private suspend fun execShizuku(command: String): String {
        bindUserService()
        val svc = userService
        if (svc != null) {
            return withContext(Dispatchers.IO) { svc.exec(command) }
        }
        return execNewProcess(command)
    }

    private suspend fun bindUserService() {
        if (userService != null) return
        val done = CompletableDeferred<IShellService?>()
        val args = Shizuku.UserServiceArgs(
            ComponentName(appContext.packageName, ShellUserService::class.java.name)
        )
            .daemon(false)
            .processNameSuffix("shell")
            .debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)

        val conn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val svc = IShellService.Stub.asInterface(service)
                userService = svc
                bound = true
                if (!done.isCompleted) done.complete(svc)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                userService = null
                bound = false
            }
        }
        connection = conn
        try {
            Shizuku.bindUserService(args, conn)
            withTimeout(8_000) { done.await() }
        } catch (_: Throwable) {
            userService = null
        }
    }

    private fun execNewProcess(command: String): String {
        val method = Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java
        )
        method.isAccessible = true
        val process = method.invoke(null, arrayOf("sh", "-c", command), null, null)
            ?: error("Shizuku.newProcess indisponível")
        val input = process.javaClass.getMethod("getInputStream").invoke(process) as java.io.InputStream
        val error = process.javaClass.getMethod("getErrorStream").invoke(process) as java.io.InputStream
        val out = InputStreamReader(input).readText()
        val err = InputStreamReader(error).readText()
        try {
            process.javaClass.getMethod("waitFor").invoke(process)
        } catch (_: Throwable) {
        }
        return (out + err).trim()
    }
}
