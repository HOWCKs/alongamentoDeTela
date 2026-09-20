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
import br.com.alongamento.tela.R
import br.com.alongamento.tela.overlay.GameSession
import br.com.alongamento.tela.shell.ShellBackend
import br.com.alongamento.tela.shell.ShellExecutor
import com.google.android.material.button.MaterialButton
import rikka.shizuku.Shizuku

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
            runCatching { startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) }
        }
    }

    private fun bindStatus(view: View) {
        view.findViewById<TextView>(R.id.txtActStatus).text = when (ShellExecutor.backend()) {
            ShellBackend.SHIZUKU -> getString(R.string.ready_shizuku)
            ShellBackend.ADB -> getString(R.string.ready_adb)
            ShellBackend.NONE -> getString(R.string.not_ready)
        }
        val overlayOk = GameSession.canDraw(requireContext())
        view.findViewById<TextView>(R.id.txtOverlay).text =
            if (overlayOk) getString(R.string.overlay_perm_ok) else getString(R.string.overlay_perm_body)
        val shizukuOk = ShellExecutor.isShizukuReady()
        view.findViewById<TextView>(R.id.txtShizukuStatus).text =
            if (shizukuOk) getString(R.string.shizuku_status_ok) else getString(R.string.shizuku_status_off)
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

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
    }
}
