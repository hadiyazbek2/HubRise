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
import com.example.hubrise.utils.TimeUtils

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
        holder.tvTime.text = TimeUtils.formatRelativeTime(comment.createdAt)
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

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Comment>() {
            override fun areItemsTheSame(a: Comment, b: Comment) = a.id == b.id
            override fun areContentsTheSame(a: Comment, b: Comment) = a == b
        }
    }
}
