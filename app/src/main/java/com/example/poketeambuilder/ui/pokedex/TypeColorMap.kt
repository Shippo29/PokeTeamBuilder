package com.example.poketeambuilder.ui.pokedex

import android.graphics.Color

object TypeColorMap {
    fun colorForType(type: String?): Int {
        return when (type?.lowercase()) {
            "grass" -> Color.parseColor("#78C850")
            "fire" -> Color.parseColor("#F08030")
            "water" -> Color.parseColor("#6890F0")
            "bug" -> Color.parseColor("#A8B820")
            "normal" -> Color.parseColor("#A8A878")
            "electric" -> Color.parseColor("#F8D030")
            "ground" -> Color.parseColor("#E0C068")
            "fairy" -> Color.parseColor("#EE99AC")
            "fighting" -> Color.parseColor("#C03028")
            "psychic" -> Color.parseColor("#F85888")
            "rock" -> Color.parseColor("#B8A038")
            "ghost" -> Color.parseColor("#705898")
            "ice" -> Color.parseColor("#98D8D8")
            "dragon" -> Color.parseColor("#7038F8")
            "poison" -> Color.parseColor("#A040A0")
            "flying" -> Color.parseColor("#A3C7FF")
            "dark" -> Color.parseColor("#705848")
            "steel" -> Color.parseColor("#B8B8D0")
            else -> Color.parseColor("#BDBDBD") // default gray if type unknown
        }
    }

    // Devuelve nombre en español para cada tipo (fallback: capitalized original)
    fun displayNameSpanish(type: String?): String {
        return when (type?.lowercase()) {
            "grass" -> "Planta"
            "fire" -> "Fuego"
            "water" -> "Agua"
            "bug" -> "Bicho"
            "normal" -> "Normal"
            "electric" -> "Eléctrico"
            "ground" -> "Tierra"
            "fairy" -> "Hada"
            "fighting" -> "Lucha"
            "psychic" -> "Psíquico"
            "rock" -> "Roca"
            "ghost" -> "Fantasma"
            "ice" -> "Hielo"
            "dragon" -> "Dragón"
            "poison" -> "Veneno"
            "flying" -> "Volador"
            "dark" -> "Siniestro"
            "steel" -> "Acero"
            else -> type?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } ?: "" 
        }
    }

    // Color de texto (blanco o negro) que contraste con el color de fondo
    fun textColorForType(type: String?): Int {
        val bg = colorForType(type)
        // compute luminance
        val r = (bg shr 16 and 0xff) / 255.0
        val g = (bg shr 8 and 0xff) / 255.0
        val b = (bg and 0xff) / 255.0
        val luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b
        return if (luminance < 0.6) Color.WHITE else Color.BLACK
    }
}
