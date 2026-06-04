if (!Auth.requireAuth()) throw '';
renderNavUser();

const _bk = JSON.parse(sessionStorage.getItem('tms_last_booking') || '{}');
if (!_bk.bookingId) { location.href = 'bookings.html'; throw ''; }

setText('bRef',    `#${_bk.bookingId}`);
setText('bTrain',  `${_bk.trainName} (#${_bk.trainNumber})`);
setText('bRoute',  `${_bk.fromStation} → ${_bk.toStation}`);
setText('bDate',   fmtDate(_bk.travelDate));
setText('bPax',    (_bk.passengers || []).length);
setText('bAmount', fmtMoney(_bk.totalAmount));

const fareEl = qs('#bFare');
if (fareEl) {
  fareEl.textContent = _bk.fareBreakdown
    ? _bk.fareBreakdown
    : _bk.farePerPassenger
      ? `${fmtMoney(_bk.farePerPassenger)}/passenger · ${(_bk.journeyDistanceKm || 0).toFixed(1)} km`
      : '—';
}

const wlNotice = qs('#wlNotice');
if (wlNotice && _bk.status === 'WAITLISTED' && _bk.waitlistNumber) {
  wlNotice.textContent =
    `⌛ You are on the waitlist (WL/${_bk.waitlistNumber}). `
    + `Pay now to hold your position. Seat will be assigned when available.`;
  show(wlNotice);
}

let _timerInterval = null;

function startTimer() {
  const expiresAt = _bk.paymentExpiresAt
    ? new Date(_bk.paymentExpiresAt)
    : new Date(Date.now() + 5 * 60 * 1000);

  function tick() {
    const remaining = expiresAt - Date.now();
    if (remaining <= 0) { clearInterval(_timerInterval); onTimerExpired(); return; }
    const mins = Math.floor(remaining / 60000);
    const secs = Math.floor((remaining % 60000) / 1000);
    setText('timerDisplay',
      `${String(mins).padStart(2,'0')}:${String(secs).padStart(2,'0')}`);
    const timerEl = qs('#timerDisplay');
    if (timerEl && remaining < 60000)
      timerEl.style.opacity = Date.now() % 1000 < 500 ? '1' : '0.4';
  }

  tick();
  _timerInterval = setInterval(tick, 1000);
}

function onTimerExpired() {
  clearInterval(_timerInterval);
  sessionStorage.removeItem('tms_last_booking');
  hide(qs('#payPage'));
  hide(qs('#timerBox'));
  show(qs('#expiredState'));
}

startTimer();

qs('#payLaterBtn')?.addEventListener('click', () => {
  if (confirm('⚠️ Warning!\n\nIf you leave, your booking will expire in 5 minutes.\n\nContinue?')) {
    clearInterval(_timerInterval);
    location.href = 'bookings.html';
  }
});

qs('#payBtn').addEventListener('click', async () => {
  const btn   = qs('#payBtn');
  const alert = qs('#payAlert');
  alert.innerHTML = '';

  const method = qs('input[name="pm"]:checked')?.value;
  if (!method) { Toast.warning('Select a payment method'); return; }

  btnLoad(btn, true, 'Processing...');

  try {
    const res = await Api.makePayment({
      bookingId:     _bk.bookingId,
      paymentMethod: method,
      amount:        parseFloat(_bk.totalAmount),
    });

    clearInterval(_timerInterval);
    sessionStorage.removeItem('tms_last_booking');

    hide(qs('#payPage'));
    hide(qs('#timerBox'));
    show(qs('#successState'));

    setText('sPnr',    res.pnrNumber);
    setText('sTxn',    res.transactionId);
    setText('sAmt',    fmtMoney(res.amount));
    setText('sMethod', res.paymentMethod);
    setText('sDate',   fmtDateTime(res.paymentDate));

    const successMsg = qs('#successMsg');
    if (successMsg) {
      successMsg.textContent = _bk.status === 'WAITLISTED'
        ? '✅ Payment done! You are on the waitlist. Seat confirmed when someone cancels.'
        : '✅ Booking confirmed! Have a great journey. 🚂';
    }

  } catch (err) {
    if (err.message?.toLowerCase().includes('expired')) {
      clearInterval(_timerInterval);
      sessionStorage.removeItem('tms_last_booking');
      onTimerExpired();
      return;
    }
    alert.innerHTML = `<div class="alert alert-error">❌ ${err.message}</div>`;
    btnLoad(btn, false);
  }
});