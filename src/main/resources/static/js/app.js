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
// Dashboard
// --------------------
async function loadDashboardTickets() {
    const list = document.getElementById("ticketList");
    if (!list) return;
    try {
        const s = document.getElementById("statusFilter")?.value || "";
        const p = document.getElementById("priorityFilter")?.value || "";
        const q = document.getElementById("searchInput")?.value || "";
        
        const res = await apiFetch(`/tickets?status=${s}&priority=${p}&search=${q}`);
        if (!res.ok) return list.innerHTML = "<p>Kunde inte hämta tickets.</p>";
        const tickets = await res.json();
        
        const stats = { total: tickets.length, open: 0, inProgress: 0, closed: 0 };
        list.innerHTML = "";
        
        tickets.forEach(t => {
            if (t.status === 'SUBMITTED') stats.open++;
            else if (t.status === 'IN_PROGRESS') stats.inProgress++;
            else if (t.status === 'CLOSED') stats.closed++;

            const item = document.createElement("div");
            item.className = "ticket-item";
            item.onclick = () => window.location.href = `/pages/ticket-detail.html?id=${t.id}`;
            item.innerHTML = `
                <div class="ticket-info">
                    <h3>${t.title}</h3>
                    <div class="muted">#${t.id} • ${t.priority} • ${t.createdBy?.fullName || 'Okänd'}</div>
                </div>
                <span class="badge badge-${t.status}">${t.status}</span>`;
            list.appendChild(item);
        });

        ['total', 'open', 'inProgress', 'closed'].forEach(k => {
            const el = document.getElementById(k + 'Tickets');
            if (el) el.textContent = stats[k];
        });
    } catch (e) { list.innerHTML = "<p>Något gick fel.</p>"; }
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
                <p style="margin: 1.5rem 0; font-size: 1.1rem; white-space: pre-wrap;">${t.description}</p>
                <div class="muted" style="border-top:1px solid var(--border); padding-top:1rem;">
                    Skapad av: ${t.createdBy?.fullName || 'Okänd'} • Typ: ${t.issueType} • Prioritet: ${t.priority}
                </div>
            </div>`;
        loadComments(id);
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
            issueType: document.getElementById("issueType").value
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
