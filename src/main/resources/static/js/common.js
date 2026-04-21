const API_BASE = "/api";

function getCsrfToken() {
    const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
    return match ? decodeURIComponent(match[1]) : null;
}

function escapeHtml(text) {
    if (!text) return "";
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function getHeaders() {
    const headers = {"Content-Type": "application/json"};
    const csrf = getCsrfToken();
    if (csrf) headers["X-XSRF-TOKEN"] = csrf;
    return headers;
}

async function apiFetch(endpoint, options = {}) {
    const response = await fetch(`${API_BASE}${endpoint}`, {
        ...options,
        credentials: "same-origin",
        headers: {...getHeaders(), ...options.headers}
    });
    if (response.status === 401) logout();
    return response;
}

function logout() {
    window.location.href = "/logout";
}

async function renderNavbar() {
    const navbar = document.getElementById("navbar");
    if (!navbar) return;

    const path = window.location.pathname;
    let currentUser = null;

    try {
        const res = await apiFetch("/staff/me");
        if (res.ok) {
            currentUser = await res.json();
        }
    } catch (e) {
        console.error("Could not load current user", e);
    }

    const fullName =
        currentUser?.fullName ||
        (currentUser?.firstName || currentUser?.lastName ? `${currentUser.firstName || ""} ${currentUser.lastName || ""}`.trim() : "Användare");

    const profileImage = currentUser?.profilePictureUrl || "https://via.placeholder.com/40";
    const status = currentUser?.status || "ONLINE";
    const role = currentUser?.role || "";

    navbar.innerHTML = `
        <aside class="sidebar">
            <a href="/pages/dashboard.html" class="sidebar-logo">
                🛡️ CyberWatch
            </a>

            <nav class="sidebar-nav">
                <a href="/pages/dashboard.html" class="sidebar-link ${path.includes('dashboard') ? 'active' : ''}">
                    Tickets
                </a>
                <a href="/pages/create-ticket.html" class="sidebar-link ${path.includes('create-ticket') ? 'active' : ''}">
                    Ny Ticket
                </a>
                <a href="/pages/employment.html" class="sidebar-link ${path.includes('employment') ? 'active' : ''}">
                    Anställda
                </a>
            </nav>

            <div class="sidebar-profile">
                <img src="${profileImage}" alt="Profilbild" class="sidebar-profile-img">
                <div class="sidebar-profile-info">
                    <div class="sidebar-profile-name">${escapeHtml(fullName)}</div>
                    <div class="sidebar-profile-role">${escapeHtml(role)}</div>
                    <select id="statusSelect" class="sidebar-status-select">
                        <option value="ONLINE" ${status === 'ONLINE' ? 'selected' : ''}>Online</option>
                        <option value="BUSY" ${status === 'BUSY' ? 'selected' : ''}>Upptagen</option>
                        <option value="AWAY" ${status === 'AWAY' ? 'selected' : ''}>Borta</option>
                        <option value="OFFLINE" ${status === 'OFFLINE' ? 'selected' : ''}>Offline</option>
                    </select>
                </div>
            </div>

            <div style="padding-top: 1.5rem;">
                <button onclick="logout()" class="btn logout-btn">Logga ut</button>
            </div>
        </aside>`;

    const statusSelect = document.getElementById("statusSelect");
    if (statusSelect) {
        statusSelect.addEventListener("change", async (e) => {
            try {
                const res = await apiFetch(`/staff/me/status?status=${encodeURIComponent(e.target.value)}`, {
                    method: "PATCH"
                });

                if (!res.ok) {
                    alert("Kunde inte uppdatera status.");
                }
            } catch (err) {
                console.error(err);
                alert("Något gick fel när status skulle sparas.");
            }
        });
    }
}

async function loadStaffList(selectId, selectedIds = []) {
    const select = document.getElementById(selectId);
    if (!select) return;
    try {
        const res = await apiFetch("/staff");
        if (!res.ok) return;
        const staff = await res.json();

        const firstOption = select.querySelector('option[value=""]');
        const existingDefault = firstOption ? firstOption.outerHTML : '<option value="">Välj...</option>';

        let staffOptions = staff.map(s => {
            const isAssigned = selectedIds.includes(s.id);
            const className = isAssigned ? 'class="badge-assigned"' : '';
            return `<option value="${s.id}" ${className}>${escapeHtml(s.fullName)} (${escapeHtml(s.email)})</option>`;
        }).join('');

        select.innerHTML = existingDefault + staffOptions;
    } catch (e) {
        console.error("Error loading staff", e);
    }
}
