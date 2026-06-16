package com.example.hubrise.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

object ValidationHelper {

    // Email validation using regex
    fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}$"
        return Pattern.compile(emailRegex).matcher(email).matches()
    }

    // Password validation
    fun isValidPassword(password: String): String? {
        return when {
            password.length < 8 -> "Password must be at least 8 characters"
            !password.any { it.isUpperCase() } -> "Password must contain uppercase letter"
            !password.any { it.isLowerCase() } -> "Password must contain lowercase letter"
            !password.any { it.isDigit() } -> "Password must contain number"
            else -> null
        }
    }

    // Get password strength
    fun getPasswordStrength(password: String): PasswordStrength {
        var score = 0

        if (password.length >= 8) score++
        if (password.length >= 12) score++
        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isLowerCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++

        return when (score) {
            0, 1 -> PasswordStrength.WEAK
            2, 3 -> PasswordStrength.FAIR
            4, 5 -> PasswordStrength.GOOD
            else -> PasswordStrength.STRONG
        }
    }

    // Username validation
    fun isValidUsername(username: String): String? {
        return when {
            username.length < 3 -> "Username must be at least 3 characters"
            username.length > 20 -> "Username must not exceed 20 characters"
            !username.matches(Regex("^[a-zA-Z0-9_]+$")) -> "Username can only contain letters, numbers, and underscores"
            username[0].isDigit() -> "Username cannot start with a number"
            else -> null
        }
    }

    // Full name validation
    fun isValidFullName(fullName: String): String? {
        return when {
            fullName.length < 2 -> "Name must be at least 2 characters"
            !fullName.matches(Regex("^[a-zA-Z\\s'-]+$")) -> "Name can only contain letters, spaces, hyphens, and apostrophes"
            else -> null
        }
    }

    // Date of birth validation
    fun isValidDateOfBirth(dateString: String): String? {
        return try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val dob = LocalDate.parse(dateString, formatter)
            val today = LocalDate.now()

            val age = today.year - dob.year - (if (today.monthValue > dob.monthValue || (today.monthValue == dob.monthValue && today.dayOfMonth >= dob.dayOfMonth)) 0 else 1)

            when {
                dob.isAfter(today) -> "Date of birth cannot be in the future"
                age < 13 -> "You must be at least 13 years old"
                else -> null
            }
        } catch (e: Exception) {
            "Invalid date format. Please use yyyy-MM-dd"
        }
    }

    // Phone number validation (basic)
    fun isValidPhoneNumber(phoneNumber: String): String? {
        return when {
            phoneNumber.isNotEmpty() && phoneNumber.replace(Regex("[^0-9+\\-\\s()]"), "").length < 10 -> {
                "Phone number must have at least 10 digits"
            }
            else -> null
        }
    }

    // Bio validation
    fun isValidBio(bio: String): String? {
        return when {
            bio.length > 160 -> "Bio must not exceed 160 characters"
            else -> null
        }
    }

    // Check if passwords match
    fun passwordsMatch(password: String, confirmPassword: String): Boolean {
        return password == confirmPassword
    }
}

enum class PasswordStrength {
    WEAK, FAIR, GOOD, STRONG
}
