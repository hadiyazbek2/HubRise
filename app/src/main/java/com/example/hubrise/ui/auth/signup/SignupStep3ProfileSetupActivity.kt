package com.example.hubrise.ui.auth.signup

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.hubrise.R
import com.example.hubrise.data.local.UserPreferences
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.io.FileOutputStream

class SignupStep3ProfileSetupActivity : AppCompatActivity() {

    private lateinit var viewModel: SignupStep3ViewModel
    private lateinit var userPreferences: UserPreferences

    // Views
    private lateinit var ivProfilePicture: ImageView
    private lateinit var etBio: TextInputEditText
    private lateinit var btnPickPhoto: Button
    private lateinit var btnSkipPhoto: Button
    private lateinit var btnComplete: Button
    private lateinit var btnBack: ImageView
    private lateinit var tvError: TextView
    private lateinit var tvBioError: TextView
    private lateinit var pbLoading: ProgressBar

    // Data from previous steps
    private var email: String = ""
    private var password: String = ""
    private var fullName: String = ""
    private var username: String = ""
    private var dateOfBirth: String = ""
    private var phoneNumber: String = ""
    private var interests: List<Int> = emptyList()
    private var selectedImageUri: Uri? = null

    // Photo picker launcher
    private val pickPhotoLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedImageUri = it
                ivProfilePicture.setImageURI(it)
            }
        }

    // Camera launcher
    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val bitmap = result.data?.getParcelableExtra<Bitmap>("data")
                bitmap?.let {
                    ivProfilePicture.setImageBitmap(it)
                    selectedImageUri = saveBitmapToFile(it)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup_step3_screen)

        // Get data from previous activities
        getIntentData()

        // Initialize UserPreferences and ViewModel
        userPreferences = UserPreferences(this)
        viewModel = ViewModelProvider(
            this,
            object : androidx.lifecycle.ViewModelProvider.Factory {
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return SignupStep3ViewModel(userPreferences) as T
                }
            }
        ).get(SignupStep3ViewModel::class.java)

        // Initialize views
        initializeViews()

        // Observe ViewModel
        observeViewModel()

        // Setup click listeners
        setupClickListeners()
    }

    private fun getIntentData() {
        email = intent.getStringExtra("email") ?: ""
        password = intent.getStringExtra("password") ?: ""
        fullName = intent.getStringExtra("full_name") ?: ""
        username = intent.getStringExtra("username") ?: ""
        dateOfBirth = intent.getStringExtra("date_of_birth") ?: ""
        phoneNumber = intent.getStringExtra("phone_number") ?: ""
        interests = intent.getIntArrayExtra("interests")?.toList() ?: emptyList()
    }

    private fun initializeViews() {
        ivProfilePicture = findViewById(R.id.iv_profile_picture)
        etBio = findViewById(R.id.et_bio)
        btnPickPhoto = findViewById(R.id.btn_pick_photo)
        btnSkipPhoto = findViewById(R.id.btn_skip_photo)
        btnComplete = findViewById(R.id.btn_complete)
        btnBack = findViewById<ImageView>(R.id.btn_back)
        tvError = findViewById(R.id.tv_error)
        tvBioError = findViewById(R.id.tv_bio_error)
        pbLoading = findViewById(R.id.pb_loading)
    }

    private fun observeViewModel() {
        // Bio error
        viewModel.bioError.observe(this) { error ->
            tvBioError.text = error
            tvBioError.visibility = if (error.isNullOrEmpty()) View.GONE else View.VISIBLE
        }

        // Signup error
        viewModel.signupError.observe(this) { error ->
            tvError.text = error
            tvError.visibility = if (error.isNullOrEmpty()) View.GONE else View.VISIBLE
        }

        // Loading state
        viewModel.isLoading.observe(this) { isLoading ->
            btnComplete.visibility = if (isLoading) View.GONE else View.VISIBLE
            pbLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Signup success
        viewModel.signupSuccess.observe(this) {
            navigateToHome()
        }
    }

    private fun setupClickListeners() {
        // Bio input
        etBio.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setBio(s?.toString() ?: "")
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // Pick photo button
        btnPickPhoto.setOnClickListener {
            pickPhotoLauncher.launch("image/*")
        }

        // Skip photo button
        btnSkipPhoto.setOnClickListener {
            selectedImageUri = null
            performSignup()
        }

        // Complete button
        btnComplete.setOnClickListener {
            performSignup()
        }

        // Back button
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun performSignup() {
        viewModel.completeSignup(
            email = email,
            password = password,
            fullName = fullName,
            username = username,
            dateOfBirth = dateOfBirth,
            phoneNumber = phoneNumber,
            interests = interests
        )
    }

    private fun saveBitmapToFile(bitmap: Bitmap): Uri {
        val file = File(cacheDir, "profile_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return Uri.fromFile(file)
    }

    private fun navigateToHome() {
        startActivity(Intent(this, com.example.hubrise.MainActivity::class.java))
        finishAffinity()
    }
}
