function getStatusLabel(status) {
    switch (status) {
        case "ONLINE":
            return "Online";
        case "BUSY":
            return "Upptagen";
        case "AWAY":
            return "Borta";
        case "OFFLINE":
            return "Offline";
        default:
            return "Okänd";
    }
}

function getStatusClass(status) {
    switch (status) {
        case "ONLINE":
            return "status-online";
        case "BUSY":
            return "status-busy";
        case "AWAY":
            return "status-away";
        default:
            return "status-offline";
    }
}

async function fetchActiveStaff() {
    const res = await apiFetch("/staff");
    if (!res.ok) {
        throw new Error("Kunde inte hämta staff");
    }

    const staff = await res.json();
    const validStatuses = ["ONLINE", "BUSY", "AWAY"];
    return staff.filter(person => validStatuses.includes(person.status));
}

async function loadOnlineStaffCount() {
    const onlineCountElement = document.getElementById("onlineStaffCount");
    if (!onlineCountElement) return;

    try {
        const activeStaff = await fetchActiveStaff();
        onlineCountElement.textContent = activeStaff.length;
    } catch (e) {
        console.error("Error loading online staff count", e);
        onlineCountElement.textContent = "-";
    }
}

async function renderOnlineStaffList() {
    const listElement = document.getElementById("onlineStaffList");
    if (!listElement) return;

    listElement.innerHTML = `<div class="online-staff-loading">Laddar...</div>`;

    try {
        const activeStaff = await fetchActiveStaff();

        if (activeStaff.length === 0) {
            listElement.innerHTML = `<div class="online-staff-empty">Ingen är online just nu.</div>`;
            return;
        }

        listElement.innerHTML = activeStaff.map(person => {
            const fullName =
                person.fullName ||
                `${person.firstName || ""} ${person.lastName || ""}`.trim() ||
                "Användare";

            return `
                <div class="online-staff-item">
                    <div class="online-staff-name">${escapeHtml(fullName)}</div>
                    <div class="online-staff-status ${getStatusClass(person.status)}">
                        ${escapeHtml(getStatusLabel(person.status))}
                    </div>
                </div>
            `;
        }).join("");
    } catch (e) {
        console.error("Error loading online staff list", e);
        listElement.innerHTML = `<div class="online-staff-empty">Något gick fel.</div>`;
    }
}

async function toggleOnlineStaffList() {
    const listElement = document.getElementById("onlineStaffList");
    const arrowElement = document.getElementById("onlineStaffArrow");
    const toggleButton = document.getElementById("onlineStaffToggle");

    if (!listElement) return;

    const isOpen = listElement.classList.contains("show");

    if (isOpen) {
        listElement.classList.remove("show");
        if (toggleButton) {
            toggleButton.setAttribute("aria-expanded", "false");
        }
        if (arrowElement) {
            arrowElement.textContent = "▾";
        }
        return;
    }

    listElement.classList.add("show");
    if (toggleButton) {
        toggleButton.setAttribute("aria-expanded", "true");
    }
    if (arrowElement) {
        arrowElement.textContent = "▴";
    }

    await renderOnlineStaffList();
}

async function refreshOnlineStaffWidget() {
    await loadOnlineStaffCount();

    const listElement = document.getElementById("onlineStaffList");
    if (listElement && listElement.classList.contains("show")) {
        await renderOnlineStaffList();
    }
}
let onlineStaffRefreshTimer = null;
function initOnlineStaffWidget() {
    const toggleButton = document.getElementById("onlineStaffToggle");
    if (!toggleButton) return;

    toggleButton.addEventListener("click", toggleOnlineStaffList);
    refreshOnlineStaffWidget();

    if (!onlineStaffRefreshTimer) {
        onlineStaffRefreshTimer = window.setInterval(refreshOnlineStaffWidget, 30000);
    }

    document.addEventListener("visibilitychange", () => {
        if (!document.hidden) {
            refreshOnlineStaffWidget();
        }
    });
}