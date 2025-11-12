package com.example.poketeambuilder.ui.pokedex

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import coil.load
import com.example.poketeambuilder.R
import com.example.poketeambuilder.data.teams.TeamManager
import com.example.poketeambuilder.data.repository.PokemonUiModel
import com.example.poketeambuilder.data.repository.PokemonRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class TeamSelectionBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_POKEMON_ID = "arg_pokemon_id"
        fun newInstance(pokemonId: Int): TeamSelectionBottomSheet {
            val b = Bundle()
            b.putInt(ARG_POKEMON_ID, pokemonId)
            val f = TeamSelectionBottomSheet()
            f.arguments = b
            return f
        }
    }

    private var pokemonId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { pokemonId = it.getInt(ARG_POKEMON_ID, 0) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_team_selector, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val teamsContainer = view.findViewById<LinearLayout>(R.id.teams_container)
        val tvNo = view.findViewById<TextView>(R.id.tv_no_teams)
        val btnCreate = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_create_team)

        refreshTeams(teamsContainer, tvNo)

        btnCreate.setOnClickListener {
            // ask for a name quickly using AlertDialog and create off main thread
            val ctx = requireContext()
            val input = android.widget.EditText(ctx).apply { hint = "Nombre del equipo" }
            AlertDialog.Builder(ctx)
                .setTitle("Crear equipo")
                .setView(input)
                .setPositiveButton("Crear") { d, _ ->
                    val name = input.text.toString().ifBlank { "Equipo" }
                    // create team in IO
                    viewLifecycleOwner.lifecycleScope.launch {
                        val t = try {
                            TeamManager.createTeam(ctx, name)
                        } catch (e: Exception) {
                            null
                        }
                        if (t != null) {
                            Toast.makeText(ctx, "Equipo '${t.name}' creado", Toast.LENGTH_SHORT).show()
                            refreshTeams(teamsContainer, tvNo)
                        } else {
                            Toast.makeText(ctx, "No se pudo crear el equipo", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun refreshTeams(container: LinearLayout, tvNo: TextView) {
        container.removeAllViews()
        val teams = TeamManager.getTeams(requireContext())
        if (teams.isEmpty()) {
            tvNo.visibility = View.VISIBLE
            return
        }
        tvNo.visibility = View.GONE
        teams.forEach { t ->
            val tv = TextView(requireContext()).apply {
                text = "${t.name} (${t.pokemons.size}/6)"
                setPadding(20,20,20,20)
                textSize = 16f
                setOnClickListener {
                    // perform fetch+save off the main thread
                    val ownerScope = viewLifecycleOwner.lifecycleScope
                    ownerScope.launch {
                        val ctx = requireContext()
                        val success = try {
                            withContext(Dispatchers.IO) {
                                val repo = PokemonRepository()
                                val p = repo.getPokemonCached(pokemonId.toString())
                                TeamManager.addPokemonToTeam(ctx, t.id, p)
                            }
                        } catch (e: Exception) {
                            false
                        }

                        if (success) {
                            Toast.makeText(requireContext(), "Pokémon agregado a ${t.name}", Toast.LENGTH_SHORT).show()
                            dismiss()
                        } else {
                            Toast.makeText(requireContext(), "Este equipo ya tiene 6 Pokémon. No puedes agregar más.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            container.addView(tv)
        }
    }

}
