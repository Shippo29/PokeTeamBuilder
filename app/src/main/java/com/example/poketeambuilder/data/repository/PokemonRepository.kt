package com.example.poketeambuilder.data.repository

import com.example.poketeambuilder.data.model.*
import com.example.poketeambuilder.data.network.RetrofitClient
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class PokemonRepository {
    private val api = RetrofitClient.api

    companion object {
        // Tamaño de página por defecto — ajustar si es necesario
        const val PAGE_SIZE = 50
    }

    // Caché en memoria para evitar reconsultas repetidas mientras la app corre
    private val memoryCache = mutableMapOf<Int, PokemonUiModel>()
    // Caché de nombres completos (para búsqueda parcial sin descargar todos los detalles)
    private var allNamesCache: List<String>? = null

    // Funciones públicas básicas
    suspend fun getTypes(): List<String> {
        val resp = api.getTypes()
        return resp.results.map { it.name }.filter { !it.equals("stellar", ignoreCase = true) && !it.equals("unknown", ignoreCase = true) }
    }

    suspend fun getGenerationSpecies(genId: Int): List<NamedResource> {
        val gen = api.getGeneration(genId)
        return gen.pokemonSpecies.sortedBy { it.name }
    }

    suspend fun getPokemonDetail(name: String): PokemonResponse = api.getPokemon(name)

    suspend fun getPokemonSpecies(name: String): PokemonSpeciesResponse = api.getPokemonSpecies(name)

    // --- New: paginated listing using /pokemon?limit=&offset= ---
    suspend fun getPokemonPage(offset: Int, limit: Int = PAGE_SIZE): List<PokemonUiModel> = coroutineScope {
        val listResp = api.listPokemon(limit, offset)
        val names = listResp.results.map { it.name }

    // Limitar peticiones concurrentes para no sobrecargar la API ni el dispositivo
        val semaphore = Semaphore(8)

        val deferred = names.map { name ->
            async {
                semaphore.withPermit {
                    try {
                        getPokemonCached(name)
                    } catch (e: Exception) {
                        // en caso de fallo devolver un marcador que luego se filtrará
                        PokemonUiModel(
                            id = 0,
                            name = name.replaceFirstChar { it.uppercaseChar() },
                            imageUrl = "",
                            types = emptyList(),
                            description = "",
                            category = "",
                            stats = emptyList(),
                            abilities = emptyList()
                        )
                    }
                }
            }
        }

        val results = deferred.mapNotNull { it.await() }
        results.filter { it.id > 0 }.sortedBy { it.id }
    }

    // Obtener o recuperar con caché en memoria
    suspend fun getPokemonCached(nameOrId: String): PokemonUiModel = coroutineScope {
        val p = api.getPokemon(nameOrId)
    // si ya está en caché por id, devolver rápidamente
        memoryCache[p.id]?.let { return@coroutineScope it }

        val speciesInfo = try {
            api.getPokemonSpecies(nameOrId)
        } catch (e: Exception) {
            null
        }

        val desc = speciesInfo?.flavorTextEntries?.firstOrNull { it.language.name == "es" }
            ?: speciesInfo?.flavorTextEntries?.firstOrNull { it.language.name == "en" }
            val descText = desc?.flavorText?.replace(Regex("\\s+"), " ") ?: ""
        val genus = speciesInfo?.genera?.firstOrNull { it.language.name == "es" }
            ?: speciesInfo?.genera?.firstOrNull { it.language.name == "en" }
        val category = genus?.genus ?: ""

        val ui = PokemonUiModel(
            id = p.id,
            name = p.name.replaceFirstChar { it.uppercaseChar() },
            imageUrl = p.sprites.other?.officialArtwork?.frontDefault ?: "",
            types = p.types.map { it.type.name },
            description = descText,
            category = category,
            stats = p.stats.map { it.baseStat },
            abilities = p.abilities.map { it.ability.name }
        )

        memoryCache[ui.id] = ui
        ui
    }

    suspend fun loadPokemonForGeneration(genId: Int): List<PokemonUiModel> = coroutineScope {
        // Obtener nombres por generación y luego detalles con concurrencia limitada
        val species = getGenerationSpecies(genId)
        val semaphore = Semaphore(8)
        val deferred = species.map { s ->
            async {
                semaphore.withPermit {
                    try {
                        getPokemonCached(s.name)
                    } catch (e: Exception) {
                        PokemonUiModel(
                            id = 0,
                            name = s.name.replaceFirstChar { it.uppercaseChar() },
                            imageUrl = "",
                            types = emptyList(),
                            description = "",
                            category = "",
                            stats = emptyList(),
                            abilities = emptyList()
                        )
                    }
                }
            }
        }
        val results = deferred.mapNotNull { it.await() }
        results.filter { it.id > 0 }.sortedBy { it.id }
    }

    suspend fun loadAllGenerations(): List<PokemonUiModel> {
        val acc = mutableListOf<PokemonUiModel>()
        for (g in 1..8) {
            try {
                val list = loadPokemonForGeneration(g)
                acc.addAll(list)
            } catch (e: Exception) {
                // si una generación falla, continuamos con las demás
            }
        }
        return acc.distinctBy { it.id }.sortedBy { it.id }
    }

    // Buscar por fragmento de nombre (case-insensitive). Strategy:
    // 1. Obtener lista de nombres (cacheada). 2. Filtrar nombres que contienen la query.
    // 3. Recuperar detalles sólo de los nombres coincidentes (concurrencia limitada).
    suspend fun searchPokemonByNamePartial(query: String, maxResults: Int = 100): List<PokemonUiModel> = coroutineScope {
        // Obtener lista de todos los nombres (si no se tiene, pedir con un limit grande)
        val names = allNamesCache ?: run {
            val resp = api.listPokemon(2000, 0)
            val list = resp.results.map { it.name }
            allNamesCache = list
            list
        }

        val matched = names.filter { it.contains(query, ignoreCase = true) }.take(maxResults)
        val semaphore = Semaphore(8)
        val deferred = matched.map { name ->
            async {
                semaphore.withPermit {
                    try {
                        getPokemonCached(name)
                    } catch (e: Exception) {
                        null
                    }
                }
            }
        }
        deferred.mapNotNull { it.await() }.sortedBy { it.id }
    }
}

// UI model used by ViewModel/Adapter
data class PokemonUiModel(
    val id: Int,
    val name: String,
    val imageUrl: String,
    val types: List<String>,
    val description: String,
    val category: String,
    val stats: List<Int>,
    val abilities: List<String>
) : java.io.Serializable {
    fun toBundle(): android.os.Bundle {
        val b = android.os.Bundle()
        b.putInt("id", id)
        b.putString("name", name)
        b.putString("imageUrl", imageUrl)
        b.putStringArrayList("types", ArrayList(types))
    b.putString("description", description)
    b.putString("category", category)
        b.putIntegerArrayList("stats", ArrayList(stats))
        b.putStringArrayList("abilities", ArrayList(abilities))
        return b
    }

    companion object {
        fun fromBundle(b: android.os.Bundle): PokemonUiModel {
            return PokemonUiModel(
                id = b.getInt("id", 0),
                name = b.getString("name") ?: "",
                imageUrl = b.getString("imageUrl") ?: "",
                types = b.getStringArrayList("types") ?: emptyList<String>(),
                description = b.getString("description") ?: "",
                category = b.getString("category") ?: "",
                stats = b.getIntegerArrayList("stats") ?: emptyList<Int>(),
                abilities = b.getStringArrayList("abilities") ?: emptyList<String>()
            )
        }
    }
}
