package com.example.hubrise.ui.create

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.hubrise.R
import com.example.hubrise.data.model.Challenge
import com.example.hubrise.data.model.Hub
import com.example.hubrise.data.model.ProgressModel
import com.example.hubrise.utils.MediaPickerHelper
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class CreatePostFragment : Fragment() {

    private lateinit var viewModel: CreatePostViewModel
    private lateinit var mediaPicker: MediaPickerHelper

    private lateinit var btnBack: ImageView
    private lateinit var btnPost: Button
    private lateinit var rowHubSelector: LinearLayout
    private lateinit var tvSelectedHub: TextView
    private lateinit var rowChallengeSelector: LinearLayout
    private lateinit var tvSelectedChallenge: TextView
    private lateinit var dividerChallenge: View
    private lateinit var rowAmount: TextInputLayout
    private lateinit var etAmount: TextInputEditText
    private lateinit var layoutNoHubs: LinearLayout
    private lateinit var etContent: TextInputEditText
    private lateinit var tvCharCount: TextView
    private lateinit var pbPosting: ProgressBar
    private lateinit var tvError: TextView
    private lateinit var layoutMediaPreview: LinearLayout
    private lateinit var ivMediaPreview: ImageView
    private lateinit var btnRemoveMedia: ImageView
    private lateinit var btnAddMedia: ImageView
    private var returnToCaller = false

    // Register launcher before onCreateView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaPicker = MediaPickerHelper(this) { uri, file ->
            viewModel.setMedia(uri, file)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_create_post, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[CreatePostViewModel::class.java]

        btnBack = view.findViewById(R.id.btn_back)
        btnPost = view.findViewById(R.id.btn_post)
        rowHubSelector = view.findViewById(R.id.row_hub_selector)
        tvSelectedHub = view.findViewById(R.id.tv_selected_hub)
        rowChallengeSelector = view.findViewById(R.id.row_challenge_selector)
        tvSelectedChallenge = view.findViewById(R.id.tv_selected_challenge)
        dividerChallenge = view.findViewById(R.id.divider_challenge)
        rowAmount = view.findViewById(R.id.row_amount)
        etAmount = view.findViewById(R.id.et_amount)
        layoutNoHubs = view.findViewById(R.id.layout_no_hubs)
        etContent = view.findViewById(R.id.et_content)
        tvCharCount = view.findViewById(R.id.tv_char_count)
        pbPosting = view.findViewById(R.id.pb_posting)
        tvError = view.findViewById(R.id.tv_error)
        layoutMediaPreview = view.findViewById(R.id.layout_media_preview)
        ivMediaPreview = view.findViewById(R.id.iv_media_preview)
        btnRemoveMedia = view.findViewById(R.id.btn_remove_media)
        btnAddMedia = view.findViewById(R.id.btn_add_media)

        btnBack.setOnClickListener { findNavController().popBackStack() }
        rowHubSelector.setOnClickListener { showHubPickerBottomSheet() }
        rowChallengeSelector.setOnClickListener { showChallengePickerBottomSheet() }

        btnAddMedia.setOnClickListener { showMediaPickerBottomSheet() }
        btnRemoveMedia.setOnClickListener { viewModel.clearMedia() }

        etContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val len = s?.length ?: 0
                tvCharCount.text = "$len / 500"
                updatePostButtonState(len)
            }
        })

        val hubIdArg = arguments?.getInt("hubId", -1)?.takeIf { it != -1 }
        val challengeIdArg = arguments?.getInt("challengeId", -1)?.takeIf { it != -1 }
        returnToCaller = hubIdArg != null
        if (hubIdArg != null) viewModel.preselect(hubIdArg, challengeIdArg)

        btnPost.setOnClickListener {
            val amount = etAmount.text?.toString()?.trim()?.toDoubleOrNull()
            viewModel.createPost(etContent.text?.toString() ?: "", amount)
        }

        observeViewModel()
    }

    private fun updatePostButtonState(contentLen: Int) {
        // Content is required for regular posts, but optional for challenge posts
        // (the backend auto-generates a description when left blank).
        val enabled = contentLen > 0 || viewModel.selectedChallenge.value != null
        btnPost.isEnabled = enabled
        btnPost.alpha = if (enabled) 1f else 0.5f
    }

    private fun showMediaPickerBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val sheetView = LayoutInflater.from(requireContext())
            .inflate(R.layout.bottom_sheet_media_picker, null)
        dialog.setContentView(sheetView)

        sheetView.findViewById<LinearLayout>(R.id.option_gallery).setOnClickListener {
            dialog.dismiss()
            mediaPicker.pickFromGallery()
        }
        sheetView.findViewById<LinearLayout>(R.id.option_camera).setOnClickListener {
            dialog.dismiss()
            mediaPicker.captureFromCamera()
        }
        dialog.show()
    }

    private fun showHubPickerBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val sheetView = LayoutInflater.from(requireContext())
            .inflate(R.layout.bottom_sheet_hub_picker, null)
        dialog.setContentView(sheetView)

        val sectionPersonal = sheetView.findViewById<View>(R.id.section_personal)
        val personalContainer = sheetView.findViewById<LinearLayout>(R.id.ll_personal_container)
        val sectionHubs = sheetView.findViewById<View>(R.id.section_hubs)
        val hubsContainer = sheetView.findViewById<LinearLayout>(R.id.ll_hubs_container)
        val tvNoHubs = sheetView.findViewById<TextView>(R.id.tv_no_hubs)
        val currentHub = viewModel.selectedHub.value

        val allHubs = viewModel.joinedHubs.value.orEmpty()
        val personalHubs = allHubs.filter { !it.isPublic && it.name == "Personal" }
        val regularHubs = allHubs.filter { it.isPublic || it.name != "Personal" }

        fun addRow(container: LinearLayout, hub: Hub) {
            val row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_hub_picker_row, container, false)
            val label = if (!hub.isPublic && hub.name == "Personal") "Personal Space" else hub.name
            row.findViewById<TextView>(R.id.tv_hub_initial).text =
                label.first().uppercaseChar().toString()
            row.findViewById<TextView>(R.id.tv_hub_name).text = label
            row.findViewById<TextView>(R.id.tv_hub_members).text =
                "${hub.membersCount} member${if (hub.membersCount != 1) "s" else ""}"
            val tvPrivacy = row.findViewById<TextView>(R.id.tv_hub_privacy)
            tvPrivacy.visibility = if (!hub.isPublic) View.VISIBLE else View.GONE
            row.findViewById<ImageView>(R.id.iv_selected_check).visibility =
                if (currentHub?.id == hub.id) View.VISIBLE else View.GONE
            row.setOnClickListener { viewModel.selectHub(hub); dialog.dismiss() }
            container.addView(row)
        }

        // Always show "Personal Post" (no hub) as first option
        sectionPersonal.visibility = View.VISIBLE
        val personalRow = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_hub_picker_row, personalContainer, false)
        personalRow.findViewById<TextView>(R.id.tv_hub_initial).text = "P"
        personalRow.findViewById<TextView>(R.id.tv_hub_name).text = "Personal Post"
        personalRow.findViewById<TextView>(R.id.tv_hub_members).text = "Only visible to you"
        personalRow.findViewById<TextView>(R.id.tv_hub_privacy).visibility = View.GONE
        personalRow.findViewById<ImageView>(R.id.iv_selected_check).visibility =
            if (currentHub == null) View.VISIBLE else View.GONE
        personalRow.setOnClickListener { viewModel.clearHub(); dialog.dismiss() }
        personalContainer.addView(personalRow)

        if (allHubs.isNotEmpty()) {
            sectionHubs.visibility = View.VISIBLE
            allHubs.forEach { addRow(hubsContainer, it) }
        } else {
            tvNoHubs.visibility = View.VISIBLE
        }

        dialog.show()
    }

    private fun showChallengePickerBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val sheetView = LayoutInflater.from(requireContext())
            .inflate(R.layout.bottom_sheet_challenge_picker, null)
        dialog.setContentView(sheetView)

        val rowNone = sheetView.findViewById<View>(R.id.row_none)
        val ivNoneCheck = sheetView.findViewById<ImageView>(R.id.iv_none_check)
        val challengesContainer = sheetView.findViewById<LinearLayout>(R.id.ll_challenges_container)
        val tvNoChallenges = sheetView.findViewById<TextView>(R.id.tv_no_challenges)
        val currentChallenge = viewModel.selectedChallenge.value
        val challenges = viewModel.availableChallenges.value.orEmpty()

        ivNoneCheck.visibility = if (currentChallenge == null) View.VISIBLE else View.GONE
        rowNone.setOnClickListener { viewModel.clearChallenge(); dialog.dismiss() }

        if (challenges.isEmpty()) {
            tvNoChallenges.visibility = View.VISIBLE
        } else {
            challenges.forEach { challenge ->
                val row = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_challenge_picker_row, challengesContainer, false)
                row.findViewById<TextView>(R.id.tv_challenge_title).text = challenge.title
                row.findViewById<TextView>(R.id.tv_challenge_summary).text =
                    "${challenge.summary} · ${challenge.percentComplete}%"
                row.findViewById<ImageView>(R.id.iv_selected_check).visibility =
                    if (currentChallenge?.id == challenge.id) View.VISIBLE else View.GONE
                row.setOnClickListener { viewModel.selectChallenge(challenge); dialog.dismiss() }
                challengesContainer.addView(row)
            }
        }

        dialog.show()
    }

    private fun observeViewModel() {
        layoutNoHubs.visibility = View.GONE
        rowHubSelector.visibility = View.VISIBLE

        viewModel.selectedHub.observe(viewLifecycleOwner) { hub ->
            tvSelectedHub.text = hub?.name ?: "Personal Post"
            updatePostButtonState(etContent.text?.length ?: 0)
        }

        viewModel.availableChallenges.observe(viewLifecycleOwner) { challenges ->
            val hasChallenges = challenges.isNotEmpty()
            rowChallengeSelector.visibility = if (hasChallenges) View.VISIBLE else View.GONE
            dividerChallenge.visibility = if (hasChallenges) View.VISIBLE else View.GONE
            if (!hasChallenges) viewModel.clearChallenge()
        }

        viewModel.selectedChallenge.observe(viewLifecycleOwner) { challenge ->
            tvSelectedChallenge.text = challenge?.title ?: "None — regular post"
            rowAmount.visibility = if (challenge?.progressModel == ProgressModel.COUNT) View.VISIBLE else View.GONE
            etContent.hint = if (challenge != null) {
                "Add a note about your progress (optional)"
            } else {
                "What's on your mind?"
            }
            updatePostButtonState(etContent.text?.length ?: 0)
        }

        viewModel.currentStage.observe(viewLifecycleOwner) {
            updatePostButtonState(etContent.text?.length ?: 0)
        }

        viewModel.selectedCountConfig.observe(viewLifecycleOwner) { config ->
            rowAmount.hint = if (config?.isCumulative == true) {
                "Your new total so far (optional)"
            } else {
                "Amount to add (optional, defaults to the challenge's step size)"
            }
        }

        viewModel.selectedMediaUri.observe(viewLifecycleOwner) { uri ->
            if (uri != null) {
                layoutMediaPreview.visibility = View.VISIBLE
                ivMediaPreview.load(uri) { crossfade(true) }
            } else {
                layoutMediaPreview.visibility = View.GONE
            }
        }

        viewModel.isPosting.observe(viewLifecycleOwner) { posting ->
            pbPosting.visibility = if (posting) View.VISIBLE else View.GONE
            if (posting) {
                btnPost.isEnabled = false
            } else {
                updatePostButtonState(etContent.text?.length ?: 0)
            }
        }

        viewModel.postSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                if (returnToCaller) findNavController().popBackStack()
                else findNavController().popBackStack(R.id.homeFragment, false)
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { err ->
            if (err != null) {
                tvError.text = err
                tvError.visibility = View.VISIBLE
            } else {
                tvError.visibility = View.GONE
            }
        }
    }
}
