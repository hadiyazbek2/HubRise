package com.example.hubrise.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.hubrise.R
import com.example.hubrise.data.model.Hub

class HubSearchAdapter(
    private val onHubClick: (Int) -> Unit
) : ListAdapter<Hub, HubSearchAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivCover: ImageView = view.findViewById(R.id.iv_cover)
        private val tvName: TextView = view.findViewById(R.id.tv_hub_name)
        private val tvMeta: TextView = view.findViewById(R.id.tv_meta)

        fun bind(hub: Hub) {
            tvName.text = hub.name
            val meta = buildString {
                if (!hub.categoryName.isNullOrEmpty()) append("${hub.categoryName} · ")
                append("${hub.membersCount} members")
            }
            tvMeta.text = meta

            if (!hub.coverImageUrl.isNullOrEmpty()) {
                ivCover.load(hub.coverImageUrl) { crossfade(true) }
            } else {
                ivCover.setImageResource(R.drawable.hub_cover_placeholder)
            }

            itemView.setOnClickListener { onHubClick(hub.id) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_search_hub, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Hub>() {
            override fun areItemsTheSame(old: Hub, new: Hub) = old.id == new.id
            override fun areContentsTheSame(old: Hub, new: Hub) = old == new
        }
    }
}
