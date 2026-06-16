# HubRise - Quick Start Testing Guide

## 🚀 Get Started in 5 Minutes

### Step 1: Uninstall Old App (30 seconds)
```bash
adb uninstall com.example.hubrise
```

### Step 2: Rebuild Fresh (30 seconds)
```bash
cd /home/hadi/AndroidStudioProjects/HubRise
./gradlew clean build
```

### Step 3: Install App (30 seconds)
```bash
./gradlew installDebug
```

### Step 4: Start Backend (in another terminal)
```bash
cd your-django-project
python manage.py runserver
```

Expected output:
```
Starting development server at http://127.0.0.1:8000/
```

### Step 5: Open App and Test!

---

## ✅ What Should Work Now

### 1. **No CLEARTEXT Error**
- ✅ App connects to 10.0.2.2:8000
- ✅ No "CLEARTEXT communication" error
- ❌ If error persists: Clear app data and reinstall

### 2. **Modern Button Design**
- ✅ Login button is **light blue gradient**
- ✅ Button has **ripple effect** when tapped
- ✅ Buttons look modern and professional
- ❌ If still pink: Uninstall and rebuild

### 3. **Signup Navigation**
- ✅ Click "Don't have an account? Sign Up"
- ✅ Navigates to Signup Step 1
- ❌ If doesn't navigate: Check logcat

### 4. **Clean Layout**
- ✅ DOB field doesn't overlap with interests
- ✅ All fields properly spaced
- ✅ Interest chips display cleanly
- ❌ If overlaps: Rebuild

---

## 🧪 Test Signup Flow

1. **Step 1 - Email & Password**
   ```
   Email: test@example.com
   Password: TestPass123! (8+ chars, uppercase, lowercase, number)
   Confirm: TestPass123!
   ✅ Agree to terms
   Click "Continue"
   ```

2. **Step 2 - Profile**
   ```
   Full Name: John Doe
   Username: johndoe123
   DOB: 1999-01-01 (click to open calendar)
   Phone: +1234567890 (optional)
   Select interests (optional)
   Click "Continue"
   ```

3. **Step 3 - Picture**
   ```
   Upload photo (optional)
   Write bio (optional)
   Click "Complete Signup"
   ```

---

## 🔧 Troubleshooting

### "CLEARTEXT communication to 10.0.2.2"
```bash
adb uninstall com.example.hubrise
./gradlew clean build installDebug
```

### Button still shows pink
```bash
# Clear everything and rebuild
./gradlew clean build
./gradlew installDebug
```

### "Connection refused"
```bash
# Check Django is running
ps aux | grep runserver

# If not running, start it:
cd your-django-project
python manage.py runserver
```

### DOB field overlapping
```bash
./gradlew clean build installDebug
```

### AuthApiService error
Already fixed! Just rebuild:
```bash
./gradlew clean build
```

---

## 📊 Expected Results

| Test | Before | After |
|------|--------|-------|
| Connect to backend | ❌ CLEARTEXT error | ✅ Works |
| Button design | ❌ Old, static | ✅ Modern, ripple |
| Signup navigation | ❌ Broken | ✅ Works |
| DOB field layout | ❌ Overlapping | ✅ Clean |
| AuthApiService | ❌ Import errors | ✅ Compiles |

---

## 💡 Pro Tips

- **Screen Capture**: `adb shell screencap -p > screenshot.png`
- **View Logs**: `adb logcat | grep "E"`
- **Restart Emulator**: Close and reopen Android Studio emulator
- **Reset Emulator**: `emulator -avd your_avd -wipe-data`

---

## ✨ Success Indicators

When everything is working:
- ✅ App opens without errors
- ✅ Login/Signup buttons are modern blue (not pink)
- ✅ Buttons have ripple effect
- ✅ No layout overlapping
- ✅ Connect to Django backend
- ✅ User created in database
- ✅ JWT token saved

---

**Ready to test? Follow the 5 steps above!** 🚀

Still having issues? Check documentation in `/Readme/` folder:
- `ALL_FIXES_COMPLETE.md` - Detailed explanation of all fixes
- `TESTING_GUIDE.md` - Comprehensive testing guide
- `BUGS_FIXED.md` - Bug fix details
