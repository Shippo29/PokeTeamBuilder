package com.example.poketeambuilder.ui.home

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import coil.load
import androidx.gridlayout.widget.GridLayout
import com.example.poketeambuilder.R
import com.example.poketeambuilder.data.teams.TeamManager

class TeamsListDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val ctx = requireContext()
        val teams = TeamManager.getTeams(ctx)

        val builder = AlertDialog.Builder(ctx)
        builder.setTitle("Equipos")

        val content = LayoutInflater.from(ctx).inflate(R.layout.dialog_team_selector, null)
        val container = content.findViewById<LinearLayout>(R.id.teams_container)
        val tvNo = content.findViewById<TextView>(R.id.tv_no_teams)

        if (teams.isEmpty()) {
            tvNo.visibility = View.VISIBLE
        } else {
            tvNo.visibility = View.GONE
            teams.forEach { t ->
                val tv = TextView(ctx).apply {
                    text = "${t.name} (${t.pokemons.size}/6)"
                    setPadding(20,20,20,20)
                    textSize = 16f
                    // clicking selects the team as active
                    setOnClickListener {
                        com.example.poketeambuilder.data.teams.TeamManager.setSelectedTeamId(ctx, t.id)
                        // notify HomeFragment (or any listener) about selection so UI can refresh immediately
                        androidx.core.os.bundleOf()
                        parentFragmentManager.setFragmentResult("team_selected", androidx.core.os.bundleOf("teamId" to t.id))
                        Toast.makeText(ctx, "Equipo '${t.name}' seleccionado", Toast.LENGTH_SHORT).show()
                        dismiss()
                    }
                    // long click shows detailed view without changing selection
                    setOnLongClickListener {
                        showTeamView(t.name)
                        true
                    }
                }
                container.addView(tv)
            }
        }

        builder.setView(content)
        builder.setPositiveButton("Cerrar", null)
        return builder.create()
    }

    private fun showTeamView(teamName: String) {
        val ctx = requireContext()
        val t = TeamManager.getTeams(ctx).firstOrNull { it.name == teamName } ?: run {
            Toast.makeText(ctx, "Equipo no encontrado", Toast.LENGTH_SHORT).show(); return
        }
        val d = AlertDialog.Builder(ctx).create()
        val v = LayoutInflater.from(ctx).inflate(R.layout.dialog_team_view, null)
        val title = v.findViewById<TextView>(R.id.tv_team_title)
        val pokemonsGrid = v.findViewById<androidx.gridlayout.widget.GridLayout>(R.id.pokemons_grid)
        title.text = t.name
        pokemonsGrid.removeAllViews()
        // ensure we display up to 6 slots; if fewer than 6, show placeholders
        val slots = (0 until 6)
        slots.forEachIndexed { idx, _ ->
            val p = t.pokemons.getOrNull(idx)
            val iv = android.widget.ImageView(ctx).apply {
                val size = (96 * resources.displayMetrics.density).toInt()
                layoutParams = androidx.gridlayout.widget.GridLayout.LayoutParams().apply {
                    width = size
                    height = size
                    setMargins(8,8,8,8)
                }
                scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
                if (p != null && p.imageUrl.isNotEmpty()) this.load(p.imageUrl) else this.setImageResource(R.drawable.ic_launcher_foreground)
                contentDescription = p?.name ?: "vacío"
            }
            pokemonsGrid.addView(iv)
        }
        d.setView(v)
        d.setButton(AlertDialog.BUTTON_POSITIVE, "Cerrar") { dlg, _ -> dlg.dismiss() }
        d.show()
    }
}
