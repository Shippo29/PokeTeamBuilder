package com.example.poketeambuilder.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Minimal ViewModel que obtiene la URL del artwork oficial desde PokéAPI. */
class HomeViewModel : ViewModel() {

    private val _imageUrl = MutableStateFlow<String?>(null)
    val imageUrl: StateFlow<String?> = _imageUrl

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadFeaturedPokemon(pokemonId: Int = 25) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val api = "https://pokeapi.co/api/v2/pokemon/$pokemonId"
                val url = URL(api)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
                conn.connect()
                val code = conn.responseCode
                if (code == HttpURLConnection.HTTP_OK) {
                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    val obj = JSONObject(text)
                    val sprites = obj.optJSONObject("sprites")
                    val other = sprites?.optJSONObject("other")
                    val official = other?.optJSONObject("official-artwork")
                    val front = official?.optString("front_default")
                    _imageUrl.value = front
                } else {
                    _imageUrl.value = null
                }
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
                _imageUrl.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

}
