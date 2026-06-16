# HubRise - Step 1: Login & Signup Implementation Plan

## Overview
Build a clean, modern login and signup flow inspired by TikTok, Instagram, and ChatGPT. The flow will be split into multiple screens for optimal UX, include comprehensive validation, and integrate JWT token-based authentication with Django backend.

---

## 1. Architecture & Tech Stack

### Technology Choices
- **UI Framework**: XML Layouts (Traditional Android Views)
- **Architecture**: MVVM with ViewModel + Repository Pattern
- **Local Storage**: DataStore (for tokens, user preferences)
- **Authentication**: JWT Token-based with Django backend
- **State Management**: ViewModel + LiveData/StateFlow
- **Networking**: Retrofit + OkHttp
- **Image Loading**: Glide or Coil

### Folder Structure
```
app/src/main/java/com/hubrise/
├── ui/
│   ├── auth/
│   │   ├── login/
│   │   │   ├── LoginActivity.kt
│   │   │   ├── LoginViewModel.kt
│   │   │   └── login_screen.xml
│   │   ├── signup/
│   │   │   ├── SignupStep1Activity.kt
│   │   │   ├── SignupStep1ViewModel.kt
│   │   │   ├── signup_step1_screen.xml
│   │   │   ├── SignupStep2Activity.kt
│   │   │   ├── SignupStep2ViewModel.kt
│   │   │   ├── signup_step2_screen.xml
│   │   │   ├── ProfileSetupActivity.kt
│   │   │   ├── ProfileSetupViewModel.kt
│   │   │   └── profile_setup_screen.xml
│   │   └── components/
│   │       ├── DatePickerCustom.kt
│   │       └── validation_error_view.xml
├── data/
│   ├── repository/
│   │   ├── AuthRepository.kt
│   │   └── UserRepository.kt
│   ├── datasource/
│   │   ├── RemoteAuthDataSource.kt
│   │   └── LocalUserDataSource.kt
│   ├── api/
│   │   └── AuthApiService.kt
│   ├── model/
│   │   ├── LoginRequest.kt
│   │   ├── LoginResponse.kt
│   │   ├── SignupRequest.kt
│   │   ├── SignupResponse.kt
│   │   ├── User.kt
│   │   └── ApiResponse.kt
│   └── local/
│       └── UserPreferences.kt (DataStore wrapper)
└── utils/
    ├── ValidationHelper.kt
    ├── Constants.kt
    └── TokenManager.kt
```

---

## 2. Screen Breakdown

### Screen 1: Welcome/Login Screen
**Purpose**: Initial entry point - login or navigate to signup
**Fields**:
- Email input field (with validation)
- Password input field (masked)
- "Stay logged in" checkbox
- Login button
- Social login buttons (Google, Facebook, Apple)
- "Don't have an account? Sign up" link

**Validation**:
- Email format validation (RFC 5322 simplified)
- Password not empty
- Show real-time error messages

**Interactions**:
- On successful login → Navigate to Home Screen with JWT token stored
- On social login → Redirect to social provider's OAuth flow
- On "Sign up" click → Navigate to SignupStep1

---

### Screen 2: Signup Step 1 - Account Creation
**Purpose**: Collect email and password for new account
**Fields**:
- Email input field (real-time validation)
- Password input field (with strength indicator)
- Confirm password input field
- "Terms & Conditions" + "Privacy Policy" checkbox
- Create Account button
- Already have account? Login link

**Validation**:
- Email format validation + check if email exists (backend call)
- Password: minimum 8 characters, must contain uppercase, lowercase, number
- Password strength indicator (Weak/Fair/Good/Strong)
- Passwords match
- T&C checkbox must be checked

**Interactions**:
- Real-time email validation with debounce (check server availability)
- Password strength shown as progress bar
- On "Create Account" → Validate locally, call backend API
- On success → Navigate to SignupStep2

---

### Screen 3: Signup Step 2 - Profile Information
**Purpose**: Collect personal info and preferences
**Fields**:
- Full Name input
- Username input (unique, checked in real-time)
- Date of Birth (modern date picker - calendar style)
- Phone Number (optional)
- One or more interest/goal categories (checkboxes or chips)
- Continue button
- Back button

**Validation**:
- Full name: minimum 2 characters, no special characters
- Username: 3-20 characters, alphanumeric + underscore only, unique
- DOB: Must be 13+ years old (legal requirement), must be in the past
- Phone: Valid format (optional)
- At least one interest selected

**Special UI Elements**:
- Modern date picker (Material-style calendar)
- Real-time username availability check with debounce
- Category selection as material chips
- Progress indicator (Step 2 of 3)

**Interactions**:
- Back button → Return to SignupStep1 (preserve data)
- On "Continue" → Validate all fields, proceed to Step 3

---

### Screen 4: Profile Setup (Step 3)
**Purpose**: Profile picture and initial hub selection
**Fields**:
- Profile picture upload (camera + gallery options)
- Bio text (optional, 160 characters max)
- Skip or Browse Hubs button
- Complete Signup button

**Validation**:
- Profile picture: Optional, but if provided check file size (<5MB), format (JPG/PNG)
- Bio: Max 160 characters

**Interactions**:
- Camera/Gallery selection → Crop to square, optimize image size
- On "Complete Signup" → Create user account with all data, store JWT token
- Navigate to Home Screen or Hub Discovery Screen

---

## 3. API Endpoints Required (Django Backend)

### 1. Login
```
POST /api/auth/login/
Request: { email, password }
Response: { access_token, refresh_token, user: {...} }
Error: 401 Unauthorized, 400 Bad Request
```

### 2. Social Login (Google, Facebook, Apple)
```
POST /api/auth/social-login/
Request: { provider, id_token }
Response: { access_token, refresh_token, user: {...} }
```

### 3. Check Email Availability
```
GET /api/auth/check-email/?email={email}
Response: { available: boolean }
```

### 4. Check Username Availability
```
GET /api/auth/check-username/?username={username}
Response: { available: boolean }
```

### 5. Signup (Create Account)
```
POST /api/auth/signup/
Request: {
  email,
  password,
  full_name,
  username,
  date_of_birth,
  phone_number (optional),
  interests (array of IDs)
}
Response: { access_token, refresh_token, user: {...} }
Error: 400 Bad Request (validation errors), 409 Conflict (email/username exists)
```

### 6. Upload Profile Picture
```
POST /api/users/{user_id}/profile-picture/
Content-Type: multipart/form-data
Request: { image_file }
Response: { profile_picture_url }
```

---

## 4. Data Models

### LocalUser (DataStore)
```kotlin
data class LocalUser(
    val userId: String,
    val email: String,
    val username: String,
    val accessToken: String,
    val refreshToken: String,
    val profilePictureUrl: String?,
    val lastLoginTimestamp: Long
)
```

### SignupFormData (In-Memory during signup flow)
```kotlin
data class SignupFormData(
    var email: String = "",
    var password: String = "",
    var fullName: String = "",
    var username: String = "",
    var dateOfBirth: String = "", // "YYYY-MM-DD"
    var phoneNumber: String = "",
    var interestIds: List<String> = emptyList(),
    var agreedToTerms: Boolean = false
)
```

---

## 5. Validation Rules

### Email Validation
- Format: RFC 5322 simplified (basic regex)
- Backend check: Must be unique

### Password Validation
- Minimum 8 characters
- At least 1 uppercase letter (A-Z)
- At least 1 lowercase letter (a-z)
- At least 1 number (0-9)
- At least 1 special character (optional but recommended)
- Show strength indicator with real-time feedback

### Username Validation
- 3-20 characters
- Alphanumeric + underscore only
- Cannot start with numbers
- Backend check: Must be unique

### Date of Birth Validation
- Must be 13+ years old
- Must be in the past
- Format: YYYY-MM-DD

### Phone Number Validation
- If provided: Valid format for target country
- Minimum 10 digits (adjust based on region)

---

## 6. UI/UX Design Principles

### Color Scheme (TikTok/Instagram inspired)
- Primary Color: Vibrant gradient (suggest #FF006E to #FB5607 or similar)
- Background: Clean white or light gray
- Error: Red (#FF0000 or #DC143C)
- Success: Green (#00D084 or #28A745)

### Typography
- Headers: Bold, large (20-24sp)
- Body text: Regular, medium (14-16sp)
- Input fields: 16sp (prevents auto-zoom on iOS-like behavior)
- Error messages: Small, red (12sp)

### Spacing & Padding
- Screen padding: 16-20dp
- Input field margin: 16dp between fields
- Button height: 48-56dp
- Form container margin: 24dp top/bottom

### Interactive Elements
- Smooth transitions (200-300ms)
- Material ripple effects on buttons
- Soft shadows on cards/buttons
- Real-time feedback on form validation
- Loading indicators for API calls
- Toast/Snackbar for success/error messages

---

## 7. Error Handling Strategy

### Network Errors
- Network timeout → "Check your internet connection"
- Network unavailable → "No internet connection"
- Server error (5xx) → "Server error. Please try again later"

### Validation Errors
- Display inline under each field
- Color: Red, small text (12sp)
- Clear messaging: "Password must contain uppercase letter"

### Authentication Errors
- Invalid credentials → "Email or password is incorrect"
- Account locked → "Too many login attempts. Try again later"
- Email not verified → "Please verify your email before login"

### API Response Errors
- Handle all error codes from backend
- Log errors for debugging
- Show user-friendly messages

---

## 8. State Management Strategy

### ViewModel Pattern
Each screen has its own ViewModel that manages:
- Form data (MutableLiveData)
- Loading state (MutableLiveData<Boolean>)
- Error messages (SingleLiveEvent or Channel)
- Field validation states

### Example LiveData Structure
```kotlin
class LoginViewModel : ViewModel() {
    private val _email = MutableLiveData("")
    private val _password = MutableLiveData("")
    private val _isLoading = MutableLiveData(false)
    private val _loginError = SingleLiveEvent<String>()
    private val _loginSuccess = SingleLiveEvent<Unit>()

    // + validation methods
}
```

---

## 9. Security Considerations

- **Token Storage**: Store JWT tokens in encrypted SharedPreferences or DataStore (avoid plain text)
- **Password**: Use hashing on backend (bcrypt), never log passwords
- **HTTPS Only**: All API calls must use HTTPS
- **Token Refresh**: Implement refresh token flow when access token expires
- **Biometric Authentication**: Consider future enhancement for login
- **Input Sanitization**: Sanitize all user inputs to prevent injection attacks

---

## 10. Implementation Dependencies

### build.gradle.kts additions
```kotlin
// Networking
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("com.squareup.okhttp3:okhttp:4.11.0")

// Architecture Components
implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.1")
implementation("androidx.activity:activity-ktx:1.8.0")

// DataStore
implementation("androidx.datastore:datastore-preferences:1.0.0")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")

// Image Loading
implementation("io.coil-kt:coil:2.4.0")

// Validation
implementation("commons-validator:commons-validator:1.7")

// Material Components
implementation("com.google.android.material:material:1.10.0")
```

---

## 11. Implementation Steps

### Phase 1: Project Setup
1. Configure Retrofit + OkHttp
2. Setup DataStore for token management
3. Create API service interface
4. Setup Repository pattern with data sources

### Phase 2: Login Screen
1. Build UI layout (XML)
2. Create ViewModel with validation logic
3. Implement API integration
4. Handle token storage and app navigation

### Phase 3: Signup Flow
1. Build SignupStep1 UI + ViewModel
2. Build SignupStep2 UI + ViewModel
3. Build ProfileSetup UI + ViewModel
4. Implement data persistence across steps
5. API integration for all signup endpoints

### Phase 4: Validation & Error Handling
1. Implement comprehensive validation helpers
2. Setup error handling throughout flows
3. Add loading states and user feedback

### Phase 5: Testing & Refinement
1. Test with backend API
2. UI/UX refinement
3. Performance optimization

---

## 12. Questions for Backend Developer

Before implementation, confirm with Django backend:

1. **API Response Format**: What's the exact structure of `LoginResponse`? Include user object structure?
2. **Error Response Format**: What's the error response structure? How are validation errors returned?
3. **Image Upload**: What's the endpoint and size limits for profile pictures?
4. **Interests/Categories**: Is there an API to fetch available interest categories for signup?
5. **Social Login**: Will you implement OAuth endpoints, or should frontend use third-party SDKs?
6. **Token Refresh**: When should refresh token be used? Silent refresh or explicit?
7. **CORS**: Will CORS headers be configured properly for Android HTTP requests?

---

## 13. Success Criteria

- ✅ Login flow works end-to-end with backend
- ✅ Signup process keeps form data across screens
- ✅ All validation works as specified
- ✅ Modern DOB picker implements smoothly
- ✅ Error messages display clearly
- ✅ JWT tokens stored securely
- ✅ UI is clean, simple, and responsive
- ✅ Loading states show during API calls
- ✅ No crashes or unhandled errors

---

**Ready to start implementation once backend API is ready!**
