package com.example.hubrise.ui.explore

import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.GestureDetectorCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.hubrise.R
import com.example.hubrise.data.model.Post
import com.example.hubrise.utils.TimeUtils

class ExploreAdapter(
    private val player: ExoPlayer,
    private val onLikeClick: (Post) -> Unit,
    private val onCommentClick: (Post) -> Unit,
    private val onUserClick: (Int) -> Unit,
    private val onHubClick: (Int) -> Unit,
    private val onNearEnd: () -> Unit,
) : RecyclerView.Adapter<ExploreAdapter.ExploreViewHolder>() {

    private val posts = mutableListOf<Post>()
    private var currentPosition = -1
    private var isMuted = true

    fun submitList(newPosts: List<Post>) {
        posts.clear()
        posts.addAll(newPosts)
        notifyDataSetChanged()
    }

    fun appendList(newPosts: List<Post>) {
        val start = posts.size
        posts.addAll(newPosts)
        notifyItemRangeInserted(start, newPosts.size)
    }

    fun setCurrentPosition(position: Int) {
        if (position == currentPosition) return
        player.pause()    // stop audio immediately
        player.seekTo(0)  // reset position so returning to it starts fresh
        currentPosition = position
        notifyItemChanged(position)
        if (position >= posts.size - 3) onNearEnd()
    }

    fun pausePlayer() { player.pause() }
    fun resumePlayer() { if (currentPosition >= 0 && posts.getOrNull(currentPosition)?.mediaType == "video") player.play() }
    fun releasePlayer() { player.release() }

    override fun getItemCount(): Int = posts.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExploreViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_explore_post, parent, false)
        return ExploreViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExploreViewHolder, position: Int) {
        holder.bind(posts[position], position == currentPosition)
    }

    // Release player when an item that was playing scrolls off screen
    override fun onViewRecycled(holder: ExploreViewHolder) {
        super.onViewRecycled(holder)
        holder.detachPlayer()
    }

    inner class ExploreViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val playerView: PlayerView = view.findViewById(R.id.player_view)
        private val ivImage: ImageView = view.findViewById(R.id.iv_image)
        private val ivAvatar: ImageView = view.findViewById(R.id.iv_avatar)
        private val tvUsername: TextView = view.findViewById(R.id.tv_username)
        private val tvHubName: TextView = view.findViewById(R.id.tv_hub_name)
        private val tvCaption: TextView = view.findViewById(R.id.tv_caption)
        private val btnLike: ImageView = view.findViewById(R.id.btn_like)
        private val tvLikesCount: TextView = view.findViewById(R.id.tv_likes_count)
        private val btnComment: ImageView = view.findViewById(R.id.btn_comment)
        private val tvCommentsCount: TextView = view.findViewById(R.id.tv_comments_count)
        private val btnShare: ImageView = view.findViewById(R.id.btn_share)
        private val btnMute: ImageView = view.findViewById(R.id.btn_mute)

        fun bind(post: Post, isActive: Boolean) {
            tvUsername.text = "@${post.authorUsername}"
            tvCaption.text = post.content

            if (!post.hubName.isNullOrEmpty()) {
                tvHubName.text = post.hubName
                tvHubName.visibility = View.VISIBLE
                tvHubName.setOnClickListener { post.hub?.let { onHubClick(it) } }
            } else {
                tvHubName.visibility = View.GONE
            }

            tvUsername.setOnClickListener { if (post.author != 0) onUserClick(post.author) }
            ivAvatar.setOnClickListener { if (post.author != 0) onUserClick(post.author) }
            ivAvatar.load(post.authorAvatarUrl?.takeIf { it.isNotEmpty() }) {
                transformations(CircleCropTransformation())
                crossfade(true)
                fallback(R.drawable.ic_default_avatar)
                error(R.drawable.ic_default_avatar)
            }

            applyLikeState(post)
            btnLike.setOnClickListener { onLikeClick(post) }
            tvCommentsCount.text = post.commentsCount.toString()
            btnComment.setOnClickListener { onCommentClick(post) }
            btnShare.setOnClickListener {
                val text = buildString {
                    append("@${post.authorUsername}")
                    if (!post.content.isNullOrEmpty()) append(": ${post.content}")
                }
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                itemView.context.startActivity(Intent.createChooser(intent, "Share post"))
            }

            if (post.mediaType == "video" && !post.mediaUrl.isNullOrEmpty()) {
                bindVideo(post, isActive)
            } else if (!post.mediaUrl.isNullOrEmpty()) {
                bindImage(post)
            }
        }

        private fun bindVideo(post: Post, isActive: Boolean) {
            playerView.visibility = View.VISIBLE
            ivImage.visibility = View.GONE
            btnMute.visibility = View.VISIBLE

            btnMute.setImageResource(if (isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_on)
            btnMute.setOnClickListener {
                isMuted = !isMuted
                player.volume = if (isMuted) 0f else 1f
                btnMute.setImageResource(if (isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_on)
            }

            if (isActive) {
                playerView.player = player
                val mediaItem = MediaItem.fromUri(post.mediaUrl!!)
                if (player.currentMediaItem?.localConfiguration?.uri?.toString() != post.mediaUrl) {
                    player.setMediaItem(mediaItem)
                    player.repeatMode = ExoPlayer.REPEAT_MODE_ONE
                    player.volume = if (isMuted) 0f else 1f
                    player.prepare()
                } else {
                    // same video still active (e.g. data rebind) — just sync volume, don't seek
                    player.volume = if (isMuted) 0f else 1f
                }
                player.play()

                val gestureDetector = GestureDetectorCompat(
                    itemView.context,
                    object : GestureDetector.SimpleOnGestureListener() {
                        override fun onDown(e: MotionEvent): Boolean = true

                        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                            if (player.isPlaying) player.pause() else player.play()
                            return true
                        }

                        override fun onDoubleTap(e: MotionEvent): Boolean {
                            onLikeClick(post)
                            return true
                        }
                    },
                )
                playerView.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event) }
            } else {
                playerView.setOnTouchListener(null)
                detachPlayer()
            }
        }

        private fun bindImage(post: Post) {
            playerView.visibility = View.GONE
            ivImage.visibility = View.VISIBLE
            btnMute.visibility = View.GONE
            playerView.setOnTouchListener(null)
            detachPlayer()
            ivImage.load(post.mediaUrl) { crossfade(true) }

            val gestureDetector = GestureDetectorCompat(
                itemView.context,
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onDown(e: MotionEvent): Boolean = true
                    override fun onDoubleTap(e: MotionEvent): Boolean {
                        onLikeClick(post)
                        return true
                    }
                },
            )
            ivImage.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event) }
        }

        fun detachPlayer() {
            playerView.player = null
        }

        private fun applyLikeState(post: Post) {
            if (post.isLiked) {
                btnLike.setImageResource(R.drawable.ic_heart_filled)
                btnLike.setColorFilter(Color.parseColor("#E53935"), PorterDuff.Mode.SRC_IN)
            } else {
                btnLike.setImageResource(R.drawable.ic_heart_outline)
                btnLike.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
            }
            tvLikesCount.text = post.likesCount.toString()
        }
    }
}
