// doctorCard.js: builds one doctor card, with actions that depend on the user's role.
import { deleteDoctor } from "../services/doctorServices.js";
import { getPatientData } from "../services/patientServices.js";

// Doctor fields come from the database, so they are set with textContent, never innerHTML.
function textElement(tag, text) {
  const el = document.createElement(tag);
  el.textContent = text;
  return el;
}

export function createDoctorCard(doctor) {
  const card = document.createElement("div");
  card.classList.add("doctor-card");

  const role = localStorage.getItem("userRole");

  const info = document.createElement("div");
  info.classList.add("doctor-info");
  info.append(
    textElement("h3", doctor.name),
    textElement("p", `Specialty: ${doctor.specialty}`),
    textElement("p", `Email: ${doctor.email}`),
    textElement("p", `Available: ${(doctor.availableTimes || []).join(", ")}`),
  );

  const actions = document.createElement("div");
  actions.classList.add("card-actions");

  if (role === "admin") {
    const removeBtn = textElement("button", "Delete");
    removeBtn.addEventListener("click", async () => {
      if (!confirm(`Delete ${doctor.name} and all of their appointments?`)) return;
      const token = localStorage.getItem("token");
      const { success, message } = await deleteDoctor(doctor.id, token);
      alert(message);
      if (success) card.remove();
    });
    actions.appendChild(removeBtn);
  } else if (role === "patient") {
    const bookNow = textElement("button", "Book Now");
    bookNow.addEventListener("click", () => alert("Please log in to book an appointment."));
    actions.appendChild(bookNow);
  } else if (role === "loggedPatient") {
    const bookNow = textElement("button", "Book Now");
    bookNow.addEventListener("click", async (e) => {
      const token = localStorage.getItem("token");
      if (!token) {
        localStorage.setItem("userRole", "patient");
        window.location.href = "/pages/patientDashboard.html";
        return;
      }
      const patient = await getPatientData(token);
      // Loaded on demand: importing loggedPatient.js at the top would run that page's
      // start-up code (loading and filter listeners) on the admin and patient pages too.
      const { showBookingOverlay } = await import("../loggedPatient.js");
      showBookingOverlay(e, doctor, patient);
    });
    actions.appendChild(bookNow);
  }

  card.append(info, actions);
  return card;
}
