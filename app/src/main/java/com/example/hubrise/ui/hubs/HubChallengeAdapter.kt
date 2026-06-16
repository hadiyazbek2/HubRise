package com.example.hubrise.ui.hubs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.hubrise.R
import com.example.hubrise.data.model.Challenge
import com.example.hubrise.data.model.ProgressModel

class HubChallengeAdapter(
    private val onChallengeClick: (Challenge) -> Unit,
) : ListAdapter<Challenge, HubChallengeAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTitle: TextView = view.findViewById(R.id.tv_title)
        private val tvDescription: TextView = view.findViewById(R.id.tv_description)
        private val tvTarget: TextView = view.findViewById(R.id.tv_target)
        private val tvEndsAt: TextView = view.findViewById(R.id.tv_ends_at)
        private val tvMainBadge: TextView = view.findViewById(R.id.tv_main_badge)

        fun bind(c: Challenge) {
            tvTitle.text = c.title
            tvDescription.text = c.description.ifEmpty { "No description" }
            val modelLabel = when (c.progressModel) {
                ProgressModel.STAGE -> "Stages"
                ProgressModel.STREAK -> "Streak"
                else -> "Count"
            }
            tvTarget.text = "$modelLabel: ${c.summary} (${c.percentComplete}%)"
            tvEndsAt.text = if (c.endsAt != null) "Ends: ${c.endsAt.take(10)}" else ""
            tvMainBadge.visibility = if (c.isMain) View.VISIBLE else View.GONE
            itemView.setOnClickListener { onChallengeClick(c) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_hub_challenge_detail, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Challenge>() {
            override fun areItemsTheSame(old: Challenge, new: Challenge) = old.id == new.id
            override fun areContentsTheSame(old: Challenge, new: Challenge) = old == new
        }
    }
}
