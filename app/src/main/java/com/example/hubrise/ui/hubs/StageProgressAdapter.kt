package com.example.hubrise.ui.hubs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.hubrise.R
import com.example.hubrise.data.model.ChallengeStageStatus
import com.example.hubrise.data.model.StageStatus

class StageProgressAdapter(
    private val onMarkComplete: (ChallengeStageStatus) -> Unit,
) : ListAdapter<ChallengeStageStatus, StageProgressAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvStatusIcon: TextView = view.findViewById(R.id.tv_status_icon)
        private val tvTitle: TextView = view.findViewById(R.id.tv_stage_title)
        private val tvMeta: TextView = view.findViewById(R.id.tv_stage_meta)
        private val btnMarkComplete: Button = view.findViewById(R.id.btn_mark_complete)

        fun bind(stage: ChallengeStageStatus, isCurrentActionable: Boolean) {
            tvTitle.text = "${stage.orderIndex}. ${stage.title}"
            tvMeta.text = when (stage.status) {
                StageStatus.COMPLETED -> "Completed"
                else -> if (isCurrentActionable) "Current stage" else "Locked"
            }

            tvStatusIcon.text = when (stage.status) {
                StageStatus.COMPLETED -> "✓"
                else -> stage.orderIndex.toString()
            }
            tvStatusIcon.alpha = if (stage.status == StageStatus.COMPLETED || isCurrentActionable) 1f else 0.4f

            btnMarkComplete.visibility =
                if (stage.status != StageStatus.COMPLETED && isCurrentActionable) View.VISIBLE else View.GONE
            btnMarkComplete.setOnClickListener { onMarkComplete(stage) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_stage_progress, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // The first stage that is not yet completed is the one the user can act on (sequential model).
        val firstIncompleteIndex = currentList.indexOfFirst { it.status != StageStatus.COMPLETED }
        holder.bind(getItem(position), position == firstIncompleteIndex)
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<ChallengeStageStatus>() {
            override fun areItemsTheSame(old: ChallengeStageStatus, new: ChallengeStageStatus) = old.id == new.id
            override fun areContentsTheSame(old: ChallengeStageStatus, new: ChallengeStageStatus) = old == new
        }
    }
}
