package br.com.alongamento.tela.ui

import android.os.Bundle
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
import br.com.alongamento.tela.overlay.GameSession
import br.com.alongamento.tela.overlay.SessionStart
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class GamesFragment : Fragment() {
    private var all = listOf<LaunchApp>()
    private var adapter: AppAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_games, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val selected = view.findViewById<TextView>(R.id.txtSelected)
        val search = view.findViewById<TextInputEditText>(R.id.edtSearch)
        val list = view.findViewById<RecyclerView>(R.id.recycler)

        fun showSelected() {
            selected.text = if (AppPrefs.selectedPackage.isBlank()) {
                getString(R.string.no_game)
            } else {
                getString(R.string.selected_game, AppPrefs.selectedLabel, AppPrefs.selectedPackage)
            }
            adapter?.notifyDataSetChanged()
        }

        adapter = AppAdapter { app ->
            AppPrefs.selectedPackage = app.packageName
            AppPrefs.selectedLabel = app.label
            showSelected()
            Toast.makeText(requireContext(), getString(R.string.game_picked, app.label), Toast.LENGTH_SHORT).show()
        }
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter
        list.setHasFixedSize(true)

        all = InstalledApps.list(requireContext().packageManager)
        adapter?.submit(all)
        showSelected()

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val q = s?.toString()?.trim()?.lowercase().orEmpty()
                adapter?.submit(if (q.isBlank()) all else all.filter {
                    it.label.lowercase().contains(q) || it.packageName.contains(q)
                })
            }
        })

        view.findViewById<MaterialButton>(R.id.btnOpenGame).setOnClickListener {
            when (val r = GameSession.prepare(requireContext())) {
                SessionStart.Ok -> Toast.makeText(requireContext(), R.string.session_started, Toast.LENGTH_LONG).show()
                SessionStart.NeedShell -> Toast.makeText(requireContext(), R.string.need_shell, Toast.LENGTH_LONG).show()
                SessionStart.NeedGame -> Toast.makeText(requireContext(), R.string.need_game, Toast.LENGTH_LONG).show()
                SessionStart.NeedOverlay -> {
                    Toast.makeText(requireContext(), R.string.need_overlay, Toast.LENGTH_LONG).show()
                    GameSession.requestOverlayPermission(requireActivity())
                }
                is SessionStart.Error -> Toast.makeText(requireContext(), r.message, Toast.LENGTH_LONG).show()
            }
        }
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
