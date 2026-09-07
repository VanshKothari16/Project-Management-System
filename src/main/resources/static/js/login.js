const form = document.getElementById("loginForm");
const errorBox = document.getElementById("errorBox");
const loginBtn = document.getElementById("loginBtn");

// If already logged in, skip straight to the right dashboard.
if (Auth.isLoggedIn()) {
  routeToDashboard(Auth.getRole());
}

function routeToDashboard(role) {
  if (role === "ADMIN") window.location.href = "admin.html";
  else if (role === "MANAGER") window.location.href = "manager.html";
  else window.location.href = "employee.html";
}

function roleFromToken(token) {
  const payload = Auth.decode(token);
  if (!payload || !payload.authorities) return "EMPLOYEE";
  const authorities = Array.isArray(payload.authorities) ? payload.authorities : Object.values(payload.authorities);
  if (authorities.includes("ROLE_ADMIN")) return "ADMIN";
  if (authorities.includes("ROLE_MANAGER")) return "MANAGER";
  return "EMPLOYEE";
}

form.addEventListener("submit", async (e) => {
  e.preventDefault();
  errorBox.classList.remove("show");

  const username = document.getElementById("username").value.trim();
  const password = document.getElementById("password").value;

  if (!username || !password) {
    errorBox.textContent = "Please fill in both fields.";
    errorBox.classList.add("show");
    return;
  }

  loginBtn.disabled = true;
  loginBtn.textContent = "Signing in…";

  try {
    const token = await UserApi.login({ username, password });
    Auth.saveToken(token);
    Auth.saveRole(roleFromToken(token));
    routeToDashboard(Auth.getRole());
  } catch (err) {
    errorBox.textContent = err.message || "Invalid email or password.";
    errorBox.classList.add("show");
  } finally {
    loginBtn.disabled = false;
    loginBtn.textContent = "Sign in";
  }
});
