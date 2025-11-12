package com.example.poketeambuilder.ui.pokedex

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.poketeambuilder.R
import com.example.poketeambuilder.data.repository.PokemonRepository
import kotlinx.coroutines.launch

class PokemonDetailFragment : Fragment() {

    companion object {
        private const val ARG_POKEMON = "arg_pokemon"
        fun newInstance(pokemon: com.example.poketeambuilder.data.repository.PokemonUiModel): PokemonDetailFragment {
            val f = PokemonDetailFragment()
            val b = Bundle()
            b.putSerializable(ARG_POKEMON, pokemon)
            f.arguments = b
            return f
        }
    }

    private var pokemon: com.example.poketeambuilder.data.repository.PokemonUiModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { b ->
            pokemon = b.getSerializable(ARG_POKEMON) as? com.example.poketeambuilder.data.repository.PokemonUiModel
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvName = view.findViewById<TextView>(R.id.tv_name)
        val tvNumber = view.findViewById<TextView>(R.id.tv_number)
        val iv = view.findViewById<ImageView>(R.id.iv_sprite)
        val typesContainer = view.findViewById<LinearLayout>(R.id.types_container)
        val tvDesc = view.findViewById<TextView>(R.id.tv_description)
    val statsCol1 = view.findViewById<LinearLayout>(R.id.stats_col1)
    val statsCol2 = view.findViewById<LinearLayout>(R.id.stats_col2)

        if (pokemon == null) {
            // nothing to show
            tvName.text = ""
            tvNumber.text = ""
            return
        }

        // Render synchronously from provided model
        val p = pokemon!!

        // background based on primary type
        val primaryType = p.types.firstOrNull()
        val bgColor = TypeColorMap.colorForType(primaryType)
        val topArea = view.findViewById<View>(R.id.top_color_area)
        topArea.setBackgroundColor(bgColor)

        tvName.text = p.name
        tvNumber.text = String.format("#%03d", p.id)
        if (p.imageUrl.isNotEmpty()) iv.load(p.imageUrl)

        // types
        typesContainer.removeAllViews()
        if (p.types.isEmpty()) {
            val chip = TextView(requireContext()).apply {
                text = "Tipo desconocido"
                setPadding((resources.displayMetrics.density * 12).toInt(), (resources.displayMetrics.density * 6).toInt(), (resources.displayMetrics.density * 12).toInt(), (resources.displayMetrics.density * 6).toInt())
                val gd = android.graphics.drawable.GradientDrawable()
                gd.cornerRadius = resources.displayMetrics.density * 8
                gd.setColor(android.graphics.Color.parseColor("#BDBDBD"))
                background = gd
                setTextColor(android.graphics.Color.BLACK)
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.setMargins((resources.displayMetrics.density * 8).toInt(), 0, (resources.displayMetrics.density * 8).toInt(), 0)
                layoutParams = lp
            }
            typesContainer.addView(chip)
        } else {
            p.types.forEach { t ->
                val display = TypeColorMap.displayNameSpanish(t)
                val bg = TypeColorMap.colorForType(t)
                val textColor = TypeColorMap.textColorForType(t)
                val chip = TextView(requireContext()).apply {
                    text = display
                    setPadding((resources.displayMetrics.density * 12).toInt(), (resources.displayMetrics.density * 6).toInt(), (resources.displayMetrics.density * 12).toInt(), (resources.displayMetrics.density * 6).toInt())
                    val gd = android.graphics.drawable.GradientDrawable()
                    gd.cornerRadius = resources.displayMetrics.density * 8
                    gd.setColor(bg)
                    background = gd
                    setTextColor(textColor)
                    val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    lp.setMargins((resources.displayMetrics.density * 8).toInt(), 0, (resources.displayMetrics.density * 8).toInt(), 0)
                    layoutParams = lp
                }
                typesContainer.addView(chip)
            }
        }

        // category and description
        view.findViewById<TextView>(R.id.tv_category).text = p.category
        view.findViewById<TextView>(R.id.tv_description).text = p.description

        // stats: p.stats is a list of ints in the order they were created in repository (we assume hp,atk,def,spAtk,spDef,speed)
        statsCol1.removeAllViews(); statsCol2.removeAllViews()
        val names = listOf("hp", "attack", "defense", "Sp. Atk", "Sp. Def", "speed")
        p.stats.forEachIndexed { idx, value ->
            val container = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.setMargins(0, (resources.displayMetrics.density * 8).toInt(), 0, (resources.displayMetrics.density * 8).toInt())
                layoutParams = lp
                gravity = android.view.Gravity.CENTER_HORIZONTAL
            }
            val tvLabel = TextView(requireContext()).apply {
                text = names.getOrNull(idx) ?: "stat"
                setTextColor(Color.DKGRAY)
                textSize = 12f
            }
            val tvValue = TextView(requireContext()).apply {
                text = value.toString()
                setTextColor(Color.BLACK)
                textSize = 20f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
            container.addView(tvLabel)
            container.addView(tvValue)
            if (idx % 2 == 0) statsCol1.addView(container) else statsCol2.addView(container)
        }

        // abilities
        val abilitiesContainer = view.findViewById<LinearLayout>(R.id.abilities_container)
        abilitiesContainer.removeAllViews()
        p.abilities.forEach { a ->
            val display = a
            val tv = TextView(requireContext()).apply {
                text = display
                textSize = 16f
                setTextColor(Color.DKGRAY)
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.setMargins(0, (resources.displayMetrics.density * 6).toInt(), 0, (resources.displayMetrics.density * 6).toInt())
                layoutParams = lp
            }
            abilitiesContainer.addView(tv)
        }

    }
}
