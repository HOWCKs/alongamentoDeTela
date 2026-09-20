package br.com.alongamento.tela.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import br.com.alongamento.tela.R
import br.com.alongamento.tela.adb.AlongamentoAdb
import br.com.alongamento.tela.overlay.GameSession
import br.com.alongamento.tela.data.AppPrefs
import br.com.alongamento.tela.shell.ShellBackend
import br.com.alongamento.tela.shell.ShellExecutor
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import io.github.muntashirakon.adb.android.AdbMdns
import io.github.muntashirakon.adb.android.AndroidUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class ActivationFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_activation, container, false)
    }

    override fun onResume() {
        super.onResume()
        view?.let { bindStatus(it) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindStatus(view)
        view.findViewById<MaterialButton>(R.id.btnOverlayPerm).setOnClickListener {
            GameSession.requestOverlayPermission(requireActivity())
        }
        view.findViewById<MaterialButton>(R.id.btnShizukuAuth).setOnClickListener {
            if (!Shizuku.pingBinder()) {
                toast(getString(R.string.shizuku_off))
                openShizuku()
            } else {
                ShellExecutor.requestShizukuPermission(4001)
            }
        }
        view.findViewById<MaterialButton>(R.id.btnOpenShizuku).setOnClickListener { openShizuku() }
        view.findViewById<MaterialButton>(R.id.btnDevOptions).setOnClickListener {
            startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
        }
        val host = view.findViewById<TextInputEditText>(R.id.edtHost)
        val pairPort = view.findViewById<TextInputEditText>(R.id.edtPairPort)
        val code = view.findViewById<TextInputEditText>(R.id.edtCode)
        val connPort = view.findViewById<TextInputEditText>(R.id.edtConnPort)
        host.setText(AppPrefs.adbHost.ifBlank { "127.0.0.1" })
        if (AppPrefs.adbPairPort > 0) pairPort.setText(AppPrefs.adbPairPort.toString())
        if (AppPrefs.adbConnectPort > 0) connPort.setText(AppPrefs.adbConnectPort.toString())

        view.findViewById<MaterialButton>(R.id.btnDiscover).setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val ip = withContext(Dispatchers.IO) {
                        AndroidUtils.getHostIpAddress(requireContext())
                    }
                    host.setText(ip)
                    val p = discover(AdbMdns.SERVICE_TYPE_TLS_PAIRING)
                    if (p > 0) pairPort.setText(p.toString())
                    val c = discover(AdbMdns.SERVICE_TYPE_TLS_CONNECT)
                    if (c > 0) connPort.setText(c.toString())
                    toast(getString(R.string.discovered, ip, p, c))
                } catch (e: Exception) {
                    toast(e.message ?: "falha")
                }
            }
        }
        view.findViewById<MaterialButton>(R.id.btnPair).setOnClickListener {
            val h = host.text.toString().trim()
            val p = pairPort.text.toString().toIntOrNull()
            val c = code.text.toString().trim()
            if (h.isBlank() || p == null || c.length < 6) {
                toast("Preencha IP, porta de pareamento e o código de 6 dígitos")
                return@setOnClickListener
            }
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    AlongamentoAdb.pair(requireContext(), h, p, c)
                    toast(getString(R.string.paired))
                    bindStatus(view)
                } catch (e: Exception) {
                    toast(e.message ?: "falha")
                }
            }
        }
        view.findViewById<MaterialButton>(R.id.btnConnect).setOnClickListener {
            val h = host.text.toString().trim()
            val p = connPort.text.toString().toIntOrNull()
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    if (p != null && p > 0) {
                        AlongamentoAdb.connect(requireContext(), h, p)
                    } else {
                        AlongamentoAdb.autoConnect(requireContext())
                    }
                    val out = AlongamentoAdb.shell("id")
                    toast(getString(R.string.adb_ok, out.take(80)))
                    bindStatus(view)
                } catch (e: Exception) {
                    toast(e.message ?: "falha")
                }
            }
        }
    }

    private fun bindStatus(view: View) {
        val txt = view.findViewById<TextView>(R.id.txtActStatus)
        txt.text = when (ShellExecutor.backend()) {
            ShellBackend.SHIZUKU -> getString(R.string.ready_shizuku)
            ShellBackend.ADB -> getString(R.string.ready_adb)
            ShellBackend.NONE -> getString(R.string.not_ready)
        }
    }

    private fun openShizuku() {
        val pm = requireContext().packageManager
        val launch = pm.getLaunchIntentForPackage("moe.shizuku.privileged.api")
        if (launch != null) startActivity(launch)
        else {
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/"))
            )
        }
    }

    private suspend fun discover(type: String): Int = withContext(Dispatchers.IO) {
        val port = AtomicInteger(-1)
        val latch = CountDownLatch(1)
        val mdns = AdbMdns(requireContext(), type) { _, p ->
            port.set(p)
            latch.countDown()
        }
        mdns.start()
        latch.await(8, TimeUnit.SECONDS)
        mdns.stop()
        port.get()
    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
    }
}
