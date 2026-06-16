package com.example.hubrise.ui.hubs

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hubrise.R
import com.example.hubrise.data.model.Challenge
import com.example.hubrise.data.model.ProgressModel
import com.example.hubrise.ui.profile.UserProfileFragment

class ChallengeDetailFragment : Fragment() {

    private lateinit var viewModel: ChallengeDetailViewModel
    private lateinit var leaderboardAdapter: LeaderboardAdapter
    private lateinit var stageAdapter: StageProgressAdapter
    private lateinit var calendarAdapter: CalendarHeatmapAdapter

    private lateinit var tvTitle: TextView
    private lateinit var tvHubChip: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvEndsAt: TextView
    private lateinit var rvLeaderboard: RecyclerView
    private lateinit var tvLeaderboardEmpty: TextView
    private lateinit var btnDelete: Button
    private lateinit var pbLoading: ProgressBar

    // Count section
    private lateinit var sectionCount: View
    private lateinit var pbCountProgress: ProgressBar
    private lateinit var tvCountProgressLabel: TextView
    private lateinit var btnLogCount: Button

    // Stage section
    private lateinit var sectionStage: View
    private lateinit var rvStages: RecyclerView

    // Streak section
    private lateinit var sectionStreak: View
    private lateinit var tvCurrentStreak: TextView
    private lateinit var tvLongestStreak: TextView
    private lateinit var pbStreakProgress: ProgressBar
    private lateinit var tvStreakProgressLabel: TextView
    private lateinit var rvCalendar: RecyclerView
    private lateinit var btnCheckin: Button

    private var challengeId = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_challenge_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[ChallengeDetailViewModel::class.java]

        tvTitle = view.findViewById(R.id.tv_title)
        tvHubChip = view.findViewById(R.id.tv_hub_chip)
        tvDescription = view.findViewById(R.id.tv_description)
        tvEndsAt = view.findViewById(R.id.tv_ends_at)
        rvLeaderboard = view.findViewById(R.id.rv_leaderboard)
        tvLeaderboardEmpty = view.findViewById(R.id.tv_leaderboard_empty)
        btnDelete = view.findViewById(R.id.btn_delete_challenge)
        pbLoading = view.findViewById(R.id.pb_loading)

        sectionCount = view.findViewById(R.id.section_count)
        pbCountProgress = view.findViewById(R.id.pb_count_progress)
        tvCountProgressLabel = view.findViewById(R.id.tv_count_progress_label)
        btnLogCount = view.findViewById(R.id.btn_log_count)

        sectionStage = view.findViewById(R.id.section_stage)
        rvStages = view.findViewById(R.id.rv_stages)

        sectionStreak = view.findViewById(R.id.section_streak)
        tvCurrentStreak = view.findViewById(R.id.tv_current_streak)
        tvLongestStreak = view.findViewById(R.id.tv_longest_streak)
        pbStreakProgress = view.findViewById(R.id.pb_streak_progress)
        tvStreakProgressLabel = view.findViewById(R.id.tv_streak_progress_label)
        rvCalendar = view.findViewById(R.id.rv_calendar)
        btnCheckin = view.findViewById(R.id.btn_checkin)

        view.findViewById<View>(R.id.btn_back).setOnClickListener { findNavController().popBackStack() }

        leaderboardAdapter = LeaderboardAdapter { userId ->
            val bundle = Bundle().apply { putInt(UserProfileFragment.ARG_USER_ID, userId) }
            findNavController().navigate(R.id.userProfileFragment, bundle)
        }
        rvLeaderboard.layoutManager = LinearLayoutManager(requireContext())
        rvLeaderboard.adapter = leaderboardAdapter

        // Tapping a stage's action button doesn't complete it directly anymore —
        // it opens the post composer, since creating a post is now the only way to progress.
        stageAdapter = StageProgressAdapter { navigateToCreatePost() }
        rvStages.layoutManager = LinearLayoutManager(requireContext())
        rvStages.adapter = stageAdapter

        calendarAdapter = CalendarHeatmapAdapter()
        rvCalendar.layoutManager = GridLayoutManager(requireContext(), 7)
        rvCalendar.adapter = calendarAdapter

        challengeId = arguments?.getInt("challengeId") ?: return
        viewModel.load(challengeId)

        btnLogCount.setOnClickListener { navigateToCreatePost() }
        btnCheckin.setOnClickListener { navigateToCreatePost() }
        btnDelete.setOnClickListener { confirmDelete() }

        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        // Picks up progress made via a post created on the Create Post screen.
        if (challengeId != 0) viewModel.load(challengeId)
    }

    private fun navigateToCreatePost() {
        val challenge = viewModel.challenge.value ?: return
        val bundle = Bundle().apply {
            putInt("hubId", challenge.hub)
            putInt("challengeId", challenge.id)
        }
        findNavController().navigate(R.id.createPostFragment, bundle)
    }

    private fun confirmDelete() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Challenge?")
            .setMessage("This will permanently delete the challenge and everyone's progress.")
            .setPositiveButton("Delete") { _, _ -> viewModel.deleteChallenge(challengeId) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeViewModel() {
        viewModel.challenge.observe(viewLifecycleOwner) { challenge ->
            challenge ?: return@observe
            bindChallenge(challenge)
        }

        viewModel.leaderboard.observe(viewLifecycleOwner) { entries ->
            leaderboardAdapter.submitList(entries)
            tvLeaderboardEmpty.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
            rvLeaderboard.visibility = if (entries.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            pbLoading.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.deleted.observe(viewLifecycleOwner) { deleted ->
            if (deleted) {
                Toast.makeText(requireContext(), "Challenge deleted", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { err ->
            err?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show() }
        }
    }

    private fun bindChallenge(challenge: Challenge) {
        tvTitle.text = challenge.title
        tvHubChip.text = challenge.hubName ?: ""
        tvDescription.text = challenge.description.ifEmpty { "No description" }
        tvEndsAt.text = if (challenge.endsAt != null) "Ends: ${challenge.endsAt.take(10)}" else "No end date"
        btnDelete.visibility = if (challenge.canManage) View.VISIBLE else View.GONE

        sectionCount.visibility = View.GONE
        sectionStage.visibility = View.GONE
        sectionStreak.visibility = View.GONE

        when (challenge.progressModel) {
            ProgressModel.STAGE -> bindStageSection(challenge)
            ProgressModel.STREAK -> bindStreakSection(challenge)
            else -> bindCountSection(challenge)
        }
    }

    private fun bindCountSection(challenge: Challenge) {
        sectionCount.visibility = View.VISIBLE
        val config = challenge.countConfig
        val progress = challenge.myProgress
        val target = config?.targetCount ?: 0.0
        val current = progress?.currentCount ?: 0.0
        val percent = if (target > 0) ((current / target) * 100).toInt().coerceIn(0, 100) else 0

        pbCountProgress.max = 100
        pbCountProgress.progress = percent

        val unit = config?.unitLabel.orEmpty()
        val completed = progress?.isComplete == true
        tvCountProgressLabel.text = "${formatNumber(current)}/${formatNumber(target)} $unit".trim() +
            if (completed) "  ·  Completed! 🎉" else ""

        btnLogCount.text = "+ Add Progress Post"
    }

    private fun bindStageSection(challenge: Challenge) {
        sectionStage.visibility = View.VISIBLE
        stageAdapter.submitList(challenge.stages ?: emptyList())
    }

    private fun bindStreakSection(challenge: Challenge) {
        sectionStreak.visibility = View.VISIBLE
        val config = challenge.streakConfig
        val progress = challenge.myProgress
        val target = config?.targetDays ?: 0
        val totalCheckins = progress?.totalCheckins ?: 0
        val percent = if (target > 0) ((totalCheckins.toDouble() / target) * 100).toInt().coerceIn(0, 100) else 0

        tvCurrentStreak.text = "🔥 ${progress?.currentStreak ?: 0} day streak"
        tvLongestStreak.text = "Best: ${progress?.longestStreak ?: 0}"
        pbStreakProgress.max = 100
        pbStreakProgress.progress = percent

        val completed = progress?.isComplete == true
        tvStreakProgressLabel.text = "$totalCheckins/$target days" + if (completed) "  ·  Completed! 🎉" else ""

        calendarAdapter.submitCheckinDates(progress?.checkinCalendar ?: emptyList())

        val checkedInToday = progress?.lastCheckinDate == java.time.LocalDate.now().toString()
        btnCheckin.isEnabled = !checkedInToday
        btnCheckin.text = if (checkedInToday) "Checked in today ✓" else "+ Add Progress Post"
    }

    private fun formatNumber(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
}
