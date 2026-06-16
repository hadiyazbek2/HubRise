package com.example.hubrise.ui.hubs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.hubrise.R
import com.example.hubrise.data.model.Hub

sealed class HubListItem {
    data class Header(val title: String) : HubListItem()
    data class HubCard(val hub: Hub) : HubListItem()
}

class HubsAdapter(
    private val onHubClick: (Hub) -> Unit,
    private val onJoinClick: (Hub) -> Unit
) : ListAdapter<HubListItem, RecyclerView.ViewHolder>(DiffCallback) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_HUB = 1

        private val DiffCallback = object : DiffUtil.ItemCallback<HubListItem>() {
            override fun areItemsTheSame(old: HubListItem, new: HubListItem): Boolean = when {
                old is HubListItem.Header && new is HubListItem.Header -> old.title == new.title
                old is HubListItem.HubCard && new is HubListItem.HubCard -> old.hub.id == new.hub.id
                else -> false
            }
            override fun areContentsTheSame(old: HubListItem, new: HubListItem) = old == new
        }
    }

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is HubListItem.Header -> TYPE_HEADER
        is HubListItem.HubCard -> TYPE_HUB
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(inflater.inflate(R.layout.item_section_header, parent, false))
            else -> HubViewHolder(inflater.inflate(R.layout.item_hub_card, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is HubListItem.Header -> (holder as HeaderViewHolder).bind(item.title)
            is HubListItem.HubCard -> (holder as HubViewHolder).bind(item.hub)
        }
    }

    inner class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tv: TextView = view.findViewById(R.id.tv_section_title)
        fun bind(title: String) { tv.text = title }
    }

    inner class HubViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivCover: ImageView = view.findViewById(R.id.iv_cover)
        private val tvName: TextView = view.findViewById(R.id.tv_hub_name)
        private val tvCategory: TextView = view.findViewById(R.id.tv_category)
        private val tvMembers: TextView = view.findViewById(R.id.tv_members)
        private val btnJoin: Button = view.findViewById(R.id.btn_join)

        fun bind(hub: Hub) {
            tvName.text = hub.name
            tvMembers.text = "${hub.membersCount} members"

            if (!hub.categoryName.isNullOrEmpty()) {
                tvCategory.text = hub.categoryName
                tvCategory.visibility = View.VISIBLE
            } else {
                tvCategory.visibility = View.GONE
            }

            if (!hub.coverImageUrl.isNullOrEmpty()) {
                ivCover.load(hub.coverImageUrl) { crossfade(true) }
            }

            applyJoinState(hub.isMember)

            itemView.setOnClickListener { onHubClick(hub) }
            btnJoin.setOnClickListener { onJoinClick(hub) }
        }

        private fun applyJoinState(isMember: Boolean) {
            if (isMember) {
                btnJoin.text = "Joined"
                btnJoin.setBackgroundResource(R.drawable.btn_joined)
                btnJoin.setTextColor(btnJoin.context.getColor(R.color.white))
            } else {
                btnJoin.text = "Join"
                btnJoin.setBackgroundResource(R.drawable.btn_join)
                btnJoin.setTextColor(btnJoin.context.getColor(R.color.blue_primary))
            }
        }
    }
}
