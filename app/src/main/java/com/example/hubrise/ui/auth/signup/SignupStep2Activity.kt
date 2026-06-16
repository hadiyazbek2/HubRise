package com.example.hubrise.ui.auth.signup

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.hubrise.R
import com.example.hubrise.ui.auth.components.ModernDatePickerDialog
import com.example.hubrise.ui.auth.components.parseDateString
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText

class SignupStep2Activity : AppCompatActivity() {

    private lateinit var viewModel: SignupStep2ViewModel

    // Views
    private lateinit var etFullName: TextInputEditText
    private lateinit var etUsername: TextInputEditText
    private lateinit var etDOB: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var btnContinue: Button
    private lateinit var btnBack: ImageView
    private lateinit var tvFullNameError: TextView
    private lateinit var tvUsernameError: TextView
    private lateinit var tvDOBError: TextView
    private lateinit var tvPhoneError: TextView
    private lateinit var pbCheckingUsername: ProgressBar
    private lateinit var cgInterests: ChipGroup

    // Data from Step 1
    private var email: String = ""
    private var password: String = ""

    // Sample interests (should come from API)
    private val sampleInterests = listOf(
        1 to "Weight Loss",
        2 to "Reading",
        3 to "Fitness",
        4 to "Coding",
        5 to "Learning",
        6 to "Meditation"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup_step2_screen)

        // Get data from previous activity
        getIntentData()

        // Initialize ViewModel
        viewModel = ViewModelProvider(this).get(SignupStep2ViewModel::class.java)

        // Initialize views
        initializeViews()

        // Setup interests chips
        setupInterestChips()

        // Observe ViewModel
        observeViewModel()

        // Setup click listeners
        setupClickListeners()
    }

    private fun getIntentData() {
        email = intent.getStringExtra("email") ?: ""
        password = intent.getStringExtra("password") ?: ""
    }

    private fun initializeViews() {
        etFullName = findViewById(R.id.et_full_name)
        etUsername = findViewById(R.id.et_username)
        etDOB = findViewById(R.id.et_dob)
        etPhone = findViewById(R.id.et_phone)
        btnContinue = findViewById(R.id.btn_continue)
        btnBack = findViewById<ImageView>(R.id.btn_back)
        tvFullNameError = findViewById(R.id.tv_full_name_error)
        tvUsernameError = findViewById(R.id.tv_username_error)
        tvDOBError = findViewById(R.id.tv_dob_error)
        tvPhoneError = findViewById(R.id.tv_phone_error)
        pbCheckingUsername = findViewById(R.id.pb_checking_username)
        cgInterests = findViewById(R.id.ll_interests)
    }

    private fun setupInterestChips() {
        sampleInterests.forEach { (id, name) ->
            val chip = Chip(this).apply {
                text = name
                tag = id
                isCheckable = true
                isCheckedIconVisible = true
                chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#E8F0FE")
                )
                setTextColor(android.graphics.Color.parseColor("#1A73E8"))
                setOnCheckedChangeListener { _, _ -> viewModel.toggleInterest(id) }
            }
            cgInterests.addView(chip)
        }
    }

    private fun observeViewModel() {
        // Full name error
        viewModel.fullNameError.observe(this) { error ->
            tvFullNameError.text = error
            tvFullNameError.visibility = if (error.isNullOrEmpty()) View.GONE else View.VISIBLE
        }

        // Username error
        viewModel.usernameError.observe(this) { error ->
            tvUsernameError.text = error
            tvUsernameError.visibility = if (error.isNullOrEmpty()) View.GONE else View.VISIBLE
        }

        // Username checking
        viewModel.isCheckingUsername.observe(this) { isChecking ->
            pbCheckingUsername.visibility = if (isChecking) View.VISIBLE else View.GONE
        }

        // DOB error
        viewModel.dateOfBirthError.observe(this) { error ->
            tvDOBError.text = error
            tvDOBError.visibility = if (error.isNullOrEmpty()) View.GONE else View.VISIBLE
        }

        // Phone error
        viewModel.phoneNumberError.observe(this) { error ->
            tvPhoneError.text = error
            tvPhoneError.visibility = if (error.isNullOrEmpty()) View.GONE else View.VISIBLE
        }

        // Proceed to step 3
        viewModel.proceedToStep3.observe(this) {
            navigateToStep3()
        }
    }

    private fun setupClickListeners() {
        // Full name input
        etFullName.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setFullName(s?.toString() ?: "")
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // Username input
        etUsername.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setUsername(s?.toString() ?: "")
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // DOB input - Open date picker on click
        etDOB.setOnClickListener {
            openDatePicker()
        }

        // Phone input
        etPhone.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setPhoneNumber(s?.toString() ?: "")
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // Continue button
        btnContinue.setOnClickListener {
            viewModel.proceedToStep3()
        }

        // Back button
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun openDatePicker() {
        val currentDOB = etDOB.text?.toString() ?: ""

        val (year, month, day) = if (currentDOB.isNotEmpty()) {
            parseDateString(currentDOB) ?: Triple(2000, 0, 1)
        } else {
            Triple(2000, 0, 1)
        }

        ModernDatePickerDialog(
            context = this,
            initialYear = year,
            initialMonth = month,
            initialDay = day,
            onDateSelected = { selectedDate ->
                etDOB.setText(selectedDate)
                viewModel.setDateOfBirth(selectedDate)
            }
        )
    }

    private fun navigateToStep3() {
        val step1Data = SignupStep1ViewModel.Step1Data(email, password)
        val step2Data = viewModel.getFormData()

        val intent = Intent(this, SignupStep3ProfileSetupActivity::class.java).apply {
            putExtra("email", step1Data.email)
            putExtra("password", step1Data.password)
            putExtra("full_name", step2Data.fullName)
            putExtra("username", step2Data.username)
            putExtra("date_of_birth", step2Data.dateOfBirth)
            putExtra("phone_number", step2Data.phoneNumber)
            putExtra("interests", step2Data.interests.toIntArray())
        }
        startActivity(intent)
    }
}
