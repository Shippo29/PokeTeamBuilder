package com.example.poketeambuilder.data.teams

import android.content.Context
import android.content.SharedPreferences
import com.example.poketeambuilder.data.repository.PokemonUiModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Team(
    val id: Int,
    val name: String,
    val pokemons: List<PokemonUiModel>
)

object TeamManager {
    private const val PREFS = "poke_teams_prefs"
    private const val KEY_TEAMS = "teams_json"
    private const val KEY_SELECTED_TEAM = "selected_team_id"

    private fun prefs(ctx: Context): SharedPreferences = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val listType = Types.newParameterizedType(List::class.java, Team::class.java)
    private val adapter = moshi.adapter<List<Team>>(listType)

    fun getTeams(ctx: Context): List<Team> {
        val json = prefs(ctx).getString(KEY_TEAMS, null) ?: return emptyList()
        return try {
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getSelectedTeamId(ctx: Context): Int? {
        val v = prefs(ctx).getInt(KEY_SELECTED_TEAM, -1)
        return if (v == -1) null else v
    }

    fun setSelectedTeamId(ctx: Context, id: Int?) {
        if (id == null) prefs(ctx).edit().remove(KEY_SELECTED_TEAM).apply() else prefs(ctx).edit().putInt(KEY_SELECTED_TEAM, id).apply()
    }

    fun getSelectedTeam(ctx: Context): Team? {
        val id = getSelectedTeamId(ctx) ?: return null
        return getTeams(ctx).firstOrNull { it.id == id }
    }

    private fun saveTeams(ctx: Context, teams: List<Team>) {
        val json = try { adapter.toJson(teams) } catch (e: Exception) { "[]" }
        prefs(ctx).edit().putString(KEY_TEAMS, json).apply()
    }

    suspend fun createTeam(ctx: Context, name: String): Team = withContext(Dispatchers.IO) {
        val current = getTeams(ctx).toMutableList()
        val nextId = (current.maxOfOrNull { it.id } ?: 0) + 1
        val t = Team(id = nextId, name = name, pokemons = emptyList())
        current.add(t)
        saveTeams(ctx, current)
        t
    }

    suspend fun addPokemonToTeam(ctx: Context, teamId: Int, pokemon: PokemonUiModel): Boolean = withContext(Dispatchers.IO) {
        val current = getTeams(ctx).toMutableList()
        val idx = current.indexOfFirst { it.id == teamId }
        if (idx == -1) return@withContext false
        val team = current[idx]
        if (team.pokemons.size >= 6) return@withContext false
        // prevent duplicates by id
        if (team.pokemons.any { it.id == pokemon.id }) {
            // already present — consider as success
            return@withContext true
        }
        val updated = team.copy(pokemons = team.pokemons + pokemon)
        current[idx] = updated
        saveTeams(ctx, current)
        return@withContext true
    }

    suspend fun removePokemonFromTeam(ctx: Context, teamId: Int, pokemonId: Int): Boolean = withContext(Dispatchers.IO) {
        val current = getTeams(ctx).toMutableList()
        val idx = current.indexOfFirst { it.id == teamId }
        if (idx == -1) return@withContext false
        val team = current[idx]
        val updatedList = team.pokemons.filterNot { it.id == pokemonId }
        current[idx] = team.copy(pokemons = updatedList)
        saveTeams(ctx, current)
        return@withContext true
    }

    suspend fun deleteTeam(ctx: Context, teamId: Int): Boolean = withContext(Dispatchers.IO) {
        val current = getTeams(ctx).toMutableList()
        val idx = current.indexOfFirst { it.id == teamId }
        if (idx == -1) return@withContext false
        current.removeAt(idx)
        saveTeams(ctx, current)
        // if it was the selected team, clear selection
        val sel = prefs(ctx).getInt(KEY_SELECTED_TEAM, -1)
        if (sel == teamId) prefs(ctx).edit().remove(KEY_SELECTED_TEAM).apply()
        return@withContext true
    }
}
