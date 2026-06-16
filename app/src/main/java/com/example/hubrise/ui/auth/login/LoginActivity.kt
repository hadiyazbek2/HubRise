package com.example.hubrise.ui.auth.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.hubrise.R
import com.example.hubrise.data.api.RetrofitClient
import com.example.hubrise.data.local.UserPreferences
import com.google.android.material.textfield.TextInputEditText

class LoginActivity : AppCompatActivity() {

    private lateinit var viewModel: LoginViewModel
    private lateinit var userPreferences: UserPreferences

    // Views
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var pbLoading: ProgressBar
    private lateinit var tvEmailError: TextView
    private lateinit var tvPasswordError: TextView
    private lateinit var tvLoginError: TextView
    private lateinit var cbStayLoggedIn: CheckBox
    private lateinit var tvSignUp: TextView
    private lateinit var tvForgotPassword: TextView
    private lateinit var btnGoogle: Button
    private lateinit var btnFacebook: Button
    private lateinit var btnApple: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_screen)

        RetrofitClient.init(this)

        // Initialize UserPreferences
        userPreferences = UserPreferences(this)

        // Initialize ViewModel
        viewModel = ViewModelProvider(
            this,
            object : androidx.lifecycle.ViewModelProvider.Factory {
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return LoginViewModel(userPreferences) as T
                }
            }
        ).get(LoginViewModel::class.java)

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
        btnLogin = findViewById(R.id.btn_login)
        pbLoading = findViewById(R.id.pb_loading)
        tvEmailError = findViewById(R.id.tv_email_error)
        tvPasswordError = findViewById(R.id.tv_password_error)
        tvLoginError = findViewById(R.id.tv_login_error)
        cbStayLoggedIn = findViewById(R.id.cb_stay_logged_in)
        tvSignUp = findViewById(R.id.tv_sign_up)
        tvForgotPassword = findViewById(R.id.tv_forgot_password)
        btnGoogle = findViewById(R.id.btn_google)
        btnFacebook = findViewById(R.id.btn_facebook)
        btnApple = findViewById(R.id.btn_apple)
    }

    private fun observeViewModel() {
        // Email changes
        viewModel.email.observe(this) { email ->
            if (etEmail.text?.toString() != email) {
                etEmail.setText(email)
            }
        }

        // Password changes
        viewModel.password.observe(this) { password ->
            if (etPassword.text?.toString() != password) {
                etPassword.setText(password)
            }
        }

        // Loading state
        viewModel.isLoading.observe(this) { isLoading ->
            btnLogin.visibility = if (isLoading) View.GONE else View.VISIBLE
            pbLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnGoogle.isEnabled = !isLoading
            btnFacebook.isEnabled = !isLoading
            btnApple.isEnabled = !isLoading
        }

        // Email error
        viewModel.emailError.observe(this) { error ->
            if (error.isNullOrEmpty()) {
                tvEmailError.visibility = View.GONE
            } else {
                tvEmailError.text = error
                tvEmailError.visibility = View.VISIBLE
            }
        }

        // Password error
        viewModel.passwordError.observe(this) { error ->
            if (error.isNullOrEmpty()) {
                tvPasswordError.visibility = View.GONE
            } else {
                tvPasswordError.text = error
                tvPasswordError.visibility = View.VISIBLE
            }
        }

        // Login error
        viewModel.loginError.observe(this) { error ->
            if (error.isNullOrEmpty()) {
                tvLoginError.visibility = View.GONE
            } else {
                tvLoginError.text = error
                tvLoginError.visibility = View.VISIBLE
            }
        }

        // Login success
        viewModel.loginSuccess.observe(this) {
            // Navigate to home/main screen
            navigateToHome()
        }
    }

    private fun setupClickListeners() {
        // Email input
        etEmail.setOnTextChangedListener { text ->
            viewModel.setEmail(text.toString())
        }

        // Password input
        etPassword.setOnTextChangedListener { text ->
            viewModel.setPassword(text.toString())
        }

        // Login button
        btnLogin.setOnClickListener {
            viewModel.login()
        }

        // Sign up link
        tvSignUp.setOnClickListener {
            navigateToSignup()
        }

        // Forgot password
        tvForgotPassword.setOnClickListener {
            // TODO: Navigate to forgot password screen
        }

        // Social login buttons
        btnGoogle.setOnClickListener {
            // TODO: Implement Google login
        }

        btnFacebook.setOnClickListener {
            // TODO: Implement Facebook login
        }

        btnApple.setOnClickListener {
            // TODO: Implement Apple login
        }
    }

    private fun navigateToHome() {
        startActivity(Intent(this, com.example.hubrise.MainActivity::class.java))
        finishAffinity()
    }

    private fun navigateToSignup() {
        startActivity(Intent(this, com.example.hubrise.ui.auth.signup.SignupStep1Activity::class.java))
    }
}

// Extension function for TextInputEditText
private fun com.google.android.material.textfield.TextInputEditText.setOnTextChangedListener(callback: (CharSequence) -> Unit) {
    addTextChangedListener(object : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            s?.let { callback(it) }
        }
        override fun afterTextChanged(s: android.text.Editable?) {}
    })
}
