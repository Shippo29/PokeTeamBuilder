package com.example.poketeambuilder.ui.pokedex

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.text.Editable
import android.text.TextWatcher
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.poketeambuilder.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PokedexFragment : Fragment() {

    private val vm: PokedexViewModel by viewModels()
    private lateinit var adapter: PokedexAdapter
    private lateinit var genContainer: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_pokedex, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val searchInput = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_search)
        val searchDropdown = view.findViewById<View>(R.id.search_dropdown)
        val filtersPanel = view.findViewById<View>(R.id.filters_panel)

        // Mostrar filtros al enfocar la búsqueda; ocultar al perder foco
        searchInput.setOnFocusChangeListener { _, has ->
            searchDropdown.visibility = if (has) View.VISIBLE else View.GONE
        }

        // Listener para texto: delegar al ViewModel (búsqueda)
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                vm.searchQueryChanged(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        val rv = view.findViewById<RecyclerView>(R.id.rv_pokedex)
        val prog = view.findViewById<View>(R.id.progress)
        genContainer = view.findViewById<LinearLayout>(R.id.genContainer)
        val typeContainer = view.findViewById<LinearLayout>(R.id.typeContainer)

        adapter = PokedexAdapter(onClick = { /* no-op: don't open detail */ }, onAddToTeam = { pokemonId ->
            // Open bottom sheet to select team for this pokemon ID (only ID passed)
            val sheet = com.example.poketeambuilder.ui.pokedex.TeamSelectionBottomSheet.newInstance(pokemonId)
            if (isAdded) sheet.show(parentFragmentManager, "team_select")
        })
        rv.layoutManager = LinearLayoutManager(requireContext())
    rv.adapter = adapter
    // Pequeñas mejoras de rendimiento para RecyclerView
    rv.setHasFixedSize(true)
    rv.setItemViewCacheSize(20)
        rv.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        // Endless scroll: cargar siguiente página cuando se acerca al final (si no hay búsqueda activa)
        val layoutManager = rv.layoutManager as LinearLayoutManager
        rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val total = layoutManager.itemCount
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                if (total > 0 && lastVisible >= total - 10) {
                    vm.loadNextPage()
                }
            }
        })

        lifecycleScope.launch {
            vm.pokemonList.collectLatest { list ->
                adapter.submitList(list)
            }
        }
        lifecycleScope.launch {
            vm.isLoading.collectLatest { loading ->
                prog.visibility = if (loading) View.VISIBLE else View.GONE
            }
        }

        // generation chips 1..8
        for (i in 1..8) {
            val chip = makeChip("Gen $i", bgRes = R.drawable.pill_background, textColor = requireContext().getColor(R.color.white)) {
                vm.setGeneration(i)
            }
            chip.tag = i
            genContainer.addView(chip)
        }

        // Observa cambios de generación seleccionada y actualiza UI
        lifecycleScope.launch {
            vm.currentGenerationFlow.collectLatest { gen ->
                updateGenChipSelection(gen)
            }
        }

        // types will be populated from ViewModel
        lifecycleScope.launch {
            vm.types.collectLatest { types ->
                typeContainer.removeAllViews()
                // add an "All" chip
                // "Todos" — limpia los filtros de tipos
                val allChip = makeChip("Todos", bgColor = android.graphics.Color.parseColor("#EEEEEE"), textColor = requireContext().getColor(android.R.color.black)) {
                    vm.clearTypeFilters()
                }
                allChip.tag = "todos"
                typeContainer.addView(allChip)

                // Crear chips por tipo y añadir listener que alterna selección (máx 2)
                types.forEach { t ->
                    val color = com.example.poketeambuilder.ui.pokedex.TypeColorMap.colorForType(t)
                    val textColor = com.example.poketeambuilder.ui.pokedex.TypeColorMap.textColorForType(t)
                    val chip = makeChip(com.example.poketeambuilder.ui.pokedex.TypeColorMap.displayNameSpanish(t), bgColor = color, textColor = textColor) {
                        vm.toggleTypeFilter(t)
                    }
                    // tag: clave en minúsculas para búsqueda rápida
                    chip.tag = t.lowercase()
                    typeContainer.addView(chip)
                }
            }
        }

        // Observa selección de tipos para actualizar estilos de chips (centralizado)
        lifecycleScope.launch {
            vm.selectedTypesFlow.collectLatest { selected ->
                val normalized = selected.map { it.lowercase() }
                val childCount = typeContainer.childCount
                for (i in 0 until childCount) {
                    val child = typeContainer.getChildAt(i) as? TextView ?: continue
                    val tag = (child.tag as? String) ?: continue
                    val isSelected = if (tag == "todos") normalized.isEmpty() else normalized.contains(tag)
                    applyTypeStyle(child, isSelected, tag)
                }
            }
        }
    }

    private fun updateGenChipSelection(selectedGen: Int) {
        val count = genContainer.childCount
        for (i in 0 until count) {
            val child = genContainer.getChildAt(i)
            val gen = child.tag as? Int ?: continue
            val isSelected = gen == selectedGen
            applyGenStyle(child as TextView, isSelected)
        }
    }

    private fun applyGenStyle(tv: TextView, selected: Boolean) {
        if (selected) {
            // Estilo activo: fondo intenso, texto blanco y prefijo check
            tv.setTextColor(requireContext().getColor(android.R.color.white))
            tv.background = ChipUtils.createPillDrawable(requireContext(), requireContext().getColor(R.color.pokedex_red), 12f)
            val base = tv.text.toString().replaceFirst("^✔\\s+".toRegex(), "")
            tv.text = "✔ $base"
            tv.setPadding(20)
        } else {
            // Estilo normal: usar recurso base y texto por defecto
            val base = tv.text.toString().replaceFirst("^✔\\s+".toRegex(), "")
            tv.text = base
            tv.setTextColor(requireContext().getColor(R.color.white))
            tv.setBackgroundResource(R.drawable.pill_background)
            tv.setPadding(20)
        }
        // Opcional: pequeña animación de re-paint
        tv.animate().setDuration(120).alpha(1f)
    }

    // Helper para crear un chip estilizado y consistente
    private fun makeChip(text: String, bgRes: Int? = null, bgColor: Int? = null, textColor: Int = requireContext().getColor(android.R.color.black), onClick: () -> Unit): TextView {
        val chip = TextView(requireContext()).apply {
            this.text = text
            setPadding(20)
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, 0, 12, 0)
            layoutParams = lp
            setTextColor(textColor)
            when {
                bgRes != null -> setBackgroundResource(bgRes)
                bgColor != null -> {
                    background = ChipUtils.createPillDrawable(requireContext(), bgColor, 12f)
                }
            }
            setOnClickListener { onClick() }
        }
        return chip
    }

    private fun applyTypeStyle(tv: TextView, selected: Boolean, typeTag: String) {
        if (selected) {
            tv.setTextColor(requireContext().getColor(android.R.color.white))
            tv.background = ChipUtils.createPillDrawable(requireContext(), requireContext().getColor(R.color.pokedex_red), 12f)
            val base = tv.text.toString().replaceFirst("^✔\\s+".toRegex(), "")
            tv.text = "✔ $base"
            tv.setPadding(20)
        } else {
            val base = tv.text.toString().replaceFirst("^✔\\s+".toRegex(), "")
            tv.text = base
            tv.setTextColor(requireContext().getColor(R.color.white))
            if (typeTag == "todos") {
                tv.background = ChipUtils.createPillDrawable(requireContext(), android.graphics.Color.parseColor("#EEEEEE"), 12f)
                tv.setTextColor(requireContext().getColor(android.R.color.black))
            } else {
                val color = com.example.poketeambuilder.ui.pokedex.TypeColorMap.colorForType(typeTag)
                tv.background = ChipUtils.createPillDrawable(requireContext(), color, 12f)
                val textColor = com.example.poketeambuilder.ui.pokedex.TypeColorMap.textColorForType(typeTag)
                tv.setTextColor(textColor)
            }
            tv.setPadding(20)
        }
    }
}

