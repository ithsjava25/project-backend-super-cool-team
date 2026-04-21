async function initEditTicket() {
    await renderNavbar();
    await loadEditTicketData();
    setupEditTicketForm();
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
        const res = await apiFetch(`/tickets/${id}/status?status=${body.status}`, {method: "PATCH"});
        if (res.ok) {
            msg.textContent = "Status uppdaterad! Omdirigerar...";
            setTimeout(() => window.location.href = `/pages/ticket-detail.html?id=${id}`, 1000);
        } else {
            msg.textContent = "Kunde inte uppdatera: " + res.statusText;
        }
    });
}
