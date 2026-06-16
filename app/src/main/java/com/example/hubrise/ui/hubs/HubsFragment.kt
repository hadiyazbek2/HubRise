package com.example.hubrise.ui.hubs

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.hubrise.R
import com.example.hubrise.ui.search.HubSearchAdapter
import com.example.hubrise.ui.search.SearchViewModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class HubsFragment : Fragment() {

    private lateinit var searchViewModel: SearchViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_hubs, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tabLayout = view.findViewById<TabLayout>(R.id.tab_layout)
        val viewPager = view.findViewById<ViewPager2>(R.id.view_pager)
        val btnCreate = view.findViewById<Button>(R.id.btn_create_hub)
        val btnSearchHubs = view.findViewById<ImageView>(R.id.btn_search_hubs)
        val searchBarRow = view.findViewById<View>(R.id.search_bar_row)
        val etHubSearch = view.findViewById<EditText>(R.id.et_hub_search)
        val btnCloseSearch = view.findViewById<ImageView>(R.id.btn_close_search)
        val rvHubSearch = view.findViewById<RecyclerView>(R.id.rv_hub_search)
        val tvHubSearchEmpty = view.findViewById<TextView>(R.id.tv_hub_search_empty)

        // Normal mode: pager with tabs
        viewPager.adapter = HubsPagerAdapter(this)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) { 0 -> "Your Hubs"; else -> "Explore" }
        }.attach()

        btnCreate.setOnClickListener {
            findNavController().navigate(R.id.createHubFragment)
        }

        // Hub search
        searchViewModel = ViewModelProvider(this)[SearchViewModel::class.java]

        val hubSearchAdapter = HubSearchAdapter { hubId ->
            val bundle = Bundle().apply { putInt("hubId", hubId) }
            findNavController().navigate(R.id.hubDetailFragment, bundle)
        }
        rvHubSearch.layoutManager = LinearLayoutManager(requireContext())
        rvHubSearch.adapter = hubSearchAdapter

        btnSearchHubs.setOnClickListener {
            searchBarRow.visibility = View.VISIBLE
            tabLayout.visibility = View.GONE
            viewPager.visibility = View.GONE
            rvHubSearch.visibility = View.VISIBLE
            etHubSearch.requestFocus()
        }

        btnCloseSearch.setOnClickListener {
            etHubSearch.text?.clear()
            searchBarRow.visibility = View.GONE
            tabLayout.visibility = View.VISIBLE
            viewPager.visibility = View.VISIBLE
            rvHubSearch.visibility = View.GONE
            tvHubSearchEmpty.visibility = View.GONE
        }

        etHubSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchViewModel.search(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        searchViewModel.hubs.observe(viewLifecycleOwner) { hubs ->
            hubSearchAdapter.submitList(hubs)
            val query = etHubSearch.text?.toString() ?: ""
            tvHubSearchEmpty.visibility = if (query.isNotBlank() && hubs.isEmpty()) View.VISIBLE else View.GONE
        }
    }
}
