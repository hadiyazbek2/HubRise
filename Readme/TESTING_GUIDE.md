# HubRise Frontend - Testing Guide

## ✅ All Fixes Applied

### 1. Connection Error - FIXED ✅
- Updated BASE_URL from `127.0.0.1:8000` → `10.0.2.2:8000`
- `10.0.2.2` is the Android emulator's gateway to the host machine
- **Files changed**: `RetrofitClient.kt`

### 2. Signup Navigation - FIXED ✅
- Implemented `navigateToSignup()` in LoginActivity
- Now properly navigates from Login → SignupStep1
- **Files changed**: `LoginActivity.kt`

### 3. Theme Colors - FIXED ✅
- All pink colors (#FF006E) replaced with light blue (#0066FF)
- Button gradients updated to blue (#0066FF → #00B4FF)
- **Challenge**: App needs to be fully uninstalled and reinstalled to remove old cache
- **Files changed**: All `*_screen.xml` layouts, `button_gradient.xml`, `camera_button.xml`

---

## 🚀 How to Test (Important! Follow These Steps)

### Step 1: Start Django Backend

```bash
cd your-django-project
python manage.py runserver
```

**Output should show**:
```
Starting development server at http://127.0.0.1:8000/
Quit the server with CONTROL-C.
```

### Step 2: Clear Android Emulator Data (Important!)

Since the app was already installed with old code, you MUST clear it:

```bash
# List running emulators
emulator -list-avds

# Option A: Clear app data without reinstalling (faster)
adb uninstall com.example.hubrise

# Option B: Wipe emulator completely (if having persistent issues)
emulator -avd your_emulator_name -wipe-data
```

### Step 3: Build and Run

```bash
# From project root
./gradlew installDebug

# Or in Android Studio: Run → Run 'app'
```

### Step 4: Test Login Screen

1. **Check colors**: Button should be **light blue gradient** (not pink)
2. **Click "Don't have an account? Sign Up"**
3. **Signup link should navigate** to SignupStep1 screen

### Step 5: Test Signup Flow

1. **Step 1**: Fill email, password, confirm password, agree to terms
2. **Click Continue** - should go to Step 2
3. **Step 2**: Fill name, username, select DOB
4. **Click Continue** - should go to Step 3
5. **Step 3**: Complete signup

### Step 6: Test Backend Connection

When you complete signup:
- ✅ If successful: Token saved, navigate to next screen
- ❌ If error: Check Django logs for API errors
- ❌ If "Connection refused": Django backend not running
- ❌ If "Connection timeout": Check firewall/network settings

---

## 📝 Troubleshooting

### Colors Still Show Pink
**Solution**:
```bash
# Completely remove old app
adb uninstall com.example.hubrise

# Rebuild fresh
./gradlew clean build
./gradlew installDebug
```

### "Connection refused" Error
**Causes**:
- Django server not running
- Django running but not on `http://127.0.0.1:8000`
- Firewall blocking port 8000

**Solutions**:
```bash
# 1. Check Django is running
ps aux | grep "runserver"

# 2. Check port is open
netstat -an | grep 8000

# 3. Verify from emulator (ADB shell)
adb shell
curl http://10.0.2.2:8000/api/auth/login/
exit
```

### "Invalid email or password" Error
**This is expected!** If you're testing with non-existent credentials.

**To test properly**:
1. Create a test user in Django first:
   ```bash
   python manage.py shell
   >>> from django.contrib.auth.models import User
   >>> User.objects.create_user(username='test@example.com', email='test@example.com', password='TestPass123!')
   ```

2. Then test login with those credentials

### Signup Button Not Working
**Check**:
- All form fields filled correctly
- Terms & Conditions checkbox is checked
- Password matches confirmation password
- Check logcat for errors: `adb logcat | grep "E/"`

---

## 🔍 What URLs the App Hits

With `10.0.2.2:8000`:

```
POST   http://10.0.2.2:8000/api/auth/login/
POST   http://10.0.2.2:8000/api/auth/signup/
GET    http://10.0.2.2:8000/api/auth/check-email/?email=...
GET    http://10.0.2.2:8000/api/auth/check-username/?username=...
```

**Verify these work**:
```bash
# From your terminal (not emulator)
curl -X POST http://127.0.0.1:8000/api/auth/login/ \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"TestPass123!"}'
```

---

## 📦 Build Artifacts

**Debug APK**: `app/build/outputs/apk/debug/app-debug.apk`

Can be manually installed:
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## ✅ Testing Checklist

- [ ] Colors are light blue (not pink)
- [ ] Login screen loads
- [ ] "Sign Up" link navigates to SignupStep1
- [ ] Signup Step 1 validation works
- [ ] Can navigate through all 3 signup steps
- [ ] Django server accepts signup request
- [ ] User created in database
- [ ] JWT token saved to app storage
- [ ] Can login with created credentials

---

## 🆘 Getting Logs

**View app logs**:
```bash
adb logcat -s "HubRise" | grep -E "E|W|I"
```

**View network requests**:
```bash
adb logcat -s "System.out" "OkHttp"
```

**View all errors**:
```bash
adb logcat "*:E"
```

---

## 📱 Device vs Emulator

**If testing on physical device**:
- Replace `10.0.2.2` with your computer's IP address
- Edit `RetrofitClient.kt`:
  ```kotlin
  private const val BASE_URL = "http://192.168.x.x:8000/"  // Your IP
  ```
- Device must be on same WiFi network as backend

---

**Ready? Start with Step 1 above! 🚀**
