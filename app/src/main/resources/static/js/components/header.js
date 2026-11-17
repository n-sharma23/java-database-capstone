// header.js: renders the shared header, with actions that depend on the user's role and session.

function renderHeader() {
  const headerDiv = document.getElementById("header");
  if (!headerDiv) return;

  const logo = `
    <div class="logo-section">
      <img src="../assets/images/logo/logo.png" alt="Hospital CRM Logo" class="logo-img">
      <span class="logo-title">Hospital CMS</span>
    </div>`;

  // The role-selection page starts a fresh session.
  if (window.location.pathname === "/" || window.location.pathname.endsWith("/index.html")) {
    localStorage.removeItem("userRole");
    localStorage.removeItem("token");
    headerDiv.innerHTML = `<header class="header">${logo}</header>`;
    return;
  }

  const role = localStorage.getItem("userRole");
  const token = localStorage.getItem("token");

  if ((role === "loggedPatient" || role === "admin" || role === "doctor") && !token) {
    localStorage.removeItem("userRole");
    alert("Session expired or invalid login. Please log in again.");
    window.location.href = "/";
    return;
  }

  let headerContent = `<header class="header">${logo}<nav>`;

  if (role === "admin") {
    headerContent += `
      <button id="addDocBtn" class="adminBtn">Add Doctor</button>
      <a href="#" id="logoutLink">Logout</a>`;
  } else if (role === "doctor") {
    headerContent += `
      <button id="doctorHome" class="adminBtn">Home</button>
      <a href="#" id="logoutLink">Logout</a>`;
  } else if (role === "patient") {
    headerContent += `
      <button id="patientLogin" class="adminBtn">Login</button>
      <button id="patientSignup" class="adminBtn">Sign Up</button>`;
  } else if (role === "loggedPatient") {
    headerContent += `
      <button id="home" class="adminBtn" onclick="window.location.href='/pages/loggedPatientDashboard.html'">Home</button>
      <button id="patientAppointments" class="adminBtn" onclick="window.location.href='/pages/patientAppointments.html'">Appointments</button>
      <a href="#" id="logoutPatientLink">Logout</a>`;
  }

  headerContent += `</nav></header>`;
  headerDiv.innerHTML = headerContent;
  attachHeaderButtonListeners();
}

function attachHeaderButtonListeners() {
  const addDocBtn = document.getElementById("addDocBtn");
  if (addDocBtn) {
    // openModal is an ES module export, exposed on window by the admin dashboard script.
    addDocBtn.addEventListener("click", () => window.openModal && window.openModal("addDoctor"));
  }
  const doctorHome = document.getElementById("doctorHome");
  if (doctorHome) {
    doctorHome.addEventListener("click", () => selectRole("doctor"));
  }
  const logoutLink = document.getElementById("logoutLink");
  if (logoutLink) {
    logoutLink.addEventListener("click", (e) => { e.preventDefault(); logout(); });
  }
  const logoutPatientLink = document.getElementById("logoutPatientLink");
  if (logoutPatientLink) {
    logoutPatientLink.addEventListener("click", (e) => { e.preventDefault(); logoutPatient(); });
  }
}

function logout() {
  localStorage.removeItem("token");
  localStorage.removeItem("userRole");
  window.location.href = "/";
}

function logoutPatient() {
  localStorage.removeItem("token");
  localStorage.setItem("userRole", "patient");
  window.location.href = "/pages/patientDashboard.html";
}

renderHeader();
