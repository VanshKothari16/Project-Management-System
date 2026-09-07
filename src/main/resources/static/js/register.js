const tabEmployee = document.getElementById("tabEmployee");
const tabCompany = document.getElementById("tabCompany");
const employeeForm = document.getElementById("employeeForm");
const companyForm = document.getElementById("companyForm");
const errorBox = document.getElementById("errorBox");
const successBox = document.getElementById("successBox");

function clearAlerts() {
  errorBox.classList.remove("show");
  successBox.classList.remove("show");
}

tabEmployee.addEventListener("click", () => {
  tabEmployee.classList.add("active");
  tabCompany.classList.remove("active");
  employeeForm.style.display = "block";
  companyForm.style.display = "none";
  clearAlerts();
});

tabCompany.addEventListener("click", () => {
  tabCompany.classList.add("active");
  tabEmployee.classList.remove("active");
  companyForm.style.display = "block";
  employeeForm.style.display = "none";
  clearAlerts();
});

/* ---------------- Employee registration ----------------
   Maps 1:1 to EmployeeRequestDto (extends UserRequestDto):
   name, email, password, phone, qualification, specialization, companyCode
---------------------------------------------------------- */
employeeForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  clearAlerts();

  const dto = {
    name: document.getElementById("e_name").value.trim(),
    email: document.getElementById("e_email").value.trim(),
    password: document.getElementById("e_password").value,
    phone: document.getElementById("e_phone").value.trim(),
    qualification: document.getElementById("e_qualification").value.trim(),
    specialization: document.getElementById("e_specialization").value.trim(),
    companyCode: document.getElementById("e_companyCode").value.trim()
  };

  if (!/^[0-9]{10}$/.test(dto.phone)) {
    errorBox.textContent = "Phone number must be exactly 10 digits.";
    errorBox.classList.add("show");
    return;
  }
  if (dto.password.length < 8 || dto.password.length > 14) {
    errorBox.textContent = "Password must be between 8 and 14 characters.";
    errorBox.classList.add("show");
    return;
  }

  const btn = document.getElementById("employeeBtn");
  btn.disabled = true; btn.textContent = "Creating account…";

  try {
    await EmployeeApi.register(dto);
    successBox.textContent = "Account created! You can now sign in.";
    successBox.classList.add("show");
    setTimeout(() => window.location.href = "index.html", 1200);
  } catch (err) {
    errorBox.textContent = err.message || "Could not create account.";
    errorBox.classList.add("show");
  } finally {
    btn.disabled = false; btn.textContent = "Create employee account";
  }
});

/* ---------------- Company (Admin) registration ----------------
   Backend controller uses @ModelAttribute, so this MUST be sent
   as form data (handled inside CompanyApi.register), split across
   UserRequestDto: name, email, password, phone
   CompanyRequestDto: companyName, occupation, address
------------------------------------------------------------------ */
companyForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  clearAlerts();

  const userRequestDto = {
    name: document.getElementById("c_name").value.trim(),
    email: document.getElementById("c_email").value.trim(),
    password: document.getElementById("c_password").value,
    phone: document.getElementById("c_phone").value.trim()
  };
  const companyRequestDto = {
    companyName: document.getElementById("c_companyName").value.trim(),
    occupation: document.getElementById("c_occupation").value.trim(),
    address: document.getElementById("c_address").value.trim()
  };

  if (!/^[0-9]{10}$/.test(userRequestDto.phone)) {
    errorBox.textContent = "Phone number must be exactly 10 digits.";
    errorBox.classList.add("show");
    return;
  }
  if (userRequestDto.password.length < 8 || userRequestDto.password.length > 14) {
    errorBox.textContent = "Password must be between 8 and 14 characters.";
    errorBox.classList.add("show");
    return;
  }

  const btn = document.getElementById("companyBtn");
  btn.disabled = true; btn.textContent = "Registering company…";

  try {
    const company = await CompanyApi.register(userRequestDto, companyRequestDto);
    successBox.innerHTML = `Company registered! Your Company ID (share this with employees): <b>${escapeHtml(company.id)}</b>. Redirecting to sign in…`;
    successBox.classList.add("show");
    setTimeout(() => window.location.href = "index.html", 3500);
  } catch (err) {
    errorBox.textContent = err.message || "Could not register company.";
    errorBox.classList.add("show");
  } finally {
    btn.disabled = false; btn.textContent = "Register company & become Admin";
  }
});
