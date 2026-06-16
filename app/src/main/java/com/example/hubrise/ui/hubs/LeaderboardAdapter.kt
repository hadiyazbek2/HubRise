package com.example.hubrise.ui.hubs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.hubrise.R
import com.example.hubrise.data.api.RetrofitClient
import com.example.hubrise.data.model.LeaderboardEntry

class LeaderboardAdapter(
    private val onUserClick: (Int) -> Unit,
) : ListAdapter<LeaderboardEntry, LeaderboardAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvRank: TextView = view.findViewById(R.id.tv_rank)
        private val ivAvatar: ImageView = view.findViewById(R.id.iv_avatar)
        private val tvUsername: TextView = view.findViewById(R.id.tv_username)
        private val tvCount: TextView = view.findViewById(R.id.tv_count)

        fun bind(entry: LeaderboardEntry) {
            tvRank.text = when (entry.rank) {
                1 -> "🥇"
                2 -> "🥈"
                3 -> "🥉"
                else -> entry.rank.toString()
            }
            tvUsername.text = "@${entry.username}"
            tvCount.text = if (entry.score == entry.score.toLong().toDouble()) {
                entry.score.toLong().toString()
            } else {
                entry.score.toString()
            }

            ivAvatar.load(RetrofitClient.absoluteUrl(entry.avatarUrl)) {
                transformations(CircleCropTransformation())
                crossfade(true)
                fallback(R.drawable.ic_default_avatar)
                error(R.drawable.ic_default_avatar)
            }

            itemView.setOnClickListener { onUserClick(entry.userId) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_leaderboard_entry, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<LeaderboardEntry>() {
            override fun areItemsTheSame(old: LeaderboardEntry, new: LeaderboardEntry) = old.userId == new.userId
            override fun areContentsTheSame(old: LeaderboardEntry, new: LeaderboardEntry) = old == new
        }
    }
}
