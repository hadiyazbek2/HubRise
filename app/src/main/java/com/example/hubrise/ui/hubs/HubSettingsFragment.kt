package com.example.hubrise.ui.hubs

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
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

    private var hubId = 0
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
        val isPublic = arguments?.getBoolean("isPublic") ?: true
        val coverUrl = arguments?.getString("coverUrl") ?: ""

        etName = view.findViewById(R.id.et_name)
        etDescription = view.findViewById(R.id.et_description)
        swPublic = view.findViewById(R.id.sw_public)
        ivCoverPreview = view.findViewById(R.id.iv_cover_preview)
        pbSaving = view.findViewById(R.id.pb_saving)
        rvMembers = view.findViewById(R.id.rv_members)

        etName.setText(hubName)
        etDescription.setText(hubDescription)
        swPublic.isChecked = isPublic

        if (coverUrl.isNotEmpty()) {
            ivCoverPreview.load(RetrofitClient.absoluteUrl(coverUrl)) { crossfade(true) }
        }

        view.findViewById<View>(R.id.btn_back).setOnClickListener { findNavController().navigateUp() }

        view.findViewById<View>(R.id.fl_cover_picker).setOnClickListener {
            pickImage.launch("image/*")
        }

        view.findViewById<View>(R.id.btn_save).setOnClickListener { save() }

        view.findViewById<View>(R.id.btn_delete_hub).setOnClickListener { confirmDelete() }

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

        viewModel.loadMembers(hubId)

        observeViewModel()
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
        viewModel.members.observe(viewLifecycleOwner) { members ->
            membersAdapter.submitList(members)
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
                // Pop back twice: settings → hub detail → hubs list
                findNavController().popBackStack(R.id.hubsFragment, false)
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { err ->
            err?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show() }
        }
    }
}
