package com.example.hubrise.ui.hubs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.hubrise.R
import com.example.hubrise.data.api.RetrofitClient
import com.example.hubrise.data.model.Hub

class MyHubsAdapter(
    private val onHubClick: (Hub) -> Unit,
    private val onLeaveClick: (Hub) -> Unit
) : ListAdapter<Hub, MyHubsAdapter.ViewHolder>(DiffCallback) {

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Hub>() {
            override fun areItemsTheSame(old: Hub, new: Hub) = old.id == new.id
            override fun areContentsTheSame(old: Hub, new: Hub) = old == new
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivCover: ImageView = view.findViewById(R.id.iv_cover)
        private val tvName: TextView = view.findViewById(R.id.tv_hub_name)
        private val tvCategory: TextView = view.findViewById(R.id.tv_category)
        private val tvMembers: TextView = view.findViewById(R.id.tv_members)
        private val btnJoin: Button = view.findViewById(R.id.btn_join)
        private val tvChallengeTitle: TextView = view.findViewById(R.id.tv_challenge_title)
        private val tvChallengeDesc: TextView = view.findViewById(R.id.tv_challenge_desc)
        private val tvProgressLabel: TextView = view.findViewById(R.id.tv_challenge_progress_label)
        private val pbChallenge: ProgressBar = view.findViewById(R.id.pb_challenge)

        fun bind(hub: Hub) {
            tvName.text = hub.name
            tvMembers.text = "${hub.membersCount} members"

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

            val mc = hub.mainChallenge
            if (mc != null) {
                tvChallengeTitle.text = mc.title
                tvChallengeDesc.text = mc.summary.ifEmpty { mc.title }
                tvProgressLabel.text = "${mc.percentComplete}%"
                pbChallenge.progress = mc.percentComplete
            } else {
                tvChallengeTitle.text = "Main Challenge"
                tvChallengeDesc.text = "No challenge set yet"
                tvProgressLabel.text = "0%"
                pbChallenge.progress = 0
            }

            btnJoin.text = "Joined"
            btnJoin.setBackgroundResource(R.drawable.btn_joined)
            btnJoin.setOnClickListener { onLeaveClick(hub) }
            itemView.setOnClickListener { onHubClick(hub) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hub_card_my, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
