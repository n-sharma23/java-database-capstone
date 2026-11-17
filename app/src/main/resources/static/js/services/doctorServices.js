// doctorServices.js: API calls for listing, filtering, adding and deleting doctors.
import { API_BASE_URL } from "../config/config.js";

const DOCTOR_API = API_BASE_URL + "/doctor";

export async function getDoctors() {
  try {
    const response = await fetch(DOCTOR_API);
    const data = await response.json();
    return data.doctors || [];
  } catch (error) {
    console.error("Error fetching doctors:", error);
    return [];
  }
}

export async function deleteDoctor(id, token) {
  try {
    const response = await fetch(`${DOCTOR_API}/${id}/${token}`, { method: "DELETE" });
    const data = await response.json();
    return { success: response.ok, message: data.message };
  } catch (error) {
    console.error("Error deleting doctor:", error);
    return { success: false, message: "Could not delete doctor. Please try again." };
  }
}

export async function saveDoctor(doctor, token) {
  try {
    const response = await fetch(`${DOCTOR_API}/${token}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(doctor),
    });
    const data = await response.json();
    return { success: response.ok, message: data.message };
  } catch (error) {
    console.error("Error saving doctor:", error);
    return { success: false, message: "Could not save doctor. Please try again." };
  }
}

// Search fires on every keystroke, so responses can arrive out of order. Each new search
// aborts the previous request; an aborted search never settles, so a stale result is never rendered.
let currentFilter = null;

/** name, time (AM/PM) and specialty are optional; missing ones are sent as "null". */
export async function filterDoctors(name, time, specialty) {
  const segment = (value) => encodeURIComponent(value || "null");
  if (currentFilter) currentFilter.abort();
  const controller = new AbortController();
  currentFilter = controller;
  try {
    const response = await fetch(`${DOCTOR_API}/filter/${segment(name)}/${segment(time)}/${segment(specialty)}`,
      { signal: controller.signal });
    if (response.ok) {
      return await response.json();
    }
    console.error("Failed to filter doctors:", response.statusText);
    return { doctors: [] };
  } catch (error) {
    if (error.name === "AbortError") {
      return new Promise(() => {});
    }
    console.error("Error filtering doctors:", error);
    alert("❌ Could not load doctors. Please try again.");
    return { doctors: [] };
  }
}
