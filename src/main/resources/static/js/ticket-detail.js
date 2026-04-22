async function initTicketDetail() {
    await renderNavbar();
    await loadTicketDetail();
    setupCommentForm();
    setupUploadForm();
    setupAssignmentUI();

    // Starta polling för aktivitetslogg
    const id = new URLSearchParams(window.location.search).get("id");
    if (id) subscribeToActivityUpdates(id);
}

async function loadTicketDetail() {
    const id = new URLSearchParams(window.location.search).get("id");
    const container = document.getElementById("ticketDetail");
    if (!id || !container) return;
    try {
        const res = await apiFetch(`/tickets/${id}`);
        if (!res.ok) return container.innerHTML = "<p>Kunde inte hämta ticket.</p>";
        const t = await res.json();
        const editLink = document.getElementById("editTicketLink");
        if (editLink) editLink.href = `/pages/edit-ticket.html?id=${encodeURIComponent(t.id)}`;

        const attachmentsHtml = t.attachments && t.attachments.length > 0
            ? `<div style="margin-top: 1rem;">
                <strong>Bilagor:</strong>
                <ul style="margin-top: 0.5rem; padding-left: 1.2rem;">
                    ${t.attachments.map(a => `
                        <li style="margin-bottom: 0.4rem;">
                            <a href="${escapeHtml(safeImageUrl(a.downloadUrl))}" target="_blank" rel="noopener noreferrer">
                                ${escapeHtml(a.fileName)}
                            </a>
                        </li>
                    `).join('')}
                </ul>
               </div>`
            : `<p class="muted" style="margin-top: 1rem;">Inga bilagor uppladdade.</p>`;

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
                ${attachmentsHtml}
                <div class="muted" style="border-top:1px solid var(--border); padding-top:1rem; margin-top: 1rem;">
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
                    <span class="muted">${new Date(c.createdAt).toLocaleString('sv-SE')}</span></div>
        <div class="comment-text">${escapeHtml(c.text || c.content || c.commentText || "...")}</div>
    </div>`).join('') : "<p>Inga kommentarer ännu.</p>";
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
        const res = await fetch(`/api/tickets/${id}/upload`, {
            method: "POST",
            credentials: "same-origin",
            headers: csrf ? {"X-XSRF-TOKEN": csrf} : {},
            body: formData
        });

        if (res.ok) {
            form.reset();
            loadTicketDetail();
        } else {
            const err = await res.json().catch(() => ({}));
            alert("Uppladdning misslyckades: " + (err.error || res.statusText));
        }
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

// Realtidsuppdatering av aktivitetslogg via polling
function subscribeToActivityUpdates(ticketId) {
    // Hämta aktivitetslogg var 5:e sekund - samma intervall som systemloggen
    const activityInterval = setInterval(async () => {
        try {
            const res = await apiFetch(`/tickets/${ticketId}/logs`);
            if (res.ok) {
                const logs = await res.json();
                updateActivityLog(logs);
            }
        } catch (e) {
            console.error("Could not update activity log", e);
        }
    }, 5000);

    // Stoppa polling när sidan lämnas
    window.addEventListener('beforeunload', () => clearInterval(activityInterval));
}

// Uppdatera aktivitetsloggen i DOM
function updateActivityLog(logs) {
    const container = document.getElementById("activityLog");
    if (!container) return;

    if (!logs || logs.length === 0) {
        container.innerHTML = "<p class=\"muted\">Ingen aktivitet än.</p>";
        return;
    }

    container.innerHTML = logs.map(log => `
        <div class="activity-item" style="padding: 0.75rem 0; border-bottom: 1px solid var(--border); display: flex; justify-content: space-between;">
            <div>
                <strong>${escapeHtml(log.activityType.replace(/_/g, ' '))}</strong>
                <p class="muted" style="margin: 0.25rem 0; font-size: 0.9rem;">${escapeHtml(log.details || '')}</p>
                <small class="muted">Av ${escapeHtml(log.performedBy || 'Okänd')}</small>
            </div>
            <small class="muted">${new Date(log.timestamp).toLocaleString('sv-SE')}</small>
        </div>
    `).join('');
}
