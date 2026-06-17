package com.example.hubrise.ui.explore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.example.hubrise.R
import com.example.hubrise.ui.comments.CommentsBottomSheetFragment
import com.example.hubrise.ui.profile.UserProfileFragment

class ExploreFragment : Fragment() {

    private lateinit var viewModel: ExploreViewModel
    private lateinit var adapter: ExploreAdapter
    private lateinit var player: ExoPlayer
    private lateinit var viewPager: ViewPager2

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.fragment_explore, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[ExploreViewModel::class.java]
        player = ExoPlayer.Builder(requireContext()).build()

        viewPager = view.findViewById(R.id.vp_explore)
        val pbExplore = view.findViewById<ProgressBar>(R.id.pb_explore)
        val layoutEmpty = view.findViewById<View>(R.id.layout_empty)

        adapter = ExploreAdapter(
            player = player,
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
            onNearEnd = { viewModel.loadMore() },
        )
        viewPager.adapter = adapter
        viewPager.offscreenPageLimit = 1

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                adapter.setCurrentPosition(position)
            }
        })

        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            adapter.submitList(posts)
            if (posts.isNotEmpty() && viewPager.currentItem == 0) {
                adapter.setCurrentPosition(0)
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            pbExplore.visibility = if (loading && adapter.itemCount == 0) View.VISIBLE else View.GONE
        }

        viewModel.isEmpty.observe(viewLifecycleOwner) { empty ->
            layoutEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        adapter.resumePlayer()
    }

    override fun onPause() {
        super.onPause()
        adapter.pausePlayer()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapter.releasePlayer()
    }
}
