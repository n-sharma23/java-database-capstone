// patientRows.js
export function createPatientRow(patient, appointmentId, doctorId) {
  const tr = document.createElement("tr");
  console.log("CreatePatientRow :: ", doctorId)

  // Patient details are user-entered, so they go in with textContent rather than innerHTML.
  const cells = [patient.id, patient.name, patient.phone, patient.email].map((value) => {
    const td = document.createElement("td");
    td.textContent = value;
    return td;
  });
  cells[0].className = "patient-id";

  const actionCell = document.createElement("td");
  const icon = document.createElement("img");
  icon.src = "../assets/images/addPrescriptionIcon/addPrescription.png";
  icon.alt = "addPrescriptionIcon";
  icon.className = "prescription-btn";
  icon.dataset.id = patient.id;
  actionCell.appendChild(icon);

  tr.append(...cells, actionCell);

  cells[0].addEventListener("click", () => {
    window.location.href = `/pages/patientRecord.html?id=${encodeURIComponent(patient.id)}&doctorId=${encodeURIComponent(doctorId)}`;
  });
  icon.addEventListener("click", () => {
    window.location.href = `/pages/addPrescription.html?appointmentId=${encodeURIComponent(appointmentId)}&patientName=${encodeURIComponent(patient.name)}`;
  });

  return tr;
}
