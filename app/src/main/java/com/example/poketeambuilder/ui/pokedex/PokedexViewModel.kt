package com.example.poketeambuilder.ui.pokedex

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.poketeambuilder.data.repository.PokemonRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PokedexViewModel : ViewModel() {
    private val repo = PokemonRepository()

    private val _pokemonList = MutableStateFlow<List<com.example.poketeambuilder.data.repository.PokemonUiModel>>(emptyList())
    val pokemonList: StateFlow<List<com.example.poketeambuilder.data.repository.PokemonUiModel>> = _pokemonList

    private val _types = MutableStateFlow<List<String>>(emptyList())
    val types: StateFlow<List<String>> = _types

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var currentGeneration = 1
    private var currentTypeFilter: String? = null
    private var currentOffset = 0
    private val pageSize = PokemonRepository.PAGE_SIZE
    private var endReached = false

    init {
        loadTypes()
        // Iniciar con una carga paginada rápida del Pokédex
        loadFirstPage()
    }

    private fun loadAllOrFallback() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val all = repo.loadAllGenerations()
                _pokemonList.value = applyFilters(all)
            } catch (e: Exception) {
                // fallback a una generación (p. ej. Gen 1)
                loadGeneration(currentGeneration)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadTypes() {
        viewModelScope.launch {
            try {
                val t = repo.getTypes()
                _types.value = t
            } catch (e: Exception) {
                // ignorar error
            }
        }
    }

    private fun loadFirstPage() {
        currentOffset = 0
        endReached = false
        _pokemonList.value = emptyList()
        loadNextPage()
    }

    fun loadNextPage() {
    // si hay filtro de generación activo, saltar la carga paginada del Pokédex
        if (currentTypeFilter != null) return
        if (endReached) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val page = repo.getPokemonPage(currentOffset, pageSize)
                if (page.isEmpty()) {
                    endReached = true
                } else {
                    val combined = _pokemonList.value + page
                    _pokemonList.value = applyFilters(combined)
                    currentOffset += pageSize
                }
            } catch (e: Exception) {
                // ignorar fallo de página
                endReached = true
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadGeneration(genId: Int) {
        currentGeneration = genId
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val list = repo.loadPokemonForGeneration(genId)
                // reemplazar la lista actual por la de la generación (lista pequeña)
                _pokemonList.value = applyFilters(list)
                // disable paginated national dex until cleared
                endReached = true
            } catch (e: Exception) {
                _pokemonList.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setTypeFilter(type: String?) {
        currentTypeFilter = type
        // Si aplica un filtro por tipo, se filtran los ítems ya cargados.
        // Para filtrar todo el catálogo habría que recargar todas las páginas o hacerlo en servidor.
        _pokemonList.value = applyFilters(_pokemonList.value)
    }

    fun setGeneration(gen: Int) {
        loadGeneration(gen)
    }

    private fun applyFilters(input: List<com.example.poketeambuilder.data.repository.PokemonUiModel>): List<com.example.poketeambuilder.data.repository.PokemonUiModel> {
        var out = input
        currentTypeFilter?.let { t ->
            if (t.isNotEmpty()) out = out.filter { it.types.contains(t.lowercase()) }
        }
        return out
    }
}
