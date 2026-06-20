package com.example.hubrise.ui.hubs

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.hubrise.R
import com.example.hubrise.data.api.RetrofitClient
import com.google.android.material.textfield.TextInputEditText
import java.io.File

class HubSettingsFragment : Fragment() {

    private lateinit var viewModel: HubSettingsViewModel

    private lateinit var etName: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var swPublic: SwitchCompat
    private lateinit var ivCoverPreview: ImageView
    private lateinit var pbSaving: ProgressBar
    private lateinit var rvMembers: RecyclerView
    private lateinit var membersAdapter: HubMembersAdapter
    private lateinit var tvCompletionRequestsCount: TextView
    private lateinit var layoutCategoryPicker: LinearLayout
    private lateinit var tvCategoryValue: TextView
    private lateinit var layoutInviteCode: LinearLayout
    private lateinit var tvInviteCode: TextView
    private lateinit var btnCopyCode: TextView
    private lateinit var btnResetCode: TextView

    private var hubId = 0
    private var isPublic = true
    private var selectedCoverUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@registerForActivityResult
        selectedCoverUri = uri
        ivCoverPreview.load(uri) { crossfade(true) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_hub_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[HubSettingsViewModel::class.java]

        hubId = arguments?.getInt("hubId") ?: return
        val hubName = arguments?.getString("hubName") ?: ""
        val hubDescription = arguments?.getString("hubDescription") ?: ""
        isPublic = arguments?.getBoolean("isPublic") ?: true
        val coverUrl = arguments?.getString("coverUrl") ?: ""
        val categoryName = arguments?.getString("categoryName") ?: ""
        val initialInviteCode = arguments?.getString("inviteCode")

        etName = view.findViewById(R.id.et_name)
        etDescription = view.findViewById(R.id.et_description)
        swPublic = view.findViewById(R.id.sw_public)
        ivCoverPreview = view.findViewById(R.id.iv_cover_preview)
        pbSaving = view.findViewById(R.id.pb_saving)
        rvMembers = view.findViewById(R.id.rv_members)
        tvCompletionRequestsCount = view.findViewById(R.id.tv_completion_requests_count)
        layoutCategoryPicker = view.findViewById(R.id.layout_category_picker)
        tvCategoryValue = view.findViewById(R.id.tv_category_value)
        layoutInviteCode = view.findViewById(R.id.layout_invite_code)
        tvInviteCode = view.findViewById(R.id.tv_invite_code)
        btnCopyCode = view.findViewById(R.id.btn_copy_code)
        btnResetCode = view.findViewById(R.id.btn_reset_code)

        etName.setText(hubName)
        etDescription.setText(hubDescription)
        swPublic.isChecked = isPublic

        if (coverUrl.isNotEmpty()) {
            ivCoverPreview.load(RetrofitClient.absoluteUrl(coverUrl)) { crossfade(true) }
        }

        view.findViewById<View>(R.id.btn_back).setOnClickListener { findNavController().navigateUp() }
        view.findViewById<View>(R.id.fl_cover_picker).setOnClickListener { pickImage.launch("image/*") }
        view.findViewById<View>(R.id.btn_save).setOnClickListener { save() }
        view.findViewById<View>(R.id.btn_delete_hub).setOnClickListener { confirmDelete() }
        view.findViewById<View>(R.id.row_completion_requests).setOnClickListener {
            val bundle = Bundle().apply { putInt("hubId", hubId) }
            findNavController().navigate(R.id.completionRequestsFragment, bundle)
        }

        layoutCategoryPicker.setOnClickListener { showCategoryPicker() }

        // Invite code section — show only for private hubs
        if (!isPublic && initialInviteCode != null) {
            viewModel.setInviteCode(initialInviteCode)
        }
        layoutInviteCode.visibility = if (!isPublic) View.VISIBLE else View.GONE

        swPublic.setOnCheckedChangeListener { _, checked ->
            layoutInviteCode.visibility = if (!checked) View.VISIBLE else View.GONE
        }

        btnCopyCode.setOnClickListener {
            val code = tvInviteCode.text.toString()
            if (code.isNotBlank() && code != "--------") {
                val clip = ClipData.newPlainText("HubRise invite code", code)
                (requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                Toast.makeText(requireContext(), "Code copied!", Toast.LENGTH_SHORT).show()
            }
        }

        btnResetCode.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Reset invite code?")
                .setMessage("The old code will stop working immediately. Anyone who hasn't joined yet will need the new code.")
                .setPositiveButton("Reset") { _, _ -> viewModel.resetInviteCode(hubId) }
                .setNegativeButton("Cancel", null)
                .show()
        }

        viewModel.loadPendingCompletionRequestsCount(hubId)
        viewModel.loadMembers(hubId)

        membersAdapter = HubMembersAdapter(
            currentUserId = -1,
            isCreator = true,
            onRemoveClick = { member ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Remove @${member.username}?")
                    .setMessage("This will remove them from the hub.")
                    .setPositiveButton("Remove") { _, _ -> viewModel.removeMember(hubId, member.userId) }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )
        rvMembers.layoutManager = LinearLayoutManager(requireContext())
        rvMembers.adapter = membersAdapter

        // Pre-select the current category once categories load
        viewModel.categories.observe(viewLifecycleOwner) { cats ->
            if (viewModel.selectedCategory.value == null && categoryName.isNotEmpty()) {
                val match = cats.firstOrNull { it.name == categoryName }
                if (match != null) viewModel.selectCategory(match)
            }
        }

        observeViewModel()
    }

    private fun showCategoryPicker() {
        val categories = viewModel.categories.value ?: emptyList()
        if (categories.isEmpty()) {
            Toast.makeText(requireContext(), "Categories still loading…", Toast.LENGTH_SHORT).show()
            return
        }
        val names = categories.map { it.name }.toTypedArray()
        val currentIndex = viewModel.selectedCategory.value
            ?.let { sel -> categories.indexOfFirst { it.id == sel.id } } ?: -1
        AlertDialog.Builder(requireContext())
            .setTitle("Select a category")
            .setSingleChoiceItems(names, currentIndex) { dialog, which ->
                viewModel.selectCategory(categories[which])
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun save() {
        val name = etName.text?.toString()?.trim() ?: ""
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Hub name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        val description = etDescription.text?.toString() ?: ""
        val isPublic = swPublic.isChecked
        val coverFile = selectedCoverUri?.let { uri -> copyUriToFile(uri) }
        viewModel.saveSettings(hubId, name, description, isPublic, coverFile)
    }

    private fun confirmDelete() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Hub?")
            .setMessage("This will permanently delete the hub and all its posts. This cannot be undone.")
            .setPositiveButton("Delete") { _, _ -> viewModel.deleteHub(hubId) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun copyUriToFile(uri: Uri): File? = try {
        val stream = requireContext().contentResolver.openInputStream(uri) ?: return null
        val file = File(requireContext().cacheDir, "cover_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { stream.copyTo(it) }
        file
    } catch (e: Exception) { null }

    private fun observeViewModel() {
        viewModel.selectedCategory.observe(viewLifecycleOwner) { category ->
            if (category != null) {
                tvCategoryValue.text = category.name
                tvCategoryValue.setTextColor(resources.getColor(R.color.text_primary, null))
            } else {
                tvCategoryValue.text = "Select a category"
                tvCategoryValue.setTextColor(resources.getColor(R.color.text_secondary, null))
            }
        }

        viewModel.inviteCode.observe(viewLifecycleOwner) { code ->
            if (code != null) tvInviteCode.text = code
        }

        viewModel.members.observe(viewLifecycleOwner) { members ->
            membersAdapter.submitList(members)
        }

        viewModel.pendingCompletionRequestsCount.observe(viewLifecycleOwner) { count ->
            tvCompletionRequestsCount.visibility = if (count > 0) View.VISIBLE else View.GONE
            tvCompletionRequestsCount.text = count.toString()
        }

        viewModel.isSaving.observe(viewLifecycleOwner) { saving ->
            pbSaving.visibility = if (saving) View.VISIBLE else View.GONE
        }

        viewModel.saved.observe(viewLifecycleOwner) { saved ->
            if (saved) {
                Toast.makeText(requireContext(), "Hub updated", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }

        viewModel.deleted.observe(viewLifecycleOwner) { deleted ->
            if (deleted) {
                Toast.makeText(requireContext(), "Hub deleted", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack(R.id.hubsFragment, false)
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { err ->
            err?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show() }
        }
    }
}
