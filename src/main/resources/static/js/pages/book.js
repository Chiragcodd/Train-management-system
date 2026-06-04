if (!Auth.requireAuth()) throw '';
renderNavUser();

const _bdata = JSON.parse(sessionStorage.getItem('tms_booking') || '{}');
if (!_bdata.trainId) {
  location.href = 'search.html';
  throw '';
}

// ── Journey Summary ─────────────────────────────
setText('sumTrain', `${_bdata.trainName} — #${_bdata.trainNumber}`);
setText('sumRoute', `${_bdata.fromStationCode} → ${_bdata.toStationCode}`);
setText('sumDate', fmtDate(_bdata.travelDate));

const COACH_LABELS = {
  SLEEPER: 'Sleeper (SL)',
  AC_3: 'AC 3 Tier (3A)',
  AC_2: 'AC 2 Tier (2A)',
  AC_1: 'AC 1st Class (1A)',
};

// ── Coach change + fare ─────────────────────────
qs('#coachType').addEventListener('change', async () => {
  const v = qs('#coachType').value;

  setText('spCoach', COACH_LABELS[v] || '—');

  if (v) {
    try {
      const fare = await Api.getFarePreview(
        _bdata.trainId,
        _bdata.fromStationCode,
        _bdata.toStationCode,
        v
      );

      setText(
        'spFare',
        fare.breakdown || `₹${fare.farePerPassenger} per passenger`
      );

      const preview = qs('#farePreview');
      const fareText = qs('#fareText');

      if (preview && fareText) {
        fareText.textContent =
          fare.breakdown || `₹${fare.farePerPassenger}`;
        show(preview);
      }
    } catch {
      setText('spFare', 'Fare not available');
    }
  } else {
    setText('spFare', 'Select coach to see fare');
    hide(qs('#farePreview'));
  }
});

// ── PASSENGER SYSTEM (FIXED) ─────────────────────────

// no counter needed anymore ❌
function addPassenger() {
  const count = qsa('.pax-block').length;

  if (count >= 6) {
    Toast.warning('Maximum 6 passengers allowed');
    return;
  }

  const n = count + 1;

  const frag = cloneTpl('tpl-pax-block');
  const root = frag.querySelector('.pax-block');

  frag.querySelector('[data-f="label"]').textContent = `Passenger ${n}`;

  const rmBtn = frag.querySelector('.remove-pax');

  if (n > 1) {
    show(rmBtn);

    rmBtn.addEventListener('click', () => {
      root.remove();
      renumberPassengers(); // 🔥 IMPORTANT
    });
  }

  qs('#paxList').appendChild(frag);

  updatePassengerCount();
}

// ── IMPORTANT: Re-number after delete ───────────
function renumberPassengers() {
  const blocks = qsa('.pax-block');

  blocks.forEach((b, i) => {
    const label = b.querySelector('[data-f="label"]');
    if (label) label.textContent = `Passenger ${i + 1}`;
  });

  updatePassengerCount();
}

// ── update count ────────────────────────────────
function updatePassengerCount() {
  setText('spCount', qsa('.pax-block').length);
}

// ── INIT ────────────────────────────────────────
qs('#addPaxBtn').addEventListener('click', addPassenger);

// start with 1 passenger
addPassenger();

// ── FORM SUBMIT ────────────────────────────────
qs('#bookForm').addEventListener('submit', async e => {
  e.preventDefault();

  const btn = qs('#bookBtn');
  const alert = qs('#bookAlert');
  alert.innerHTML = '';

  const coachType = qs('#coachType').value;
  if (!coachType) {
    Toast.warning('Select coach class');
    return;
  }

  const blocks = qsa('.pax-block');

  if (!blocks.length) {
    Toast.warning('Add at least 1 passenger');
    return;
  }

  const passengers = [];

  let valid = true;

  blocks.forEach(block => {
    const name = block.querySelector('.pax-name')?.value?.trim();
    const age = parseInt(block.querySelector('.pax-age')?.value || '0');
    const gender = block.querySelector('.pax-gender')?.value;

    if (!name || !age || !gender) valid = false;

    passengers.push({ name, age, gender });
  });

  if (!valid) {
    Toast.warning('Fill all passenger details');
    return;
  }

  btnLoad(btn, true, 'Booking...');

  try {
    const payload = {
      trainId: _bdata.trainId,
      coachType,
      fromStationCode: _bdata.fromStationCode,
      toStationCode: _bdata.toStationCode,
      travelDate: _bdata.travelDate,
      passengers,
    };

    const res = await Api.bookTicket(payload);

    sessionStorage.setItem('tms_last_booking', JSON.stringify(res));
    sessionStorage.removeItem('tms_booking');

    if (res.status === 'WAITLISTED') {
      Toast.success(`Waitlisted WL/${res.waitlistNumber}`);
    } else {
      Toast.success('Booking created!');
    }

    setTimeout(() => {
      location.href = 'payment.html';
    }, 800);
  } catch (err) {
    alert.innerHTML = `<div class="alert alert-error">❌ ${err.message}</div>`;
    btnLoad(btn, false);
  }
});