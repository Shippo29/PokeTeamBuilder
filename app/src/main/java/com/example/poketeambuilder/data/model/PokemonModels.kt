package com.example.poketeambuilder.data.model

import com.squareup.moshi.Json

// Respuesta de generación (solo se necesita la lista de especies)
data class GenerationResponse(
    @Json(name = "pokemon_species") val pokemonSpecies: List<NamedResource>
)

data class NamedResource(
    val name: String,
    val url: String
)

// Respuesta genérica usada por /pokemon?limit=&offset=
data class PokemonListResponse(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<NamedResource>
)

// Lista de tipos
data class TypesListResponse(
    val results: List<NamedResource>
)

// Respuesta mínima de Pokémon
data class PokemonResponse(
    val id: Int,
    val name: String,
    val sprites: Sprites,
    val types: List<PokemonTypeSlot>,
    val stats: List<PokemonStat>,
    val abilities: List<PokemonAbility>
)

data class Sprites(
    val other: OtherSprites?
)

data class OtherSprites(
    @Json(name = "official-artwork") val officialArtwork: OfficialArtwork?
)

data class OfficialArtwork(
    @Json(name = "front_default") val frontDefault: String?
)

data class PokemonTypeSlot(
    val slot: Int,
    val type: NamedResource
)

data class PokemonStat(
    @Json(name = "base_stat") val baseStat: Int,
    val stat: NamedResource
)

data class PokemonAbility(
    val ability: NamedResource,
    @Json(name = "is_hidden") val isHidden: Boolean
)

// Especie (para texto de sabor / descripción)
data class PokemonSpeciesResponse(
    @Json(name = "flavor_text_entries") val flavorTextEntries: List<FlavorEntry>,
    val genera: List<GenusEntry>?
)

data class FlavorEntry(
    @Json(name = "flavor_text") val flavorText: String,
    val language: NamedResource
)

data class GenusEntry(
    val genus: String,
    val language: NamedResource
)
