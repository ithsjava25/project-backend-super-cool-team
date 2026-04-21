async function initCreateTicket() {
    await renderNavbar();
    await loadStaffList("assignedStaff");
    setupCreateTicketForm();
}

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
