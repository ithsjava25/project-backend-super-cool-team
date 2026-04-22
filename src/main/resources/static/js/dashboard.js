async function initDashboard() {
    await renderNavbar();
    await loadDashboardTickets();
    await loadStaffList("staffFilter");

    document.getElementById("statusFilter")?.addEventListener("change", loadDashboardTickets);
    document.getElementById("priorityFilter")?.addEventListener("change", loadDashboardTickets);
    document.getElementById("staffFilter")?.addEventListener("change", loadDashboardTickets);
    document.getElementById("searchInput")?.addEventListener("input", loadDashboardTickets);
}

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

        const params = new URLSearchParams({
            status: s,
            priority: p,
            search: q,
            assignedStaffId: staffId
        });
        const res = await apiFetch(`/tickets?${params.toString()}`);

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
    <div class="ticket-main"
         role="link"
         tabindex="0"
         aria-label="Ticket ${t.id}: ${escapeHtml(t.title)}"
         onclick="window.location.href='/pages/ticket-detail.html?id=${t.id}'"
         onkeydown="if(event.key==='Enter'||event.key===' ')window.location.href='/pages/ticket-detail.html?id=${t.id}'">                    
                        <div class="ticket-header">
                        <span class="badge badge-${t.status}">${t.status}</span>
                        <span class="ticket-title">${escapeHtml(t.title)}</span>
                    </div>
                    <div class="ticket-meta">
                        <span>#${t.id}</span>
                        <span><span class="badge badge-${t.priority}">${t.priority}</span></span>
                        <span>${assignedStaffNames}</span>
                    </div>
                </div>
                <div class="ticket-actions">
                    <select class="dashboard-assignment-select" data-id="${t.id}" style="max-width: 150px;">
                        <option value="" disabled>Tilldela...</option>
                        ${allStaff.map(staff => {
                const isAssigned = assignedIds.includes(staff.id);
                return `<option value="${staff.id}" ${isAssigned ? 'selected' : ''}>
                                ${escapeHtml(staff.fullName)}
                            </option>`;
            }).join('')}
                    </select>
                    <select class="dashboard-status-select" data-id="${t.id}" style="width: auto;">
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
                const staffIds = Array.from(e.target.selectedOptions)
                    .map(o => Number.parseInt(o.value, 10))
                    .filter(Number.isFinite);
                if (staffIds.length === 0) return;

                const res = await apiFetch(`/tickets/${ticketId}/assign`, {
                    method: "PUT",
                    body: JSON.stringify({staffIds})
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
