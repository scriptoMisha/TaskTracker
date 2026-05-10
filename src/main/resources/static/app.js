// ============== Token storage ==============
const TOKEN_KEY = "minitasks.tokens";

function saveTokens(t) { localStorage.setItem(TOKEN_KEY, JSON.stringify(t)); }
function loadTokens() {
  try { return JSON.parse(localStorage.getItem(TOKEN_KEY)); } catch { return null; }
}
function clearTokens() { localStorage.removeItem(TOKEN_KEY); }

// ============== API helper ==============
async function api(path, opts = {}) {
  const tokens = loadTokens();
  const headers = { "Content-Type": "application/json", ...(opts.headers || {}) };
  if (tokens?.accessToken) headers["Authorization"] = `Bearer ${tokens.accessToken}`;

  const res = await fetch(path, { ...opts, headers });
  if (res.status === 401) {
    clearTokens();
    showAuthView();
    throw new Error("unauthorized");
  }
  if (res.status === 204) return null;
  const body = await res.json().catch(() => null);
  if (!res.ok) {
    const msg = body?.detail || body?.title || `HTTP ${res.status}`;
    throw new Error(msg);
  }
  return body;
}

// ============== Views ==============
const $ = (sel) => document.querySelector(sel);
const authView = $("#auth-view");
const appView = $("#app-view");
const who = $("#who");

function showAuthView() {
  authView.classList.remove("hidden");
  appView.classList.add("hidden");
  who.innerHTML = "";
}

async function showAppView() {
  authView.classList.add("hidden");
  appView.classList.remove("hidden");
  await renderUserBadge();
  await loadTasks();
}

async function renderUserBadge() {
  try {
    const me = await api("/api/users/me");
    who.innerHTML = `
      <span>${me.email} <span class="badge">${me.role}</span></span>
      <button id="logout-btn" class="secondary">Log out</button>
    `;
    $("#logout-btn").addEventListener("click", logout);
  } catch {
    showAuthView();
  }
}

// ============== Auth ==============
function setAuthError(msg) { $("#auth-error").textContent = msg || ""; }

document.querySelectorAll(".tab").forEach((tab) => {
  tab.addEventListener("click", () => {
    document.querySelectorAll(".tab").forEach((t) => t.classList.toggle("active", t === tab));
    const which = tab.dataset.tab;
    $("#login-form").classList.toggle("hidden", which !== "login");
    $("#register-form").classList.toggle("hidden", which !== "register");
    setAuthError("");
  });
});

$("#login-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  setAuthError("");
  const f = e.target;
  try {
    const tokens = await api("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ email: f.email.value, password: f.password.value }),
    });
    saveTokens(tokens);
    await showAppView();
    f.reset();
  } catch (err) { setAuthError(err.message); }
});

$("#register-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  setAuthError("");
  const f = e.target;
  try {
    const tokens = await api("/api/auth/register", {
      method: "POST",
      body: JSON.stringify({ email: f.email.value, password: f.password.value }),
    });
    saveTokens(tokens);
    await showAppView();
    f.reset();
  } catch (err) { setAuthError(err.message); }
});

async function logout() {
  try { await api("/api/auth/logout", { method: "POST" }); } catch { /* ignore */ }
  clearTokens();
  showAuthView();
}

// ============== Tasks ==============
let currentFilter = { status: "", tag: "" };

$("#filter-form").addEventListener("submit", (e) => {
  e.preventDefault();
  const f = e.target;
  currentFilter = { status: f.status.value, tag: f.tag.value.trim() };
  loadTasks();
});

$("#create-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  const f = e.target;
  const tags = f.tags.value.split(",").map((s) => s.trim()).filter(Boolean);
  try {
    await api("/api/tasks", {
      method: "POST",
      body: JSON.stringify({
        title: f.title.value,
        priority: parseInt(f.priority.value, 10),
        tags,
      }),
    });
    f.reset();
    f.priority.value = "3";
    await loadTasks();
  } catch (err) { alert(err.message); }
});

async function loadTasks() {
  const params = new URLSearchParams({ size: "50", sort: "createdAt,desc" });
  if (currentFilter.status) params.set("status", currentFilter.status);
  if (currentFilter.tag) params.set("tag", currentFilter.tag);

  const page = await api(`/api/tasks?${params}`);
  const ul = $("#tasks");
  ul.innerHTML = "";
  const items = page?.content || [];
  $("#tasks-empty").classList.toggle("hidden", items.length > 0);
  items.forEach((t) => ul.appendChild(renderTask(t)));
}

function renderTask(t) {
  const li = document.createElement("li");
  li.className = "task";
  const titleClass = t.status === "DONE" ? "title done" : "title";
  const tags = (t.tags || []).map((x) => `<span class="tag">#${escapeHtml(x)}</span>`).join(" ");

  let actionBtn = "";
  if (t.status === "TODO") {
    actionBtn = `<button data-act="take">Take</button>`;
  } else if (t.status === "IN_PROGRESS") {
    actionBtn = `<button data-act="done">Done</button>`;
  } else if (t.status === "DONE") {
    actionBtn = `<button class="secondary" data-act="reopen">Reopen</button>`;
  }

  li.innerHTML = `
    <div class="${titleClass}">
      ${escapeHtml(t.title)}
      <span class="badge ${t.status.toLowerCase()}">${t.status}</span>
      <span class="badge">P${t.priority}</span>
      ${tags}
    </div>
    <div class="actions">
      ${actionBtn}
      <button class="danger" data-act="delete">Delete</button>
    </div>
  `;
  li.querySelector('[data-act="take"]')?.addEventListener("click", () => updateStatus(t.id, "IN_PROGRESS"));
  li.querySelector('[data-act="done"]')?.addEventListener("click", () => updateStatus(t.id, "DONE"));
  li.querySelector('[data-act="reopen"]')?.addEventListener("click", () => updateStatus(t.id, "TODO"));
  li.querySelector('[data-act="delete"]').addEventListener("click", () => deleteTask(t.id));
  return li;
}

async function updateStatus(id, status) {
  try {
    await api(`/api/tasks/${id}`, { method: "PATCH", body: JSON.stringify({ status }) });
    await loadTasks();
  } catch (err) { alert(err.message); }
}

async function deleteTask(id) {
  if (!confirm("Delete this task?")) return;
  try {
    await api(`/api/tasks/${id}`, { method: "DELETE" });
    await loadTasks();
  } catch (err) { alert(err.message); }
}

function escapeHtml(s) {
  return String(s).replace(/[&<>"']/g, (c) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
  }[c]));
}

// ============== Boot ==============
(async () => {
  if (loadTokens()) {
    try { await showAppView(); return; } catch { /* fallthrough */ }
  }
  showAuthView();
})();
