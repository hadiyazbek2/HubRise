package com.example.hubrise.ui.hubs

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.hubrise.R
import com.example.hubrise.utils.MediaPickerHelper
import com.google.android.material.textfield.TextInputEditText

class CreateHubFragment : Fragment() {

    private lateinit var viewModel: CreateHubViewModel
    private lateinit var mediaPicker: MediaPickerHelper

    private lateinit var btnBack: ImageView
    private lateinit var btnCreate: Button
    private lateinit var etName: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var tvNameCount: TextView
    private lateinit var tvDescCount: TextView
    private lateinit var rgPrivacy: RadioGroup
    private lateinit var rbPublic: RadioButton
    private lateinit var tvError: TextView
    private lateinit var pbCreating: ProgressBar
    private lateinit var layoutCoverPicker: FrameLayout
    private lateinit var ivCoverPreview: ImageView
    private lateinit var layoutCoverPlaceholder: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaPicker = MediaPickerHelper(this) { uri, file ->
            viewModel.setCover(uri, file)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_create_hub, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[CreateHubViewModel::class.java]

        btnBack = view.findViewById(R.id.btn_back)
        btnCreate = view.findViewById(R.id.btn_create)
        etName = view.findViewById(R.id.et_name)
        etDescription = view.findViewById(R.id.et_description)
        tvNameCount = view.findViewById(R.id.tv_name_count)
        tvDescCount = view.findViewById(R.id.tv_desc_count)
        rgPrivacy = view.findViewById(R.id.rg_privacy)
        rbPublic = view.findViewById(R.id.rb_public)
        tvError = view.findViewById(R.id.tv_error)
        pbCreating = view.findViewById(R.id.pb_creating)
        layoutCoverPicker = view.findViewById(R.id.layout_cover_picker)
        ivCoverPreview = view.findViewById(R.id.iv_cover_preview)
        layoutCoverPlaceholder = view.findViewById(R.id.layout_cover_placeholder)

        btnBack.setOnClickListener { findNavController().popBackStack() }

        layoutCoverPicker.setOnClickListener { mediaPicker.pickFromGallery() }

        etName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val len = s?.length ?: 0
                tvNameCount.text = "$len / 50"
                btnCreate.isEnabled = len > 0
                btnCreate.alpha = if (len > 0) 1f else 0.5f
            }
        })

        etDescription.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tvDescCount.text = "${s?.length ?: 0} / 300"
            }
        })

        btnCreate.setOnClickListener {
            val name = etName.text?.toString() ?: ""
            val desc = etDescription.text?.toString() ?: ""
            val isPublic = rbPublic.isChecked
            viewModel.createHub(name, desc, isPublic)
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.coverUri.observe(viewLifecycleOwner) { uri ->
            if (uri != null) {
                ivCoverPreview.visibility = View.VISIBLE
                layoutCoverPlaceholder.visibility = View.GONE
                ivCoverPreview.load(uri) { crossfade(true) }
            } else {
                ivCoverPreview.visibility = View.GONE
                layoutCoverPlaceholder.visibility = View.VISIBLE
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            pbCreating.visibility = if (loading) View.VISIBLE else View.GONE
            btnCreate.isEnabled = !loading
            btnCreate.alpha = if (loading) 0.5f else 1f
        }

        viewModel.error.observe(viewLifecycleOwner) { err ->
            if (err != null) {
                tvError.text = err
                tvError.visibility = View.VISIBLE
            } else {
                tvError.visibility = View.GONE
            }
        }

        viewModel.createdHubId.observe(viewLifecycleOwner) { hubId ->
            hubId ?: return@observe
            val bundle = Bundle().apply { putInt("hubId", hubId) }
            findNavController().navigate(R.id.hubDetailFragment, bundle)
        }
    }
}
