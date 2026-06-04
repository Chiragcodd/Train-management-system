if (!Auth.requireAdmin()) throw '';
renderNavUser();

let _coachCount = 0;

qs('#addSeatsBtn').addEventListener('click', () => openModal('seatMod'));
qs('#addCoachBtn').addEventListener('click', addCoach);
qs('#seatForm').addEventListener('submit', onSave);
qs('#viewBtn').addEventListener('click', loadSeats);

qs('#dateSel').value = new Date().toISOString().split('T')[0];

async function init() {
  const data   = await Api.getAllTrains(0).catch(() => ({ content: [] }));
  const trains = data.content || [];
  const opts   = trains.map(t =>
    `<option value="${t.id}">${t.trainName} (#${t.trainNumber})</option>`
  ).join('');
  qs('#trainSel').innerHTML += opts;
  qs('#modTrain').innerHTML += opts;
}

async function loadSeats() {
  const tid  = qs('#trainSel').value;
  const date = qs('#dateSel').value;
  if (!tid)  { Toast.warning('Please select a train'); return; }
  if (!date) { Toast.warning('Please select a travel date'); return; }

  const el = qs('#seatsView');
  el.innerHTML = '<div class="empty"><div class="spinner spinner-lg"></div></div>';

  try {
    const [allSeats, availableSeats] = await Promise.all([
      Api.getSeatsByTrain(tid),
      Api.get(`/api/seats/train/${tid}/available?travelDate=${date}`)
    ]);

    if (!allSeats.length) {
      el.innerHTML = `
        <div class="empty">
          <div class="empty-icon">💺</div>
          <div class="empty-title">No seats added yet</div>
          <button class="btn btn-primary" onclick="openModal('seatMod')">+ Add Seats</button>
        </div>`;
      return;
    }

    const availableIds = new Set(availableSeats.map(s => s.id));
    const grouped = allSeats.reduce((acc, s) => {
      (acc[s.coachType] = acc[s.coachType] || []).push(s); return acc;
    }, {});

    const fmtD = d => new Date(d).toLocaleDateString('en-IN',
      { day:'2-digit', month:'short', year:'numeric' });

    el.innerHTML = `
      <div style="margin-bottom:14px; font-size:.85rem; color:var(--text-muted);">
        Availability for <strong style="color:var(--blue);">${fmtD(date)}</strong>
        — Dynamic fare based on distance &amp; coach type
      </div>
      <div class="grid-3">
        ${Object.entries(grouped).map(([coach, seats]) => {
          const availCount  = seats.filter(s => availableIds.has(s.id)).length;
          const bookedCount = seats.length - availCount;
          return `
          <div class="card">
            <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:10px;">
              <span class="badge badge-blue">${coach}</span>
              <span style="font-size:.78rem;color:var(--text-muted);">${seats.length} total</span>
            </div>
            <div style="display:flex;gap:8px;margin-bottom:14px;">
              <div style="flex:1;text-align:center;padding:8px;
                          background:rgba(0,212,170,.08);border:1px solid rgba(0,212,170,.25);
                          border-radius:8px;">
                <div style="font-size:1.3rem;font-weight:700;color:var(--success);">${availCount}</div>
                <div style="font-size:.7rem;color:var(--text-muted);">Available</div>
              </div>
              <div style="flex:1;text-align:center;padding:8px;
                          background:rgba(248,113,113,.08);border:1px solid rgba(248,113,113,.25);
                          border-radius:8px;">
                <div style="font-size:1.3rem;font-weight:700;color:var(--error);">${bookedCount}</div>
                <div style="font-size:.7rem;color:var(--text-muted);">Booked</div>
              </div>
            </div>
            <div style="display:flex;flex-wrap:wrap;gap:5px;margin-bottom:10px;">
              ${seats.map(s => {
                const isAvail = availableIds.has(s.id);
                return `<div title="Seat ${s.seatNumber} · ${isAvail ? 'Available' : 'Booked'}"
                  style="width:32px;height:32px;border-radius:5px;font-size:.65rem;
                    font-weight:700;font-family:var(--mono);display:flex;
                    align-items:center;justify-content:center;
                    background:${isAvail ? 'rgba(0,212,170,.1)' : 'rgba(248,113,113,.12)'};
                    border:1px solid ${isAvail ? 'rgba(0,212,170,.3)' : 'rgba(248,113,113,.3)'};
                    color:${isAvail ? 'var(--success)' : 'var(--error)'};">
                  ${s.seatNumber}
                </div>`;
              }).join('')}
            </div>
            <div style="font-size:.78rem;color:var(--text-muted);">
              Rate: <strong style="color:var(--blue);">
                ₹${seats[0]?.ratePerKm || '?'}/km
              </strong>
              (base ₹${seats[0]?.baseFare || '?'})
            </div>
          </div>`;
        }).join('')}
      </div>
      <div style="display:flex;gap:14px;margin-top:14px;font-size:.78rem;color:var(--text-muted);">
        <span>🟢 Available</span>
        <span>🔴 Booked</span>
      </div>`;

  } catch (err) {
    el.innerHTML = `<div class="alert alert-error">❌ ${err.message}</div>`;
  }
}

function addCoach() {
  _coachCount++;
  const n   = _coachCount;
  const div = document.createElement('div');
  div.id    = `ch${n}`;
  div.style.cssText = `background:var(--bg-elevated);border:1px solid var(--border);
    border-radius:var(--r-lg);padding:14px;margin-bottom:10px;`;

  // NOTE: price field removed — dynamic fare, admin sirf coach type + count deta hai
  div.innerHTML = `
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px;">
      <span style="font-size:.78rem;font-weight:700;color:var(--blue);">Coach ${n}</span>
      ${n > 1
        ? `<button type="button" class="btn btn-danger btn-xs rm-coach"
             data-n="${n}">Remove</button>`
        : ''}
    </div>
    <div class="form-row">
      <div class="form-group" style="margin:0;">
        <label class="form-label">Coach Type</label>
        <select name="ct${n}" class="form-control" required>
          <option value="">Select</option>
          <option value="SLEEPER">Sleeper (SL)</option>
          <option value="AC_3">AC 3 Tier (3A)</option>
          <option value="AC_2">AC 2 Tier (2A)</option>
          <option value="AC_1">AC 1st Class (1A)</option>
        </select>
      </div>
      <div class="form-group" style="margin:0;">
        <label class="form-label">Seat Count (1–100)</label>
        <input type="number" name="ck${n}" class="form-control"
               min="1" max="100" placeholder="72" required/>
      </div>
    </div>`;

  qs('#coachList').appendChild(div);
  div.querySelector('.rm-coach')?.addEventListener('click', e => {
    qs(`#ch${e.target.dataset.n}`)?.remove();
  });
}

async function onSave(e) {
  e.preventDefault();
  const btn   = qs('#sSaveBtn');
  const alert = qs('#seatAlert');
  alert.innerHTML = '';

  const tid = qs('#modTrain').value;
  if (!tid) { alert.innerHTML = '<div class="alert alert-error">Select a train</div>'; return; }

  const coachEls = qsa('[id^="ch"]');
  if (!coachEls.length) {
    alert.innerHTML = '<div class="alert alert-error">Add at least one coach</div>'; return;
  }

  const coaches = [];
  for (const el of coachEls) {
    const n    = el.id.replace('ch', '');
    const type = qs(`[name="ct${n}"]`, el)?.value;
    const cnt  = qs(`[name="ck${n}"]`, el)?.value;
    if (!type || !cnt) {
      alert.innerHTML = '<div class="alert alert-error">Fill all coach fields</div>';
      return;
    }
    // price nahi bhejte — dynamic fare hai
    coaches.push({ coachType: type, count: +cnt });
  }

  btnLoad(btn, true, 'Adding...');

  try {
    const res = await Api.addSeats({ trainId: +tid, coaches });
    Toast.success(`${res.length} seats added successfully!`);
    closeModal('seatMod');
    qs('#trainSel').value = tid;
    loadSeats();
  } catch (err) {
    alert.innerHTML = `<div class="alert alert-error">❌ ${err.message}</div>`;
    btnLoad(btn, false);
  }
}

init();