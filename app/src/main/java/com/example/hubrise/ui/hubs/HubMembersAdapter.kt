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
import com.example.hubrise.data.model.HubMember

class HubMembersAdapter(
    private val currentUserId: Int,
    private val isCreator: Boolean,
    private val onRemoveClick: (HubMember) -> Unit,
) : ListAdapter<HubMember, HubMembersAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivAvatar: ImageView = view.findViewById(R.id.iv_avatar)
        private val tvUsername: TextView = view.findViewById(R.id.tv_username)
        private val tvRole: TextView = view.findViewById(R.id.tv_role)
        private val btnRemove: TextView = view.findViewById(R.id.btn_remove)

        fun bind(member: HubMember) {
            tvUsername.text = "@${member.username}"
            tvRole.text = when {
                member.isCreator -> "Creator"
                member.role == "admin" -> "Admin"
                member.role == "moderator" -> "Moderator"
                else -> "Member"
            }

            ivAvatar.load(RetrofitClient.absoluteUrl(member.avatarUrl)) {
                transformations(CircleCropTransformation())
                crossfade(true)
                fallback(R.drawable.ic_default_avatar)
                error(R.drawable.ic_default_avatar)
            }

            // Show remove only if current user is creator and this isn't themselves or the creator
            if (isCreator && !member.isCreator && member.userId != currentUserId) {
                btnRemove.visibility = View.VISIBLE
                btnRemove.setOnClickListener { onRemoveClick(member) }
            } else {
                btnRemove.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_hub_member, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<HubMember>() {
            override fun areItemsTheSame(old: HubMember, new: HubMember) = old.userId == new.userId
            override fun areContentsTheSame(old: HubMember, new: HubMember) = old == new
        }
    }
}
