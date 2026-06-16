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
import com.example.hubrise.data.model.PostSearchResult

class PostSearchAdapter(
    private val onPostClick: (PostSearchResult) -> Unit
) : ListAdapter<PostSearchResult, PostSearchAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivAvatar: ImageView = view.findViewById(R.id.iv_avatar)
        private val tvContent: TextView = view.findViewById(R.id.tv_content)
        private val tvMeta: TextView = view.findViewById(R.id.tv_meta)

        fun bind(post: PostSearchResult) {
            tvContent.text = post.content
            tvMeta.text = buildString {
                append("@${post.authorUsername}")
                if (!post.hubName.isNullOrEmpty()) append(" · ${post.hubName}")
            }

            ivAvatar.load(RetrofitClient.absoluteUrl(post.authorAvatarUrl)) {
                transformations(CircleCropTransformation())
                crossfade(true)
                fallback(R.drawable.ic_default_avatar)
                error(R.drawable.ic_default_avatar)
            }

            itemView.setOnClickListener { onPostClick(post) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_search_post, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<PostSearchResult>() {
            override fun areItemsTheSame(old: PostSearchResult, new: PostSearchResult) = old.id == new.id
            override fun areContentsTheSame(old: PostSearchResult, new: PostSearchResult) = old == new
        }
    }
}
