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
import com.example.hubrise.data.model.CompletionRequest
import com.example.hubrise.data.model.CompletionStatus

class CompletionRequestAdapter(
    private val onApprove: (CompletionRequest) -> Unit,
    private val onReject: (CompletionRequest) -> Unit,
) : ListAdapter<CompletionRequest, CompletionRequestAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivAvatar: ImageView = view.findViewById(R.id.iv_avatar)
        private val tvUsername: TextView = view.findViewById(R.id.tv_username)
        private val tvChallengeTitle: TextView = view.findViewById(R.id.tv_challenge_title)
        private val tvSubmittedAt: TextView = view.findViewById(R.id.tv_submitted_at)
        private val tvMemberNote: TextView = view.findViewById(R.id.tv_member_note)
        private val rowActions: View = view.findViewById(R.id.row_actions)
        private val btnApprove: View = view.findViewById(R.id.btn_approve)
        private val btnReject: View = view.findViewById(R.id.btn_reject)
        private val rowOutcome: View = view.findViewById(R.id.row_outcome)
        private val tvStatusBadge: TextView = view.findViewById(R.id.tv_status_badge)
        private val tvAdminNote: TextView = view.findViewById(R.id.tv_admin_note)

        fun bind(request: CompletionRequest) {
            tvUsername.text = "@${request.username}"
            tvChallengeTitle.text = "🏆 ${request.challengeTitle ?: ""}"
            tvSubmittedAt.text = request.submittedAt.take(10)

            if (request.memberNote.isNotBlank()) {
                tvMemberNote.visibility = View.VISIBLE
                tvMemberNote.text = "\"${request.memberNote}\""
            } else {
                tvMemberNote.visibility = View.GONE
            }

            ivAvatar.load(RetrofitClient.absoluteUrl(request.userAvatarUrl)) {
                transformations(CircleCropTransformation())
                crossfade(true)
                fallback(R.drawable.ic_default_avatar)
                error(R.drawable.ic_default_avatar)
            }

            if (request.status == CompletionStatus.PENDING) {
                rowActions.visibility = View.VISIBLE
                rowOutcome.visibility = View.GONE
                btnApprove.setOnClickListener { onApprove(request) }
                btnReject.setOnClickListener { onReject(request) }
            } else {
                rowActions.visibility = View.GONE
                rowOutcome.visibility = View.VISIBLE
                if (request.status == CompletionStatus.APPROVED) {
                    tvStatusBadge.text = "✅ Approved by @${request.reviewedByUsername ?: "admin"}"
                    tvStatusBadge.setBackgroundResource(R.drawable.success_chip_bg)
                    tvStatusBadge.setTextColor(tvStatusBadge.context.getColor(R.color.success_green))
                } else {
                    tvStatusBadge.text = "❌ Rejected by @${request.reviewedByUsername ?: "admin"}"
                    tvStatusBadge.setBackgroundResource(R.drawable.error_chip_bg)
                    tvStatusBadge.setTextColor(tvStatusBadge.context.getColor(R.color.error_red))
                }
                if (request.adminNote.isNotBlank()) {
                    tvAdminNote.visibility = View.VISIBLE
                    tvAdminNote.text = "\"${request.adminNote}\""
                } else {
                    tvAdminNote.visibility = View.GONE
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_completion_request, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<CompletionRequest>() {
            override fun areItemsTheSame(old: CompletionRequest, new: CompletionRequest) = old.id == new.id
            override fun areContentsTheSame(old: CompletionRequest, new: CompletionRequest) = old == new
        }
    }
}
