# Firebase OTP Troubleshooting Guide

## Issue: "This app is not authorized to use Firebase Authentication"

This error typically occurs when the SHA-1/SHA-256 fingerprints of your app's signing certificate are not registered in the Firebase Console.

## Why It Works for You But Not Your User

Even though you're using the same debug APK, the issue can occur if:
1. **Different debug keystores**: Each developer machine has its own debug keystore. If your user installed the app from a different machine or built it themselves, they might have a different SHA-1/SHA-256 fingerprint.
2. **Missing fingerprints in Firebase**: The user's device/keystore SHA-1/SHA-256 might not be registered in Firebase Console.

## Solution Steps

### Step 1: Get SHA-1 and SHA-256 Fingerprints

#### For Debug Build:
```bash
# On Windows (PowerShell)
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android

# On macOS/Linux
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

#### For Release Build:
```bash
keytool -list -v -keystore path/to/your/keystore.jks -alias your-key-alias
```

### Step 2: Add Fingerprints to Firebase Console

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project: **the-hotel-media**
3. Go to **Project Settings** (gear icon)
4. Scroll down to **Your apps** section
5. Select your Android app: **com.thehotelmedia.android**
6. Click **Add fingerprint**
7. Add both **SHA-1** and **SHA-256** fingerprints
8. Click **Save**

### Step 3: Download Updated google-services.json

After adding the fingerprints:
1. Download the updated `google-services.json` file
2. Replace the existing file in `app/google-services.json`
3. Rebuild the app

### Step 4: Verify Phone Authentication is Enabled

1. In Firebase Console, go to **Authentication** > **Sign-in method**
2. Ensure **Phone** is enabled
3. Check if there are any restrictions or quotas

## Alternative: Get Fingerprints from User's Device

If you can't get the user's keystore, you can:

1. Ask the user to run this command on their device/computer:
   ```bash
   keytool -list -v -keystore path/to/keystore -alias alias-name
   ```

2. Or, if they installed via Android Studio, check:
   - Windows: `%USERPROFILE%\.android\debug.keystore`
   - macOS/Linux: `~/.android/debug.keystore`

## Debugging with Logs

The updated code now logs detailed error information. Check Logcat with filter:
```
tag:OtpDialogManager
```

This will show:
- Exact error messages
- Exception types
- Phone numbers being verified
- Verification status

## Common Error Messages

| Error Message | Solution |
|--------------|----------|
| "not authorized" | Add SHA-1/SHA-256 to Firebase Console |
| "quota exceeded" | Check Firebase quotas or wait |
| "invalid phone number" | Verify phone number format |
| "too many requests" | Wait before retrying |

## Testing

After adding fingerprints:
1. Wait a few minutes for Firebase to update
2. Uninstall and reinstall the app
3. Try OTP verification again
4. Check Logcat for detailed error logs

## Notes

- It can take 5-10 minutes for Firebase to recognize new fingerprints
- Always add both SHA-1 and SHA-256 fingerprints
- For production, ensure release keystore fingerprints are added
- Debug and release builds use different keystores


