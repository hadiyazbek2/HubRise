package com.example.hubrise.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.hubrise.R
import com.example.hubrise.data.api.RetrofitClient
import com.example.hubrise.ui.home.PostAdapter
import com.example.hubrise.utils.PostSupportHelper

class UserProfileFragment : Fragment() {

    companion object {
        const val ARG_USER_ID = "user_id"
    }

    private lateinit var viewModel: UserProfileViewModel
    private lateinit var postsAdapter: PostAdapter
    private var videoPlayer: ExoPlayer? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_user_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = arguments?.getInt(ARG_USER_ID) ?: run {
            findNavController().popBackStack(); return
        }

        viewModel = ViewModelProvider(this)[UserProfileViewModel::class.java]

        val btnBack = view.findViewById<ImageView>(R.id.btn_back)
        val tvToolbar = view.findViewById<TextView>(R.id.tv_toolbar_username)
        val ivAvatar = view.findViewById<ImageView>(R.id.iv_avatar)
        val tvFullName = view.findViewById<TextView>(R.id.tv_full_name)
        val tvUsername = view.findViewById<TextView>(R.id.tv_username)
        val tvBio = view.findViewById<TextView>(R.id.tv_bio)
        val btnFollow = view.findViewById<Button>(R.id.btn_follow)
        val tvPostCount = view.findViewById<TextView>(R.id.tv_post_count)
        val tvFollowersCount = view.findViewById<TextView>(R.id.tv_followers_count)
        val tvFollowingCount = view.findViewById<TextView>(R.id.tv_following_count)
        val tvHubsCount = view.findViewById<TextView>(R.id.tv_hubs_count)
        val pbLoading = view.findViewById<ProgressBar>(R.id.pb_loading)
        val rvPosts = view.findViewById<RecyclerView>(R.id.rv_posts)
        val tvNoPosts = view.findViewById<TextView>(R.id.tv_no_posts)

        btnBack.setOnClickListener { findNavController().popBackStack() }

        val supportHelper = PostSupportHelper(this)
        postsAdapter = PostAdapter(
            onUserClick = { authorId ->
                if (authorId != userId) {
                    val bundle = Bundle().apply { putInt(ARG_USER_ID, authorId) }
                    findNavController().navigate(R.id.userProfileFragment, bundle)
                }
            },
            onHubClick = { hubId ->
                val bundle = Bundle().apply { putInt("hubId", hubId) }
                findNavController().navigate(R.id.hubDetailFragment, bundle)
            },
            onMentalSupportClick = { post -> supportHelper.showMentalSupportDialog(post) },
            onPhysicalSupportClick = { post -> supportHelper.showPhysicalSupportDialog(post) },
            onGiftClick = { post -> supportHelper.handleGiftClick(post) },
        )
        videoPlayer = ExoPlayer.Builder(requireContext()).build()
        postsAdapter.setPlayer(videoPlayer!!)

        rvPosts.layoutManager = LinearLayoutManager(requireContext())
        rvPosts.adapter = postsAdapter

        viewModel.isOwnProfile.observe(viewLifecycleOwner) { isOwn ->
            btnFollow.visibility = if (isOwn) View.GONE else View.VISIBLE
        }

        btnFollow.setOnClickListener {
            viewModel.toggleFollow(userId)
        }

        viewModel.isFollowing.observe(viewLifecycleOwner) { isFollowing ->
            if (isFollowing) {
                btnFollow.text = "Following"
                btnFollow.setBackgroundResource(R.drawable.btn_join)
                btnFollow.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue_primary))
            } else {
                btnFollow.text = "Follow"
                btnFollow.setBackgroundResource(R.drawable.btn_joined)
                btnFollow.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            }
        }

        viewModel.profile.observe(viewLifecycleOwner) { profile ->
            profile ?: return@observe
            tvToolbar.text = profile.username
            tvFullName.text = profile.fullName.ifEmpty { profile.username }
            tvUsername.text = "@${profile.username}"
            tvPostCount.text = profile.postCount.toString()
            tvFollowersCount.text = profile.followersCount.toString()
            tvFollowingCount.text = profile.followingCount.toString()
            tvHubsCount.text = profile.hubsCount.toString()

            if (profile.bio.isNotEmpty()) {
                tvBio.text = profile.bio
                tvBio.visibility = View.VISIBLE
            } else {
                tvBio.visibility = View.GONE
            }

            ivAvatar.load(RetrofitClient.absoluteUrl(profile.profilePictureUrl)) {
                transformations(CircleCropTransformation())
                crossfade(true)
                fallback(R.drawable.ic_default_avatar)
                error(R.drawable.ic_default_avatar)
            }
        }

        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            postsAdapter.submitList(posts)
            tvNoPosts.visibility = if (posts.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            pbLoading.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { err ->
            err?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
        }

        viewModel.load(userId)
    }

    override fun onResume() {
        super.onResume()
        postsAdapter.resumeActive()
    }

    override fun onPause() {
        super.onPause()
        postsAdapter.pauseAll()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        postsAdapter.releasePlayer()
        videoPlayer = null
    }
}
