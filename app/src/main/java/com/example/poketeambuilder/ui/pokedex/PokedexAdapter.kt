package com.example.poketeambuilder.ui.pokedex

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.poketeambuilder.R

class PokedexAdapter(
    private var items: List<com.example.poketeambuilder.data.repository.PokemonUiModel>,
    private val onClick: (com.example.poketeambuilder.data.repository.PokemonUiModel) -> Unit,
    private val onAddToTeam: (Int) -> Unit
) : RecyclerView.Adapter<PokedexAdapter.Holder>() {

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
        val p = items[position]
        holder.tvName.text = p.name
        holder.tvNumber.text = String.format("#%03d", p.id)
        if (p.imageUrl.isNotEmpty()) {
            holder.ivSprite.load(p.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_pokeball_logo)
                error(R.drawable.ic_pokeball_logo)
                // let Coil decide size based on ImageView bounds to avoid extra downscaling
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
                val bg = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = ctx.resources.displayMetrics.density * 16
                    setColor(color)
                }
                background = bg
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

    override fun getItemCount(): Int = items.size

    fun submit(list: List<com.example.poketeambuilder.data.repository.PokemonUiModel>) {
        items = list
        notifyDataSetChanged()
    }
}
