package com.example.hubrise.ui.hubs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import coil.load
import com.example.hubrise.R
import com.example.hubrise.data.api.RetrofitClient
import com.example.hubrise.data.model.Hub

class ExploreHubsFragment : Fragment() {

    private lateinit var viewModel: HubsViewModel
    private lateinit var adapter: ExploreAdapter

    private lateinit var rv: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var pbLoading: ProgressBar
    private lateinit var layoutEmpty: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_explore_hubs, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireParentFragment())[HubsViewModel::class.java]

        rv = view.findViewById(R.id.rv_explore)
        swipeRefresh = view.findViewById(R.id.swipe_refresh)
        pbLoading = view.findViewById(R.id.pb_loading)
        layoutEmpty = view.findViewById(R.id.layout_empty)

        adapter = ExploreAdapter(
            onHubClick = { hub ->
                val bundle = Bundle().apply { putInt("hubId", hub.id) }
                findNavController().navigate(R.id.hubDetailFragment, bundle)
            },
            onJoinClick = { hub -> viewModel.toggleJoin(hub) }
        )

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        swipeRefresh.setColorSchemeResources(R.color.blue_primary)
        swipeRefresh.setOnRefreshListener { viewModel.refresh() }

        viewModel.recommended.observe(viewLifecycleOwner) { hubs ->
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
    }

    // Inline adapter for Explore cards
    private class ExploreAdapter(
        private val onHubClick: (Hub) -> Unit,
        private val onJoinClick: (Hub) -> Unit
    ) : ListAdapter<Hub, ExploreAdapter.VH>(DiffCb) {

        companion object {
            private val DiffCb = object : DiffUtil.ItemCallback<Hub>() {
                override fun areItemsTheSame(old: Hub, new: Hub) = old.id == new.id
                override fun areContentsTheSame(old: Hub, new: Hub) = old == new
            }
        }

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            private val ivCover: ImageView = view.findViewById(R.id.iv_cover)
            private val tvName: TextView = view.findViewById(R.id.tv_hub_name)
            private val tvCategory: TextView = view.findViewById(R.id.tv_category)
            private val tvMembers: TextView = view.findViewById(R.id.tv_members)
            private val tvPrivacy: TextView = view.findViewById(R.id.tv_privacy)
            private val btnJoin: Button = view.findViewById(R.id.btn_join)

            fun bind(hub: Hub) {
                tvName.text = hub.name
                tvMembers.text = "${hub.membersCount} members"
                tvPrivacy.text = if (hub.isPublic) "🌐 Public" else "🔒 Private"

                if (!hub.categoryName.isNullOrEmpty()) {
                    tvCategory.text = hub.categoryName
                    tvCategory.visibility = View.VISIBLE
                } else {
                    tvCategory.visibility = View.GONE
                }

                val coverUrl = RetrofitClient.absoluteUrl(hub.coverImageUrl)
                if (coverUrl != null) {
                    ivCover.load(coverUrl) { crossfade(true) }
                }

                if (hub.isMember) {
                    btnJoin.text = "Joined"
                    btnJoin.setBackgroundResource(R.drawable.btn_joined)
                    btnJoin.setTextColor(btnJoin.context.getColor(R.color.white))
                } else {
                    btnJoin.text = "Join"
                    btnJoin.setBackgroundResource(R.drawable.btn_join)
                    btnJoin.setTextColor(btnJoin.context.getColor(R.color.blue_primary))
                }

                btnJoin.setOnClickListener { onJoinClick(hub) }
                itemView.setOnClickListener { onHubClick(hub) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_hub_card_explore, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))
    }
}
