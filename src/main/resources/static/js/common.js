const API_BASE = "/api";

function getCsrfToken() {
    const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
    return match ? decodeURIComponent(match[1]) : null;
}

function escapeHtml(text) {
    if (!text) return "";
    return String(text)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}

function safeImageUrl(url) {
    const fallback = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='40' height='40' viewBox='0 0 40 40'%3E%3Ccircle cx='20' cy='20' r='20' fill='%23cccccc'/%3E%3Ctext x='50%25' y='55%25' dominant-baseline='middle' text-anchor='middle' font-size='18' fill='%23666666'%3E%3F%3C/text%3E%3C/svg%3E";
    if (!url) return fallback;

    try {
        const parsed = new URL(url, window.location.origin);
        return ["http:", "https:"].includes(parsed.protocol) ? parsed.href : fallback;
    } catch {
        return fallback;
    }
}

function getHeaders() {
    const headers = { "Content-Type": "application/json" };
    const csrf = getCsrfToken();
    if (csrf) headers["X-XSRF-TOKEN"] = csrf;
    return headers;
}

async function apiFetch(endpoint, options = {}) {
    const response = await fetch(`${API_BASE}${endpoint}`, {
        ...options,
        credentials: "same-origin",
        headers: { ...getHeaders(), ...options.headers }
    });

    if (response.status === 401) {
        logout();
        return new Promise(() => {});
    }

    return response;
}

async function logout() {
    try {
        await fetch("/logout", {
            method: "POST",
            credentials: "same-origin",
            headers: getHeaders()
        });
    } finally {
        window.location.href = "/pages/login.html";
    }
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
        (currentUser?.firstName || currentUser?.lastName
            ? `${currentUser.firstName || ""} ${currentUser.lastName || ""}`.trim()
            : "Användare");

    const profileImage = safeImageUrl(currentUser?.profilePictureUrl);
    const status = currentUser?.status || "ONLINE";
    const role = currentUser?.role || "";

    navbar.innerHTML = `
        <aside class="sidebar">
            <a href="/pages/dashboard.html" class="sidebar-logo">
                🛡️ CyberWatch
            </a>

            <nav class="sidebar-nav">
                <a href="/pages/dashboard.html" class="sidebar-link ${path.includes("dashboard") ? "active" : ""}">
                    Tickets
                </a>
                <a href="/pages/create-ticket.html" class="sidebar-link ${path.includes("create-ticket") ? "active" : ""}">
                    Ny Ticket
                </a>
                <a href="/pages/employment.html" class="sidebar-link ${path.includes("employment") ? "active" : ""}">
                    Anställda
                </a>
                ${role === 'ADMIN' ? `
                <a href="/pages/logs.html" class="sidebar-link ${path.includes('logs') ? 'active' : ''}">
                    🔍 Systemloggar
                </a>` : ''}
            </nav>

            <div class="sidebar-online-wrapper">
                <button type="button" class="sidebar-online-box sidebar-online-button" id="onlineStaffToggle">
                    <div>
                        <div class="sidebar-online-title">Online just nu</div>
                        <div class="sidebar-online-count" id="onlineStaffCount">0</div>
                        <div class="sidebar-online-text">Klicka för att visa vilka</div>
                    </div>
                    <div class="sidebar-online-arrow" id="onlineStaffArrow">▾</div>
                </button>

                <div class="online-staff-list" id="onlineStaffList"></div>
            </div>

            <div class="sidebar-profile">
                <img src="${escapeHtml(profileImage)}" alt="Profilbild" class="sidebar-profile-img">
                <div class="sidebar-profile-info">
                    <div class="sidebar-profile-name">${escapeHtml(fullName)}</div>
                    <div class="sidebar-profile-role">${escapeHtml(role)}</div>
                    <select id="statusSelect" class="sidebar-status-select">
                        <option value="ONLINE" ${status === "ONLINE" ? "selected" : ""}>Online</option>
                        <option value="BUSY" ${status === "BUSY" ? "selected" : ""}>Upptagen</option>
                        <option value="AWAY" ${status === "AWAY" ? "selected" : ""}>Borta</option>
                        <option value="OFFLINE" ${status === "OFFLINE" ? "selected" : ""}>Offline</option>
                    </select>
                </div>
            </div>

            <div style="padding-top: 1.5rem;">
                <button onclick="logout()" class="btn logout-btn">Logga ut</button>
            </div>
        </aside>`;

    const statusSelect = document.getElementById("statusSelect");
    if (statusSelect) {
        let previousStatus = statusSelect.value;

        statusSelect.addEventListener("change", async (e) => {
            const nextStatus = e.target.value;

            try {
                const res = await apiFetch(`/staff/me/status?status=${encodeURIComponent(nextStatus)}`, {
                    method: "PATCH"
                });

                if (!res.ok) {
                    e.target.value = previousStatus;
                    alert("Kunde inte uppdatera status.");
                    return;
                }

                previousStatus = nextStatus;

                if (typeof refreshOnlineStaffWidget === "function") {
                    await refreshOnlineStaffWidget();
                }
            } catch (err) {
                e.target.value = previousStatus;
                console.error(err);
                alert("Något gick fel när status skulle sparas.");
            }
        });
    }

    if (typeof initOnlineStaffWidget === "function") {
        initOnlineStaffWidget();
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
        const existingDefault = firstOption
            ? firstOption.outerHTML
            : '<option value="">Välj...</option>';

        const selectedIdSet = new Set(selectedIds.map(String));

        const staffOptions = staff.map(s => {
            const staffId = String(s.id);
            const isAssigned = selectedIdSet.has(staffId);
            const className = isAssigned ? 'class="badge-assigned"' : "";
            const selectedAttr = isAssigned ? "selected" : "";

            return `<option value="${escapeHtml(staffId)}" ${className} ${selectedAttr}>${escapeHtml(s.fullName)} (${escapeHtml(s.email)})</option>`;
        }).join("");

        select.innerHTML = existingDefault + staffOptions;
    } catch (e) {
        console.error("Error loading staff", e);
    }
}