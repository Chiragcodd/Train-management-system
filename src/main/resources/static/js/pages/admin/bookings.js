if (!Auth.requireAdmin()) throw '';
renderNavUser();

let _all = [], _filter = 'ALL', _curPage = 0;

qsa('.tab').forEach(btn => {
  btn.addEventListener('click', () => {
    _filter = btn.dataset.filter;
    qsa('.tab').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    renderTable();
  });
});

async function loadBookings(page = 0) {
  _curPage = page;
  const el = qs('#bkTable');
  el.innerHTML = '<div class="empty"><div class="spinner spinner-lg"></div></div>';
  try {
    const data = await Api.getAllBookings(page);
    _all = data.content || [];
    setText('totalB', `${data.totalElements || _all.length} total`);
    renderTable();
    paginate(data.totalPages || 1, page, 'loadBookings');
  } catch (err) {
    el.innerHTML = `<div class="alert alert-error">❌ ${err.message}</div>`;
  }
}

function renderTable() {
  const rows = _filter === 'ALL'
    ? _all : _all.filter(b => b.status === _filter);
  const el = qs('#bkTable');

  if (!rows.length) {
    el.innerHTML = '<div class="empty"><div class="empty-title">No bookings found</div></div>';
    return;
  }

  el.innerHTML = `
    <table>
      <thead>
        <tr>
          <th>PNR / Booking</th><th>User</th><th>Train</th>
          <th>Route</th><th>Travel Date</th><th>Coach</th>
          <th>Fare Info</th><th>Total</th><th>Status</th><th>Action</th>
        </tr>
      </thead>
      <tbody>
        ${rows.map(b => `
          <tr>
            <td style="font-family:var(--mono); font-weight:600;">
              ${b.pnrNumber
                ? `<span style="color:var(--blue);">${b.pnrNumber}</span>`
                : `<span style="color:var(--text-muted); font-size:.78rem;">#${b.bookingId}</span>`}
              ${b.status === 'PENDING_PAYMENT' && b.paymentExpiresAt
                ? `<div style="font-size:.7rem; color:var(--amber); margin-top:2px;">
                     ⏳ Expires: ${fmtDateTime(b.paymentExpiresAt)}
                   </div>` : ''}
              ${b.status === 'WAITLISTED'
                ? `<div style="font-size:.7rem; color:var(--blue); margin-top:2px;">
                     ${b.waitlistNumber ? `WL/${b.waitlistNumber}` : 'WL'}
                     ${b.pnrNumber ? ' ✅ Paid' : ' ⏳ Unpaid'}
                   </div>` : ''}
            </td>
            <td>${b.userName}</td>
            <td>
              <div>${b.trainName}</div>
              <div style="font-size:.72rem; color:var(--text-muted); font-family:var(--mono);">
                #${b.trainNumber}
              </div>
            </td>
            <td>${b.fromStation} → ${b.toStation}</td>
            <td>${fmtDate(b.travelDate)}</td>
            <td><span class="badge badge-blue">${b.coachType}</span></td>
            <td style="font-size:.75rem; color:var(--text-muted);">
              ${b.fareBreakdown
                ? b.fareBreakdown
                : b.farePerPassenger
                  ? `${fmtMoney(b.farePerPassenger)}/pax · ${(b.journeyDistanceKm||0).toFixed(0)}km`
                  : '—'}
            </td>
            <td style="font-family:var(--mono); font-weight:600;">${fmtMoney(b.totalAmount)}</td>
            <td>${_statusBadgeHtml(b.status)}</td>
            <td>
              ${['PENDING_PAYMENT','CONFIRMED','WAITLISTED'].includes(b.status)
                ? `<button class="btn btn-danger btn-sm cancel-btn"
                           data-id="${b.bookingId}"
                           data-status="${b.status}"
                           data-pnr="${b.pnrNumber || ''}">Cancel</button>`
                : '—'}
            </td>
          </tr>`).join('')}
      </tbody>
    </table>`;

  el.onclick = async e => {
    const btn = e.target.closest('.cancel-btn');
    if (!btn) return;
    const status  = btn.dataset.status;
    const hasPnr  = !!btn.dataset.pnr;
    const refundNote = status === 'CONFIRMED'
      ? 'Refund: time-based (48h=75%, 12h=50%, 4h=25%, <4h=0%)'
      : (status === 'WAITLISTED' && hasPnr)
        ? 'Refund: full amount (paid waitlist)'
        : 'Refund: ₹0 (payment not completed)';
    if (!confirm(`Cancel this booking?\n\n${refundNote}`)) return;
    Loader.show('Cancelling...');
    try {
      await Api.cancelBooking(btn.dataset.id);
      Toast.success('Booking cancelled');
      loadBookings(_curPage);
    } catch (err) {
      Toast.error(err.message);
    } finally {
      Loader.hide();
    }
  };
}

loadBookings();

