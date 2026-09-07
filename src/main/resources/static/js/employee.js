requireAuth(["EMPLOYEE", "MANAGER"]);

const sidebarUser = document.getElementById("sidebarUser");
const tokenInfo = Auth.decode(Auth.getToken());
sidebarUser.textContent = tokenInfo && tokenInfo.sub ? tokenInfo.sub : "Employee";

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

/* ---------------- Board ---------------- */
function taskCardHtml(task) {
  return `
    <div class="task-card p-${task.priority}" data-id="${escapeHtml(task.id)}">
      <div class="t-title">${escapeHtml(task.title)}</div>
      <div class="t-meta"><span>Due ${fmtDate(task.dueDate)}</span><span>${escapeHtml(task.projectName || "")}</span></div>
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
    const [todo, inProgress, completed] = await Promise.all([
      TaskApi.myTodo(), TaskApi.myInProgress(), TaskApi.myCompleted()
    ]);
    renderColumn("col-todo", "count-todo", todo);
    renderColumn("col-inprogress", "count-inprogress", inProgress);
    renderColumn("col-completed", "count-completed", completed);
  } catch (err) {
    ["col-todo", "col-inprogress", "col-completed"].forEach(id => {
      document.getElementById(id).innerHTML = `<div class="empty-note">Error: ${escapeHtml(err.message)}</div>`;
    });
  }
}

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
    // Per the product flow: a Completed task is read-only — no commitment form shown.
    document.getElementById("commitFormWrap").style.display = task.status === "COMPLETED" ? "none" : "block";
    document.getElementById("commitText").value = "";
  } catch (err) {
    alert("Could not load task: " + err.message);
  }
}

function renderDetail(task) {
  document.getElementById("detailTitle").textContent = task.title;
  document.getElementById("detailSubtitle").textContent = `Task in ${task.projectName || "your project"}`;
  document.getElementById("detailDescription").textContent = task.description || "No description provided.";
  document.getElementById("detailMetaRow").innerHTML = `
    <div class="meta-cell"><div class="lbl">Project</div><div class="val">${escapeHtml(task.projectName)}</div></div>
    <div class="meta-cell"><div class="lbl">Due date</div><div class="val">${fmtDate(task.dueDate)}</div></div>
    <div class="meta-cell"><div class="lbl">Priority</div><div class="val">${task.priority}</div></div>
    <div class="meta-cell"><div class="lbl">Status</div><div class="val"><span class="status-pill status-${task.status}">${task.status.replace("_"," ")}</span></div></div>
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

    const myEmail = (Auth.decode(Auth.getToken()) || {}).sub || "";

    el.innerHTML = items.length ? items.map(c => {
      const isMine = c.employeeEmail && c.employeeEmail === myEmail;
      return `
      <div class="commit-item" data-id="${escapeHtml(c.id)}">
        <div class="c-text" data-role="text">${escapeHtml(c.commitment)}</div>
        <div class="c-meta">
          👤 ${escapeHtml(c.employeeName)} &nbsp;·&nbsp; 🕒 ${escapeHtml(c.createdAt)}
          ${isMine ? `&nbsp;·&nbsp; <button type="button" class="commit-edit-btn" data-role="editBtn">Edit</button>
                       &nbsp;·&nbsp; <button type="button" class="commit-delete-btn" data-role="deleteBtn">Delete</button>` : ``}
        </div>
      </div>`;
    }).join("") : `<div class="empty-note">No commitments yet — add the first one below.</div>`;

    // Wire up "Edit" buttons only for commitments that belong to the logged-in employee.
    el.querySelectorAll(".commit-edit-btn").forEach(btn => {
      btn.addEventListener("click", () => {
        const item = btn.closest(".commit-item");
        enterCommitEditMode(item, taskId);
      });
    });
    // Wire up "Delete" buttons the same way.
    el.querySelectorAll(".commit-delete-btn").forEach(btn => {
      btn.addEventListener("click", () => {
        const item = btn.closest(".commit-item");
        deleteCommitment(item, taskId);
      });
    });
  } catch (err) {
    el.innerHTML = `<div class="empty-note">Could not load activity: ${escapeHtml(err.message)}</div>`;
  }
}
/**
 * Deletes one of the logged-in employee's own commitments, after a quick
 * confirmation, then refreshes the activity log.
 */
async function deleteCommitment(item, taskId) {
  if (!confirm("Delete this commitment? This cannot be undone.")) return;

  const commitId = item.dataset.id;
  const deleteBtn = item.querySelector('[data-role="deleteBtn"]');
  if (deleteBtn) { deleteBtn.disabled = true; deleteBtn.textContent = "Deleting…"; }

  try {
    await CommitmentApi.remove(commitId);
    await loadCommitments(taskId);
  } catch (err) {
    alert("Could not delete: " + err.message);
    if (deleteBtn) { deleteBtn.disabled = false; deleteBtn.textContent = "Delete"; }
  }
}
/**
 * Swaps one commitment's plain text for an inline textarea + Save/Cancel,
 * so the employee can correct their own entry without leaving the modal.
 */
function enterCommitEditMode(item, taskId) {
  const commitId = item.dataset.id;
  const textEl = item.querySelector('[data-role="text"]');
  const originalText = textEl.textContent;

  item.innerHTML = `
    <div class="field" style="margin-bottom:8px;">
      <textarea data-role="editInput">${escapeHtml(originalText)}</textarea>
    </div>
    <div style="display:flex; gap:8px;">
      <button type="button" class="btn btn-amber btn-sm" data-role="saveBtn">Save</button>
      <button type="button" class="btn btn-ghost btn-sm" data-role="cancelBtn">Cancel</button>
    </div>
  `;

  item.querySelector('[data-role="cancelBtn"]').addEventListener("click", () => {
    loadCommitments(taskId);
  });

  item.querySelector('[data-role="saveBtn"]').addEventListener("click", async (e) => {
    const newText = item.querySelector('[data-role="editInput"]').value.trim();
    if (!newText) return;
    const btn = e.currentTarget;
    btn.disabled = true; btn.textContent = "Saving…";
    try {
      await CommitmentApi.update(commitId, { taskId: taskId, commitment: newText });
      await loadCommitments(taskId);
    } catch (err) {
      alert("Could not save your edit: " + err.message);
      btn.disabled = false; btn.textContent = "Save";
    }
  });
}
document.getElementById("addCommitBtn").addEventListener("click", async () => {
  const text = document.getElementById("commitText").value.trim();
  if (!text || !currentTask) return;
  const btn = document.getElementById("addCommitBtn");
  btn.disabled = true; btn.textContent = "Adding…";
  try {
    await CommitmentApi.add({ taskId: currentTask.id, commitment: text });
    document.getElementById("commitText").value = "";
    // Adding a commitment may move the task from TO_DO -> IN_PROGRESS automatically.
    await openTaskDetail(currentTask.id);
    loadBoard();
  } catch (err) {
    alert("Could not add commitment: " + err.message);
  } finally {
    btn.disabled = false; btn.textContent = "Add commitment";
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
    password: "________",  // backend requires this field to be present but never actually changes it here
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
