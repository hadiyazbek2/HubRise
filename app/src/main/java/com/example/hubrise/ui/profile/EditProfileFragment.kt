package com.example.hubrise.ui.profile

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.CircleCropTransformation
import com.example.hubrise.R
import com.example.hubrise.data.api.RetrofitClient
import com.google.android.material.textfield.TextInputEditText
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class EditProfileFragment : Fragment() {

    private lateinit var viewModel: EditProfileViewModel
    private var selectedImageUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@registerForActivityResult
        selectedImageUri = uri
        view?.findViewById<ImageView>(R.id.iv_avatar)?.load(uri) {
            transformations(CircleCropTransformation())
            crossfade(true)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_edit_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[EditProfileViewModel::class.java]

        val userId = arguments?.getInt("userId") ?: run {
            findNavController().popBackStack(); return
        }
        val initialFullName = arguments?.getString("fullName") ?: ""
        val initialBio = arguments?.getString("bio") ?: ""
        val avatarUrl = arguments?.getString("avatarUrl")

        val btnBack = view.findViewById<ImageView>(R.id.btn_back)
        val btnSave = view.findViewById<TextView>(R.id.btn_save)
        val ivAvatar = view.findViewById<ImageView>(R.id.iv_avatar)
        val etFullName = view.findViewById<TextInputEditText>(R.id.et_full_name)
        val etBio = view.findViewById<TextInputEditText>(R.id.et_bio)
        val pbSaving = view.findViewById<ProgressBar>(R.id.pb_saving)

        etFullName.setText(initialFullName)
        etBio.setText(initialBio)

        ivAvatar.load(RetrofitClient.absoluteUrl(avatarUrl)) {
            transformations(CircleCropTransformation())
            crossfade(true)
            fallback(R.drawable.ic_default_avatar)
            error(R.drawable.ic_default_avatar)
        }

        btnBack.setOnClickListener { findNavController().popBackStack() }

        ivAvatar.setOnClickListener { pickImage.launch("image/*") }
        view.findViewById<ImageView>(R.id.iv_camera_badge)?.setOnClickListener { pickImage.launch("image/*") }

        btnSave.setOnClickListener {
            val fullName = etFullName.text?.toString()?.trim() ?: ""
            val bio = etBio.text?.toString()?.trim() ?: ""
            val imagePart = selectedImageUri?.let { createImagePart(it) }
            viewModel.saveProfile(userId, fullName, bio, imagePart)
        }

        viewModel.isSaving.observe(viewLifecycleOwner) { saving ->
            pbSaving.visibility = if (saving) View.VISIBLE else View.GONE
            btnSave.isEnabled = !saving
        }

        viewModel.saved.observe(viewLifecycleOwner) { saved ->
            if (saved) findNavController().popBackStack()
        }

        viewModel.error.observe(viewLifecycleOwner) { err ->
            err?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
        }
    }

    private fun createImagePart(uri: Uri): MultipartBody.Part? {
        return try {
            val contentResolver = requireContext().contentResolver
            val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
            val bytes = contentResolver.openInputStream(uri)?.readBytes() ?: return null
            val requestBody = bytes.toRequestBody(mimeType.toMediaType())
            MultipartBody.Part.createFormData("image_file", "profile_picture.jpg", requestBody)
        } catch (e: Exception) {
            null
        }
    }
}
