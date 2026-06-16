package com.example.hubrise.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.hubrise.R
import com.example.hubrise.data.local.UserPreferences
import com.example.hubrise.ui.comments.CommentsBottomSheetFragment
import com.example.hubrise.ui.profile.UserProfileFragment
import com.example.hubrise.utils.PostSupportHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class HomeFragment : Fragment() {

    private lateinit var viewModel: HomeViewModel
    private lateinit var adapter: PostAdapter
    private lateinit var supportHelper: PostSupportHelper

    private lateinit var rvPosts: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var pbLoading: ProgressBar
    private lateinit var layoutEmpty: View
    private lateinit var btnExploreHubs: Button
    private lateinit var vNotifDot: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        rvPosts = view.findViewById(R.id.rv_posts)
        swipeRefresh = view.findViewById(R.id.swipe_refresh)
        pbLoading = view.findViewById(R.id.pb_loading)
        layoutEmpty = view.findViewById(R.id.layout_empty)
        btnExploreHubs = view.findViewById(R.id.btn_explore_hubs)
        vNotifDot = view.findViewById(R.id.v_notif_dot)

        view.findViewById<ImageView>(R.id.btn_search).setOnClickListener {
            findNavController().navigate(R.id.searchFragment)
        }

        view.findViewById<ImageView>(R.id.btn_notifications).setOnClickListener {
            vNotifDot.visibility = View.GONE
            findNavController().navigate(R.id.notificationsFragment)
        }

        view.findViewById<ImageView>(R.id.btn_messages).setOnClickListener {
            Toast.makeText(requireContext(), "Direct messages — coming soon", Toast.LENGTH_SHORT).show()
        }

        val currentUserId = runBlocking { UserPreferences(requireContext()).userId.first() ?: -1 }
        supportHelper = PostSupportHelper(this)

        adapter = PostAdapter(
            currentUserId = currentUserId,
            onLikeClick = { post -> viewModel.toggleLike(post) },
            onCommentClick = { post ->
                CommentsBottomSheetFragment.newInstance(post.id, post.commentsCount)
                    .show(childFragmentManager, "comments")
            },
            onUserClick = { authorId ->
                val bundle = Bundle().apply { putInt(UserProfileFragment.ARG_USER_ID, authorId) }
                findNavController().navigate(R.id.userProfileFragment, bundle)
            },
            onHubClick = { hubId ->
                val bundle = Bundle().apply { putInt("hubId", hubId) }
                findNavController().navigate(R.id.hubDetailFragment, bundle)
            },
            onValidateClick = { post -> viewModel.toggleValidate(post) },
            onMentalSupportClick = { post -> supportHelper.showMentalSupportDialog(post) },
            onPhysicalSupportClick = { post -> supportHelper.showPhysicalSupportDialog(post) },
            onGiftClick = { post -> supportHelper.handleGiftClick(post) },
        )
        rvPosts.layoutManager = LinearLayoutManager(requireContext())
        rvPosts.adapter = adapter

        swipeRefresh.setColorSchemeResources(R.color.blue_primary)
        swipeRefresh.setOnRefreshListener { viewModel.refresh() }

        btnExploreHubs.setOnClickListener {
            findNavController().navigate(R.id.exploreFragment)
        }

        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchUnreadCount()
    }

    private fun observeViewModel() {
        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            adapter.submitList(posts)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (!swipeRefresh.isRefreshing) {
                pbLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
            if (!isLoading) swipeRefresh.isRefreshing = false
        }

        viewModel.isEmpty.observe(viewLifecycleOwner) { isEmpty ->
            layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
            rvPosts.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show() }
        }

        viewModel.unreadCount.observe(viewLifecycleOwner) { count ->
            vNotifDot.visibility = if (count > 0) View.VISIBLE else View.GONE
        }
    }
}
