package com.example.hubrise.ui.comments

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
import com.example.hubrise.data.model.Comment

class CommentAdapter(
    private val currentUserId: Int,
    private val onDelete: (Comment) -> Unit,
) : ListAdapter<Comment, CommentAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivAvatar: ImageView = view.findViewById(R.id.iv_comment_avatar)
        val tvUsername: TextView = view.findViewById(R.id.tv_comment_username)
        val tvContent: TextView = view.findViewById(R.id.tv_comment_content)
        val tvTime: TextView = view.findViewById(R.id.tv_comment_time)
        val btnDelete: ImageView = view.findViewById(R.id.btn_delete_comment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val comment = getItem(position)
        holder.tvUsername.text = comment.authorUsername
        holder.tvContent.text = comment.content
        holder.tvTime.text = formatTime(comment.createdAt)
        holder.btnDelete.visibility = if (comment.author == currentUserId) View.VISIBLE else View.GONE
        holder.btnDelete.setOnClickListener { onDelete(comment) }

        if (!comment.authorAvatarUrl.isNullOrEmpty()) {
            holder.ivAvatar.load(comment.authorAvatarUrl) {
                transformations(CircleCropTransformation())
                crossfade(true)
                placeholder(R.drawable.ic_default_avatar)
            }
        } else {
            holder.ivAvatar.setImageResource(R.drawable.ic_default_avatar)
        }
    }

    private fun formatTime(iso: String): String = try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val date = sdf.parse(iso.substringBefore(".")) ?: return iso
        val diff = (System.currentTimeMillis() - date.time) / 1000
        when {
            diff < 60 -> "just now"
            diff < 3600 -> "${diff / 60}m"
            diff < 86400 -> "${diff / 3600}h"
            else -> "${diff / 86400}d"
        }
    } catch (e: Exception) { "" }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Comment>() {
            override fun areItemsTheSame(a: Comment, b: Comment) = a.id == b.id
            override fun areContentsTheSame(a: Comment, b: Comment) = a == b
        }
    }
}
