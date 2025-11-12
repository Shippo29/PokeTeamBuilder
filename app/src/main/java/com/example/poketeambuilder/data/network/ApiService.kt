package com.example.poketeambuilder.data.network

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import com.example.poketeambuilder.data.model.*

// Definición de endpoints de la API de Pokémon
interface ApiService {

    @GET("generation/{id}")
    suspend fun getGeneration(@Path("id") id: Int): GenerationResponse

    @GET("pokemon")
    suspend fun listPokemon(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): PokemonListResponse

    @GET("pokemon/{nameOrId}")
    suspend fun getPokemon(@Path("nameOrId") nameOrId: String): PokemonResponse

    @GET("pokemon-species/{nameOrId}")
    suspend fun getPokemonSpecies(@Path("nameOrId") nameOrId: String): PokemonSpeciesResponse

    @GET("type")
    suspend fun getTypes(): TypesListResponse
}
