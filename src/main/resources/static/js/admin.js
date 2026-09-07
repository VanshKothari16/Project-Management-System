requireAuth(["ADMIN"]);

const listWrap = document.getElementById("listWrap");
const sidebarUser = document.getElementById("sidebarUser");

const tokenInfo = Auth.decode(Auth.getToken());
sidebarUser.textContent = tokenInfo && tokenInfo.sub ? tokenInfo.sub : "Admin";

let currentProject = null;

async function loadProjects() {
  try {
    const projects = await ProjectApi.listAll();
    renderList(projects);
  } catch (err) {
    listWrap.innerHTML = `<div class="empty-note">Could not load projects: ${escapeHtml(err.message)}</div>`;
  }
}

function renderList(projects) {
  if (!projects.length) {
    listWrap.innerHTML = `<div class="ledger-list"><div class="empty-note">No projects yet. Use the + button to create your first one.</div></div>`;
    return;
  }
  listWrap.innerHTML = `<div class="ledger-list">${projects.map(p => `
    <div class="ledger-row" data-id="${escapeHtml(p.id)}">
      <div>
        <div class="title">${escapeHtml(p.name)}</div>
        <div class="desc">Managed by ${escapeHtml(p.manager || "—")} · Due ${fmtDate(p.deadline)}</div>
      </div>
      <span class="status-pill status-${p.status}">${p.status.replace("_"," ")}</span>
    </div>
  `).join("")}</div>`;

  listWrap.querySelectorAll(".ledger-row").forEach(row => {
    row.addEventListener("click", () => openDetail(row.dataset.id));
  });
}

/* ---------------- Add / Edit Project Modal ---------------- */
const projectModal = document.getElementById("projectModal");
const projectForm = document.getElementById("projectForm");
const projectError = document.getElementById("projectError");

document.getElementById("addProjectFab").addEventListener("click", () => {
  document.getElementById("projectModalTitle").textContent = "New project";
  projectForm.reset();
  document.getElementById("p_id").value = "";
  projectError.classList.remove("show");
  projectModal.classList.add("show");
});

function closeProjectModal() { projectModal.classList.remove("show"); }

projectForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  projectError.classList.remove("show");

  const dto = {
    name: document.getElementById("p_name").value.trim(),
    description: document.getElementById("p_description").value.trim(),
    deadline: document.getElementById("p_deadline").value,
    manager_Id: document.getElementById("p_manager").value.trim()
  };
  const id = document.getElementById("p_id").value;

  const btn = document.getElementById("projectSaveBtn");
  btn.disabled = true; btn.textContent = "Saving…";

  try {
    if (id) await ProjectApi.update(id, dto);
    else await ProjectApi.create(dto);
    closeProjectModal();
    loadProjects();
  } catch (err) {
    projectError.textContent = err.message || "Could not save project.";
    projectError.classList.add("show");
  } finally {
    btn.disabled = false; btn.textContent = "Save project";
  }
});

/* ---------------- Project Detail Modal ---------------- */
const detailModal = document.getElementById("detailModal");

function daysRemaining(deadline) {
  if (!deadline) return null;
  const diff = Math.ceil((new Date(deadline) - new Date()) / (1000 * 60 * 60 * 24));
  return diff;
}

async function openDetail(id) {
  try {
    const project = await ProjectApi.get(id);
    currentProject = project;

    document.getElementById("detailTitle").textContent = project.name;
    document.getElementById("detailSubtitle").textContent = `Project · ${project.companyName || ""}`;
    document.getElementById("detailDescription").textContent = project.description || "No description provided.";

    document.getElementById("detailMetaRow").innerHTML = `
      <div class="meta-cell"><div class="lbl">Manager</div><div class="val">${escapeHtml(project.manager || "Not appointed")}</div></div>
      <div class="meta-cell"><div class="lbl">Deadline</div><div class="val">${fmtDate(project.deadline)}</div></div>
      <div class="meta-cell"><div class="lbl">Status</div><div class="val"><span class="status-pill status-${project.status}">${project.status.replace("_"," ")}</span></div></div>
      <div class="meta-cell"><div class="lbl">Company</div><div class="val">${escapeHtml(project.companyName)}</div></div>
    `;

    detailModal.classList.add("show");
    loadDashboardStats(project.id, project.deadline);
  } catch (err) {
    alert("Could not load project: " + err.message);
  }
}

async function loadDashboardStats(projectId, deadline) {
  const wrap = document.getElementById("dashboardWrap");
  wrap.innerHTML = `<div class="spinner-line">Loading stats…</div>`;
  try {
    const stats = await ProjectApi.dashboard(projectId);
    const remaining = daysRemaining(deadline);
    const remainingLabel = remaining === null ? "—"
      : remaining < 0 ? `${Math.abs(remaining)}d overdue`
      : `${remaining}d left`;

    wrap.innerHTML = `
      <div class="stat-row" style="margin-bottom:0;">
        <div class="stat-cell" style="cursor:pointer;" onclick="openDrillList('managers')"><div class="num">${stats.totalManager ?? "—"}</div><div class="lbl">Manager</div></div>
        <div class="stat-cell" style="cursor:pointer;" onclick="openDrillList('employees')"><div class="num">${stats.totalEmployee ?? "—"}</div><div class="lbl">Employees</div></div>
        <div class="stat-cell" style="cursor:pointer;" onclick="openDrillList('TO_DO')"><div class="num">${stats.toDoTask ?? "—"}</div><div class="lbl">To-Do</div></div>
        <div class="stat-cell accent" style="cursor:pointer;" onclick="openDrillList('IN_PROGRESS')"><div class="num">${stats.inProgressTask ?? "—"}</div><div class="lbl">In Progress</div></div>
        <div class="stat-cell success" style="cursor:pointer;" onclick="openDrillList('COMPLETED')"><div class="num">${stats.completedTask ?? "—"}</div><div class="lbl">Completed</div></div>
        <div class="stat-cell danger" style="cursor:pointer;" onclick="openDrillList('OVERDUE')"><div class="num">${stats.overDueTask ?? "—"}</div><div class="lbl">Overdue</div></div>
        <div class="stat-cell"><div class="num">${remainingLabel}</div><div class="lbl">Deadline</div></div>
      </div>
    `;
  } catch (err) {
    wrap.innerHTML = `<div class="empty-note">Team &amp; task stats aren't available right now (${escapeHtml(err.message)}).</div>`;
  }
}

/* ---------------- Drill-down: task/people lists ---------------- */
const drillListModal = document.getElementById("drillListModal");
const taskViewModal = document.getElementById("taskViewModal");
const employeeViewModal = document.getElementById("employeeViewModal");

const DRILL_LABELS = {
  TO_DO: "To-Do tasks",
  IN_PROGRESS: "In-Progress tasks",
  COMPLETED: "Completed tasks",
  OVERDUE: "Overdue tasks",
  managers: "Manager",
  employees: "Employees"
};

async function openDrillList(kind) {
  if (!currentProject) return;
  document.getElementById("drillListTitle").textContent = DRILL_LABELS[kind] || kind;
  document.getElementById("drillListSubtitle").textContent = currentProject.name;
  const body = document.getElementById("drillListBody");
  body.innerHTML = `<div class="spinner-line">Loading…</div>`;
  drillListModal.classList.add("show");

  try {
    if (kind === "managers") {
      const manager = await ProjectApi.manager(currentProject.id);
      renderEmployeeList(body, manager ? [manager] : []);
    } else if (kind === "employees") {
      const employees = await ProjectApi.employees(currentProject.id);
      renderEmployeeList(body, employees);
    } else {
      const tasks = await ProjectApi.tasksByStatus(currentProject.id, kind);
      renderTaskList(body, tasks);
    }
  } catch (err) {
    body.innerHTML = `<div class="empty-note">Could not load: ${escapeHtml(err.message)}</div>`;
  }
}

function renderTaskList(body, tasks) {
  if (!tasks.length) { body.innerHTML = `<div class="empty-note">Nothing here.</div>`; return; }
  body.innerHTML = `<div class="ledger-list">${tasks.map(t => `
    <div class="ledger-row" data-id="${escapeHtml(t.id)}">
      <div><div class="title">${escapeHtml(t.title)}</div><div class="desc">Due ${fmtDate(t.dueDate)} · ${escapeHtml(t.priority)}</div></div>
      <span class="status-pill status-${t.status}">${t.status.replace("_"," ")}</span>
    </div>`).join("")}</div>`;
  body.querySelectorAll(".ledger-row").forEach(row => {
    row.addEventListener("click", () => openTaskView(row.dataset.id));
  });
}

function renderEmployeeList(body, employees) {
  if (!employees.length) { body.innerHTML = `<div class="empty-note">Nobody here yet.</div>`; return; }
  body.innerHTML = `<div class="ledger-list">${employees.map((e, i) => `
    <div class="ledger-row" data-index="${i}">
      <div><div class="title">${escapeHtml(e.name)}</div><div class="desc">${escapeHtml(e.email)}</div></div>
      <span class="status-pill" style="background:var(--parchment-2); color:var(--slate);">${escapeHtml(e.role || "")}</span>
    </div>`).join("")}</div>`;
  body.querySelectorAll(".ledger-row").forEach(row => {
    row.addEventListener("click", () => openEmployeeView(employees[Number(row.dataset.index)]));
  });
}

function closeDrillListModal() { drillListModal.classList.remove("show"); }

/* ---------------- Read-only task view (with activity log) ---------------- */
async function openTaskView(taskId) {
  try {
    const task = await TaskApi.get(taskId);
    document.getElementById("taskViewTitle").textContent = task.title;
    document.getElementById("taskViewSubtitle").textContent = `Task in ${task.projectName || currentProject.name}`;
    document.getElementById("taskViewDescription").textContent = task.description || "No description provided.";
    document.getElementById("taskViewMetaRow").innerHTML = `
      <div class="meta-cell"><div class="lbl">Due date</div><div class="val">${fmtDate(task.dueDate)}</div></div>
      <div class="meta-cell"><div class="lbl">Priority</div><div class="val">${task.priority}</div></div>
      <div class="meta-cell"><div class="lbl">Status</div><div class="val"><span class="status-pill status-${task.status}">${task.status.replace("_"," ")}</span></div></div>
      <div class="meta-cell"><div class="lbl">Assigned to</div><div class="val">${(task.employee||[]).join(", ") || "Nobody yet"}</div></div>
    `;
    taskViewModal.classList.add("show");
    loadTaskViewCommitments(taskId);
  } catch (err) {
    alert("Could not load task: " + err.message);
  }
}

async function loadTaskViewCommitments(taskId) {
  const el = document.getElementById("taskViewTimeline");
  const tag = document.getElementById("taskViewCommitCount");
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
      </div>`).join("") : `<div class="empty-note">No activity logged on this task yet.</div>`;
  } catch (err) {
    el.innerHTML = `<div class="empty-note">Could not load activity: ${escapeHtml(err.message)}</div>`;
  }
}

function closeTaskViewModal() { taskViewModal.classList.remove("show"); }

/* ---------------- Employee view (no extra API call — reuses list data) ---------------- */
function openEmployeeView(emp) {
  document.getElementById("employeeViewTitle").textContent = emp.name;
  document.getElementById("employeeViewMetaRow").innerHTML = `
    <div class="meta-cell"><div class="lbl">Email</div><div class="val">${escapeHtml(emp.email)}</div></div>
    <div class="meta-cell"><div class="lbl">Phone</div><div class="val">${escapeHtml(emp.phone)}</div></div>
    <div class="meta-cell"><div class="lbl">Role</div><div class="val">${escapeHtml(emp.role)}</div></div>
    <div class="meta-cell"><div class="lbl">Qualification</div><div class="val">${escapeHtml(emp.qualification || "—")}</div></div>
    <div class="meta-cell"><div class="lbl">Specialization</div><div class="val">${escapeHtml(emp.specialization || "—")}</div></div>
    <div class="meta-cell"><div class="lbl">Company</div><div class="val">${escapeHtml(emp.company || "—")}</div></div>
  `;
  employeeViewModal.classList.add("show");
}

function closeEmployeeViewModal() { employeeViewModal.classList.remove("show"); }

function closeDetailModal() { detailModal.classList.remove("show"); currentProject = null; }

document.getElementById("editProjectBtn").addEventListener("click", () => {
  if (!currentProject) return;
  closeDetailModal();
  document.getElementById("projectModalTitle").textContent = "Edit project";
  document.getElementById("p_id").value = currentProject.id;
  document.getElementById("p_name").value = currentProject.name;
  document.getElementById("p_description").value = currentProject.description;
  document.getElementById("p_deadline").value = currentProject.deadline;
  document.getElementById("p_manager").value = "";
  projectError.classList.remove("show");
  projectModal.classList.add("show");
});

document.getElementById("deleteProjectBtn").addEventListener("click", async () => {
  if (!currentProject) return;
  if (!confirm(`Delete project "${currentProject.name}"? This cannot be undone.`)) return;
  try {
    await ProjectApi.remove(currentProject.id);
    closeDetailModal();
    loadProjects();
  } catch (err) {
    alert("Could not delete project: " + err.message);
  }
});

loadProjects();
