package com.example.hubrise.ui.auth.signup

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.hubrise.R
import com.example.hubrise.ui.auth.login.LoginActivity
import com.example.hubrise.utils.PasswordStrength
import com.google.android.material.textfield.TextInputEditText

class SignupStep1Activity : AppCompatActivity() {

    private lateinit var viewModel: SignupStep1ViewModel

    // Views
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var btnContinue: Button
    private lateinit var btnBack: ImageView
    private lateinit var cbTerms: CheckBox
    private lateinit var tvEmailError: TextView
    private lateinit var tvPasswordError: TextView
    private lateinit var tvConfirmPasswordError: TextView
    private lateinit var tvTermsError: TextView
    private lateinit var tvLogin: TextView
    private lateinit var pbCheckingEmail: ProgressBar
    private lateinit var ivEmailCheck: ImageView
    private lateinit var tvStrengthLabel: TextView
    private lateinit var tvTerms: TextView
    private lateinit var tvPrivacy: TextView

    // Password strength views
    private lateinit var vStrength1: View
    private lateinit var vStrength2: View
    private lateinit var vStrength3: View
    private lateinit var vStrength4: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup_step1_screen)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this).get(SignupStep1ViewModel::class.java)

        // Initialize views
        initializeViews()

        // Observe ViewModel
        observeViewModel()

        // Setup click listeners
        setupClickListeners()
    }

    private fun initializeViews() {
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        etConfirmPassword = findViewById(R.id.et_confirm_password)
        btnContinue = findViewById(R.id.btn_continue)
        btnBack = findViewById<ImageView>(R.id.btn_back)
        cbTerms = findViewById(R.id.cb_terms)
        tvEmailError = findViewById(R.id.tv_email_error)
        tvPasswordError = findViewById(R.id.tv_password_error)
        tvConfirmPasswordError = findViewById(R.id.tv_confirm_password_error)
        tvTermsError = findViewById(R.id.tv_terms_error)
        tvLogin = findViewById(R.id.tv_login)
        pbCheckingEmail = findViewById(R.id.pb_checking_email)
        ivEmailCheck = findViewById(R.id.iv_email_check)
        tvStrengthLabel = findViewById(R.id.tv_strength_label)
        tvTerms = findViewById(R.id.tv_terms)
        tvPrivacy = findViewById(R.id.tv_privacy)
        vStrength1 = findViewById(R.id.v_strength_1)
        vStrength2 = findViewById(R.id.v_strength_2)
        vStrength3 = findViewById(R.id.v_strength_3)
        vStrength4 = findViewById(R.id.v_strength_4)
    }

    private fun observeViewModel() {
        // Email validation
        viewModel.emailError.observe(this) { error ->
            tvEmailError.text = error
            tvEmailError.visibility = if (error.isNullOrEmpty()) View.GONE else View.VISIBLE
        }

        // Email checking state
        viewModel.isCheckingEmail.observe(this) { isChecking ->
            pbCheckingEmail.visibility = if (isChecking) View.VISIBLE else View.GONE
        }

        // Email availability
        viewModel.emailAvailable.observe(this) { available ->
            ivEmailCheck.visibility = if (!available) View.VISIBLE else View.GONE
        }

        // Password error
        viewModel.passwordError.observe(this) { error ->
            tvPasswordError.text = error
            tvPasswordError.visibility = if (error.isNullOrEmpty()) View.GONE else View.VISIBLE
        }

        // Password strength
        viewModel.passwordStrength.observe(this) { strength ->
            updatePasswordStrengthIndicator(strength)
        }

        // Confirm password error
        viewModel.confirmPasswordError.observe(this) { error ->
            tvConfirmPasswordError.text = error
            tvConfirmPasswordError.visibility = if (error.isNullOrEmpty()) View.GONE else View.VISIBLE
        }

        // Terms error
        viewModel.termsError.observe(this) { error ->
            tvTermsError.text = error
            tvTermsError.visibility = if (error.isNullOrEmpty()) View.GONE else View.VISIBLE
        }

        // Proceed to step 2
        viewModel.proceedToStep2.observe(this) {
            navigateToStep2()
        }
    }

    private fun setupClickListeners() {
        // Email input
        etEmail.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setEmail(s?.toString() ?: "")
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // Password input
        etPassword.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setPassword(s?.toString() ?: "")
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // Confirm password input
        etConfirmPassword.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setConfirmPassword(s?.toString() ?: "")
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // Terms checkbox
        cbTerms.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleTermsAgreement(isChecked)
        }

        // Continue button
        btnContinue.setOnClickListener {
            viewModel.proceedToStep2()
        }

        // Back button
        btnBack.setOnClickListener {
            finish()
        }

        // Login link
        tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Terms link
        tvTerms.setOnClickListener {
            // TODO: Open terms and conditions
        }

        // Privacy link
        tvPrivacy.setOnClickListener {
            // TODO: Open privacy policy
        }
    }

    private fun updatePasswordStrengthIndicator(strength: PasswordStrength) {
        val colors = when (strength) {
            PasswordStrength.WEAK -> {
                tvStrengthLabel.text = "Weak"
                tvStrengthLabel.setTextColor(android.graphics.Color.parseColor("#FF0000"))
                listOf(
                    android.graphics.Color.parseColor("#FF0000"),
                    android.graphics.Color.parseColor("#E0E0E0"),
                    android.graphics.Color.parseColor("#E0E0E0"),
                    android.graphics.Color.parseColor("#E0E0E0")
                )
            }

            PasswordStrength.FAIR -> {
                tvStrengthLabel.text = "Fair"
                tvStrengthLabel.setTextColor(android.graphics.Color.parseColor("#FFA500"))
                listOf(
                    android.graphics.Color.parseColor("#FFA500"),
                    android.graphics.Color.parseColor("#FFA500"),
                    android.graphics.Color.parseColor("#E0E0E0"),
                    android.graphics.Color.parseColor("#E0E0E0")
                )
            }

            PasswordStrength.GOOD -> {
                tvStrengthLabel.text = "Good"
                tvStrengthLabel.setTextColor(android.graphics.Color.parseColor("#FFD700"))
                listOf(
                    android.graphics.Color.parseColor("#FFD700"),
                    android.graphics.Color.parseColor("#FFD700"),
                    android.graphics.Color.parseColor("#FFD700"),
                    android.graphics.Color.parseColor("#E0E0E0")
                )
            }

            PasswordStrength.STRONG -> {
                tvStrengthLabel.text = "Strong"
                tvStrengthLabel.setTextColor(android.graphics.Color.parseColor("#00D084"))
                listOf(
                    android.graphics.Color.parseColor("#00D084"),
                    android.graphics.Color.parseColor("#00D084"),
                    android.graphics.Color.parseColor("#00D084"),
                    android.graphics.Color.parseColor("#00D084")
                )
            }
        }

        vStrength1.setBackgroundColor(colors[0])
        vStrength2.setBackgroundColor(colors[1])
        vStrength3.setBackgroundColor(colors[2])
        vStrength4.setBackgroundColor(colors[3])
    }

    private fun navigateToStep2() {
        val formData = viewModel.getFormData()
        val intent = Intent(this, SignupStep2Activity::class.java).apply {
            putExtra("email", formData.email)
            putExtra("password", formData.password)
        }
        startActivity(intent)
    }
}
