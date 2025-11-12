package com.example.poketeambuilder.ui.pokedex

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_pokedex, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnFilters = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_filters_toggle)
        val filtersPanel = view.findViewById<View>(R.id.filters_panel)
        btnFilters.setOnClickListener {
            if (filtersPanel.visibility == View.VISIBLE) filtersPanel.visibility = View.GONE else filtersPanel.visibility = View.VISIBLE
        }

        val rv = view.findViewById<RecyclerView>(R.id.rv_pokedex)
        val prog = view.findViewById<View>(R.id.progress)
        val genContainer = view.findViewById<LinearLayout>(R.id.gen_chips_container)
        val typeContainer = view.findViewById<LinearLayout>(R.id.type_chips_container)

        adapter = PokedexAdapter(emptyList(), onClick = { /* no-op: don't open detail */ }, onAddToTeam = { pokemonId ->
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

        // Endless scroll: load next page when nearing the end
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
                adapter.submit(list)
            }
        }
        lifecycleScope.launch {
            vm.isLoading.collectLatest { loading ->
                prog.visibility = if (loading) View.VISIBLE else View.GONE
            }
        }

        // generation chips 1..8
        for (i in 1..8) {
            val chip = TextView(requireContext()).apply {
                text = "Gen $i"
                setPadding(20)
                setBackgroundResource(R.drawable.pill_background)
                setTextColor(requireContext().getColor(R.color.white))
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.setMargins(0,0,12,0)
                layoutParams = lp
                setOnClickListener { vm.setGeneration(i) }
            }
            genContainer.addView(chip)
        }

        // types will be populated from ViewModel
        lifecycleScope.launch {
            vm.types.collectLatest { types ->
                typeContainer.removeAllViews()
                // add an "All" chip
                val allChip = TextView(requireContext()).apply {
                    text = "Todos"
                    setPadding(20)
                    val bg = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = resources.displayMetrics.density * 12
                        setColor(android.graphics.Color.parseColor("#EEEEEE"))
                    }
                    background = bg
                    setTextColor(requireContext().getColor(android.R.color.black))
                    val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    lp.setMargins(0,0,12,0)
                    layoutParams = lp
                    setOnClickListener { vm.setTypeFilter(null) }
                }
                typeContainer.addView(allChip)

                types.forEach { t ->
                    val color = com.example.poketeambuilder.ui.pokedex.TypeColorMap.colorForType(t)
                    val textColor = com.example.poketeambuilder.ui.pokedex.TypeColorMap.textColorForType(t)
                    val chip = TextView(requireContext()).apply {
                        text = com.example.poketeambuilder.ui.pokedex.TypeColorMap.displayNameSpanish(t)
                        setPadding(20)
                        val bg = android.graphics.drawable.GradientDrawable().apply {
                            cornerRadius = resources.displayMetrics.density * 12
                            setColor(color)
                        }
                        background = bg
                        setTextColor(textColor)
                        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                        lp.setMargins(0,0,12,0)
                        layoutParams = lp
                        setOnClickListener { vm.setTypeFilter(t) }
                    }
                    typeContainer.addView(chip)
                }
            }
        }
    }
}
