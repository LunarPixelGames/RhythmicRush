# Firebase Admin Claims Setup Guide

## Quick Start (3 steps)

### Step 1: Install Python Dependencies

```bash
pip install firebase-admin
```

### Step 2: Download Firebase Service Account Key

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your **rhythmic-rush-dev** project
3. Click ⚙️ **Project Settings** (top-left, near project name)
4. Go to **Service Accounts** tab
5. Click **Generate New Private Key** button
6. A JSON file will download (usually named `rhythmic-rush-dev-firebase-adminsdk-*.json`)
7. **Rename it to `serviceAccountKey.json`** and save it **in the RhythmicRush root directory**

### Step 3: Run Commands

```bash
# Make a user an admin
python firebase_claims.py set_admin your-user@example.com

# Or use their Firebase UID
python firebase_claims.py set_admin "HxXvZ1a2b3c4d5e6f7g8h9"

# Check who has admin privileges
python firebase_claims.py list_admins

# Check a specific user's claims
python firebase_claims.py check your-user@example.com

# Remove admin from a user
python firebase_claims.py remove_admin your-user@example.com
```

---

## Detailed Instructions

### Finding Your Firebase Credentials

#### Option A: Via Firebase Console (Recommended)

1. Open [Firebase Console](https://console.firebase.google.com/)
2. Click on your **rhythmic-rush-dev** project
3. In the top-left corner, click the ⚙️ icon next to "Project Overview"
4. Select **Project Settings**
5. Click the **Service Accounts** tab
6. You should see "Firebase Admin SDK" section with a dropdown for language selection
7. Make sure the language is set to **Node.js** (or any, doesn't matter)
8. Click **Generate New Private Key**
9. A JSON file downloads automatically
10. Save it as `serviceAccountKey.json` in the **E:\GameDev\RhythmicRush** folder

#### Option B: Via gcloud CLI

```bash
# Install gcloud CLI if not already installed
# Then authenticate:
gcloud auth login

# Download the key:
gcloud iam service-accounts keys create serviceAccountKey.json \
  --iam-account=firebase-adminsdk-xxx@rhythmic-rush-dev.iam.gserviceaccount.com
```

### Finding User UIDs

If you don't know a user's Firebase UID:

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Go to **Authentication** tab
3. Find the user in the list
4. Click on them to open their detail panel
5. Copy the **UID** field (long alphanumeric string)

---

## Usage Examples

### Make someone an admin by email

```bash
python firebase_claims.py set_admin admin@yourcompany.com
```

Output:
```
✅ Connected to Firebase
✅ Found user by email: admin@yourcompany.com

✅ Successfully set admin claims for user:
   UID: HxXvZ1a2b3c4d5e6f7g8h9
   Email: admin@yourcompany.com
   Claims: {'admin': true, 'rhythmic_admin': true}

📝 Note: Changes take effect on user's next login
```

### List all admin users

```bash
python firebase_claims.py list_admins
```

Output:
```
📋 Fetching all users...

✅ Found 2 admin(s):

  • admin@yourcompany.com
    UID: HxXvZ1a2b3c4d5e6f7g8h9
    Email Verified: ✅
    Custom Claims: {"admin": true, "rhythmic_admin": true}

  • moderator@yourcompany.com
    UID: IyYwA2b3c4d5e6f7g8h9i0
    Email Verified: ✅
    Custom Claims: {"admin": true}
```

### Check a user's current claims

```bash
python firebase_claims.py check player@example.com
```

Output:
```
📋 User Details:
   UID: JzZxB3c4d5e6f7g8h9i0j1
   Email: player@example.com
   Email Verified: ✅
   Disabled: No
   Custom Claims: {}

   Admin Status: 👤 Regular User
```

### Remove admin from someone

```bash
python firebase_claims.py remove_admin former-admin@example.com
```

Output:
```
✅ Connected to Firebase
✅ Found user by email: former-admin@example.com

✅ Successfully removed admin claims for user:
   UID: JzZxB3c4d5e6f7g8h9i0j1
   Email: former-admin@example.com

📝 Note: Changes take effect on user's next login
```

---

## Accessing the Admin Portal

Once you've set admin claims:

1. The user needs to **log out and log back in** (to refresh their token)
2. Go to: `https://rhythmic-rush-api-dev.sameerthecoolguy2006.workers.dev/admin`
3. Click "Enter your Firebase ID token"
4. To get an ID token:
   - **In-game**: Check app logs after login (token will be printed)
   - **From browser**: Open DevTools > Application > LocalStorage > `firebase` > Copy `id_token`
   - **Via Firebase Emulator**: `firebase emulators:start` and use auth from emulator UI

---

## Troubleshooting

### "Service account key not found"

**Solution**: 
1. Download the key from Firebase Console (see Step 2 above)
2. Rename it to `serviceAccountKey.json` (exact name, case-sensitive on Linux/Mac)
3. Place it in `E:\GameDev\RhythmicRush\` directory
4. Verify with `ls serviceAccountKey.json` or check File Explorer

### "Failed to connect to Firebase"

**Possible causes**:
1. Service account key file is invalid or corrupted
   - Delete it and download a fresh one from Firebase Console
2. You're using the wrong Firebase project
   - Check your `firebase_claims.py` is using the right service account (rhythmic-rush-dev)
3. Firebase Admin SDK not installed
   - Run: `pip install firebase-admin --upgrade`

**Solution**:
```bash
# Verify the service account key is valid JSON
python -c "import json; json.load(open('serviceAccountKey.json'))"  # Should print nothing if valid

# Reinstall Firebase Admin SDK
pip install --force-reinstall firebase-admin
```

### "User not found"

**Solution**:
1. Make sure the email/UID is exactly correct (case-sensitive for UIDs)
2. The user exists in THIS Firebase project (rhythmic-rush-dev), not another one
3. Check Firebase Console > Authentication > Users to see list of users

### Token expiration when accessing admin portal

**Solution**: 
1. Tokens expire after 1 hour
2. Get a fresh token from the game app after re-authenticating
3. Or use Firebase Emulator which gives long-lived dev tokens

---

## Getting an ID Token for Portal Login

### Method 1: From Game Client Logs

After user logs in to the game:
1. Check game logs for a line like: `ID Token: eyJhbGc...`
2. Copy that token
3. Paste into admin portal login prompt

### Method 2: Browser DevTools (if using web auth)

1. Open DevTools (F12 / Right-click > Inspect)
2. Go to **Application** tab
3. Click **LocalStorage** (left sidebar)
4. Find entry with key starting with `firebase:` 
5. Look for a `auth*` entry
6. Inside, find `authUser` > `stsTokenManager` > `accessToken` value
7. Copy that token

### Method 3: Firebase Auth Emulator (Easiest for Dev)

```bash
# Start Firebase emulator
firebase emulators:start

# Open http://localhost:4200 in browser
# Sign in with a test user (can create new one)
# Copy token from DevTools as described in Method 2

# Update your game to use emulator:
# In Kotlin: set firebaseAuthEmulatorHost = "localhost:9099"
```

### Method 4: Get Token Programmatically (Python)

```python
from firebase_admin import auth

# You already have service account loaded
user = auth.get_user("user_uid_here")
# Note: Admin SDK doesn't generate tokens, only manages users
# Use Firebase client SDK instead for token generation
```

---

## Setting Admin Claims via Firebase Console UI (Alternative)

You can also set custom claims directly via Firebase Console:

1. Go to [Firebase Console](https://console.firebase.google.com/) > Your Project
2. Go to **Authentication** tab
3. Find the user, click on their email to open detail panel
4. Scroll to **Custom claims** section
5. Click pencil icon to edit
6. Paste this JSON:
   ```json
   {
     "admin": true,
     "rhythmic_admin": true
   }
   ```
7. Click **Save**
8. User needs to log out and back in for claims to take effect

---

## After Setup

1. ✅ Admin user logs out and logs back in (to refresh token with new claims)
2. ✅ User goes to `https://rhythmic-rush-api-dev.sameerthecoolguy2006.workers.dev/admin`
3. ✅ Gets prompted for Firebase ID token (see "Getting an ID Token" section above)
4. ✅ Pastes token and gains access to:
   - Global leaderboard with ban status
   - User profile lookup & stats
   - Ban/unban controls
   - Username management
   - Account deletion tools
   - Level-by-level progression inspection

---

## API Deployment Info

Your backend is now deployed at:
```
Base URL: https://rhythmic-rush-api-dev.sameerthecoolguy2006.workers.dev
Admin Portal: https://rhythmic-rush-api-dev.sameerthecoolguy2006.workers.dev/admin
API Endpoints: https://rhythmic-rush-api-dev.sameerthecoolguy2006.workers.dev/v1/dev/*
Health Check: https://rhythmic-rush-api-dev.sameerthecoolguy2006.workers.dev/health
```

All endpoints require Firebase ID token with `admin: true` custom claim (except `/health` and `/admin`).

---

## Questions?

Refer back to `SECURITY_AND_MODERATION.md` in the project root for more details on:
- How the admin system works
- API endpoint documentation
- Security architecture
- Preventing false bans
- Production deployment checklist

