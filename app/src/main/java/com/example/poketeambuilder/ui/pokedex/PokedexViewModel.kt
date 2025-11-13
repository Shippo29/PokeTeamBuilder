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

    // Búsqueda de texto (cadena actual)
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private var currentGeneration = 1
    private val _currentGeneration = MutableStateFlow(1)
    val currentGenerationFlow: StateFlow<Int> = _currentGeneration
    private val _selectedTypes = kotlinx.coroutines.flow.MutableStateFlow<List<String>>(emptyList())
    val selectedTypesFlow: kotlinx.coroutines.flow.StateFlow<List<String>> = _selectedTypes

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
                val t = repo.getTypes().filter { !it.equals("stellar", ignoreCase = true) && !it.equals("unknown", ignoreCase = true) }
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
        // si hay filtro de tipos activo, o hay búsqueda, saltar la carga paginada
        if (_selectedTypes.value.isNotEmpty()) return
        if (_searchQuery.value.isNotBlank()) return
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

    // Llamar cuando cambia el texto de búsqueda
    fun searchQueryChanged(q: String) {
        val qTrim = q.trim().lowercase()
        _searchQuery.value = qTrim
        if (qTrim.isEmpty() && _selectedTypes.value.isEmpty()) {
            // Si no hay búsqueda ni tipos seleccionados, volver al listado paginado
            loadFirstPage()
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Buscar por nombre si hay query, o si no hay query pero hay tipos seleccionados, cargar generación completa
                val results = if (qTrim.isNotEmpty()) {
                    repo.searchPokemonByNamePartial(qTrim, maxResults = 120)
                } else if (_selectedTypes.value.isNotEmpty()) {
                    // Si hay tipos pero no hay búsqueda, cargar generación actual y aplicar filtros de tipo
                    repo.loadPokemonForGeneration(currentGeneration)
                } else {
                    emptyList()
                }
                _pokemonList.value = applyFilters(results)
            } catch (e: Exception) {
                // en fallo, no romper UI
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadGeneration(genId: Int) {
        currentGeneration = genId
        _currentGeneration.value = genId
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

    /**
     * Alterna la selección de un tipo en la lista de filtros.
     * Sólo permite hasta 2 tipos simultáneamente.
     */
    fun toggleTypeFilter(type: String) {
        val current = _selectedTypes.value.toMutableList()
        val key = type.lowercase()
        if (current.contains(key)) {
            current.remove(key)
            _selectedTypes.value = current
        } else {
            // si ya hay 2, no añadir más
            if (current.size >= 2) return
            current.add(key)
            _selectedTypes.value = current
        }
        // Re-ejecutar búsqueda/filtrado con los nuevos tipos
        if (_searchQuery.value.isNotEmpty()) {
            searchQueryChanged(_searchQuery.value)
        } else {
            // Si no hay búsqueda, cargar generación actual con nuevo filtro de tipos
            loadGeneration(currentGeneration)
        }
    }

    fun clearTypeFilters() {
        _selectedTypes.value = emptyList()
        if (_searchQuery.value.isEmpty()) {
            loadFirstPage()
        } else {
            searchQueryChanged(_searchQuery.value)
        }
    }

    fun setGeneration(gen: Int) {
        loadGeneration(gen)
        _currentGeneration.value = gen
    }

    private fun applyFilters(input: List<com.example.poketeambuilder.data.repository.PokemonUiModel>): List<com.example.poketeambuilder.data.repository.PokemonUiModel> {
        var out = input
        // Filtrar por tipos seleccionados (si los hay)
        if (_selectedTypes.value.isNotEmpty()) {
            val selected = _selectedTypes.value.map { it.lowercase() }
            out = out.filter { model ->
                val modelTypes = model.types.map { it.lowercase() }
                // El Pokémon debe contener todos los tipos seleccionados
                selected.all { sel -> modelTypes.contains(sel) }
            }
        }
        return out
    }
}
