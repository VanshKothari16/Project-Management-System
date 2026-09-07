requireAuth(["MANAGER"]);

const sidebarUser = document.getElementById("sidebarUser");
const tokenInfo = Auth.decode(Auth.getToken());
sidebarUser.textContent = tokenInfo && tokenInfo.sub ? tokenInfo.sub : "Manager";

/* ---------------- Tabs ---------------- */
document.querySelectorAll(".nav-item[data-tab]").forEach(btn => {
  btn.addEventListener("click", () => {
    document.querySelectorAll(".nav-item[data-tab]").forEach(b => b.classList.remove("active"));
    btn.classList.add("active");
    document.getElementById("tab-board").style.display = btn.dataset.tab === "board" ? "block" : "none";
    document.getElementById("tab-profile").style.display = btn.dataset.tab === "profile" ? "block" : "none";
    if (btn.dataset.tab === "profile") loadProfile();
  });
});

/* ---------------- Header: show the manager's actual project name ---------------- */
async function loadProjectName() {
  const el = document.getElementById("project_name");
  try {
    const emp = await EmployeeApi.getMyProfile(); // calls GET /employee under the hood
    el.textContent = emp.project || "Your project"; // falls back if not assigned yet
  } catch (err) {
    el.textContent = "Your project"; // keep the page usable even if this call fails
  }
}
loadProjectName();

/* ---------------- Board ---------------- */
function taskCardHtml(task) {
  return `
    <div class="task-card p-${task.priority}" data-id="${escapeHtml(task.id)}">
      <div class="t-title">${escapeHtml(task.title)}</div>
      <div class="t-meta"><span>Due ${fmtDate(task.dueDate)}</span><span>${(task.employee || []).length} assigned</span></div>
      <span class="t-tag">${task.priority}</span>
    </div>`;
}

function renderColumn(colId, countId, tasks) {
  const col = document.getElementById(colId);
  document.getElementById(countId).textContent = tasks.length;
  col.innerHTML = tasks.length
    ? tasks.map(taskCardHtml).join("")
    : `<div class="empty-note">Nothing here.</div>`;
  col.querySelectorAll(".task-card").forEach(card => {
    card.addEventListener("click", () => openTaskDetail(card.dataset.id));
  });
}

async function loadBoard() {
  try {
    const [todo, inProgress, completed, overdue] = await Promise.all([
      TaskApi.todo(), TaskApi.inProgress(), TaskApi.completed(), TaskApi.overdue()
    ]);
    renderColumn("col-todo", "count-todo", todo);
    renderColumn("col-inprogress", "count-inprogress", inProgress);
    renderColumn("col-completed", "count-completed", completed);
    renderColumn("col-overdue", "count-overdue", overdue);
  } catch (err) {
    ["col-todo", "col-inprogress", "col-completed", "col-overdue"].forEach(id => {
      document.getElementById(id).innerHTML = `<div class="empty-note">Error: ${escapeHtml(err.message)}</div>`;
    });
  }
}

/* ---------------- Add / Edit Task ---------------- */
const taskModal = document.getElementById("taskModal");
const taskForm = document.getElementById("taskForm");
const taskError = document.getElementById("taskError");

document.getElementById("addTaskFab").addEventListener("click", () => {
  document.getElementById("taskModalTitle").textContent = "New task";
  taskForm.reset();
  document.getElementById("t_id").value = "";
  taskError.classList.remove("show");
  taskModal.classList.add("show");
});

function closeTaskModal() { taskModal.classList.remove("show"); }

taskForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  taskError.classList.remove("show");

  const dto = {
    title: document.getElementById("t_title").value.trim(),
    description: document.getElementById("t_description").value.trim(),
    dueDate: document.getElementById("t_dueDate").value,
    priority: document.getElementById("t_priority").value
  };
  const id = document.getElementById("t_id").value;

  const btn = document.getElementById("taskSaveBtn");
  btn.disabled = true; btn.textContent = "Saving…";

  try {
    if (id) await TaskApi.update(id, dto);
    else await TaskApi.create(dto);
    closeTaskModal();
    loadBoard();
  } catch (err) {
    taskError.textContent = err.message || "Could not save task.";
    taskError.classList.add("show");
  } finally {
    btn.disabled = false; btn.textContent = "Save task";
  }
});

/* ---------------- Task Detail ---------------- */
const detailModal = document.getElementById("detailModal");
let currentTask = null;

async function openTaskDetail(id) {
  try {
    const task = await TaskApi.get(id);
    currentTask = task;
    renderDetail(task);
    detailModal.classList.add("show");
    loadCommitments(id);
    loadAssignOptions(task.id);
    document.getElementById("completeTaskBtn").style.display = task.status === "COMPLETED" ? "none" : "inline-flex";
  } catch (err) {
    alert("Could not load task: " + err.message);
  }
}

function renderDetail(task) {
  document.getElementById("detailTitle").textContent = task.title;
  document.getElementById("detailSubtitle").textContent = `Task in ${task.projectName || "your project"}`;
  document.getElementById("detailDescription").textContent = task.description || "No description provided.";
  document.getElementById("detailMetaRow").innerHTML = `
    <div class="meta-cell"><div class="lbl">Due date</div><div class="val">${fmtDate(task.dueDate)}</div></div>
    <div class="meta-cell"><div class="lbl">Priority</div><div class="val">${task.priority}</div></div>
    <div class="meta-cell"><div class="lbl">Status</div><div class="val"><span class="status-pill status-${task.status}">${task.status.replace("_"," ")}</span></div></div>
    <div class="meta-cell"><div class="lbl">Assigned to</div><div class="val">${(task.employee||[]).join(", ") || "Nobody yet"}</div></div>
  `;
}

async function loadCommitments(taskId) {
  const el = document.getElementById("commitTimeline");
  const tag = document.getElementById("commitCountTag");
  el.innerHTML = `<div class="empty-note">Loading…</div>`;
  tag.textContent = "";
  try {
    const page = await CommitmentApi.ofTask(taskId, 0, 20);
    const items = page.content || [];
    tag.textContent = items.length ? `(${items.length})` : "";
    el.innerHTML = items.length ? items.map(c => `
      <div class="commit-item">
        <div class="c-text">${escapeHtml(c.commitment)}</div>
        <div class="c-meta">👤 ${escapeHtml(c.employeeName)} &nbsp;·&nbsp; 🕒 ${escapeHtml(c.createdAt)}</div>
      </div>
    `).join("") : `<div class="empty-note">No activity logged on this task yet.</div>`;
  } catch (err) {
    el.innerHTML = `<div class="empty-note">Could not load activity: ${escapeHtml(err.message)}</div>`;
  }
}

async function loadAssignOptions(taskId) {
  const select = document.getElementById("assignEmailSelect");
  select.innerHTML = `<option value="">Loading employees…</option>`;
  try {
    const emails = await EmployeeApi.getAllEmails(taskId);
    select.innerHTML = emails.length
      ? emails.map(e => `<option value="${escapeHtml(e)}">${escapeHtml(e)}</option>`).join("")
      : `<option value="">Everyone in the project is already assigned</option>`;
  } catch (err) {
    select.innerHTML = `<option value="">Could not load (${escapeHtml(err.message)})</option>`;
  }
}

document.getElementById("assignBtn").addEventListener("click", async () => {
  const email = document.getElementById("assignEmailSelect").value;
  if (!email || !currentTask) return;
  try {
    await TaskApi.assign(currentTask.id, email);
    alert("Assigned!");
    openTaskDetail(currentTask.id);
    loadBoard();
  } catch (err) {
    alert("Could not assign: " + err.message);
  }
});

document.getElementById("completeTaskBtn").addEventListener("click", async () => {
  if (!currentTask) return;
  try {
    await TaskApi.markCompleted(currentTask.id);
    closeDetailModal();
    loadBoard();
  } catch (err) {
    alert("Could not mark complete: " + err.message);
  }
});

document.getElementById("editTaskBtn").addEventListener("click", () => {
  if (!currentTask) return;

  const taskToEdit = currentTask;   // grab a safe copy BEFORE anything clears currentTask

  closeDetailModal();
  document.getElementById("taskModalTitle").textContent = "Edit task";
  document.getElementById("t_id").value = taskToEdit.id;
  document.getElementById("t_title").value = taskToEdit.title;
  document.getElementById("t_description").value = taskToEdit.description;
  document.getElementById("t_dueDate").value = taskToEdit.dueDate;
  document.getElementById("t_priority").value = taskToEdit.priority;
  taskError.classList.remove("show");
  taskModal.classList.add("show");
});

document.getElementById("deleteTaskBtn").addEventListener("click", async () => {
  if (!currentTask) return;
  if (!confirm(`Delete task "${currentTask.title}"?`)) return;
  try {
    await TaskApi.remove(currentTask.id);
    closeDetailModal();
    loadBoard();
  } catch (err) {
    alert("Could not delete: " + err.message);
  }
});

function closeDetailModal() { detailModal.classList.remove("show"); currentTask = null; }

/* ---------------- Profile ---------------- */
const profileForm = document.getElementById("profileForm");
const profileError = document.getElementById("profileError");
const profileSuccess = document.getElementById("profileSuccess");
let profileLoaded = false;

async function loadProfile() {
  if (profileLoaded) return;
  try {
    const emp = await EmployeeApi.getMyProfile();
    document.getElementById("pr_email").value = emp.email;
    document.getElementById("pr_name").value = emp.name;
    document.getElementById("pr_phone").value = emp.phone;
    document.getElementById("pr_qualification").value = emp.qualification;
    document.getElementById("pr_specialization").value = emp.specialization;
    profileLoaded = true;
  } catch (err) {
    profileError.textContent = "Could not load profile: " + err.message;
    profileError.classList.add("show");
  }
}

profileForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  profileError.classList.remove("show");
  profileSuccess.classList.remove("show");

  const dto = {
    name: document.getElementById("pr_name").value.trim(),
    email: document.getElementById("pr_email").value,
    password: "________",  // backend requires the field to be present; not changed here
    phone: document.getElementById("pr_phone").value.trim(),
    qualification: document.getElementById("pr_qualification").value.trim(),
    specialization: document.getElementById("pr_specialization").value.trim(),
    companyCode: ""
  };

  const btn = document.getElementById("profileSaveBtn");
  btn.disabled = true; btn.textContent = "Saving…";
  try {
    await EmployeeApi.updateProfile(dto);
    profileSuccess.textContent = "Profile updated!";
    profileSuccess.classList.add("show");
  } catch (err) {
    profileError.textContent = err.message || "Could not update profile.";
    profileError.classList.add("show");
  } finally {
    btn.disabled = false; btn.textContent = "Save changes";
  }
});

loadBoard();
