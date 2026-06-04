if (!Auth.requireAuth()) throw '';
renderNavUser();

let _filter = 'ALL', _cancelId = null, _cancelStatus = null, _curPage = 0;

qs('.tabs')?.addEventListener('click', e => {
  const btn = e.target.closest('.tab');
  if (!btn) return;
  _filter = btn.dataset.filter;
  _curPage = 0;
  qsa('.tab').forEach(b => b.classList.remove('active'));
  btn.classList.add('active');
  loadBookings(0);
});

qs('#confirmCancelBtn')?.addEventListener('click', doCancel);

function getCountdown(expiresAt) {
  if (!expiresAt) return null;
  const diff = new Date(expiresAt) - new Date();
  if (diff <= 0) return null;
  const mins = Math.floor(diff / 60000);
  const secs = Math.floor((diff % 60000) / 1000);
  return `${mins}:${secs.toString().padStart(2, '0')}`;
}

function makeCard(b) {
  const frag = cloneTpl('tpl-booking-card');

  frag.querySelector('[data-f="pnr"]').textContent =
    b.pnrNumber ? b.pnrNumber : `Booking #${b.bookingId}`;
  frag.querySelector('[data-f="booked-date"]').textContent = fmtDateTime(b.bookingDate);
  frag.querySelector('[data-f="train"]').textContent       = b.trainName;
  frag.querySelector('[data-f="route"]').textContent       = `${b.fromStation} → ${b.toStation}`;
  frag.querySelector('[data-f="travel-date"]').textContent = fmtDate(b.travelDate);
  frag.querySelector('[data-f="amount"]').textContent      = fmtMoney(b.totalAmount);

  const fareEl = frag.querySelector('[data-f="fare"]');
  if (fareEl) {
    fareEl.textContent = b.fareBreakdown
      ? b.fareBreakdown
      : b.farePerPassenger
        ? `${fmtMoney(b.farePerPassenger)}/passenger · ${(b.journeyDistanceKm || 0).toFixed(1)} km`
        : '—';
  }

  const coachEl = frag.querySelector('[data-f="coach"]');
  if (coachEl)
    coachEl.innerHTML = `<span class="badge badge-blue">${b.coachType}</span>`;

  statusBadge(b.status, frag.querySelector('[data-f="status"]'));

  // ── Per-passenger CNF / WL summary ──────────────────────────
  // FIX 1: EXPIRED booking mein CNF badge nahi dikhna chahiye
  const cnf = b.confirmedCount ?? 0;
  const wl  = b.waitlistedCount ?? 0;
  const paxSummaryEl = frag.querySelector('[data-f="pax-summary"]');
  if (paxSummaryEl && (b.passengers || []).length && b.status !== 'EXPIRED') {
    if (wl > 0 && cnf > 0) {
      paxSummaryEl.innerHTML =
        `<span class="badge badge-green" style="font-size:.7rem;">✅ ${cnf} CNF</span> `
        + `<span class="badge badge-orange" style="font-size:.7rem;">⏳ ${wl} WL</span>`;
      show(paxSummaryEl.parentElement || paxSummaryEl);
    } else if (wl > 0) {
      paxSummaryEl.innerHTML =
        `<span class="badge badge-orange" style="font-size:.7rem;">⏳ ${wl} WL</span>`;
      show(paxSummaryEl.parentElement || paxSummaryEl);
    } else if (cnf > 0) {
      paxSummaryEl.innerHTML =
        `<span class="badge badge-green" style="font-size:.7rem;">✅ ${cnf} CNF</span>`;
      show(paxSummaryEl.parentElement || paxSummaryEl);
    }
  }

  // ── Notices ──────────────────────────────────────────────────

  // PENDING_PAYMENT — countdown timer
  if (b.status === 'PENDING_PAYMENT' && b.paymentExpiresAt) {
    const notice = frag.querySelector('.expiry-notice');
    if (notice) {
      const update = () => {
        const cd = getCountdown(b.paymentExpiresAt);
        notice.textContent = cd ? `⏳ Pay within: ${cd}` : '❌ Payment window expired';
        if (!cd) notice.classList.replace('alert-warning', 'alert-error');
        show(notice);
      };
      update();
      const iv = setInterval(() => {
        if (!document.body.contains(notice)) { clearInterval(iv); return; }
        update();
        if (!getCountdown(b.paymentExpiresAt)) clearInterval(iv);
      }, 1000);
    }
  }

  // WAITLISTED notice
  if (b.status === 'WAITLISTED') {
    const notice = frag.querySelector('.waitlist-notice');
    if (notice) {
      const pos  = b.waitlistNumber ? `WL/${b.waitlistNumber}` : 'Waitlisted';
      const paid = !!b.pnrNumber;
      notice.textContent = paid
        ? `⌛ ${pos} — Payment done. Seat confirmed when someone cancels.`
        : `⌛ ${pos} — Pay now to hold your waitlist position.`;
      show(notice);
    }
  } else if (wl > 0 && cnf > 0 && b.status === 'CONFIRMED') {
    const notice = frag.querySelector('.waitlist-notice');
    if (notice) {
      notice.textContent =
        `⚠️ ${wl} passenger(s) still on waitlist — seat(s) will be assigned when available.`;
      show(notice);
    }
  }

  // FIX 2: EXPIRED booking — clear message
  if (b.status === 'EXPIRED') {
    const notice = frag.querySelector('.expiry-notice');
    if (notice) {
      notice.textContent = '❌ Payment was not completed — the booking expired automatically.';
      notice.classList.add('alert-error');
      show(notice);
    }
  }

  // FIX 3: CANCELLED — cancelledAt + refundStatus
  if (b.status === 'CANCELLED') {
    const refundNotice = frag.querySelector('.refund-notice');

    const cancelledAtText = b.cancelledAt
      ? `🕐 Cancelled on: ${fmtDateTime(b.cancelledAt)}`
      : '';

    let refundLine = '';
    if (b.refundAmount > 0) {
      refundLine = `💰 Refund: ${fmtMoney(b.refundAmount)}`;
    } else {
      refundLine = '🚫 No refund applicable (payment was not completed)';
    }

    if (refundNotice) {
      refundNotice.innerHTML =
        `<div style="display:flex;flex-direction:column;gap:4px;">
          ${cancelledAtText ? `<span>${cancelledAtText}</span>` : ''}
          <span>${refundLine}</span>
        </div>`;
      show(refundNotice);
    }
  }

  // ── Action buttons ───────────────────────────────────────────
  const payBtn    = frag.querySelector('.btn-pay');
  const cancelBtn = frag.querySelector('.btn-cancel');

  const needsPay = b.status === 'PENDING_PAYMENT'
    || (b.status === 'WAITLISTED' && !b.pnrNumber);

  if (needsPay) {
    show(payBtn);
    payBtn?.addEventListener('click', () => {
      sessionStorage.setItem('tms_last_booking', JSON.stringify(b));
      location.href = 'payment.html';
    });
  }

  if (['PENDING_PAYMENT', 'CONFIRMED', 'WAITLISTED'].includes(b.status)) {
    show(cancelBtn);
    cancelBtn?.addEventListener('click', () => {
      _cancelId     = b.bookingId;
      _cancelStatus = b.status;
      setText('cId',  `#${b.bookingId}`);
      setText('cAmt', fmtMoney(b.totalAmount));
      const noteEl = qs('#cRefundNote');
      if (noteEl) {
        if (b.status === 'CONFIRMED')
          noteEl.textContent = 'Time-based refund (48h+=75%, 12h+=50%, 4h+=25%, <4h=0%)';
        else if (b.status === 'WAITLISTED' && b.pnrNumber)
          noteEl.textContent = `${fmtMoney(b.totalAmount)} — full refund (paid waitlist)`;
        else
          noteEl.textContent = '₹0 — payment was not completed';
      }
      openModal('cancelMod');
    });
  }

  return frag;
}

// FIX 4: Server-side filtering — renderList ab rows directly leta hai
function renderList(rows) {
  const el = qs('#list');

  if (!rows.length) {
    const label = _filter === 'ALL'
      ? '' : _filter.toLowerCase().replace(/_/g, ' ') + ' ';
    el.innerHTML = `
      <div class="empty">
        <div class="empty-icon">🎫</div>
        <h3 class="empty-title">No ${label}bookings</h3>
        ${_filter === 'ALL'
          ? '<a href="search.html" class="btn btn-primary" style="margin-top:12px;">Search Trains</a>'
          : ''}
      </div>`;
    return;
  }

  const grid = document.createElement('div');
  grid.className = 'grid-2';
  rows.forEach(b => grid.appendChild(makeCard(b)));
  el.innerHTML = '';
  el.appendChild(grid);
}

// FIX 4: Status filter server ko bheja jata hai — client-side filter nahi
async function loadBookings(page = 0) {
  _curPage = page;
  qs('#list').innerHTML =
    '<div class="empty"><div class="spinner spinner-lg"></div></div>';
  try {
    const statusParam = _filter === 'ALL' ? '' : `&status=${_filter}`;
    const data = await Api.get(`/api/bookings/my-bookings?page=${page}&size=10${statusParam}`);
    renderList(data.content || []);
    paginate(data.totalPages || 1, page, 'loadBookings');
  } catch (err) {
    qs('#list').innerHTML =
      `<div class="alert alert-error">❌ ${err.message}</div>`;
  }
}

async function doCancel() {
  const btn = qs('#confirmCancelBtn');
  btnLoad(btn, true, 'Cancelling...');
  try {
    await Api.cancelBooking(_cancelId);
    Toast.success('Booking cancelled successfully');
    closeModal('cancelMod');
    loadBookings(_curPage);
  } catch (err) {
    Toast.error(err.message);
    btnLoad(btn, false);
  }
}

loadBookings();