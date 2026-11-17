// doctorDashboard.js: shows the logged-in doctor's appointments for a chosen date.
import { getAllAppointments } from "./services/appointmentRecordService.js";
import { createPatientRow } from "./components/patientRows.js";

const tableBody = document.getElementById("patientTableBody");
const token = localStorage.getItem("token");
let selectedDate = new Date().toISOString().split("T")[0];
let patientName = null;

document.getElementById("searchBar").addEventListener("input", (e) => {
  const value = e.target.value.trim();
  patientName = value !== "" ? value : "null";
  loadAppointments();
});

document.getElementById("todayButton").addEventListener("click", () => {
  selectedDate = new Date().toISOString().split("T")[0];
  document.getElementById("datePicker").value = selectedDate;
  loadAppointments();
});

document.getElementById("datePicker").addEventListener("change", (e) => {
  selectedDate = e.target.value;
  loadAppointments();
});

function messageRow(text) {
  const row = document.createElement("tr");
  const cell = document.createElement("td");
  cell.colSpan = 5;
  cell.className = "noPatientRecord";
  cell.textContent = text;
  row.appendChild(cell);
  return row;
}

async function loadAppointments() {
  try {
    const { appointments } = await getAllAppointments(selectedDate, encodeURIComponent(patientName ?? "null"), token);
    tableBody.innerHTML = "";
    if (!appointments || appointments.length === 0) {
      tableBody.appendChild(messageRow("No Appointments found for today."));
      return;
    }
    appointments.forEach((appointment) => {
      const patient = {
        id: appointment.patientId,
        name: appointment.patientName,
        phone: appointment.patientPhone,
        email: appointment.patientEmail,
      };
      tableBody.appendChild(createPatientRow(patient, appointment.id, appointment.doctorId));
    });
  } catch (error) {
    console.error("Error loading appointments:", error);
    tableBody.innerHTML = "";
    tableBody.appendChild(messageRow("Error loading appointments. Try again later."));
  }
}

document.addEventListener("DOMContentLoaded", () => {
  document.getElementById("datePicker").value = selectedDate;
  renderContent();
  loadAppointments();
});
