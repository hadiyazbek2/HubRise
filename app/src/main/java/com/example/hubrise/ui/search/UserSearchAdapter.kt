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
import coil.transform.CircleCropTransformation
import com.example.hubrise.R
import com.example.hubrise.data.api.RetrofitClient
import com.example.hubrise.data.model.UserSearchResult

class UserSearchAdapter(
    private val onUserClick: (Int) -> Unit
) : ListAdapter<UserSearchResult, UserSearchAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivAvatar: ImageView = view.findViewById(R.id.iv_avatar)
        private val tvUsername: TextView = view.findViewById(R.id.tv_username)
        private val tvFullName: TextView = view.findViewById(R.id.tv_full_name)

        fun bind(user: UserSearchResult) {
            tvUsername.text = user.username
            tvFullName.text = user.fullName.ifEmpty { user.username }

            ivAvatar.load(RetrofitClient.absoluteUrl(user.profilePictureUrl)) {
                transformations(CircleCropTransformation())
                crossfade(true)
                fallback(R.drawable.ic_default_avatar)
                error(R.drawable.ic_default_avatar)
            }

            itemView.setOnClickListener { onUserClick(user.id) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_search_user, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<UserSearchResult>() {
            override fun areItemsTheSame(old: UserSearchResult, new: UserSearchResult) = old.id == new.id
            override fun areContentsTheSame(old: UserSearchResult, new: UserSearchResult) = old == new
        }
    }
}
