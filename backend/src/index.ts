import {
  deleteCloudAccount,
  DeleteAccountError,
  RecentAuthenticationRequiredError,
} from "./accounts/delete";
import { getProfile } from "./accounts/profile";
import { authenticate, FirebaseAuthError } from "./auth/firebase";
import { ensureUser, UserStateError } from "./auth/user";
import { getLeaderboard } from "./leaderboard/get";
import { fetchProgress } from "./progress/fetch";
import {
  ProgressConflictError,
  UpdateRequiredError,
  uploadProgress,
} from "./progress/upload";
import {
  corsPreflight,
  errorResponse,
  jsonResponse,
  readJson,
  requestId,
  RequestBodyError,
} from "./shared/http";
import { FeatureDisabledError, requireFeature } from "./shared/features";
import { RateLimitError } from "./shared/rateLimit";
import { ValidationError, isRecord, requireString } from "./shared/validation";
import type { AuthenticatedUser, Env } from "./types";
import { reserveUsername, UsernameError } from "./usernames/reserve";
import * as admin from "./admin";

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const id = requestId(request);

    try {
      if (request.method === "OPTIONS") return corsPreflight(env);

      const url = new URL(request.url);

      // Serve admin portal (no auth required for landing page)
      if (url.pathname === "/admin" || url.pathname === "/admin/") {
        requireMethod(request, "GET");
        const portal = await getAdminPortal();
        return new Response(portal, {
          headers: { "Content-Type": "text/html; charset=utf-8" },
        });
      }

      if (url.pathname === "/health") {
        requireMethod(request, "GET");
        return jsonResponse(env, id, 200, {
          service: "rhythmic-rush-api",
          environment: env.ENVIRONMENT,
          status: "ok",
        });
      }

      if (!url.pathname.startsWith("/v1/")) {
        return errorResponse(env, id, 404, "NOT_FOUND", "Endpoint not found.");
      }

      const user = await authenticate(request, env);
      await ensureUser(env, user);

      if (url.pathname === "/v1/profile") {
        requireMethod(request, "GET");
        return jsonResponse(env, id, 200, await getProfile(env, user));
      }

      if (url.pathname === "/v1/usernames/reserve") {
        requireMethod(request, "POST");
        requireIdempotencyKey(request);
        const body = await readJson<unknown>(request);
        return jsonResponse(env, id, 200, await reserveUsername(env, user, body));
      }

      if (url.pathname === "/v1/progress/fetch") {
        requireMethod(request, "POST");
        return jsonResponse(env, id, 200, await fetchProgress(env, user.uid));
      }

      if (url.pathname === "/v1/progress/upload") {
        requireMethod(request, "POST");
        requireFeature(env, "progressUploads", "Cloud save uploads are temporarily disabled.");
        const operationKey = requireIdempotencyKey(request);
        const body = await readJson<unknown>(request);
        return jsonResponse(
          env,
          id,
          200,
          await uploadProgress(env, user, body, operationKey),
        );
      }

      if (url.pathname === "/v1/leaderboard") {
        requireMethod(request, "GET");
        const forceRefresh = url.searchParams.get("refresh") === "true";
        if (forceRefresh) {
          requireFeature(
            env,
            "leaderboardRefresh",
            "Manual leaderboard refresh is temporarily disabled.",
          );
        }
        return jsonResponse(env, id, 200, await getLeaderboard(env, user.uid, forceRefresh));
      }

      if (url.pathname === "/v1/accounts/merge") {
        requireMethod(request, "POST");
        return featureNotReady(env, id, "Account merge");
      }

      if (url.pathname === "/v1/account") {
        requireMethod(request, "DELETE");
        requireFeature(env, "accountDeletion", "Account deletion is not available yet.");
        const body = await readJson<unknown>(request);
        return jsonResponse(env, id, 200, await deleteCloudAccount(env, user, body));
      }

      // Admin / developer portal endpoints
      if (url.pathname.startsWith("/v1/dev/")) {
        // require admin privileges
        try {
          await admin.requireAdmin(user);
        } catch (e) {
          return errorResponse(env, id, 403, "ADMIN_REQUIRED", "Admin privileges required.");
        }

        if (url.pathname === "/v1/dev/leaderboard") {
          requireMethod(request, "GET");
          const limitParam = url.searchParams.get("limit");
          const limit = limitParam ? Number(limitParam) : 100;
          return jsonResponse(env, id, 200, await admin.getDevLeaderboard(env, limit));
        }

        if (url.pathname === "/v1/dev/user") {
          // GET /v1/dev/user?uid=... to fetch details
          if (request.method === "GET") {
            const target = url.searchParams.get("uid");
            if (!target) throw new ValidationError("Provide uid query parameter.");
            return jsonResponse(env, id, 200, await admin.getUserDetails(env, target));
          }

          if (request.method === "POST") {
            // use body.action to decide: ban/rename/delete
            const body = await readJson<unknown>(request);
            if (!isRecord(body)) throw new ValidationError("Invalid request body.");
            const action = (body as any).action;
            if (action === "ban") {
              const target = requireString((body as any).uid, "uid", 1, 256);
              const ban = Boolean((body as any).ban);
              const reason = (body as any).reason;
              return jsonResponse(env, id, 200, await admin.banUser(env, target, ban, reason));
            }
            if (action === "rename") {
              const target = requireString((body as any).uid, "uid", 1, 256);
              const newName = requireString((body as any).newUsername, "newUsername", 1, 32);
              const force = Boolean((body as any).force);
              return jsonResponse(env, id, 200, await admin.renameUser(env, target, newName, force));
            }
            if (action === "delete") {
              const target = requireString((body as any).uid, "uid", 1, 256);
              return jsonResponse(env, id, 200, await admin.adminDeleteUser(env, target));
            }
            throw new ValidationError("Unknown admin action.");
          }
          throw new MethodError("This endpoint requires GET or POST.");
        }

        return errorResponse(env, id, 404, "NOT_FOUND", "Admin endpoint not found.");
      }

      return errorResponse(env, id, 404, "NOT_FOUND", "Endpoint not found.");
    } catch (error) {
      return handleError(env, id, error);
    }
  },
};

function requireMethod(request: Request, expected: string): void {
  if (request.method !== expected) {
    throw new MethodError(`This endpoint requires ${expected}.`);
  }
}

function requireIdempotencyKey(request: Request): string {
  const value = request.headers.get("X-Idempotency-Key")?.trim();
  if (!value || !/^[A-Za-z0-9._:-]{16,128}$/.test(value)) {
    throw new ValidationError(
      "X-Idempotency-Key must contain 16-128 safe identifier characters.",
    );
  }
  return value;
}

function featureNotReady(env: Env, id: string, name: string): Response {
  return errorResponse(
    env,
    id,
    503,
    "FEATURE_NOT_READY",
    `${name} is disabled until its validation phase is complete.`,
  );
}

function handleError(env: Env, id: string, error: unknown): Response {
  if (error instanceof FirebaseAuthError) {
    const status = error.code === "AUTH_REQUIRED" ? 401 : 403;
    return errorResponse(env, id, status, error.code, error.message);
  }
  if (error instanceof MethodError) {
    return errorResponse(env, id, 405, "METHOD_NOT_ALLOWED", error.message);
  }
  if (error instanceof FeatureDisabledError) {
    return errorResponse(env, id, 503, "FEATURE_NOT_READY", error.message);
  }
  if (error instanceof admin.AdminError) {
    const status = error.code === "CONFLICT" ? 409 : error.code === "BAD_REQUEST" ? 400 : 403;
    return errorResponse(env, id, status, error.code as any, error.message);
  }
  if (
    error instanceof ValidationError ||
    error instanceof RequestBodyError ||
    error instanceof DeleteAccountError
  ) {
    return errorResponse(env, id, 400, "BAD_REQUEST", error.message);
  }
  if (error instanceof RecentAuthenticationRequiredError) {
    return errorResponse(env, id, 403, "AUTH_INVALID", error.message);
  }
  if (error instanceof UserStateError) {
    return errorResponse(env, id, 410, "ACCOUNT_DELETED", error.message);
  }
  if (error instanceof UsernameError) {
    const status = error.code === "EMAIL_VERIFICATION_REQUIRED" ? 403 : 409;
    return errorResponse(env, id, status, error.code, error.message);
  }
  if (error instanceof RateLimitError) {
    return errorResponse(
      env,
      id,
      429,
      "RATE_LIMITED",
      error.message,
      error.retryAfterSeconds,
    );
  }
  if (error instanceof ProgressConflictError) {
    return errorResponse(env, id, 409, "CONFLICT", error.message);
  }
  if (error instanceof UpdateRequiredError) {
    return errorResponse(env, id, 409, "UPDATE_REQUIRED", error.message);
  }

  console.error("Unhandled request error", {
    requestId: id,
    name: error instanceof Error ? error.name : "UnknownError",
    message: error instanceof Error ? error.message : String(error),
  });
  return errorResponse(env, id, 500, "SERVER_ERROR", "The server could not complete the request.");
}

class MethodError extends Error {}

async function getAdminPortal(): Promise<string> {
  // Return embedded admin portal HTML
  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>RhythmicRush Admin Portal</title>
  <style>
    *{margin:0;padding:0;box-sizing:border-box}body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Oxygen,Ubuntu,Cantarell,sans-serif;background:linear-gradient(135deg,#1a1a2e 0%,#16213e 100%);color:#e0e0e0;min-height:100vh;padding:20px}.container{max-width:1400px;margin:0 auto}header{text-align:center;margin-bottom:30px;padding-bottom:20px;border-bottom:2px solid #0f3460}h1{font-size:28px;color:#00d4ff;margin-bottom:10px}.status{font-size:12px;color:#888}.status.connected{color:#00d4ff}.tabs{display:flex;gap:10px;margin-bottom:20px;border-bottom:1px solid #0f3460}button.tab{background:0;border:none;color:#888;padding:12px 20px;cursor:pointer;font-size:14px;border-bottom:2px solid transparent;transition:all .3s}button.tab.active{color:#00d4ff;border-bottom-color:#00d4ff}button.tab:hover{color:#00d4ff}.tab-content{display:none}.tab-content.active{display:block}.panel{background:#0f1419;border:1px solid #0f3460;border-radius:8px;padding:20px;margin-bottom:20px}.panel h2{font-size:18px;margin-bottom:15px;color:#00d4ff}.form-group{margin-bottom:15px}label{display:block;font-size:13px;margin-bottom:5px;color:#aaa}input,textarea,select{width:100%;padding:10px;background:#1a2332;border:1px solid #0f3460;color:#e0e0e0;border-radius:4px;font-family:inherit}input:focus,textarea:focus,select:focus{outline:0;border-color:#00d4ff;box-shadow:0 0 8px rgba(0,212,255,.2)}.button-group{display:flex;gap:10px;flex-wrap:wrap}button{padding:10px 20px;background:#00d4ff;border:none;color:#0f1419;border-radius:4px;cursor:pointer;font-weight:700;font-size:13px;transition:all .3s}button:hover{background:#00b8cc;transform:translateY(-2px)}button.danger{background:#ff4444;color:#fff}button.danger:hover{background:#cc0000}button.secondary{background:#444;color:#e0e0e0}button.secondary:hover{background:#555}.leaderboard-table{width:100%;border-collapse:collapse;margin-top:15px;font-size:13px}.leaderboard-table th{background:#1a2332;padding:10px;text-align:left;border-bottom:2px solid #0f3460;color:#00d4ff;font-weight:700}.leaderboard-table td{padding:10px;border-bottom:1px solid #0f3460}.leaderboard-table tr:hover{background:#1a2332}.leaderboard-table .banned{color:#ff4444;font-weight:700}.detail-card{background:#1a2332;border:1px solid #0f3460;border-radius:4px;padding:12px;font-size:12px}.detail-card strong{color:#00d4ff}.level-stats{margin-top:15px}.level-stat{display:flex;justify-content:space-between;padding:8px;border-bottom:1px solid #0f3460;font-size:12px}.alert{padding:12px;border-radius:4px;margin-bottom:15px;font-size:13px}.alert.success{background:rgba(0,200,100,.1);border:1px solid #00c864;color:#00ff88}.alert.error{background:rgba(255,68,68,.1);border:1px solid #ff4444;color:#ff8888}.alert.info{background:rgba(0,212,255,.1);border:1px solid #00d4ff;color:#00ffff}.loading{text-align:center;color:#888;padding:20px}.spinner{display:inline-block;width:20px;height:20px;border:2px solid #0f3460;border-top-color:#00d4ff;border-radius:50%;animation:spin .8s linear infinite}@keyframes spin{to{transform:rotate(360deg)}}
  </style>
</head>
<body>
  <div class="container">
    <header>
      <h1>🎮 RhythmicRush Admin Portal</h1>
      <p class="status" id="status">⏳ Initializing...</p>
    </header>
    <div class="tabs">
      <button class="tab active" onclick="switchTab('leaderboard')">Leaderboard</button>
      <button class="tab" onclick="switchTab('user')">User Management</button>
      <button class="tab" onclick="switchTab('logout')">Logout</button>
    </div>
    <div id="leaderboard" class="tab-content active">
      <div class="panel">
        <h2>Global Leaderboard</h2>
        <div class="button-group">
          <button onclick="loadLeaderboard()">📊 Refresh Leaderboard</button>
          <input type="number" id="leaderboardLimit" placeholder="Limit (default 100)" min="1" max="1000" style="width:200px">
        </div>
        <div id="leaderboardContainer" style="margin-top:20px"></div>
      </div>
    </div>
    <div id="user" class="tab-content">
      <div class="panel">
        <h2>User Lookup & Moderation</h2>
        <div class="form-group">
          <label>User UID</label>
          <input type="text" id="userUid" placeholder="Enter Firebase UID...">
        </div>
        <div class="button-group">
          <button onclick="loadUserDetails()">🔍 Load User Details</button>
          <button class="secondary" onclick="clearUserPanel()">Clear</button>
        </div>
      </div>
      <div id="userDetailsPanel" style="display:none">
        <div class="panel">
          <h2>User Information</h2>
          <div id="userInfo"></div>
        </div>
        <div class="panel">
          <h2>Moderation Actions</h2>
          <div class="form-group">
            <label>Ban Status</label>
            <div style="display:flex;gap:10px">
              <button onclick="banUser(true)" class="danger">🚫 Ban from Leaderboard</button>
              <button onclick="banUser(false)" class="secondary">✅ Unban</button>
            </div>
          </div>
          <div class="form-group">
            <label>Ban Reason (optional)</label>
            <textarea id="banReason" placeholder="e.g., Suspected cheating, Offensive username, etc." rows="3"></textarea>
          </div>
          <div class="form-group">
            <label>Rename User</label>
            <div style="display:flex;gap:10px">
              <input type="text" id="newUsername" placeholder="New username..." style="flex:1">
              <button onclick="renameUser(false)">✏️ Rename</button>
              <button onclick="renameUser(true)" class="secondary">🔄 Force Rename</button>
            </div>
          </div>
          <div class="form-group">
            <label></label>
            <button onclick="deleteUser()" class="danger">🗑️ Delete Account & Data</button>
          </div>
        </div>
        <div class="panel">
          <h2>Game Statistics</h2>
          <div id="gameStats"></div>
        </div>
      </div>
    </div>
  </div>
  <script>
const API_BASE = window.location.origin + '/v1/dev';
let idToken = null;

window.addEventListener('load', async () => {
  await initializeAuth();
  updateStatus();
});

async function initializeAuth() {
  try {
    const stored = localStorage.getItem('admin_token');
    if (stored) {
      idToken = stored;
      return;
    }
    const token = prompt('Enter your Firebase ID token:', '');
    if (!token) {
      updateStatus('❌ No token provided', false);
      return;
    }
    idToken = token;
    localStorage.setItem('admin_token', token);
  } catch (e) {
    console.error('Auth error:', e);
    updateStatus('❌ Authentication failed', false);
  }
}

function updateStatus(msg = '✅ Connected', ok = true) {
  const el = document.getElementById('status');
  el.textContent = msg;
  el.className = ok ? 'status connected' : 'status';
}

function switchTab(t) {
  document.querySelectorAll('.tab-content').forEach(e => e.classList.remove('active'));
  document.querySelectorAll('.tab').forEach(e => e.classList.remove('active'));
  if (t === 'logout') {
    localStorage.removeItem('admin_token');
    idToken = null;
    updateStatus('❌ Logged out', false);
    location.reload();
    return;
  }
  document.getElementById(t).classList.add('active');
  event.target.classList.add('active');
}

async function fetchAPI(ep, opts = {}) {
  if (!idToken) {
    updateStatus('❌ Not authenticated', false);
    throw new Error('Not authenticated');
  }
  try {
    const r = await fetch(API_BASE + ep, {
      ...opts,
      headers: {
        'Authorization': 'Bearer ' + idToken,
        'Content-Type': 'application/json',
        ...opts.headers,
      },
    });
    const d = await r.json();
    if (!r.ok) throw new Error(d.error?.message || 'API error');
    return d.data;
  } catch (e) {
    console.error('API error:', e);
    showAlert('Error: ' + e.message, 'error');
    throw e;
  }
}

async function loadLeaderboard() {
  const limit = document.getElementById('leaderboardLimit').value || 100;
  const c = document.getElementById('leaderboardContainer');
  c.innerHTML = '<div class="loading"><div class="spinner"></div> Loading...</div>';
  try {
    const d = await fetchAPI('/leaderboard?limit=' + limit);
    let h = '<table class="leaderboard-table"><thead><tr><th>Rank</th><th>UID</th><th>Username</th><th>Points</th><th>Levels</th><th>Eligible</th><th>Status</th><th>Banned</th><th>Action</th></tr></thead><tbody>';
    d.forEach((e, i) => {
      const b = e.leaderboard_banned === 1 ? '🚫 Yes' : 'No';
      const g = e.eligible === 1 ? '✅' : '❌';
      const s = e.leaderboard_banned === 1 ? 'opacity:0.6' : '';
      h += '<tr style="' + s + '"><td>' + (i + 1) + '</td><td>' + e.uid.substring(0, 8) + '...</td><td>' + (e.username || '(unnamed)') + '</td><td>' + e.points + '</td><td>' + e.completed_levels + '</td><td>' + g + '</td><td>' + e.status + '</td><td class="' + (e.leaderboard_banned === 1 ? 'banned' : '') + '">' + b + '</td><td><button class="secondary" data-uid="' + e.uid + '" onclick="userLoadById(this)">View</button></td></tr>';
    });
    h += '</tbody></table>';
    c.innerHTML = h;
  } catch (e) {
    c.innerHTML = '<div class="alert error">Failed to load leaderboard</div>';
  }
}

function userLoadById(btn) {
  const uid = btn.getAttribute('data-uid');
  document.getElementById('userUid').value = uid;
  loadUserDetails();
}

async function loadUserDetails() {
  const uid = document.getElementById('userUid').value.trim();
  if (!uid) {
    showAlert('Please enter a UID', 'error');
    return;
  }
  try {
    const d = await fetchAPI('/user?uid=' + uid);
    displayUserDetails(d);
    document.getElementById('userDetailsPanel').style.display = 'block';
  } catch (e) {
    showAlert('User not found', 'error');
    document.getElementById('userDetailsPanel').style.display = 'none';
  }
}

function displayUserDetails(d) {
  let h = '';
  if (d.account) {
    const a = d.account;
    h += '<div class="detail-card"><strong>UID:</strong> ' + a.uid + '<br><strong>Username:</strong> ' + (a.username || '(none)') + '<br><strong>Email Verified:</strong> ' + (a.email_verified ? '✅' : '❌') + '<br><strong>Status:</strong> ' + a.status + '<br><strong>Leaderboard Banned:</strong> ' + (a.leaderboard_banned === 1 ? '🚫 Yes' : 'No') + '<br>' + (a.leaderboard_ban_reason ? '<strong>Ban Reason:</strong> ' + a.leaderboard_ban_reason + '<br>' : '') + '<strong>Created:</strong> ' + new Date(a.created_at).toLocaleString() + '</div>';
  }
  if (d.save) {
    const sv = d.save;
    h += '<div class="detail-card"><strong>Points:</strong> ' + sv.points + '<br><strong>Coins:</strong> ' + sv.earned_coins + '<br><strong>Levels Done:</strong> ' + sv.completed_levels + '<br><strong>Last Played:</strong> ' + new Date(sv.updated_at).toLocaleString() + '</div>';
  }
  if (d.level_progress && d.level_progress.length > 0) {
    h += '<div class="level-stats"><strong>Level Progress:</strong>';
    d.level_progress.forEach(lp => {
      h += '<div class="level-stat"><span>Lvl ' + lp.level_id + ': ' + lp.best_percent + '% ' + (lp.completed === 1 ? '✅' : '⏳') + '</span><span>' + new Date(lp.updated_at).toLocaleString() + '</span></div>';
    });
    h += '</div>';
  }
  if (d.device_attempts && d.device_attempts.length > 0) {
    h += '<div style="margin-top:15px"><strong>Devices:</strong>';
    d.device_attempts.forEach(da => {
      h += '<div class="level-stat"><span>' + da.device_id.substring(0, 8) + '</span><span>' + da.accepted_total + ' attempts</span></div>';
    });
    h += '</div>';
  }
  document.getElementById('userInfo').innerHTML = h;
}

async function banUser(ban) {
  const uid = document.getElementById('userUid').value.trim();
  const reason = document.getElementById('banReason').value.trim();
  if (!uid) { showAlert('Enter a UID', 'error'); return; }
  if (!confirm('Are you sure?')) return;
  try {
    await fetchAPI('/user', {
      method: 'POST',
      body: JSON.stringify({ action: 'ban', uid, ban, reason: reason || null }),
    });
    showAlert('Done!', 'success');
    loadUserDetails();
  } catch (e) {
    showAlert('Failed', 'error');
  }
}

async function renameUser(force) {
  const uid = document.getElementById('userUid').value.trim();
  const newName = document.getElementById('newUsername').value.trim();
  if (!uid || !newName) { showAlert('Need UID and username', 'error'); return; }
  if (!confirm('Rename to ' + newName + '?')) return;
  try {
    await fetchAPI('/user', {
      method: 'POST',
      body: JSON.stringify({ action: 'rename', uid, newUsername: newName, force }),
    });
    showAlert('Renamed!', 'success');
    loadUserDetails();
  } catch (e) {
    showAlert('Failed', 'error');
  }
}

async function deleteUser() {
  const uid = document.getElementById('userUid').value.trim();
  if (!uid) { showAlert('Enter UID', 'error'); return; }
  if (!confirm('DELETE user ' + uid + '?')) return;
  if (!confirm('Really?')) return;
  try {
    await fetchAPI('/user', {
      method: 'POST',
      body: JSON.stringify({ action: 'delete', uid }),
    });
    showAlert('Deleted!', 'success');
    clearUserPanel();
  } catch (e) {
    showAlert('Failed', 'error');
  }
}

function clearUserPanel() {
  document.getElementById('userUid').value = '';
  document.getElementById('userDetailsPanel').style.display = 'none';
}

function showAlert(msg, type = 'info') {
  const el = document.createElement('div');
  el.className = 'alert ' + type;
  el.textContent = msg;
  document.querySelector('.container').insertBefore(el, document.querySelector('.tabs'));
  setTimeout(() => el.remove(), 5000);
}
  <\/script>
</body>
</html>`;
}


