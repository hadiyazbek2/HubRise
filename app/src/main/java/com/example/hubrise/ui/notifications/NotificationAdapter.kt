package com.example.hubrise.ui.notifications

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
import com.example.hubrise.data.model.NotificationItem

class NotificationAdapter(
    private val onNotificationClick: (NotificationItem) -> Unit
) : ListAdapter<NotificationItem, NotificationAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivAvatar: ImageView = view.findViewById(R.id.iv_avatar)
        private val tvMessage: TextView = view.findViewById(R.id.tv_message)
        private val tvTime: TextView = view.findViewById(R.id.tv_time)
        private val vUnread: View = view.findViewById(R.id.v_unread)

        fun bind(n: NotificationItem) {
            tvMessage.text = n.message
            tvTime.text = formatTime(n.createdAt)
            vUnread.visibility = if (!n.isRead) View.VISIBLE else View.INVISIBLE

            ivAvatar.load(RetrofitClient.absoluteUrl(n.senderAvatar)) {
                transformations(CircleCropTransformation())
                crossfade(true)
                fallback(R.drawable.ic_default_avatar)
                error(R.drawable.ic_default_avatar)
            }

            itemView.setOnClickListener { onNotificationClick(n) }
        }

        private fun formatTime(iso: String): String {
            return try {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                val date = sdf.parse(iso.substringBefore("+").substringBefore("Z")) ?: return iso
                val diff = System.currentTimeMillis() - date.time
                when {
                    diff < 60_000 -> "just now"
                    diff < 3_600_000 -> "${diff / 60_000}m"
                    diff < 86_400_000 -> "${diff / 3_600_000}h"
                    else -> "${diff / 86_400_000}d"
                }
            } catch (e: Exception) { "" }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<NotificationItem>() {
            override fun areItemsTheSame(old: NotificationItem, new: NotificationItem) = old.id == new.id
            override fun areContentsTheSame(old: NotificationItem, new: NotificationItem) = old == new
        }
    }
}
