package com.example.hubrise.ui.hubs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.hubrise.R
import com.example.hubrise.data.api.RetrofitClient
import com.example.hubrise.data.local.UserPreferences
import com.example.hubrise.data.model.Hub
import androidx.media3.exoplayer.ExoPlayer
import com.example.hubrise.ui.comments.CommentsBottomSheetFragment
import com.example.hubrise.ui.home.PostAdapter
import com.example.hubrise.ui.profile.UserProfileFragment
import com.example.hubrise.utils.PostSupportHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class HubDetailFragment : Fragment() {

    private lateinit var viewModel: HubDetailViewModel
    private lateinit var supportHelper: PostSupportHelper

    private lateinit var ivCover: ImageView
    private lateinit var btnBack: ImageView
    private lateinit var btnSettings: ImageView
    private lateinit var tvName: TextView
    private lateinit var tvMeta: TextView
    private lateinit var tvDescription: TextView
    private lateinit var btnJoinLeave: Button
    private lateinit var tabLayout: TabLayout
    private lateinit var rvContent: RecyclerView
    private lateinit var pbLoading: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var fabCreateChallenge: FloatingActionButton

    private lateinit var cardMainChallenge: View
    private lateinit var tvMainChallengeTitle: TextView
    private lateinit var pbMainChallenge: ProgressBar
    private lateinit var tvMainChallengeProgress: TextView
    private lateinit var btnMainChallengePlus: TextView

    // About tab views
    private lateinit var layoutAbout: View
    private lateinit var tvAboutDescription: TextView
    private lateinit var tvAboutCategory: TextView
    private lateinit var tvAboutMembers: TextView
    private lateinit var tvAboutVisibility: TextView
    private lateinit var tvAboutCreatedBy: TextView

    private lateinit var postAdapter: PostAdapter
    private lateinit var challengeAdapter: HubChallengeAdapter
    private var videoPlayer: ExoPlayer? = null

    private var currentTab = 0
    private var hubId = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_hub_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[HubDetailViewModel::class.java]

        ivCover = view.findViewById(R.id.iv_cover)
        btnBack = view.findViewById(R.id.btn_back)
        btnSettings = view.findViewById(R.id.btn_settings)
        tvName = view.findViewById(R.id.tv_hub_name)
        tvMeta = view.findViewById(R.id.tv_hub_meta)
        tvDescription = view.findViewById(R.id.tv_description)
        btnJoinLeave = view.findViewById(R.id.btn_join_leave)
        tabLayout = view.findViewById(R.id.tab_layout)
        rvContent = view.findViewById(R.id.rv_content)
        pbLoading = view.findViewById(R.id.pb_loading)
        tvEmpty = view.findViewById(R.id.tv_empty)
        fabCreateChallenge = view.findViewById(R.id.fab_create_challenge)

        cardMainChallenge = view.findViewById(R.id.card_main_challenge)
        tvMainChallengeTitle = view.findViewById(R.id.tv_main_challenge_title)
        pbMainChallenge = view.findViewById(R.id.pb_main_challenge)
        tvMainChallengeProgress = view.findViewById(R.id.tv_main_challenge_progress)
        btnMainChallengePlus = view.findViewById(R.id.btn_main_challenge_plus)

        layoutAbout = view.findViewById(R.id.layout_about)
        tvAboutDescription = view.findViewById(R.id.tv_about_description)
        tvAboutCategory = view.findViewById(R.id.tv_about_category)
        tvAboutMembers = view.findViewById(R.id.tv_about_members)
        tvAboutVisibility = view.findViewById(R.id.tv_about_visibility)
        tvAboutCreatedBy = view.findViewById(R.id.tv_about_created_by)

        val currentUserId = runBlocking { UserPreferences(requireContext()).userId.first() ?: -1 }
        supportHelper = PostSupportHelper(this)

        postAdapter = PostAdapter(
            currentUserId = currentUserId,
            onLikeClick = {},
            onCommentClick = { post ->
                CommentsBottomSheetFragment.newInstance(post.id, post.commentsCount)
                    .show(childFragmentManager, "comments")
            },
            onUserClick = { authorId ->
                val bundle = Bundle().apply { putInt(UserProfileFragment.ARG_USER_ID, authorId) }
                findNavController().navigate(R.id.userProfileFragment, bundle)
            },
            onHubClick = {},
            onValidateClick = { post -> viewModel.toggleValidate(post) },
            onMentalSupportClick = { post -> supportHelper.showMentalSupportDialog(post) },
            onPhysicalSupportClick = { post -> supportHelper.showPhysicalSupportDialog(post) },
            onGiftClick = { post -> supportHelper.handleGiftClick(post) },
        )
        challengeAdapter = HubChallengeAdapter { challenge ->
            val bundle = Bundle().apply { putInt("challengeId", challenge.id) }
            findNavController().navigate(R.id.challengeDetailFragment, bundle)
        }

        videoPlayer = ExoPlayer.Builder(requireContext()).build()
        postAdapter.setPlayer(videoPlayer!!)

        rvContent.layoutManager = LinearLayoutManager(requireContext())

        btnBack.setOnClickListener { findNavController().popBackStack() }

        fabCreateChallenge.setOnClickListener {
            val hub = viewModel.hub.value ?: return@setOnClickListener
            CreateChallengeFragment.newInstance(hubId, canSetMain = hub.isCreator)
                .show(childFragmentManager, "create_challenge")
        }

        childFragmentManager.setFragmentResultListener(
            CreateChallengeFragment.RESULT_KEY, viewLifecycleOwner
        ) { _, _ ->
            viewModel.loadChallenges(hubId)
        }

        tabLayout.addTab(tabLayout.newTab().setText("Posts"))
        tabLayout.addTab(tabLayout.newTab().setText("Challenges"))
        tabLayout.addTab(tabLayout.newTab().setText("About"))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentTab = tab.position
                updateTab()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        hubId = arguments?.getInt("hubId") ?: return
        viewModel.load(hubId)

        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        postAdapter.resumeActive()
        // Picks up posts created by progress actions (stage complete / count log / streak
        // check-in) on the Challenge Detail screen, which this fragment doesn't observe directly.
        if (hubId != 0) viewModel.loadPosts(hubId)
    }

    override fun onPause() {
        super.onPause()
        postAdapter.pauseAll()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        postAdapter.releasePlayer()
        videoPlayer = null
    }

    private fun updateTab() {
        layoutAbout.visibility = View.GONE
        when (currentTab) {
            0 -> {
                rvContent.adapter = postAdapter
                fabCreateChallenge.visibility = View.GONE
                val posts = viewModel.posts.value ?: emptyList()
                tvEmpty.visibility = if (posts.isEmpty()) View.VISIBLE else View.GONE
                tvEmpty.text = "No posts in this hub yet"
                rvContent.visibility = if (posts.isEmpty()) View.GONE else View.VISIBLE
            }
            1 -> {
                rvContent.adapter = challengeAdapter
                if ((viewModel.challenges.value ?: emptyList()).isEmpty()) {
                    viewModel.loadChallenges(hubId)
                }
                updateFabVisibility()
                val challenges = viewModel.challenges.value ?: emptyList()
                tvEmpty.visibility = if (challenges.isEmpty()) View.VISIBLE else View.GONE
                tvEmpty.text = "No challenges yet"
                rvContent.visibility = if (challenges.isEmpty()) View.GONE else View.VISIBLE
            }
            2 -> {
                rvContent.visibility = View.GONE
                fabCreateChallenge.visibility = View.GONE
                tvEmpty.visibility = View.GONE
                showAbout()
            }
        }
    }

    private fun updateFabVisibility() {
        val hub = viewModel.hub.value
        fabCreateChallenge.visibility = if (currentTab == 1 && hub?.isMember == true) View.VISIBLE else View.GONE
    }

    private fun showAbout() {
        val hub = viewModel.hub.value ?: return
        layoutAbout.visibility = View.VISIBLE
        tvAboutDescription.text = hub.description.ifEmpty { "No description provided." }
        tvAboutCategory.text = hub.categoryName?.ifEmpty { "—" } ?: "—"
        tvAboutMembers.text = hub.membersCount.toString()
        tvAboutVisibility.text = if (hub.isPublic) "Public" else "Private"
        tvAboutCreatedBy.text = if (hub.createdByUsername.isNotEmpty()) "@${hub.createdByUsername}" else "—"
    }

    private fun observeViewModel() {
        viewModel.hub.observe(viewLifecycleOwner) { hub ->
            hub ?: return@observe
            bindHub(hub)
            if (currentTab == 2) showAbout()
        }

        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            postAdapter.submitList(posts)
            if (currentTab == 0) {
                tvEmpty.visibility = if (posts.isEmpty()) View.VISIBLE else View.GONE
                tvEmpty.text = "No posts in this hub yet"
                rvContent.visibility = if (posts.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        viewModel.challenges.observe(viewLifecycleOwner) { challenges ->
            challengeAdapter.submitList(challenges)
            if (currentTab == 1) {
                tvEmpty.visibility = if (challenges.isEmpty()) View.VISIBLE else View.GONE
                tvEmpty.text = "No challenges yet"
                rvContent.visibility = if (challenges.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            pbLoading.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { err ->
            err?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show() }
        }
    }

    private fun bindHub(hub: Hub) {
        tvName.text = hub.name

        val meta = buildString {
            if (hub.categoryName?.isNotEmpty() == true) {
                append(hub.categoryName)
                append("  ·  ")
            }
            append("${hub.membersCount} members")
        }
        tvMeta.text = meta

        tvDescription.text = hub.description.ifEmpty { "No description" }

        val coverUrl = RetrofitClient.absoluteUrl(hub.coverImageUrl)
        if (coverUrl != null) ivCover.load(coverUrl) { crossfade(true) }

        bindMainChallenge(hub)

        applyJoinLeaveButton(hub)
        btnJoinLeave.setOnClickListener { viewModel.toggleJoin(hub) }

        // Show settings gear only to creator
        if (hub.isCreator) {
            btnSettings.visibility = View.VISIBLE
            btnSettings.setOnClickListener {
                val bundle = Bundle().apply {
                    putInt("hubId", hub.id)
                    putString("hubName", hub.name)
                    putString("hubDescription", hub.description)
                    putBoolean("isPublic", hub.isPublic)
                    putString("coverUrl", hub.coverImageUrl ?: "")
                    putString("categoryName", hub.categoryName ?: "")
                }
                findNavController().navigate(R.id.hubSettingsFragment, bundle)
            }
        } else {
            btnSettings.visibility = View.GONE
        }

        updateFabVisibility()

        // Default to Posts tab content
        if (rvContent.adapter == null) rvContent.adapter = postAdapter
    }

    private fun bindMainChallenge(hub: Hub) {
        val main = hub.mainChallenge
        if (main == null) {
            cardMainChallenge.visibility = View.GONE
            return
        }
        cardMainChallenge.visibility = View.VISIBLE
        tvMainChallengeTitle.text = "🏆 ${main.title}"
        pbMainChallenge.max = 100
        pbMainChallenge.progress = main.percentComplete.coerceIn(0, 100)
        val completed = main.percentComplete >= 100
        tvMainChallengeProgress.text = main.summary + if (completed) "  ·  Completed! 🎉" else ""

        // Progress always happens by creating a post, so this just opens the Challenge
        // Detail screen — there is no direct "+1"/check-in button anywhere in the app.
        btnMainChallengePlus.text = "View"
        btnMainChallengePlus.visibility = View.VISIBLE
        btnMainChallengePlus.setOnClickListener {
            val bundle = Bundle().apply { putInt("challengeId", main.id) }
            findNavController().navigate(R.id.challengeDetailFragment, bundle)
        }
    }

    private fun applyJoinLeaveButton(hub: Hub) {
        if (hub.isMember) {
            btnJoinLeave.text = "Joined"
            btnJoinLeave.setBackgroundResource(R.drawable.btn_outline)
            btnJoinLeave.setTextColor(requireContext().getColor(R.color.blue_primary))
        } else {
            btnJoinLeave.text = "Join Hub"
            btnJoinLeave.setBackgroundResource(R.drawable.btn_primary)
            btnJoinLeave.setTextColor(requireContext().getColor(R.color.white))
        }
    }
}
