const API_BASE = "/api"; // Relativ URL — fungerar oavsett host och port

// --------------------
// Auth & Headers
// --------------------
//Updated regex to prevent a header containing ex "XSRF-TOKEN" from being matched as the cookie value, which caused issues when the header was included in the response cookies.
function getCsrfToken() {
    const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
    return match ? decodeURIComponent(match[1]) : null;
}

function escapeHtml(text) {
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
        credentials: "same-origin", // Skickar alltid sessions-cookie automatiskt
        headers: {...getHeaders(), ...options.headers}
    });
    if (response.status === 401) logout();
    return response;
}

function logout() {
    window.location.href = "/logout";
}

function requireLogin() {
    // Inget att kolla — om sessionen saknas redirectar servern till login automatiskt
}

// --------------------
// Sidebar
// --------------------
function renderNavbar() {
    const navbar = document.getElementById("navbar");
    if (!navbar) return;
    const path = window.location.pathname;
    navbar.innerHTML = `
        <aside class="sidebar">
            <a href="/pages/dashboard.html" class="sidebar-logo">🛡️ CyberWatch</a>
            <nav class="sidebar-nav">
                <a href="/pages/dashboard.html" class="sidebar-link ${path.includes('dashboard') ? 'active' : ''}">Tickets</a>
                <a href="/pages/create-ticket.html" class="sidebar-link ${path.includes('create-ticket') ? 'active' : ''}">Ny Ticket</a>
            </nav>
            <div class="sidebar-footer">
                <button onclick="logout()" class="btn logout-btn">🚪 Logga ut</button>
            </div>
        </aside>`;
}

// --------------------
// Staff
// --------------------
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

// --------------------
// Dashboard
// --------------------
async function loadDashboardTickets() {
    const list = document.getElementById("ticketList");
    if (!list) return;
    try {
        const s = document.getElementById("statusFilter")?.value || "";
        const p = document.getElementById("priorityFilter")?.value || "";
        const q = document.getElementById("searchInput")?.value || "";
        const staffId = document.getElementById("staffFilter")?.value || "";

        try {
            const statsRes = await apiFetch(`/tickets?status=&priority=&search=&assignedStaffId=`);
            if (statsRes.ok) {
                const allTickets = await statsRes.json();
                const totalCount = allTickets.length;
                const openCount = allTickets.filter(t => t.status === 'SUBMITTED').length;
                const inProgressCount = allTickets.filter(t => t.status === 'IN_PROGRESS').length;
                const closedCount = allTickets.filter(t => t.status === 'CLOSED').length;

                if (document.getElementById("totalTickets")) document.getElementById("totalTickets").innerText = totalCount;
                if (document.getElementById("openTickets")) document.getElementById("openTickets").innerText = openCount;
                if (document.getElementById("inProgressTickets")) document.getElementById("inProgressTickets").innerText = inProgressCount;
                if (document.getElementById("closedTickets")) document.getElementById("closedTickets").innerText = closedCount;
            }
        } catch (e) {
            console.error("Stats error", e);
        }

        const res = await apiFetch(`/tickets?status=${s}&priority=${p}&search=${q}&assignedStaffId=${staffId}`);
        if (!res.ok) return list.innerHTML = "<p>Kunde inte hämta tickets.</p>";
        const tickets = await res.json();

        const staffRes = await apiFetch("/staff");
        const allStaff = staffRes.ok ? await staffRes.json() : [];

        list.innerHTML = "";

        tickets.forEach(t => {
            const item = document.createElement("div");
            item.className = "ticket-item";

            const assignedStaffNames = t.assignedStaff?.map(s => escapeHtml(s.fullName)).join(', ') || 'Ingen';
            const assignedIds = t.assignedStaff?.map(s => s.id) || [];

            item.innerHTML = `
                <div class="ticket-info" onclick="window.location.href='/pages/ticket-detail.html?id=${t.id}'">
                    <h3>${escapeHtml(t.title)}</h3>
                    <div class="muted">#${t.id} • ${escapeHtml(t.priority)} • Tilldelad: ${assignedStaffNames}</div>
                </div>
                <div style="display:flex; align-items:center; gap:10px;">
                    <select class="dashboard-assignment-select" data-id="${t.id}">
                        <option value="">Ändra tilldelning...</option>
                        ${allStaff.map(staff => {
                const isAssigned = assignedIds.includes(staff.id);
                return `<option value="${staff.id}" ${isAssigned ? 'class="badge-assigned"' : ''}>
                                ${isAssigned ? '✓ ' : ''}${escapeHtml(staff.fullName)}
                            </option>`;
            }).join('')}
                    </select>
                    <select class="dashboard-status-select" data-id="${t.id}" style="padding: 4px 8px; border-radius: 4px; border: 1px solid var(--border);">
                        <option value="SUBMITTED" ${t.status === 'SUBMITTED' ? 'selected' : ''}>Submitted</option>
                        <option value="IN_PROGRESS" ${t.status === 'IN_PROGRESS' ? 'selected' : ''}>In Progress</option>
                        <option value="RESOLVED" ${t.status === 'RESOLVED' ? 'selected' : ''}>Resolved</option>
                        <option value="CLOSED" ${t.status === 'CLOSED' ? 'selected' : ''}>Closed</option>
                    </select>
                </div>`;
            list.appendChild(item);
        });

        document.querySelectorAll('.dashboard-assignment-select').forEach(select => {
            select.addEventListener('change', async (e) => {
                const ticketId = e.target.dataset.id;
                const staffId = parseInt(e.target.value);
                if (!staffId) return;

                const res = await apiFetch(`/tickets/${ticketId}/assign`, {
                    method: "PUT",
                    body: JSON.stringify({staffIds: [staffId]})
                });

                if (res.ok) loadDashboardTickets();
                else alert("Kunde inte uppdatera tilldelning.");
            });
        });

        document.querySelectorAll('.dashboard-status-select').forEach(select => {
            select.addEventListener('change', async (e) => {
                const ticketId = e.target.dataset.id;
                const newStatus = e.target.value;

                const res = await apiFetch(`/tickets/${ticketId}/status?status=${newStatus}`, {
                    method: "PATCH"
                });

                if (res.ok) loadDashboardTickets();
                else {
                    const errorMsg = await res.text();
                    alert("Kunde inte uppdatera status: " + (errorMsg || "Okänt fel"));
                    loadDashboardTickets();
                }
            });
        });
    } catch (e) {
        console.error(e);
        list.innerHTML = "<p>Något gick fel.</p>";
    }
}

// --------------------
// Ticket Detail & Comments
// --------------------
async function loadTicketDetail() {
    const id = new URLSearchParams(window.location.search).get("id");
    const container = document.getElementById("ticketDetail");
    if (!id || !container) return;
    try {
        const res = await apiFetch(`/tickets/${id}`);
        if (!res.ok) return container.innerHTML = "<p>Kunde inte hämta ticket.</p>";
        const t = await res.json();
        document.getElementById("editTicketLink").href = `/pages/edit-ticket.html?id=${t.id}`;
        container.innerHTML = `
            <div class="card">
                <div style="display:flex; justify-content:space-between; margin-bottom:1rem;">
                    <span class="badge badge-${t.status}">${t.status}</span>
                    <span class="muted">#${t.id}</span>
                </div>
                <h2>${escapeHtml(t.title)}</h2>
                <div style="margin-bottom: 1rem;">
                    <strong>Tilldelad till:</strong> 
                    ${t.assignedStaff && t.assignedStaff.length > 0
            ? t.assignedStaff.map(s => `<span class="badge badge-secondary" style="margin-right: 5px;">${escapeHtml(s.fullName)}</span>`).join('')
            : '<span class="muted">Ingen tilldelad</span>'}
                </div>
                <p style="margin: 1.5rem 0; font-size: 1.1rem; white-space: pre-wrap;">${escapeHtml(t.description)}</p>
                <div class="muted" style="border-top:1px solid var(--border); padding-top:1rem;">
                    Skapad av: ${escapeHtml(t.createdBy?.fullName || 'Okänd')} • Typ: ${escapeHtml(t.issueType)} • Prioritet: ${escapeHtml(t.priority)}
                </div>
            </div>`;
        loadComments(id);

        const assignedIds = t.assignedStaff?.map(s => s.id) || [];
        if (typeof window.clearStaffBadges === 'function') {
            window.clearStaffBadges();
            t.assignedStaff?.forEach(s => window.addStaffBadge(s.id, s.fullName));
        }
        loadStaffList("reassignStaff", assignedIds);
    } catch (e) {
        container.innerHTML = "<p>Något gick fel.</p>";
    }
}

async function loadComments(id) {
    const list = document.getElementById("commentsList");
    if (!list) return;
    const res = await apiFetch(`/tickets/${id}/comments`);
    if (!res.ok) return list.innerHTML = "<p>Kunde inte hämta kommentarer.</p>";
    const comments = await res.json();
    list.innerHTML = comments.length ? comments.map(c => `
    <div class="comment-item">
        <div class="comment-header"><span>${escapeHtml(c.authorName || c.authorEmail || 'Användare')}</span>
                    <span class="muted">${new Date(c.createdAt).toLocaleString()}</span></div>
        <div class="comment-text">${escapeHtml(c.text || c.content || c.commentText || "...")}</div>
    </div>`).join('') : "<p>Inga kommentarer ännu.</p>";
}

// --------------------
// Forms & Actions
// --------------------
function setupCreateTicketForm() {
    const form = document.getElementById("createTicketForm");
    if (!form) return;
    form.addEventListener("submit", async (e) => {
        e.preventDefault();
        const data = {
            title: document.getElementById("title").value,
            description: document.getElementById("description").value,
            priority: document.getElementById("priority").value,
            issueType: document.getElementById("issueType").value,
            assignedStaffIds: typeof window.getSelectedStaffIds === 'function' ? window.getSelectedStaffIds() : Array.from(document.getElementById("assignedStaff").selectedOptions).map(o => parseInt(o.value))
        };
        const res = await apiFetch("/tickets", {method: "POST", body: JSON.stringify(data)});
        if (res.ok) {
            const t = await res.json();
            window.location.href = `/pages/ticket-detail.html?id=${t.id}`;
        } else {
            const err = await res.json().catch(() => ({}));
            alert("Kunde inte skapa ticket: " + (err.message || err.error || res.statusText));
        }
    });
}

function setupCommentForm() {
    const form = document.getElementById("commentForm");
    const id = new URLSearchParams(window.location.search).get("id");
    if (!form || !id) return;
    form.addEventListener("submit", async (e) => {
        e.preventDefault();
        const res = await apiFetch(`/tickets/${id}/comments`, {
            method: "POST",
            body: JSON.stringify({text: document.getElementById("commentText").value})
        });
        if (res.ok) {
            form.reset();
            loadComments(id);
        } else {
            alert("Kunde inte skicka meddelande.");
        }
    });
}

function setupUploadForm() {
    const form = document.getElementById("uploadForm");
    const id = new URLSearchParams(window.location.search).get("id");
    if (!form || !id) return;
    form.addEventListener("submit", async (e) => {
        e.preventDefault();
        const file = document.getElementById("ticketFile").files[0];
        if (!file) return alert("Välj en fil.");
        const formData = new FormData();
        formData.append("file", file);
        const csrf = getCsrfToken();
        const res = await fetch(`${API_BASE}/tickets/${id}/upload`, {
            method: "POST",
            credentials: "same-origin",
            headers: csrf ? {"X-XSRF-TOKEN": csrf} : {},
            body: formData
        });
        if (res.ok) {
            alert("Fil uppladdad!");
            form.reset();
        }
    });
}

async function loadEditTicketData() {
    const id = new URLSearchParams(window.location.search).get("id");
    if (!id) return;
    const res = await apiFetch(`/tickets/${id}`);
    if (!res.ok) return;
    const t = await res.json();
    ['Title', 'Description', 'Priority', 'Status'].forEach(f => {
        const el = document.getElementById(`edit${f}`);
        if (el) el.value = t[f.toLowerCase()];
    });
}

function setupAssignmentUI() {
    const id = new URLSearchParams(window.location.search).get("id");
    const section = document.getElementById("assignmentSection");
    if (!id || !section) return;

    section.style.display = "block";

    document.getElementById("reassignBtn").addEventListener("click", async () => {
        const selectedIds = window.currentAssignedIds ? Array.from(window.currentAssignedIds) : Array.from(document.getElementById("reassignStaff").selectedOptions).map(o => parseInt(o.value));
        if (selectedIds.length === 0) return alert("Välj minst en person.");

        const res = await apiFetch(`/tickets/${id}/assign`, {
            method: "PUT",
            body: JSON.stringify({staffIds: selectedIds})
        });

        if (res.ok) {
            alert("Tilldelning uppdaterad!");
            loadTicketDetail();
        } else {
            const err = await res.json().catch(() => ({}));
            alert("Kunde inte uppdatera tilldelning: " + (err.message || res.statusText));
        }
    });
}

function setupEditTicketForm() {
    const form = document.getElementById("editTicketForm");
    const id = new URLSearchParams(window.location.search).get("id");
    if (!form || !id) return;
    form.addEventListener("submit", async (e) => {
        e.preventDefault();
        const msg = document.getElementById("editMessage");
        msg.textContent = "Sparar...";
        const body = {
            title: document.getElementById("editTitle").value,
            description: document.getElementById("editDescription").value,
            priority: document.getElementById("editPriority").value,
            status: document.getElementById("editStatus").value
        };
        const res = await apiFetch(`/tickets/${id}/status?status=${body.status}`, {method: "PATCH"});
        if (res.ok) {
            msg.textContent = "Status uppdaterad! Omdirigerar...";
            setTimeout(() => window.location.href = `/pages/ticket-detail.html?id=${id}`, 1000);
        } else {
            msg.textContent = "Kunde inte uppdatera: " + res.statusText;
        }
    });
}
