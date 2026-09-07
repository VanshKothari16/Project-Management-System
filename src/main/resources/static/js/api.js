/* =====================================================================
   api.js — one shared place for every backend call.

   NOTE ON BACKEND ASSUMPTIONS:
   This frontend is built against the backend DTOs exactly as analyzed
   in backend.md, WITH the critical bug fixes from backend.md § 15
   already applied on the server side:
     - GET /task/completed   -> list of completed tasks
     - GET /task/dashboard   -> AdminDashboardDto   (was duplicated as
       /task/completed in the original code — must be fixed server-side)
     - GET /employee/emails  -> list of employee emails (was unmapped)
     - DELETE /commitments   -> actually deletes now
     - Employee GET/PUT      -> also allowed for ROLE_EMPLOYEE
     - @PreAuthorize          -> uses hasAuthority(...)/hasAnyAuthority(...)
   If the backend has not been patched yet, these specific calls will fail.
   ===================================================================== */

const BASE_URL = "https://project-management-system-mljl.onrender.com/";

const Auth = {
  saveToken(token) { localStorage.setItem("tm_token", token); },
  getToken() { return localStorage.getItem("tm_token"); },
  clear() { localStorage.removeItem("tm_token"); localStorage.removeItem("tm_role"); },
  saveRole(role) { localStorage.setItem("tm_role", role); },
  getRole() { return localStorage.getItem("tm_role"); },
  isLoggedIn() { return !!this.getToken(); },
  /* JWT payloads are base64 — decode just enough to read the role/subject
     without needing a library. This is display-only, never used for
     security decisions (the server always re-checks permissions). */
  decode(token) {
    try {
      const payload = token.split(".")[1];
      const json = decodeURIComponent(atob(payload.replace(/-/g, "+").replace(/_/g, "/"))
        .split("").map(c => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2)).join(""));
      return JSON.parse(json);
    } catch (e) { return null; }
  }
};

async function request(path, { method = "GET", body = null, isForm = false, auth = true } = {}) {
  const headers = {};
  if (!isForm) headers["Content-Type"] = "application/json";
  if (auth && Auth.getToken()) headers["Authorization"] = "Bearer " + Auth.getToken();

  const res = await fetch(BASE_URL + path, {
    method,
    headers,
    body: body ? (isForm ? body : JSON.stringify(body)) : null
  });

  const text = await res.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch (e) { data = text; }

  if (!res.ok) {
    const message = (data && typeof data === "object" && data.message) ? data.message
      : (typeof data === "string" && data) ? data
      : `Request failed (${res.status})`;
    const err = new Error(message);
    err.status = res.status;
    err.details = data;
    throw err;
  }
  return data;
}

/* ---------------- User (Auth) ---------------- */
const UserApi = {
  register(userRequestDto) {
    return request("/user/register", { method: "POST", body: userRequestDto, auth: false });
  },
  login(loginDto) {
    // Backend returns the raw JWT string in the response body.
    return request("/user/login", { method: "POST", body: loginDto, auth: false });
  }
};

/* ---------------- Company ---------------- */
const CompanyApi = {
  register(userRequestDto, companyRequestDto) {
    // Controller uses @ModelAttribute -> must be sent as multipart/form-data.
    const form = new FormData();
    Object.entries(userRequestDto).forEach(([k, v]) => form.append(k, v));
    Object.entries(companyRequestDto).forEach(([k, v]) => form.append(k, v));
    return request("/company", { method: "POST", body: form, isForm: true, auth: false });
  }
};

/* ---------------- Employee ---------------- */
const EmployeeApi = {
  register(employeeRequestDto) {
    return request("/employee", { method: "POST", body: employeeRequestDto, auth: false });
  },
  updateProfile(employeeRequestDto) {
    return request("/employee", { method: "PUT", body: employeeRequestDto });
  },
  getMyProfile() {
    return request("/employee", { method: "GET" });
  },
  getAllEmails(taskId) {
    return request(`/employee/emails?taskId=${encodeURIComponent(taskId)}`, { method: "GET" });
  }
};

/* ---------------- Project (Admin only) ---------------- */
const ProjectApi = {
  create(projectRequestDto) {
    return request("/project", { method: "POST", body: projectRequestDto });
  },
  update(id, projectRequestDto) {
    return request(`/project?id=${encodeURIComponent(id)}`, { method: "PUT", body: projectRequestDto });
  },
  get(id) {
    return request(`/project?id=${encodeURIComponent(id)}`, { method: "GET" });
  },
  remove(id) {
    return request(`/project?id=${encodeURIComponent(id)}`, { method: "DELETE" });
  },
  listAll() {
    return request("/project/all", { method: "GET" });
  },
  dashboard(id) {
    return request(`/project/${encodeURIComponent(id)}/dashboard`, { method: "GET" });
  },
  tasksByStatus(id, status) {
    return request(`/project/${encodeURIComponent(id)}/tasks?status=${encodeURIComponent(status)}`, { method: "GET" });
  },
  manager(id) {
    return request(`/project/${encodeURIComponent(id)}/manager`, { method: "GET" });
  },
  employees(id) {
    return request(`/project/${encodeURIComponent(id)}/employees`, { method: "GET" });
  }
};

/* ---------------- Task ---------------- */
const TaskApi = {
  create(taskRequestDto) {
    return request("/task", { method: "POST", body: taskRequestDto });
  },
  update(id, taskRequestDto) {
    return request(`/task?id=${encodeURIComponent(id)}`, { method: "PUT", body: taskRequestDto });
  },
  remove(id) {
    return request(`/task?id=${encodeURIComponent(id)}`, { method: "DELETE" });
  },
  get(id) {
    return request(`/task/${encodeURIComponent(id)}`, { method: "GET" });
  },
  assign(taskId, email) {
    return request(`/task/assign?task=${encodeURIComponent(taskId)}&email=${encodeURIComponent(email)}`, { method: "POST" });
  },
  markCompleted(id) {
    return request(`/task/${encodeURIComponent(id)}`, { method: "POST" });
  },
  // ---- Manager/Admin board ----
  all() { return request("/task/all", { method: "GET" }); },
  overdue() { return request("/task/overdue", { method: "GET" }); },
  todo() { return request("/task/todo", { method: "GET" }); },
  inProgress() { return request("/task/inprogress", { method: "GET" }); },
  completed() { return request("/task/completed", { method: "GET" }); },
  adminDashboard() { return request("/task/dashboard", { method: "GET" }); },
  // ---- Employee board ----
  myAll() { return request("/task/employee/all", { method: "GET" }); },
  myTodo() { return request("/task/employee/todo", { method: "GET" }); },
  myInProgress() { return request("/task/employee/inprogress", { method: "GET" }); },
  myCompleted() { return request("/task/employee/completed", { method: "GET" }); }
};

/* ---------------- Commitments ---------------- */
const CommitmentApi = {
  add(commitmentRequestDto) {
    return request("/commitments", { method: "POST", body: commitmentRequestDto });
  },
  update(id, commitmentRequestDto) {
    return request(`/commitments?id=${encodeURIComponent(id)}`, { method: "PUT", body: commitmentRequestDto });
  },
  remove(commitId) {
    return request(`/commitments?commitId=${encodeURIComponent(commitId)}`, { method: "DELETE" });
  },
  ofTask(taskId, page = 0, size = 15) {
    return request(`/commitments?taskId=${encodeURIComponent(taskId)}&page=${page}&size=${size}`, { method: "GET" });
  }
};

/* ---------------- Guards ---------------- */
function requireAuth(allowedRoles) {
  if (!Auth.isLoggedIn()) { window.location.href = "index.html"; return false; }
  const role = Auth.getRole();
  if (allowedRoles && !allowedRoles.includes(role)) {
    window.location.href = "index.html";
    return false;
  }
  return true;
}

function logout() {
  Auth.clear();
  window.location.href = "index.html";
}

function fmtDate(d) {
  if (!d) return "—";
  const date = new Date(d);
  if (isNaN(date)) return d;
  return date.toLocaleDateString(undefined, { day: "2-digit", month: "short", year: "numeric" });
}

function escapeHtml(str) {
  if (str === null || str === undefined) return "";
  return String(str).replace(/[&<>"']/g, s => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
  }[s]));
}
