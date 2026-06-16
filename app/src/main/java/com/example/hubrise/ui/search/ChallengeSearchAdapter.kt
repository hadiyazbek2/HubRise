package com.example.hubrise.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.hubrise.R
import com.example.hubrise.data.model.ChallengeSearchResult

class ChallengeSearchAdapter(
    private val onChallengeClick: (ChallengeSearchResult) -> Unit
) : ListAdapter<ChallengeSearchResult, ChallengeSearchAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTitle: TextView = view.findViewById(R.id.tv_title)
        private val tvHub: TextView = view.findViewById(R.id.tv_hub)

        fun bind(challenge: ChallengeSearchResult) {
            tvTitle.text = challenge.title
            tvHub.text = if (!challenge.hubName.isNullOrEmpty()) "in ${challenge.hubName}" else ""
            tvHub.visibility = if (!challenge.hubName.isNullOrEmpty()) View.VISIBLE else View.GONE
            itemView.setOnClickListener { onChallengeClick(challenge) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_search_challenge, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<ChallengeSearchResult>() {
            override fun areItemsTheSame(old: ChallengeSearchResult, new: ChallengeSearchResult) = old.id == new.id
            override fun areContentsTheSame(old: ChallengeSearchResult, new: ChallengeSearchResult) = old == new
        }
    }
}
