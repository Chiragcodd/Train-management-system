if (!Auth.requireAuth()) throw '';

const _user = Auth.getUser();
renderNavUser();
setText('heroName', _user.username);

const today  = new Date().toISOString().split('T')[0];
const dateEl = qs('#qDate');
if (dateEl) { dateEl.min = today; dateEl.value = today; }

qs('#quickSearchBtn')?.addEventListener('click', () => {
  const f = qs('#qFrom').value.trim().toUpperCase();
  const t = qs('#qTo').value.trim().toUpperCase();
  const d = qs('#qDate').value;
  if (!f || !t) { Toast.warning('Enter both station codes'); return; }
  location.href = `search.html?from=${f}&to=${t}&date=${d}`;
});

function makeBookingMini(b) {
  const frag = cloneTpl('tpl-booking-mini');
  // PNR null for PENDING_PAYMENT / unpaid WAITLISTED
  frag.querySelector('[data-f="pnr"]').textContent    =
    b.pnrNumber ? b.pnrNumber : `Booking #${b.bookingId}`;
  frag.querySelector('[data-f="date"]').textContent   = fmtDateTime(b.bookingDate);
  frag.querySelector('[data-f="train"]').textContent  = b.trainName;
  frag.querySelector('[data-f="route"]').textContent  = `${b.fromStation} → ${b.toStation}`;
  frag.querySelector('[data-f="travel"]').textContent = fmtDate(b.travelDate);
  frag.querySelector('[data-f="amount"]').textContent = fmtMoney(b.totalAmount);
  statusBadge(b.status, frag.querySelector('[data-f="status"]'));
  return frag;
}

async function loadDashboard() {
  const container = qs('#recentList');
  ['sTot','sConf','sWait'].forEach(id => setText(id, '0'));
  setText('sSpent', '₹0.00');

  try {
    const data = await Api.get('/api/bookings/my-bookings?page=0&size=10');
    const list = data.content || [];

    const confirmed   = list.filter(b => b.status === 'CONFIRMED');
    const waitlisted  = list.filter(b => b.status === 'WAITLISTED');
    const spent       = confirmed.reduce((s, b) => s + parseFloat(b.totalAmount || 0), 0);

    setText('sTot',   data.totalElements ?? list.length);
    setText('sConf',  confirmed.length);
    setText('sWait',  waitlisted.length);
    setText('sSpent', fmtMoney(spent));

    if (!list.length) {
      container.innerHTML = `
        <div class="empty">
          <div class="empty-icon">🎫</div>
          <h3 class="empty-title">No bookings yet</h3>
          <a href="search.html" class="btn btn-primary" style="margin-top:12px;">
            Search Trains
          </a>
        </div>`;
      return;
    }

    container.innerHTML = '';
    const grid = document.createElement('div');
    grid.className = 'grid-2';
    list.slice(0, 4).forEach(b => grid.appendChild(makeBookingMini(b)));
    container.appendChild(grid);

  } catch (err) {
    container.innerHTML = `<div class="alert alert-error">❌ ${err.message}</div>`;
  }
}

loadDashboard();