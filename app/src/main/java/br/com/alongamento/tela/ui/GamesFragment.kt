package br.com.alongamento.tela.ui

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.alongamento.tela.R
import br.com.alongamento.tela.apps.InstalledApps
import br.com.alongamento.tela.apps.LaunchApp
import br.com.alongamento.tela.data.AppPrefs
import br.com.alongamento.tela.monitor.GameWatchService
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText

class GamesFragment : Fragment() {
    private var all = listOf<LaunchApp>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_games, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val selected = view.findViewById<TextView>(R.id.txtSelected)
        val search = view.findViewById<TextInputEditText>(R.id.edtSearch)
        val list = view.findViewById<RecyclerView>(R.id.recycler)
        val auto = view.findViewById<SwitchMaterial>(R.id.swAuto)
        val launch = view.findViewById<SwitchMaterial>(R.id.swLaunch)
        auto.isChecked = AppPrefs.autoApply
        launch.isChecked = AppPrefs.launchAfter

        fun showSelected() {
            selected.text = if (AppPrefs.selectedPackage.isBlank()) {
                getString(R.string.no_game)
            } else {
                getString(R.string.selected_game, AppPrefs.selectedLabel, AppPrefs.selectedPackage)
            }
        }

        val adapter = AppAdapter { app ->
            AppPrefs.selectedPackage = app.packageName
            AppPrefs.selectedLabel = app.label
            showSelected()
            Toast.makeText(requireContext(), getString(R.string.game_picked, app.label), Toast.LENGTH_SHORT).show()
        }
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter

        all = InstalledApps.list(requireContext().packageManager)
        adapter.submit(all)
        showSelected()

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val q = s?.toString()?.trim()?.lowercase().orEmpty()
                adapter.submit(if (q.isBlank()) all else all.filter {
                    it.label.lowercase().contains(q) || it.packageName.contains(q)
                })
            }
        })

        auto.setOnCheckedChangeListener { _, checked ->
            AppPrefs.autoApply = checked
            if (checked) {
                if (!hasUsage()) {
                    auto.isChecked = false
                    AppPrefs.autoApply = false
                    startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    Toast.makeText(requireContext(), R.string.need_usage, Toast.LENGTH_LONG).show()
                } else {
                    GameWatchService.start(requireContext())
                }
            } else {
                GameWatchService.stop(requireContext())
            }
        }
        launch.setOnCheckedChangeListener { _, c -> AppPrefs.launchAfter = c }

        view.findViewById<MaterialButton>(R.id.btnUsage).setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }

    private fun hasUsage(): Boolean {
        val appOps = requireContext().getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            requireContext().packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
}

private class AppAdapter(val onClick: (LaunchApp) -> Unit) : RecyclerView.Adapter<AppVH>() {
    private var items = listOf<LaunchApp>()
    fun submit(list: List<LaunchApp>) {
        items = list
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppVH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return AppVH(v)
    }
    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: AppVH, position: Int) {
        val item = items[position]
        holder.icon.setImageDrawable(item.icon)
        holder.title.text = item.label
        holder.sub.text = buildString {
            append(item.packageName)
            if (item.isGame) append("  ·  jogo")
        }
        holder.itemView.isSelected = item.packageName == AppPrefs.selectedPackage
        holder.itemView.setOnClickListener { onClick(item) }
    }
}

private class AppVH(v: View) : RecyclerView.ViewHolder(v) {
    val icon: ImageView = v.findViewById(R.id.imgIcon)
    val title: TextView = v.findViewById(R.id.txtTitle)
    val sub: TextView = v.findViewById(R.id.txtSub)
}
