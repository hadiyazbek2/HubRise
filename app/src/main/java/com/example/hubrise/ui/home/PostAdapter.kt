package com.example.hubrise.ui.home

import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.hubrise.R
import com.example.hubrise.data.model.Post
import com.example.hubrise.data.model.PostType
import com.example.hubrise.utils.TimeUtils

class PostAdapter(
    private val currentUserId: Int = -1,
    private val onLikeClick: (Post) -> Unit = {},
    private val onCommentClick: (Post) -> Unit = {},
    private val onUserClick: (authorId: Int) -> Unit = {},
    private val onHubClick: (hubId: Int) -> Unit = {},
    private val onValidateClick: (Post) -> Unit = {},
    private val onMentalSupportClick: (Post) -> Unit = {},
    private val onPhysicalSupportClick: (Post) -> Unit = {},
    private val onGiftClick: (Post) -> Unit = {},
) : ListAdapter<Post, PostAdapter.PostViewHolder>(DiffCallback) {

    private var player: ExoPlayer? = null
    private var activeHolder: PostViewHolder? = null

    fun setPlayer(p: ExoPlayer) { player = p }

    fun pauseAll() { player?.pause() }

    fun resumeActive() { if (activeHolder != null) player?.play() }

    fun releasePlayer() {
        activeHolder?.stopVideo()
        activeHolder = null
        player?.release()
        player = null
    }

    override fun onViewDetachedFromWindow(holder: PostViewHolder) {
        super.onViewDetachedFromWindow(holder)
        if (holder === activeHolder) {
            player?.stop()
            holder.stopVideo()
            activeHolder = null
        }
    }

    override fun onViewRecycled(holder: PostViewHolder) {
        super.onViewRecycled(holder)
        if (holder === activeHolder) {
            player?.stop()
            holder.stopVideo()
            activeHolder = null
        }
    }

    inner class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivAvatar: ImageView = view.findViewById(R.id.iv_avatar)
        private val tvUsername: TextView = view.findViewById(R.id.tv_username)
        private val tvHubName: TextView = view.findViewById(R.id.tv_hub_name)
        private val tvTimestamp: TextView = view.findViewById(R.id.tv_timestamp)
        private val containerMedia: FrameLayout = view.findViewById(R.id.container_media)
        private val ivMedia: ImageView = view.findViewById(R.id.iv_media)
        internal val playerView: PlayerView = view.findViewById(R.id.player_view)
        private val ivPlayOverlay: ImageView = view.findViewById(R.id.iv_play_overlay)
        private val btnLike: ImageView = view.findViewById(R.id.btn_like)
        private val tvLikesCount: TextView = view.findViewById(R.id.tv_likes_count)
        private val tvCommentsCount: TextView = view.findViewById(R.id.tv_comments_count)
        private val btnComment: ImageView = view.findViewById(R.id.btn_comment)
        private val btnShare: ImageView = view.findViewById(R.id.btn_share)
        private val tvCaption: TextView = view.findViewById(R.id.tv_caption)
        private val rowValidate: View = view.findViewById(R.id.row_validate)
        private val btnValidate: ImageView = view.findViewById(R.id.btn_validate)
        private val tvValidateLabel: TextView = view.findViewById(R.id.tv_validate_label)
        private val rowSupport: View = view.findViewById(R.id.row_support)
        private val btnSupportMental: View = view.findViewById(R.id.btn_support_mental)
        private val btnSupportPhysical: View = view.findViewById(R.id.btn_support_physical)
        private val btnSupportGift: View = view.findViewById(R.id.btn_support_gift)

        fun bind(post: Post) {
            tvUsername.text = post.authorUsername
            tvTimestamp.text = TimeUtils.formatRelativeTime(post.createdAt)

            if (!post.hubName.isNullOrEmpty() && post.hub != null) {
                tvHubName.text = post.hubName
                tvHubName.visibility = View.VISIBLE
                tvHubName.setOnClickListener { onHubClick(post.hub) }
            } else {
                tvHubName.visibility = View.GONE
            }

            val userClickListener = View.OnClickListener { if (post.author != 0) onUserClick(post.author) }
            ivAvatar.setOnClickListener(userClickListener)
            tvUsername.setOnClickListener(userClickListener)

            ivAvatar.load(post.authorAvatarUrl?.takeIf { it.isNotEmpty() }) {
                transformations(CircleCropTransformation())
                crossfade(true)
                fallback(R.drawable.ic_default_avatar)
                error(R.drawable.ic_default_avatar)
            }

            if (!post.mediaUrl.isNullOrEmpty()) {
                containerMedia.visibility = View.VISIBLE
                if (post.mediaType == "video") {
                    showVideoThumbnail()
                    containerMedia.setOnClickListener {
                        val p = player ?: return@setOnClickListener
                        activeHolder?.stopVideo()
                        activeHolder = this@PostViewHolder
                        startVideo(post, p)
                    }
                } else {
                    ivMedia.load(post.mediaUrl) { crossfade(true) }
                    ivMedia.visibility = View.VISIBLE
                    playerView.visibility = View.GONE
                    ivPlayOverlay.visibility = View.GONE
                    containerMedia.setOnClickListener(null)
                }
            } else {
                containerMedia.visibility = View.GONE
            }

            val caption = SpannableStringBuilder()
            val usernameEnd = post.authorUsername.length
            caption.append(post.authorUsername)
            caption.setSpan(StyleSpan(Typeface.BOLD), 0, usernameEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (post.content.isNotBlank()) {
                caption.append("  ")
                caption.append(post.content)
            }
            tvCaption.text = caption

            applyLikeState(post.isLiked, post.likesCount)
            btnLike.setOnClickListener { onLikeClick(post) }

            tvCommentsCount.text = post.commentsCount.toString()
            btnComment.setOnClickListener { onCommentClick(post) }

            btnShare.setOnClickListener {
                val text = buildString {
                    append("@${post.authorUsername}")
                    if (post.content.isNotBlank()) append(": ${post.content}")
                }
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                itemView.context.startActivity(Intent.createChooser(intent, "Share post"))
            }

            applyValidateState(post)
            applySupportRow(post)
        }

        fun startVideo(post: Post, p: ExoPlayer) {
            ivMedia.visibility = View.GONE
            ivPlayOverlay.visibility = View.GONE
            playerView.visibility = View.VISIBLE
            playerView.player = p
            playerView.setOnClickListener { if (p.isPlaying) p.pause() else p.play() }
            p.setMediaItem(MediaItem.fromUri(post.mediaUrl!!))
            p.repeatMode = ExoPlayer.REPEAT_MODE_ONE
            p.prepare()
            p.play()
        }

        fun stopVideo() {
            playerView.setOnClickListener(null)
            playerView.player = null
            playerView.visibility = View.GONE
            showVideoThumbnail()
        }

        private fun showVideoThumbnail() {
            ivMedia.setImageDrawable(null)
            ivMedia.setBackgroundColor(Color.BLACK)
            ivMedia.visibility = View.VISIBLE
            ivPlayOverlay.visibility = View.VISIBLE
        }

        private fun applySupportRow(post: Post) {
            if (post.postType != PostType.ANNOUNCEMENT) {
                rowSupport.visibility = View.GONE
                return
            }
            rowSupport.visibility = View.VISIBLE
            btnSupportMental.setOnClickListener { onMentalSupportClick(post) }
            btnSupportPhysical.setOnClickListener { onPhysicalSupportClick(post) }
            btnSupportGift.setOnClickListener { onGiftClick(post) }
        }

        private fun applyValidateState(post: Post) {
            if (post.challenge == null || post.postType !in PostType.CHALLENGE_TYPES) {
                rowValidate.visibility = View.GONE
                return
            }
            rowValidate.visibility = View.VISIBLE

            if (post.isTrusted) {
                btnValidate.setColorFilter(Color.parseColor("#16A34A"), PorterDuff.Mode.SRC_IN)
                tvValidateLabel.text = "Trusted"
                tvValidateLabel.setTextColor(Color.parseColor("#16A34A"))
            } else if (post.validatedByMe) {
                btnValidate.setColorFilter(Color.parseColor("#1A73E8"), PorterDuff.Mode.SRC_IN)
                tvValidateLabel.text = "${post.validationsCount} verified"
                tvValidateLabel.setTextColor(Color.parseColor("#1A73E8"))
            } else {
                btnValidate.setColorFilter(Color.parseColor("#64748B"), PorterDuff.Mode.SRC_IN)
                tvValidateLabel.text = if (post.validationsCount > 0) "${post.validationsCount} verified" else "I believe this"
                tvValidateLabel.setTextColor(Color.parseColor("#64748B"))
            }

            val isOwnPost = post.author != 0 && post.author == currentUserId
            rowValidate.alpha = if (isOwnPost) 0.4f else 1f
            rowValidate.isEnabled = !isOwnPost
            rowValidate.setOnClickListener { if (!isOwnPost) onValidateClick(post) }
        }

        private fun applyLikeState(liked: Boolean, count: Int) {
            if (liked) {
                btnLike.setImageResource(R.drawable.ic_heart_filled)
                btnLike.setColorFilter(Color.parseColor("#E53935"), PorterDuff.Mode.SRC_IN)
            } else {
                btnLike.setImageResource(R.drawable.ic_heart_outline)
                btnLike.setColorFilter(Color.parseColor("#64748B"), PorterDuff.Mode.SRC_IN)
            }
            tvLikesCount.text = count.toString()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Post>() {
            override fun areItemsTheSame(old: Post, new: Post) = old.id == new.id
            override fun areContentsTheSame(old: Post, new: Post) = old == new
        }
    }
}
