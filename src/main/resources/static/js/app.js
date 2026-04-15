const API_BASE = "http://localhost:8080/api";

// --------------------
// Auth & Headers
// --------------------
function getHeaders() {
    const token = localStorage.getItem("jwt_token") || localStorage.getItem("token");
    return {
        "Content-Type": "application/json",
        "Authorization": token ? `Bearer ${token}` : ""
    };
}

async function apiFetch(endpoint, options = {}) {
    const response = await fetch(`${API_BASE}${endpoint}`, {
        ...options,
        headers: { ...getHeaders(), ...options.headers }
    });
    if (response.status === 401) logout();
    return response;
}

function logout() {
    localStorage.removeItem("token");
    localStorage.removeItem("jwt_token");
    window.location.href = "/pages/login.html";
}

function requireLogin() {
    if (!(localStorage.getItem("jwt_token") || localStorage.getItem("token"))) {
        window.location.href = "/pages/login.html";
    }
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
        
        const existingOptions = select.innerHTML.startsWith('<option value="">') 
            ? '<option value="">' + select.options[0].text + '</option>' 
            : '';

        select.innerHTML = existingOptions + staff.map(s => {
            const isAssigned = selectedIds.includes(s.id);
            const className = isAssigned ? 'class="badge-assigned"' : '';
            return `<option value="${s.id}" ${className}>${s.fullName} (${s.email})</option>`;
        }).join('');
    } catch (e) { console.error("Error loading staff", e); }
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
        
        const res = await apiFetch(`/tickets?status=${s}&priority=${p}&search=${q}&assignedStaffId=${staffId}`);
        if (!res.ok) return list.innerHTML = "<p>Kunde inte hämta tickets.</p>";
        const tickets = await res.json();
        
        const staffRes = await apiFetch("/staff");
        const allStaff = staffRes.ok ? await staffRes.json() : [];

        const stats = { total: tickets.length, open: 0, inProgress: 0, closed: 0 };
        list.innerHTML = "";
        
        tickets.forEach(t => {
            if (t.status === 'SUBMITTED') stats.open++;
            else if (t.status === 'IN_PROGRESS') stats.inProgress++;
            else if (t.status === 'CLOSED') stats.closed++;

            const item = document.createElement("div");
            item.className = "ticket-item";
            
            const assignedStaffNames = t.assignedStaff?.map(s => s.fullName).join(', ') || 'Ingen';
            const assignedIds = t.assignedStaff?.map(s => s.id) || [];

            item.innerHTML = `
                <div class="ticket-info" onclick="window.location.href='/pages/ticket-detail.html?id=${t.id}'">
                    <h3>${t.title}</h3>
                    <div class="muted">#${t.id} • ${t.priority} • Tilldelad: ${assignedStaffNames}</div>
                </div>
                <div style="display:flex; align-items:center; gap:10px;">
                    <select class="dashboard-assignment-select" data-id="${t.id}">
                        <option value="">Ändra tilldelning...</option>
                        ${allStaff.map(staff => {
                            const isAssigned = assignedIds.includes(staff.id);
                            return `<option value="${staff.id}" ${isAssigned ? 'class="badge-assigned"' : ''}>
                                ${isAssigned ? '✓ ' : ''}${staff.fullName}
                            </option>`;
                        }).join('')}
                    </select>
                    <span class="badge badge-${t.status}">${t.status}</span>
                </div>`;
            list.appendChild(item);
        });

        // Setup listeners for dashboard assignment changes
        document.querySelectorAll('.dashboard-assignment-select').forEach(select => {
            select.addEventListener('change', async (e) => {
                const ticketId = e.target.dataset.id;
                const staffId = parseInt(e.target.value);
                if (!staffId) return;

                // Vi lägger till personen till den befintliga listan eller ersätter?
                // Uppgiften säger "man ska kunna ändra detta på dashboard sidan".
                // Vi gör så att man väljer EN person och det BLIR den nya tilldelningen (för enkelhet i dropdown),
                // eller så lägger vi till den. Men dropdown i dashboard brukar vara "välj ny".
                
                const res = await apiFetch(`/tickets/${ticketId}/assign?assignedById=1`, {
                    method: "PUT",
                    body: JSON.stringify({ staffIds: [staffId] })
                });

                if (res.ok) {
                    loadDashboardTickets();
                } else {
                    alert("Kunde inte uppdatera tilldelning.");
                }
            });
        });

        ['total', 'open', 'inProgress', 'closed'].forEach(k => {
            const el = document.getElementById(k + 'Tickets');
            if (el) el.textContent = stats[k];
        });
    } catch (e) { console.error(e); list.innerHTML = "<p>Något gick fel.</p>"; }
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
                <h2>${t.title}</h2>
                <div style="margin-bottom: 1rem;">
                    <strong>Tilldelad till:</strong> 
                    ${t.assignedStaff && t.assignedStaff.length > 0 
                        ? t.assignedStaff.map(s => `<span class="badge badge-secondary" style="margin-right: 5px;">${s.fullName}</span>`).join('') 
                        : '<span class="muted">Ingen tilldelad</span>'}
                </div>
                <p style="margin: 1.5rem 0; font-size: 1.1rem; white-space: pre-wrap;">${t.description}</p>
                <div class="muted" style="border-top:1px solid var(--border); padding-top:1rem;">
                    Skapad av: ${t.createdBy?.fullName || 'Okänd'} • Typ: ${t.issueType} • Prioritet: ${t.priority}
                </div>
            </div>`;
        loadComments(id);
        
        // Uppdatera assignment UI med nuvarande tilldelade
        const assignedIds = t.assignedStaff?.map(s => s.id) || [];
        if (typeof window.clearStaffBadges === 'function') {
            window.clearStaffBadges();
            t.assignedStaff?.forEach(s => window.addStaffBadge(s.id, s.fullName));
        }
        loadStaffList("reassignStaff", assignedIds);
    } catch (e) { container.innerHTML = "<p>Något gick fel.</p>"; }
}

async function loadComments(id) {
    const list = document.getElementById("commentsList");
    if (!list) return;
    const res = await apiFetch(`/tickets/${id}/comments`);
    if (!res.ok) return list.innerHTML = "<p>Kunde inte hämta kommentarer.</p>";
    const comments = await res.json();
    list.innerHTML = comments.length ? comments.map(c => `
        <div class="comment-item">
            <div class="comment-header"><span>${c.authorName || c.authorEmail || 'Användare'}</span><span class="muted">${new Date(c.createdAt).toLocaleString()}</span></div>
            <div class="comment-text">${c.text || c.content || c.commentText || "..." }</div>
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
        const res = await apiFetch("/tickets", { method: "POST", body: JSON.stringify(data) });
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
            body: JSON.stringify({ content: document.getElementById("commentText").value })
        });
        if (res.ok) { form.reset(); loadComments(id); }
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
        const res = await fetch(`${API_BASE}/tickets/${id}/upload?uploadedById=${document.getElementById("uploadedById").value}`, {
            method: "POST",
            headers: { "Authorization": `Bearer ${localStorage.getItem("jwt_token") || localStorage.getItem("token")}` },
            body: formData
        });
        if (res.ok) { alert("Fil uppladdad!"); form.reset(); }
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

        const assignedById = 1; 

        const res = await apiFetch(`/tickets/${id}/assign?assignedById=${assignedById}`, {
            method: "PUT",
            body: JSON.stringify({ staffIds: selectedIds })
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
        const res = await apiFetch(`/tickets/${id}/status?status=${body.status}&performedById=1`, { method: "PATCH" });
        if (res.ok) {
            msg.textContent = "Status uppdaterad! Omdirigerar...";
            setTimeout(() => window.location.href = `/pages/ticket-detail.html?id=${id}`, 1000);
        } else {
            msg.textContent = "Kunde inte uppdatera: " + res.statusText;
        }
    });
}
