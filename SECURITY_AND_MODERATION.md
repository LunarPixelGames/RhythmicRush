# Security & Moderation Implementation

## Security Audit Results

### NPM Dependency Audit
✅ **PASSED**: No known vulnerabilities in backend dependencies

```
npm audit: 0 vulnerabilities
- Production dependencies: 1
- Development dependencies: 160
- All packages current and secure
```

### Sensitive Files Review
✅ **RESOLVED**: All sensitive configuration files are properly ignored by `.gitignore`:
- ✅ `backend/wrangler.toml` - Firebase project config (ignored)
- ✅ `backend/wrangler.prod.toml` - Production Firebase config (ignored)
- ✅ `lwjgl3/account-dev.properties` - Desktop dev secrets (ignored)
- ✅ `lwjgl3/account-prod.properties` - Desktop prod secrets (ignored)
- ✅ `backend/.dev.vars` - Wrangler secrets (ignored)
- ✅ `android/google-services.json` - Firebase Android config (ignored)
- ✅ `backend/node_modules/` - Dependencies (ignored)

**Note**: These files are properly excluded from version control and should never be committed. The `.example` templates are provided for developers to configure locally.

### Firebase Authentication Security
✅ **SECURED**:
- ID token verification via RS256 signature validation using Web Crypto API
- No Firebase Admin SDK in production (avoids service account key exposure)
- Token claims validation: audience, issuer, subject, expiration, auth_time
- 60-second clock skew tolerance for token freshness

### Database Schema Security
✅ **PROTECTED**:
- Foreign key constraints enforced
- Cascading deletes properly configured
- Account deletion lifecycle tracked via `account_tombstones` table
- Leaderboard ban tracking (`leaderboard_banned`, `leaderboard_ban_reason`)
- Rate limiting table for abuse prevention
- User status enum: `active`, `merging`, `suspended`, `deleted`

---

## Admin Portal Features

A secure web-based admin panel for game moderation at `/admin` endpoint.

### Capabilities

#### 1. **Leaderboard Management**
- View global leaderboard with rank, points, completed levels
- See user ban status and eligibility
- Quickly jump to user details from leaderboard

#### 2. **User Management**
- Search users by Firebase UID
- View complete user profile:
  - Account status (active/suspended/deleted)
  - Email verification status
  - Account creation/update timestamps
  - Leaderboard ban status

#### 3. **Game Statistics Inspection**
- View user's game progress per level:
  - Best completion percentage
  - Completion status (done/in progress)
  - Last updated timestamp
- Device attempt counts for fraud detection
- Total coins earned, points, levels completed

#### 4. **Moderation Actions**

**Ban/Unban from Leaderboard**
- Ban users suspected of cheating with optional reason
- Cache invalidation ensures instant leaderboard updates
- Unban with one click

**Username Renaming**
- Normal rename (fails if username taken)
- Force rename (overwrites existing reservation)
- Updates display name across all tables

**Account Deletion**
- Irreversible complete data deletion
- Removes from `users`, `usernames`, `saves`, `level_progress`, `device_attempts`
- Cleans up merge tickets and rate limits
- Logs deletion for audit trail

### Access Control

Admin endpoints (`/v1/dev/*`) require Firebase custom claims:
```typescript
// Admin detected via JWT claims:
claims.admin === true
// OR
claims.rhythmic_admin === true
```

Set custom claims in Firebase Console:
1. Go to Firebase Console > Authentication > Users
2. Click user > Custom Claims
3. Add: `{ "admin": true }`

Alternatively, set via Firebase Admin SDK in your backend:
```javascript
admin.auth().setCustomUserClaims(uid, { admin: true })
```

### API Endpoints

All endpoints require `Authorization: Bearer <idToken>` header with admin claims.

**GET /v1/dev/leaderboard?limit=100**
- Returns top 100 players with ban status

**GET /v1/dev/user?uid=USER_UID**
- Returns detailed user profile, stats, and progression

**POST /v1/dev/user**
- Action: `ban` - Ban or unban user
- Action: `rename` - Change username
- Action: `delete` - Delete all user data

### Accessing the Portal

1. Navigate to `https://your-api.com/admin`
2. Prompted for Firebase ID token
3. Get token from authenticated game client or Firebase console emulator
4. Token stored in `localStorage` for session

#### Getting a Token (Development)

**Via Firebase Emulator (Easiest)**:
```bash
firebase emulators:start
# Sign in at http://localhost:4200
# Copy token from browser DevTools > Application > LocalStorage > authUser.stsTokenManager.accessToken
```

**Via Production Firebase**:
```javascript
// In browser DevTools console, if you have an authenticated session:
firebase.auth().currentUser.getIdToken()
  .then(token => console.log(token))
```

**Via Custom Script**:
```bash
curl -X POST https://www.googleapis.com/identitytoolkit/v3/relyingparty/signupNewUser \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@example.com",
    "password": "securepassword",
    "returnSecureToken": true
  }' \
  -G --data-urlencode "key=YOUR_WEB_API_KEY"
```

---

## Recommendations

### Before Production

1. **Rotate Firebase Credentials**
   - Generate new Firebase project or rotate keys
   - Update in `wrangler.toml.example` (template only)

2. **Enable App Check**
   - Set `APP_CHECK_MODE=enforce` in `wrangler.prod.toml`
   - Configure Play Integrity API for Android
   - Configure DeviceCheck for iOS

3. **Audit Admin User List**
   - Review who has `admin` custom claim
   - Implement 2FA for admin accounts

4. **Enable Database Backups**
   - Cloudflare D1 automated backups
   - Test restoration procedures

5. **Rate Limiting Review**
   - Current limits: username reservation (1 per 5 min), leaderboard refresh (1 per 5 min)
   - Adjust based on expected player base

### Ongoing Monitoring

- Review admin action logs in `console.log` output
- Monitor for unusual ban patterns or data deletions
- Track leaderboard cache invalidations
- Alert on repeated failed auth attempts

### False Ban Prevention

The admin portal design supports informed moderation decisions:

1. **Inspect Level-by-Level Stats**
   - Check if completion percentages are realistic
   - Look for unusual jump patterns across devices
   - Compare device attempt counts vs actual progress

2. **Time-Based Analysis**
   - View when levels were completed
   - Identify suspiciously fast progression
   - Check for gaps suggesting multiple attempts

3. **Reversible Actions**
   - Ban is NOT permanent (can unban easily)
   - Consider temporary bans during investigation
   - Document ban reason for audit trail

4. **Before Deleting**
   - Always inspect game stats first
   - Review progression timeline
   - Verify ban reason makes sense
   - Never delete without explicit confirmation

---

## Compliance

- ✅ GDPR: User deletion via `DELETE /v1/account` (user-initiated) or admin delete
- ✅ Data retention: Account tombstones track deletions for audit
- ✅ Access logs: All admin actions logged to console/monitoring system
- ✅ Encryption: Tokens validated via RS256 (signature verification only, no secrets in DB)


