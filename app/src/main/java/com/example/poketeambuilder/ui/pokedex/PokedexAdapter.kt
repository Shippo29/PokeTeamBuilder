package com.example.poketeambuilder.ui.pokedex

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.poketeambuilder.R

class PokedexAdapter(
    private val onClick: (com.example.poketeambuilder.data.repository.PokemonUiModel) -> Unit,
    private val onAddToTeam: (Int) -> Unit
) : ListAdapter<com.example.poketeambuilder.data.repository.PokemonUiModel, PokedexAdapter.Holder>(DIFF) {

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val ivSprite: ImageView = view.findViewById(R.id.iv_sprite)
        val tvName: TextView = view.findViewById(R.id.tv_name)
        val tvNumber: TextView = view.findViewById(R.id.tv_number)
        val typesContainer: LinearLayout = view.findViewById(R.id.types_container)
        val btnAdd: com.google.android.material.button.MaterialButton = view.findViewById(R.id.btn_add_team)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_pokemon_card, parent, false)
        return Holder(v)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val p = getItem(position)
        holder.tvName.text = p.name
        holder.tvNumber.text = String.format("#%03d", p.id)
        if (p.imageUrl.isNotEmpty()) {
            holder.ivSprite.load(p.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_pokeball_logo)
                error(R.drawable.ic_pokeball_logo)
            }
        } else holder.ivSprite.setImageResource(R.drawable.ic_pokeball_logo)

        holder.typesContainer.removeAllViews()
        val ctx = holder.itemView.context
        p.types.forEach { t ->
            val color = com.example.poketeambuilder.ui.pokedex.TypeColorMap.colorForType(t)
            val textColor = com.example.poketeambuilder.ui.pokedex.TypeColorMap.textColorForType(t)
            val chip = TextView(ctx).apply {
                text = com.example.poketeambuilder.ui.pokedex.TypeColorMap.displayNameSpanish(t)
                setPadding(12,6,12,6)
                setTextColor(textColor)
                textSize = 12f
                background = ChipUtils.createPillDrawable(ctx, color, 16f)
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.setMargins(0,0,8,0)
                layoutParams = lp
            }
            holder.typesContainer.addView(chip)
        }

        // El clic del ítem no abre detalle; usar el botón "Agregar" para añadir al equipo.
        holder.itemView.setOnClickListener { onClick(p) }
        holder.btnAdd.setOnClickListener { onAddToTeam(p.id) }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<com.example.poketeambuilder.data.repository.PokemonUiModel>() {
            override fun areItemsTheSame(
                oldItem: com.example.poketeambuilder.data.repository.PokemonUiModel,
                newItem: com.example.poketeambuilder.data.repository.PokemonUiModel
            ): Boolean = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: com.example.poketeambuilder.data.repository.PokemonUiModel,
                newItem: com.example.poketeambuilder.data.repository.PokemonUiModel
            ): Boolean = oldItem == newItem
        }
    }
}
