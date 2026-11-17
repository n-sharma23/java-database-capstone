// adminDashboard.js: lists, filters, adds and deletes doctors on the admin dashboard.
import { openModal } from "./components/modals.js";
import { getDoctors, filterDoctors, saveDoctor } from "./services/doctorServices.js";
import { createDoctorCard } from "./components/doctorCard.js";

// The header's "Add Doctor" button is rendered by a classic script, so it reaches openModal through window.
window.openModal = openModal;

document.addEventListener("DOMContentLoaded", () => {
  loadDoctorCards();
  document.getElementById("searchBar").addEventListener("input", filterDoctorsOnChange);
  document.getElementById("filterTime").addEventListener("change", filterDoctorsOnChange);
  document.getElementById("filterSpecialty").addEventListener("change", filterDoctorsOnChange);
});

async function loadDoctorCards() {
  try {
    renderDoctorCards(await getDoctors());
  } catch (error) {
    console.error("Failed to load doctors:", error);
  }
}

async function filterDoctorsOnChange() {
  const name = document.getElementById("searchBar").value.trim() || null;
  const time = document.getElementById("filterTime").value || null;
  const specialty = document.getElementById("filterSpecialty").value || null;
  try {
    const { doctors } = await filterDoctors(name, time, specialty);
    if (doctors && doctors.length > 0) {
      renderDoctorCards(doctors);
    } else {
      const content = document.getElementById("content");
      content.innerHTML = "";
      const message = document.createElement("p");
      message.textContent = "No doctors found with the given filters.";
      content.appendChild(message);
    }
  } catch (error) {
    alert("❌ An error occurred while filtering doctors.");
    console.error(error);
  }
}

function renderDoctorCards(doctors) {
  const content = document.getElementById("content");
  content.innerHTML = "";
  doctors.forEach((doctor) => content.appendChild(createDoctorCard(doctor)));
}

window.adminAddDoctor = async function () {
  const token = localStorage.getItem("token");
  if (!token) {
    alert("Your session has expired. Please log in again.");
    return;
  }
  const doctor = {
    name: document.getElementById("doctorName").value.trim(),
    specialty: document.getElementById("specialization").value,
    email: document.getElementById("doctorEmail").value.trim(),
    password: document.getElementById("doctorPassword").value,
    phone: document.getElementById("doctorPhone").value.trim(),
    availableTimes: Array.from(document.querySelectorAll('input[name="availability"]:checked')).map((cb) => cb.value),
  };

  const { success, message } = await saveDoctor(doctor, token);
  if (success) {
    alert("✅ Doctor added successfully!");
    document.getElementById("modal").style.display = "none";
    window.location.reload();
  } else {
    alert("❌ " + (message || "Could not add doctor."));
  }
};
