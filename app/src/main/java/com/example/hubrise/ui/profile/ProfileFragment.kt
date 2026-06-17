package com.example.hubrise.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.hubrise.R
import com.example.hubrise.data.api.RetrofitClient
import com.example.hubrise.ui.auth.login.LoginActivity
import com.example.hubrise.ui.home.PostAdapter
import com.example.hubrise.utils.PostSupportHelper

class ProfileFragment : Fragment() {

    private lateinit var viewModel: ProfileViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[ProfileViewModel::class.java]

        val tvToolbarUsername = view.findViewById<TextView>(R.id.tv_toolbar_username)
        val btnSettings = view.findViewById<ImageView>(R.id.btn_settings)
        val ivAvatar = view.findViewById<ImageView>(R.id.iv_avatar)
        val tvFullName = view.findViewById<TextView>(R.id.tv_full_name)
        val tvUsername = view.findViewById<TextView>(R.id.tv_username)
        val tvBio = view.findViewById<TextView>(R.id.tv_bio)
        val tvPostCount = view.findViewById<TextView>(R.id.tv_post_count)
        val tvFollowersCount = view.findViewById<TextView>(R.id.tv_followers_count)
        val tvFollowingCount = view.findViewById<TextView>(R.id.tv_following_count)
        val tvHubsCount = view.findViewById<TextView>(R.id.tv_hubs_count)
        val pbLoading = view.findViewById<ProgressBar>(R.id.pb_loading)
        val rvPosts = view.findViewById<RecyclerView>(R.id.rv_posts)
        val tvNoPosts = view.findViewById<TextView>(R.id.tv_no_posts)

        val supportHelper = PostSupportHelper(this)
        val postsAdapter = PostAdapter(
            onUserClick = { /* own posts — no navigation needed */ },
            onHubClick = { hubId ->
                val bundle = Bundle().apply { putInt("hubId", hubId) }
                findNavController().navigate(R.id.hubDetailFragment, bundle)
            },
            onMentalSupportClick = { post -> supportHelper.showMentalSupportDialog(post) },
            onPhysicalSupportClick = { post -> supportHelper.showPhysicalSupportDialog(post) },
            onGiftClick = { post -> supportHelper.handleGiftClick(post) },
        )
        rvPosts.layoutManager = LinearLayoutManager(requireContext())
        rvPosts.adapter = postsAdapter

        btnSettings.setOnClickListener { anchor ->
            val popup = PopupMenu(requireContext(), anchor)
            popup.menuInflater.inflate(R.menu.menu_profile_options, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_edit_profile -> {
                        val profile = viewModel.profile.value
                        val bundle = Bundle().apply {
                            putInt("userId", profile?.id ?: 0)
                            putString("fullName", profile?.fullName ?: "")
                            putString("bio", profile?.bio ?: "")
                            putString("wishlistUrl", profile?.wishlistUrl ?: "")
                            putString("avatarUrl", profile?.profilePictureUrl ?: "")
                        }
                        findNavController().navigate(R.id.editProfileFragment, bundle)
                        true
                    }
                    R.id.action_notifications -> {
                        findNavController().navigate(R.id.notificationsFragment)
                        true
                    }
                    R.id.action_privacy -> {
                        Toast.makeText(requireContext(), "Privacy — coming soon", Toast.LENGTH_SHORT).show()
                        true
                    }
                    R.id.action_logout -> {
                        viewModel.logout()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        // Show username immediately from DataStore while API loads
        viewModel.username.observe(viewLifecycleOwner) { username ->
            if (tvToolbarUsername.text.isNullOrEmpty() && username != null) {
                tvToolbarUsername.text = "@$username"
                tvUsername.text = "@$username"
            }
        }

        viewModel.profile.observe(viewLifecycleOwner) { profile ->
            profile ?: return@observe
            tvToolbarUsername.text = "@${profile.username}"
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

        viewModel.loggedOut.observe(viewLifecycleOwner) { loggedOut ->
            if (loggedOut) {
                val intent = Intent(requireActivity(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadProfile()
    }
}
