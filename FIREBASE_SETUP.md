# Firebase Setup Guide for Fossia

This guide walks you through setting up Firebase for Fossia.

## Step 1: Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Click **"Add project"**
3. Enter project name: **"Fossia"**
4. (Optional) Enable Google Analytics
5. Click **"Create project"**

---

## Step 2: Add Android App

1. In the Firebase Console, click the **Android icon** to add an Android app
2. Enter the following details:
   - **Android package name**: `com.jksalcedo.fossia`
   - **App nickname** (optional): "Fossia Android"
   - **Debug signing certificate SHA-1** (optional for now)

3. Click **"Register app"**

4. **Download `google-services.json`**
   - Click the download button
   - Save the file

5. **Replace the placeholder file**:
   ```bash
   # From your project root
   cp ~/Downloads/google-services.json app/google-services.json
   ```

---

## Step 3: Enable Firestore Database

1. In Firebase Console sidebar, click **"Firestore Database"**
2. Click **"Create database"**
3. Select **"Start in test mode"** (for development)
   - ⚠️ **Warning**: Test mode allows public read/write. Secure before production!
4. Choose a Cloud Firestore location (e.g., `us-central`)
5. Click **"Enable"**

---

## Step 4: Create Firestore Collections

### Collection 1: `proprietary_targets`

Click **"Start collection"** and create documents:

#### Document ID: `com_whatsapp`
```json
{
  "name": "WhatsApp",
  "package_name": "com.whatsapp",
  "category": "Communication",
  "icon": "",
  "alternatives": ["org_thoughtcrime_securesms", "org_telegram_messenger"]
}
```

#### Document ID: `com_facebook_katana`
```json
{
  "name": "Facebook",
  "package_name": "com.facebook.katana",
  "category": "Social",
  "icon": "",
  "alternatives": ["org_joinmastodon_android"]
}
```

#### Document ID: `com_google_android_youtube`
```json
{
  "name": "YouTube",
  "package_name": "com.google.android.youtube",
  "category": "Video",
  "icon": "",
  "alternatives": ["org_schabi_newpipe"]
}
```

---

### Collection 2: `foss_solutions`

Create a new collection named `foss_solutions`:

#### Document ID: `org_thoughtcrime_securesms`
```json
{
  "name": "Signal",
  "package_name": "org.thoughtcrime.securesms",
  "license": "GPLv3",
  "repo_url": "https://github.com/signalapp/Signal-Android",
  "fdroid_id": "org.thoughtcrime.securesms",
  "icon_url": "",
  "description": "Private messenger with end-to-end encryption. Signal is a cross-platform encrypted messaging service.",
  "votes": {
    "privacy": 450,
    "usability": 300
  }
}
```

#### Document ID: `org_telegram_messenger`
```json
{
  "name": "Telegram FOSS",
  "package_name": "org.telegram.messenger",
  "license": "GPLv2",
  "repo_url": "https://github.com/Telegram-FOSS-Team/Telegram-FOSS",
  "fdroid_id": "org.telegram.messenger",
  "icon_url": "",
  "description": "Fast and secure messaging app. This is the FOSS version without proprietary dependencies.",
  "votes": {
    "privacy": 380,
    "usability": 420
  }
}
```

#### Document ID: `org_joinmastodon_android`
```json
{
  "name": "Mastodon",
  "package_name": "org.joinmastodon.android",
  "license": "GPLv3",
  "repo_url": "https://github.com/mastodon/mastodon-android",
  "fdroid_id": "org.joinmastodon.android",
  "icon_url": "",
  "description": "Decentralized social network. Join the fediverse and connect with millions across independent servers.",
  "votes": {
    "privacy": 400,
    "usability": 280
  }
}
```

#### Document ID: `org_schabi_newpipe`
```json
{
  "name": "NewPipe",
  "package_name": "org.schabi.newpipe",
  "license": "GPLv3",
  "repo_url": "https://github.com/TeamNewPipe/NewPipe",
  "fdroid_id": "org.schabi.newpipe",
  "icon_url": "",
  "description": "Lightweight YouTube frontend. Watch videos, listen to music, no ads, no tracking.",
  "votes": {
    "privacy": 500,
    "usability": 350
  }
}
```

---

## Step 5: (Optional) Enable Firebase Authentication

For community features (voting, proposals):

1. In Firebase Console, click **"Authentication"**
2. Click **"Get started"**
3. Enable **"Anonymous"** sign-in (for MVP)
4. Enable **"Email/Password"** (for future user accounts)

---

## Step 6: Update Security Rules (Production)

Before deploying to production, update Firestore Security Rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Proprietary targets - public read, admin write
    match /proprietary_targets/{document} {
      allow read: if true;
      allow write: if request.auth != null && 
                      request.auth.token.admin == true;
    }
    
    // FOSS solutions - public read, reviewer write
    match /foss_solutions/{document} {
      allow read: if true;
      allow write: if request.auth != null && 
                      (request.auth.token.reviewer == true || 
                       request.auth.token.admin == true);
    }
    
    // Alternative proposals - authenticated write, public read
    match /alternative_proposals/{document} {
      allow read: if true;
      allow create: if request.auth != null;
      allow update, delete: if request.auth != null && 
                               (request.auth.uid == resource.data.user_id ||
                                request.auth.token.admin == true);
    }
  }
}
```

---

## Step 7: Verify Setup

1. **Build the project**:
   ```bash
   ./gradlew build
   ```

2. **Check for errors**:
   - Ensure `google-services.json` is in the correct location
   - Verify package name matches exactly

3. **Run the app**:
   ```bash
   ./gradlew installDebug
   ```

4. **Test Firestore connection**:
   - Open Fossia app
   - Tap refresh on dashboard
   - Check if proprietary apps are detected
   - Tap on a proprietary app to see alternatives

---

## Troubleshooting

### Error: "google-services.json not found"
- Ensure the file is at `app/google-services.json`
- Verify it's not in `.gitignore`
- Rebuild project: `./gradlew clean build`

### Error: "FirebaseApp initialization unsuccessful"
- Check package name matches in `google-services.json`
- Verify `google-services` plugin is applied in `build.gradle.kts`

### No alternatives showing
- Check Firestore rules allow public read
- Verify documents exist in `proprietary_targets` collection
- Check app package names match exactly (case-sensitive)

### Permission denied errors
- Grant `QUERY_ALL_PACKAGES` permission manually:
  - Settings → Apps → Special access → All files access → Enable for Fossia

---

## Next Steps

1. ✅ Firebase project created
2. ✅ `google-services.json` added
3. ✅ Firestore database enabled
4. ✅ Sample data added
5. ⬜ Test app on device
6. ⬜ Add more proprietary apps to database
7. ⬜ Invite community contributors

---

## Bulk Data Import (Advanced)

To add many apps at once, use Firebase Admin SDK:

```javascript
// Node.js script example
const admin = require('firebase-admin');
admin.initializeApp();
const db = admin.firestore();

const proprietaryApps = [
  { id: 'com_whatsapp', name: 'WhatsApp', ... },
  { id: 'com_spotify_music', name: 'Spotify', ... },
  // ... more apps
];

const batch = db.batch();
proprietaryApps.forEach(app => {
  const ref = db.collection('proprietary_targets').doc(app.id);
  batch.set(ref, app);
});

await batch.commit();
```

---

## Resources

- [Firebase Documentation](https://firebase.google.com/docs)
- [Firestore Data Model](https://firebase.google.com/docs/firestore/data-model)
- [Security Rules](https://firebase.google.com/docs/firestore/security/get-started)
