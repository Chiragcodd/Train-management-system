if (!Auth.requireAdmin()) throw '';
renderNavUser();

function makeRecentRow(b) {
  const frag = cloneTpl('tpl-booking-row');
  // PNR null for PENDING_PAYMENT/unpaid WAITLISTED
  frag.querySelector('[data-f="pnr"]').textContent    =
    b.pnrNumber ? b.pnrNumber : `#${b.bookingId}`;
  frag.querySelector('[data-f="user"]').textContent   = b.userName;
  frag.querySelector('[data-f="train"]').textContent  = b.trainName;
  frag.querySelector('[data-f="route"]').textContent  = `${b.fromStation} → ${b.toStation}`;
  frag.querySelector('[data-f="date"]').textContent   = fmtDate(b.travelDate);
  frag.querySelector('[data-f="amount"]').textContent = fmtMoney(b.totalAmount);
  statusBadge(b.status, frag.querySelector('[data-f="status"]'));
  return frag;
}

async function loadAdminDashboard() {
  try {
    const [trains, stations, bookings, users] = await Promise.allSettled([
      Api.getAllTrains(0),
      Api.getAllStations(),
      Api.getAllBookings(0),
      Api.getAllUsers(0),
    ]);

    setText('sTrains',   trains.value?.totalElements   ?? '—');
    setText('sStations', stations.value?.length         ?? '—');
    setText('sBookings', bookings.value?.totalElements  ?? '—');
    setText('sUsers',    users.value?.totalElements     ?? '—');

    const rows  = bookings.value?.content?.slice(0, 8) || [];
    const tbody = qs('#recentBody');
    const empty = qs('#recentEmpty');
    if (!tbody) return;
    tbody.innerHTML = '';

    if (!rows.length) { show(empty); }
    else { hide(empty); rows.forEach(b => tbody.appendChild(makeRecentRow(b))); }

  } catch (err) {
    console.error('Admin dashboard error:', err.message);
  }
}

loadAdminDashboard();

// Cards par click chalane ka naya code (Aakhiri me jodein)
document.addEventListener('DOMContentLoaded', () => {
  document.getElementById('cardTrains')?.addEventListener('click', () => {
    window.location.href = 'trains.html';
  });

  document.getElementById('cardStations')?.addEventListener('click', () => {
    window.location.href = 'stations.html';
  });

  document.getElementById('cardBookings')?.addEventListener('click', () => {
    window.location.href = 'bookings.html';
  });

  document.getElementById('cardUsers')?.addEventListener('click', () => {
    window.location.href = 'users.html';
  });
});