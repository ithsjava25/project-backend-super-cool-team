let employmentCurrentUser = null;
let allEmploymentStaff = [];

async function initEmploymentPage() {
    await renderNavbar();
    const staffListEl = document.getElementById("staffList");
    if (!staffListEl) return;

    try {
        const meRes = await apiFetch("/staff/me");
        if (meRes.ok) {
            employmentCurrentUser = await meRes.json();
        }

        setupEmploymentPageButtons();
        await loadEmploymentStaff();
        await loadPendingEmploymentForms();
    } catch (e) {
        console.error("Could not init employment page", e);
    }
}

function getInitialSsnHtml(id, type) {
    return `
        <div class="ssn-display-row">
            <span class="ssn-label">SSN:</span>
            <div class="ssn-wrapper">
                <span class="ssn-placeholder">••••••-••••</span>
                <button class="btn-ssn-action" onclick="toggleSsnView(${id}, '${type}')">
                    VISA
                </button>
            </div>
        </div>
    `;
}

function setupEmploymentPageButtons() {
    const openBtn = document.getElementById("openCreateEmploymentBtn");
    const cancelBtn = document.getElementById("cancelCreateEmploymentBtn");
    const createSection = document.getElementById("createEmploymentSection");
    const form = document.getElementById("createEmploymentForm");

    const role = employmentCurrentUser?.role || "";
    const canCreate = role === "HR" || role === "ADMIN";
    const canApprove = role === "CEO" || role === "CTO" || role === "ADMIN" || role === "HR";

    if (openBtn && canCreate) {
        openBtn.style.display = "inline-flex";
        openBtn.addEventListener("click", () => {
            createSection.style.display = "block";
            openBtn.style.display = "none";
        });
    }

    if (cancelBtn) {
        cancelBtn.addEventListener("click", () => {
            createSection.style.display = "none";
            if (canCreate && openBtn) {
                openBtn.style.display = "inline-flex";
            }
        });
    }

    if (form) {
        form.addEventListener("submit", submitEmploymentForm);
    }

    if (canApprove) {
        const pendingSection = document.getElementById("pendingFormsSection");
        if (pendingSection) pendingSection.style.display = "block";
    }

    document.getElementById("employmentSearch")?.addEventListener("input", renderEmploymentStaffList);
    document.getElementById("employmentRoleFilter")?.addEventListener("change", renderEmploymentStaffList);
    document.getElementById("employmentDepartmentFilter")?.addEventListener("change", renderEmploymentStaffList);

    document.getElementById("clearEmploymentFiltersBtn")?.addEventListener("click", () => {
        document.getElementById("employmentSearch").value = "";
        document.getElementById("employmentRoleFilter").value = "";
        document.getElementById("employmentDepartmentFilter").value = "";
        renderEmploymentStaffList();
    });
}

async function loadEmploymentStaff() {
    try {
        const res = await apiFetch("/staff");
        if (!res.ok) {
            document.getElementById("staffList").innerHTML = "<p>Kunde inte hämta staff.</p>";
            return;
        }

        allEmploymentStaff = await res.json();
        renderEmploymentStaffList();
    } catch (e) {
        console.error("Error loading employment staff", e);
        document.getElementById("staffList").innerHTML = "<p>Något gick fel när staff skulle hämtas.</p>";
    }
}

function renderEmploymentStaffList() {
    const list = document.getElementById("staffList");
    if (!list) return;

    const search = (document.getElementById("employmentSearch")?.value || "").toLowerCase().trim();
    const roleFilter = document.getElementById("employmentRoleFilter")?.value || "";
    const departmentFilter = document.getElementById("employmentDepartmentFilter")?.value || "";

    const filtered = allEmploymentStaff.filter(staff => {
        const fullName = `${staff.firstName || ""} ${staff.lastName || ""}`.toLowerCase();
        const email = (staff.email || "").toLowerCase();

        const matchesSearch =
            !search ||
            fullName.includes(search) ||
            email.includes(search);

        const matchesRole = !roleFilter || staff.role === roleFilter;
        const matchesDepartment = !departmentFilter || staff.department === departmentFilter;

        return matchesSearch && matchesRole && matchesDepartment;
    });

    if (filtered.length === 0) {
        list.innerHTML = "<p>Inga anställda hittades.</p>";
        return;
    }

    list.innerHTML = filtered.map(staff => `
        <div class="employment-card">
            <div class="employment-card-header">
                <h3>${escapeHtml(staff.fullName || `${staff.firstName} ${staff.lastName}`)}</h3>
                <span class="badge badge-secondary">${escapeHtml(staff.role || "")}</span>
            </div>

            <div class="employment-card-body">
                <p><strong>Email:</strong> ${escapeHtml(staff.email || "-")}</p>
                <p><strong>Phone:</strong> ${escapeHtml(staff.phoneNumber || "-")}</p>
                <p><strong>Department:</strong> ${escapeHtml(staff.department || "-")}</p>
                <p><strong>Status:</strong> ${escapeHtml(staff.status || "OFFLINE")}</p>
                
            <div id="ssn-container-${staff.id}" class="ssn-display">
                    ${getInitialSsnHtml(staff.id, 'staff')}
            </div>
            </div>
        </div>
    `).join("");
}

async function toggleSsnView(id, type) {
    // Bestäm prefix och endpoint baserat på typen
    const prefix = (type === 'staff') ? 'ssn' : 'form-ssn';
    const endpoint = (type === 'staff') ? 'staff' : 'forms';

    const container = document.getElementById(`${prefix}-container-${id}`);
    if (!container) return;

    container.innerHTML = "<em>Laddar...</em>";

    try {
        const res = await apiFetch(`/${endpoint}/${id}`);
        if (!res.ok) throw new Error("Behörighet saknas");

        const data = await res.json();

        container.innerHTML = `
    <div class="ssn-display-row">
        <span class="ssn-label">SSN:</span>
        <div class="ssn-wrapper">
            <strong class="ssn-value">${escapeHtml(data.socialSecurityNumber)}</strong>
            <button class="btn-ssn-action active" onclick="resetSsnView(${id}, '${type}')">
                DÖLJ
            </button>
        </div>
    </div>
`;

        // Maskera automatiskt efter 10 sekunder
        setTimeout(() => resetSsnView(id, type), 10000);

    } catch (e) {
        container.innerHTML = "<span class='text-danger'>Kunde inte hämta data.</span>";
        console.error(e);
    }
}

function resetSsnView(id, type) {
    const prefix = (type === 'staff') ? 'ssn' : 'form-ssn';
    const container = document.getElementById(`${prefix}-container-${id}`);
    if (container) {
        container.innerHTML = getInitialSsnHtml(id, type);
    }
}

async function submitEmploymentForm(e) {
    e.preventDefault();

    const message = document.getElementById("employmentMessage");
    if (message) message.textContent = "Sparar...";

    const body = {
        socialSecurityNumber: document.getElementById("socialSecurityNumber").value.trim(),
        firstName: document.getElementById("firstName").value.trim(),
        lastName: document.getElementById("lastName").value.trim(),
        email: document.getElementById("email").value.trim(),
        phoneNumber: document.getElementById("phoneNumber").value.trim(),
        role: document.getElementById("role").value,
        department: document.getElementById("department").value
    };

    try {
        const res = await apiFetch("/forms/employment", {
            method: "POST",
            body: JSON.stringify(body)
        });

        if (res.ok) {
            document.getElementById("createEmploymentForm").reset();
            if (message) message.textContent = "Staff-formuläret skapades och väntar nu på approval.";
            await loadPendingEmploymentForms();
        } else {
            const err = await res.json().catch(() => ({}));
            if (message) {
                message.textContent = "Kunde inte skapa staff-form: " + (err.message || err.error || res.statusText);
            }
        }
    } catch (e2) {
        console.error(e2);
        if (message) message.textContent = "Något gick fel när formuläret skulle sparas.";
    }
}

async function loadPendingEmploymentForms() {
    const role = employmentCurrentUser?.role || "";
    const canView = role === "CEO" || role === "CTO" || role === "ADMIN" || role === "HR";
    const canApprove = role === "CEO" || role === "CTO" || role === "ADMIN";
    const pendingList = document.getElementById("pendingFormsList");

    if (!pendingList || !canView) return;

    try {
        const res = await apiFetch("/forms?status=PENDING");
        if (!res.ok) {
            pendingList.innerHTML = "<p>Kunde inte hämta pending forms.</p>";
            return;
        }

        const forms = await res.json();

        if (!forms.length) {
            pendingList.innerHTML = "<p>Det finns inga pending approvals just nu.</p>";
            return;
        }

        pendingList.innerHTML = forms.map(form => `
            <div class="employment-card">
                <div class="employment-card-header">
                    <h3>${escapeHtml(form.firstName)} ${escapeHtml(form.lastName)}</h3>
                    <span class="badge badge-SUBMITTED">PENDING</span>
                </div>

                <div class="employment-card-body">
                    <p><strong>Email:</strong> ${escapeHtml(form.email)}</p>
                    <p><strong>Phone:</strong> ${escapeHtml(form.phoneNumber)}</p>
                    <p><strong>Role:</strong> ${escapeHtml(form.role)}</p>
                    <p><strong>Department:</strong> ${escapeHtml(form.department)}</p>
                <div id="form-ssn-container-${form.id}" class="ssn-display">
                        ${getInitialSsnHtml(form.id, 'form')}
                </div>               
            </div>

                <div style="margin-top: 1rem; display:flex; gap:0.75rem;">
            ${canApprove ? `
                <button class="btn btn-primary" onclick="approveEmploymentForm(${form.id})">Approve</button>
                <button class="btn btn-secondary" onclick="rejectEmploymentForm(${form.id})">Reject</button>
                ` : ""}
                </div>
            </div>
        `).join("");
    } catch (e) {
        console.error("Error loading pending employment forms", e);
        pendingList.innerHTML = "<p>Något gick fel när pending forms skulle hämtas.</p>";
    }
}

async function approveEmploymentForm(formId) {
    try {
        const res = await apiFetch(`/forms/${formId}/approve`, {
            method: "POST"
        });

        const text = await res.text();

        if (res.ok) {
            alert("Godkänd!\n" + text);
            await loadPendingEmploymentForms();
            await loadEmploymentStaff();
        } else {
            alert("Kunde inte godkänna formuläret: " + text);
        }
    } catch (e) {
        console.error(e);
        alert("Något gick fel vid approval.");
    }
}

async function rejectEmploymentForm(formId) {
    try {
        const res = await apiFetch(`/forms/${formId}/reject`, {
            method: "POST"
        });

        const text = await res.text();

        if (res.ok) {
            alert("Formuläret avvisades.");
            await loadPendingEmploymentForms();
        } else {
            alert("Kunde inte rejecta formuläret: " + text);
        }
    } catch (e) {
        console.error(e);
        alert("Något gick fel vid reject.");
    }
}
