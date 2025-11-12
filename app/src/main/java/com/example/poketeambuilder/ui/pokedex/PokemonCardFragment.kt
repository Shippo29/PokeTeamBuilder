package com.example.poketeambuilder.ui.pokedex

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

class PokemonCardFragment : Fragment() {
    companion object {
        private const val ARG_ID = "arg_id"
        fun newInstance(pokemonId: Int): PokemonCardFragment {
            val f = PokemonCardFragment()
            val b = Bundle()
            b.putInt(ARG_ID, pokemonId)
            f.arguments = b
            return f
        }
    }

    private var pokemonId: Int = 0
    private val repo = PokemonRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { pokemonId = it.getInt(ARG_ID, 0) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.item_pokemon_card, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ivSprite = view.findViewById<ImageView>(R.id.iv_sprite)
        val tvName = view.findViewById<TextView>(R.id.tv_name)
        val tvNumber = view.findViewById<TextView>(R.id.tv_number)
        val typesContainer = view.findViewById<LinearLayout>(R.id.types_container)

        if (pokemonId == 0) return

        lifecycleScope.launch {
            try {
                val ui = repo.getPokemonCached(pokemonId.toString())
                tvName.text = ui.name
                tvNumber.text = String.format("#%03d", ui.id)
                if (ui.imageUrl.isNotEmpty()) ivSprite.load(ui.imageUrl) else ivSprite.setImageResource(R.drawable.ic_launcher_foreground)

                typesContainer.removeAllViews()
                ui.types.forEach { t ->
                    val color = TypeColorMap.colorForType(t)
                    val textColor = TypeColorMap.textColorForType(t)
                    val chip = TextView(requireContext()).apply {
                        text = TypeColorMap.displayNameSpanish(t)
                        setPadding(12,6,12,6)
                        setTextColor(textColor)
                        background = ChipUtils.createPillDrawable(requireContext(), color, 16f)
                        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                        lp.setMargins(0,0,8,0)
                        layoutParams = lp
                    }
                    typesContainer.addView(chip)
                }

            } catch (e: Exception) {
                tvName.text = "Error"
                tvNumber.text = ""
            }
        }
    }
}
