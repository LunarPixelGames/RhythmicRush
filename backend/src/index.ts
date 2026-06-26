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
  // Return embedded admin portal HTML (minified for production)
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
const API_BASE=window.location.origin+'/v1/dev';let idToken=null;window.addEventListener('load',async()=>{await initializeAuth();updateStatus()});async function initializeAuth(){try{const stored=localStorage.getItem('admin_token');if(stored){idToken=stored;return}const token=prompt('Enter your Firebase ID token:','');if(!token){updateStatus('❌ No token provided',false);return}idToken=token;localStorage.setItem('admin_token',token)}catch(e){console.error('Auth error:',e);updateStatus('❌ Authentication failed',false)}}function updateStatus(message='✅ Connected',connected=true){const el=document.getElementById('status');el.textContent=message;el.className=connected?'status connected':'status'}function switchTab(tabName){document.querySelectorAll('.tab-content').forEach(el=>el.classList.remove('active'));document.querySelectorAll('.tab').forEach(el=>el.classList.remove('active'));if('logout'===tabName){localStorage.removeItem('admin_token');idToken=null;updateStatus('❌ Logged out',false);location.reload();return}document.getElementById(tabName).classList.add('active');event.target.classList.add('active')}async function fetchAPI(endpoint,options={}){if(!idToken){updateStatus('❌ Not authenticated',false);throw new Error('Not authenticated')}try{const response=await fetch(API_BASE+endpoint,{...options,headers:{'Authorization':'Bearer '+idToken,'Content-Type':'application/json',...options.headers}});const data=await response.json();if(!response.ok)throw new Error((data.error?.message||'API error'));return data.data}catch(e){console.error('API error:',e);showAlert('Error: '+e.message,'error');throw e}}async function loadLeaderboard(){const limit=document.getElementById('leaderboardLimit').value||100;const container=document.getElementById('leaderboardContainer');container.innerHTML='<div class="loading"><div class="spinner"></div> Loading...</div>';try{const data=await fetchAPI('/leaderboard?limit='+limit);let html='<table class="leaderboard-table"><thead><tr><th>Rank</th><th>UID</th><th>Username</th><th>Points</th><th>Levels</th><th>Eligible</th><th>Status</th><th>Banned</th><th>Action</th></tr></thead><tbody>';data.forEach((entry,idx)=>{const banned=1===entry.leaderboard_banned?'🚫 Yes':'No';const eligible=1===entry.eligible?'✅':'❌';html+='<tr '+(1===entry.leaderboard_banned?'style="opacity: 0.6;"':'')+'>\\n            <td>'+(idx+1)+'</td>\\n            <td>'+entry.uid.substring(0,8)+'...</td>\\n            <td>'+(entry.username||'(unnamed)')+'</td>\\n            <td>'+entry.points+'</td>\\n            <td>'+entry.completed_levels+'</td>\\n            <td>'+eligible+'</td>\\n            <td>'+entry.status+'</td>\\n            <td class="'+(1===entry.leaderboard_banned?'banned':'')\\'">'+banned+'</td>\\n            <td><button class="secondary" onclick="document.getElementById(\\'userUid\\').value=\\''+entry.uid+'\\'; loadUserDetails();">View</button></td>\\n          </tr>'});html+='</tbody></table>';container.innerHTML=html}catch(e){container.innerHTML='<div class="alert error">Failed to load leaderboard</div>'}}async function loadUserDetails(){const uid=document.getElementById('userUid').value.trim();if(!uid){showAlert('Please enter a UID','error');return}try{const data=await fetchAPI('/user?uid='+uid);displayUserDetails(data);document.getElementById('userDetailsPanel').style.display='block'}catch(e){showAlert('User not found or error loading details','error');document.getElementById('userDetailsPanel').style.display='none'}}function displayUserDetails(data){let html='';if(data.account){const acc=data.account;html+='<div class="detail-card"><strong>UID:</strong> '+acc.uid+'<br><strong>Username:</strong> '+(acc.username||'(none)')+'<br><strong>Email Verified:</strong> '+(acc.email_verified?'✅':'❌')+'<br><strong>Status:</strong> '+acc.status+'<br><strong>Leaderboard Banned:</strong> '+(1===acc.leaderboard_banned?'🚫 Yes':'No')+'<br>'+(acc.leaderboard_ban_reason?'<strong>Ban Reason:</strong> '+acc.leaderboard_ban_reason+'<br>':'')+'<strong>Created:</strong> '+new Date(acc.created_at).toLocaleString()+'<br><strong>Updated:</strong> '+new Date(acc.updated_at).toLocaleString()+'</div>'}if(data.save){const save=data.save;html+='<div class="detail-card"><strong>Points:</strong> '+save.points+'<br><strong>Coins Earned:</strong> '+save.earned_coins+'<br><strong>Levels Completed:</strong> '+save.completed_levels+'<br><strong>Total Attempts:</strong> '+save.schema_version+'<br><strong>Last Played:</strong> '+new Date(save.updated_at).toLocaleString()+'</div>'}if(data.level_progress&&data.level_progress.length>0){html+='<div class="level-stats"><strong>Level Progress:</strong>';data.level_progress.forEach(lp=>{html+='<div class="level-stat"><span>Level '+lp.level_id+': '+lp.best_percent+'% '+(1===lp.completed?'✅ Done':'⏳ In Progress')+'</span><span>'+new Date(lp.updated_at).toLocaleString()+'</span></div>'});html+='</div>'}if(data.device_attempts&&data.device_attempts.length>0){html+='<div style="margin-top:15px"><strong>Device Attempts Summary:</strong>';data.device_attempts.forEach(da=>{html+='<div class="level-stat"><span>Device '+da.device_id.substring(0,8)+'...</span> <span>'+da.accepted_total+' attempts</span></div>'});html+='</div>'}document.getElementById('userInfo').innerHTML=html}async function banUser(ban){const uid=document.getElementById('userUid').value.trim();const reason=document.getElementById('banReason').value.trim();if(!uid){showAlert('Please enter a UID','error');return}if(!confirm('Are you sure you want to '+(ban?'ban':'unban')+' this user?'))return;try{await fetchAPI('/user',{method:'POST',body:JSON.stringify({action:'ban',uid:uid,ban:ban,reason:reason||null})});showAlert('User '+(ban?'banned':'unbanned')+' successfully!','success');loadUserDetails()}catch(e){showAlert('Failed to update ban status','error')}}async function renameUser(force){const uid=document.getElementById('userUid').value.trim();const newUsername=document.getElementById('newUsername').value.trim();if(!uid||!newUsername){showAlert('Please enter a UID and new username','error');return}if(!confirm('Rename user to "'+newUsername+'"?'))return;try{await fetchAPI('/user',{method:'POST',body:JSON.stringify({action:'rename',uid:uid,newUsername:newUsername,force:force})});showAlert('User renamed successfully!','success');loadUserDetails()}catch(e){showAlert('Failed to rename user','error')}}async function deleteUser(){const uid=document.getElementById('userUid').value.trim();if(!uid){showAlert('Please enter a UID','error');return}if(!confirm('⚠️ DELETE ALL DATA FOR UID '+uid+'? This cannot be undone!'))return;if(!confirm('Are you absolutely sure? Type YES to confirm.'))return;try{await fetchAPI('/user',{method:'POST',body:JSON.stringify({action:'delete',uid:uid})});showAlert('User and all data deleted!','success');clearUserPanel()}catch(e){showAlert('Failed to delete user','error')}}function clearUserPanel(){document.getElementById('userUid').value='';document.getElementById('userDetailsPanel').style.display='none'}function showAlert(message,type='info'){const alertEl=document.createElement('div');alertEl.className='alert '+type;alertEl.textContent=message;document.querySelector('.container').insertBefore(alertEl,document.querySelector('.tabs'));setTimeout(()=>alertEl.remove(),5e3)}
  <\/script>
</body>
</html>`;
}


