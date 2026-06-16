package com.example.hubrise.ui.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hubrise.R
import com.example.hubrise.data.model.PostSearchResult
import com.example.hubrise.ui.profile.UserProfileFragment
import com.google.android.material.tabs.TabLayout

class SearchFragment : Fragment() {

    private companion object {
        const val TAB_ALL = 0
        const val TAB_PEOPLE = 1
        const val TAB_HUBS = 2
        const val TAB_POSTS = 3
        const val TAB_CHALLENGES = 4
    }

    private lateinit var viewModel: SearchViewModel
    private lateinit var sectionPeople: View
    private lateinit var sectionHubs: View
    private lateinit var sectionPosts: View
    private lateinit var sectionChallenges: View
    private lateinit var tvEmpty: TextView
    private lateinit var etSearch: EditText

    private var currentTab = TAB_ALL

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_search, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[SearchViewModel::class.java]

        val btnBack = view.findViewById<ImageView>(R.id.btn_back)
        etSearch = view.findViewById(R.id.et_search)
        val pbLoading = view.findViewById<ProgressBar>(R.id.pb_loading)
        val tabLayout = view.findViewById<TabLayout>(R.id.tab_layout)
        sectionPeople = view.findViewById(R.id.section_people)
        sectionHubs = view.findViewById(R.id.section_hubs)
        sectionPosts = view.findViewById(R.id.section_posts)
        sectionChallenges = view.findViewById(R.id.section_challenges)
        tvEmpty = view.findViewById(R.id.tv_empty)

        btnBack.setOnClickListener { findNavController().popBackStack() }

        // Setup tabs
        listOf("All", "People", "Hubs", "Posts", "Challenges").forEach {
            tabLayout.addTab(tabLayout.newTab().setText(it))
        }
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentTab = tab.position
                updateSectionVisibility()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        // Adapters
        val usersAdapter = UserSearchAdapter { userId ->
            val bundle = Bundle().apply { putInt(UserProfileFragment.ARG_USER_ID, userId) }
            findNavController().navigate(R.id.userProfileFragment, bundle)
        }
        val hubsAdapter = HubSearchAdapter { hubId ->
            val bundle = Bundle().apply { putInt("hubId", hubId) }
            findNavController().navigate(R.id.hubDetailFragment, bundle)
        }
        val postsAdapter = PostSearchAdapter { post -> navigateFromPost(post) }
        val challengesAdapter = ChallengeSearchAdapter { challenge ->
            challenge.hubId?.let { hubId ->
                val bundle = Bundle().apply { putInt("hubId", hubId) }
                findNavController().navigate(R.id.hubDetailFragment, bundle)
            }
        }

        view.findViewById<RecyclerView>(R.id.rv_users).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = usersAdapter
            isNestedScrollingEnabled = false
        }
        view.findViewById<RecyclerView>(R.id.rv_hubs).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = hubsAdapter
            isNestedScrollingEnabled = false
        }
        view.findViewById<RecyclerView>(R.id.rv_posts).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = postsAdapter
            isNestedScrollingEnabled = false
        }
        view.findViewById<RecyclerView>(R.id.rv_challenges).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = challengesAdapter
            isNestedScrollingEnabled = false
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.search(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        viewModel.users.observe(viewLifecycleOwner) { users ->
            usersAdapter.submitList(users)
            updateSectionVisibility()
        }
        viewModel.hubs.observe(viewLifecycleOwner) { hubs ->
            hubsAdapter.submitList(hubs)
            updateSectionVisibility()
        }
        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            postsAdapter.submitList(posts)
            updateSectionVisibility()
        }
        viewModel.challenges.observe(viewLifecycleOwner) { challenges ->
            challengesAdapter.submitList(challenges)
            updateSectionVisibility()
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            pbLoading.visibility = if (loading) View.VISIBLE else View.GONE
        }
        viewModel.error.observe(viewLifecycleOwner) { err ->
            err?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
        }

        etSearch.requestFocus()
    }

    private fun updateSectionVisibility() {
        val query = etSearch.text?.toString() ?: ""
        val hasUsers = !viewModel.users.value.isNullOrEmpty()
        val hasHubs = !viewModel.hubs.value.isNullOrEmpty()
        val hasPosts = !viewModel.posts.value.isNullOrEmpty()
        val hasChallenges = !viewModel.challenges.value.isNullOrEmpty()
        val hasAny = hasUsers || hasHubs || hasPosts || hasChallenges

        sectionPeople.visibility = when (currentTab) {
            TAB_ALL -> if (hasUsers) View.VISIBLE else View.GONE
            TAB_PEOPLE -> View.VISIBLE
            else -> View.GONE
        }
        sectionHubs.visibility = when (currentTab) {
            TAB_ALL -> if (hasHubs) View.VISIBLE else View.GONE
            TAB_HUBS -> View.VISIBLE
            else -> View.GONE
        }
        sectionPosts.visibility = when (currentTab) {
            TAB_ALL -> if (hasPosts) View.VISIBLE else View.GONE
            TAB_POSTS -> View.VISIBLE
            else -> View.GONE
        }
        sectionChallenges.visibility = when (currentTab) {
            TAB_ALL -> if (hasChallenges) View.VISIBLE else View.GONE
            TAB_CHALLENGES -> View.VISIBLE
            else -> View.GONE
        }

        val showEmpty = query.isNotBlank() && when (currentTab) {
            TAB_ALL -> !hasAny
            TAB_PEOPLE -> !hasUsers
            TAB_HUBS -> !hasHubs
            TAB_POSTS -> !hasPosts
            TAB_CHALLENGES -> !hasChallenges
            else -> false
        }
        tvEmpty.visibility = if (showEmpty) View.VISIBLE else View.GONE
    }

    private fun navigateFromPost(post: PostSearchResult) {
        if (post.hubId != null) {
            val bundle = Bundle().apply { putInt("hubId", post.hubId) }
            findNavController().navigate(R.id.hubDetailFragment, bundle)
        } else {
            val bundle = Bundle().apply { putInt(UserProfileFragment.ARG_USER_ID, post.authorId) }
            findNavController().navigate(R.id.userProfileFragment, bundle)
        }
    }
}
