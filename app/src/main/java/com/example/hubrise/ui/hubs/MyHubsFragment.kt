package com.example.hubrise.ui.hubs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.hubrise.R

class MyHubsFragment : Fragment() {

    private lateinit var viewModel: HubsViewModel
    private lateinit var adapter: MyHubsAdapter

    private lateinit var rv: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var pbLoading: ProgressBar
    private lateinit var layoutEmpty: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_my_hubs, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Share ViewModel with parent HubsFragment
        viewModel = ViewModelProvider(requireParentFragment())[HubsViewModel::class.java]

        rv = view.findViewById(R.id.rv_my_hubs)
        swipeRefresh = view.findViewById(R.id.swipe_refresh)
        pbLoading = view.findViewById(R.id.pb_loading)
        layoutEmpty = view.findViewById(R.id.layout_empty)

        adapter = MyHubsAdapter(
            onHubClick = { hub ->
                val bundle = Bundle().apply { putInt("hubId", hub.id) }
                findNavController().navigate(R.id.hubDetailFragment, bundle)
            },
            onLeaveClick = { hub -> viewModel.toggleJoin(hub) }
        )

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        swipeRefresh.setColorSchemeResources(R.color.blue_primary)
        swipeRefresh.setOnRefreshListener { viewModel.refresh() }

        viewModel.myHubs.observe(viewLifecycleOwner) { hubs ->
            adapter.submitList(hubs)
            layoutEmpty.visibility = if (hubs.isEmpty()) View.VISIBLE else View.GONE
            rv.visibility = if (hubs.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            if (!swipeRefresh.isRefreshing) {
                pbLoading.visibility = if (loading) View.VISIBLE else View.GONE
            }
            if (!loading) swipeRefresh.isRefreshing = false
        }

        viewModel.error.observe(viewLifecycleOwner) { err ->
            err?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
        }
    }
}
