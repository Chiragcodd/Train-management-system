document.addEventListener('DOMContentLoaded', init);

// ─── Constants ───────────────────────────────────────────────
const DAYS = [
  'MONDAY','TUESDAY','WEDNESDAY',
  'THURSDAY','FRIDAY','SATURDAY','SUNDAY'
];

const DAY_SHORT = {
  MONDAY:'Mon', TUESDAY:'Tue', WEDNESDAY:'Wed',
  THURSDAY:'Thu', FRIDAY:'Fri', SATURDAY:'Sat', SUNDAY:'Sun'
};

const TRAIN_TYPES = [
  'EXPRESS','SUPERFAST','LOCAL',
  'RAJDHANI','SHATABDI','DURONTO','VANDE_BHARAT'
];

// ─── State ───────────────────────────────────────────────────
let _stations  = [];
let _stopCount = 0;   
let _editId    = null;
let _delId     = null;
let _statusId  = null;
let _curPage   = 0;

// ─── HTML escape to prevent XSS ──────────────────────────────
function escHtml(str) {
  if (str == null) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

// ═══════════════════════════════════════════════════════════
//  INIT
// ═══════════════════════════════════════════════════════════
async function init() {
  if (!Auth.requireAdmin()) return;

  renderNavUser();
  renderDays();     // populate day checkboxes  ← was MISSING
  renderTypes();    // populate train type dropdown ← was MISSING
  bindEvents();
  await loadStations();
  loadTrains();
}

// ═══════════════════════════════════════════════════════════
//  RENDER DAYS CHECKBOXES
// ═══════════════════════════════════════════════════════════
function renderDays() {
  const wrap = qs('#daysWrap');
  if (!wrap) return;

  wrap.innerHTML = DAYS.map(d => `
    <label class="day-pill">
      <input type="checkbox" name="day_${d}" value="${d}" />
      ${DAY_SHORT[d]}
    </label>
  `).join('');
}

// ═══════════════════════════════════════════════════════════
//  RENDER TRAIN TYPE OPTIONS
// ═══════════════════════════════════════════════════════════
function renderTypes() {
  const sel = qs('#tTrainType');
  if (!sel) return;

  sel.innerHTML =
    `<option value="">Select Type</option>` +
    TRAIN_TYPES.map(t =>
      `<option value="${t}">${t.replace(/_/g, ' ')}</option>`
    ).join('');
}

// ═══════════════════════════════════════════════════════════
//  BIND EVENTS
// ═══════════════════════════════════════════════════════════
function bindEvents() {
  qs('#addTrainBtn')?.addEventListener('click', openAdd);
  qs('#addStopBtn')?.addEventListener('click', () => addStop());
  qs('#trainForm')?.addEventListener('submit', onSave);
  qs('#tDelBtn')?.addEventListener('click', doDelete);
  qs('#logoutBtn')?.addEventListener('click', () => Auth.logout());

  // Status change buttons
  qs('#sBtnActive')?.addEventListener('click',   () => doStatusChange('ACTIVE'));
  qs('#sBtnInactive')?.addEventListener('click', () => doStatusChange('INACTIVE'));
  qs('#sBtnDelayed')?.addEventListener('click',  () => doStatusChange('DELAYED'));
  qs('#sBtnCancelled')?.addEventListener('click', () => doStatusChange('CANCELLED'));

  // [data-close="modalId"] buttons ← was completely MISSING
  document.querySelectorAll('[data-close]').forEach(btn => {
    btn.addEventListener('click', () => closeModal(btn.dataset.close));
  });

  // Click overlay to close modal
  document.querySelectorAll('.modal-overlay').forEach(overlay => {
    overlay.addEventListener('click', e => {
      if (e.target === overlay) closeModal(overlay.id);
    });
  });
}

// ═══════════════════════════════════════════════════════════
//  LOAD STATIONS
// ═══════════════════════════════════════════════════════════
async function loadStations() {
  try {
    _stations = await Api.getAllStations();
  } catch (err) {
    console.error('loadStations error:', err);
    Toast.error('Could not load stations');
    _stations = [];
  }
}

// ═══════════════════════════════════════════════════════════
//  LOAD TRAINS (paginated)
// ═══════════════════════════════════════════════════════════
async function loadTrains(page = 0) {
  _curPage = page;

  const el = qs('#trainList');
  if (!el) return;

  el.innerHTML = `<div class="empty"><div class="spinner spinner-lg"></div></div>`;

  try {
    const data   = await Api.getAllTrains(page);
    const trains = data?.content || [];

    if (!trains.length) {
      el.innerHTML = `
        <div class="empty">
          <div class="empty-title">No trains found</div>
          <p class="empty-sub">Click "+ Add Train" to create one.</p>
        </div>`;
      return;
    }

    el.innerHTML = trains.map(t => createTrainCard(t)).join('');

    // Delegate card button clicks
    el.onclick = e => {
      const editBtn   = e.target.closest('.edit-btn');
      const delBtn    = e.target.closest('.del-btn');
      const statusBtn = e.target.closest('.status-btn');

      if (editBtn)   { openEdit(editBtn.dataset.id); return; }
      if (delBtn)    { openDel(delBtn.dataset.id);   return; }
      if (statusBtn) { openStatus(statusBtn.dataset.id, statusBtn.dataset.name); }
    };

    paginate(data.totalPages || 1, page, 'loadTrains');

  } catch (err) {
    console.error('loadTrains error:', err);
    el.innerHTML = `<div class="alert alert-error">${escHtml(err.message)}</div>`;
  }
}

// ═══════════════════════════════════════════════════════════
//  TRAIN CARD HTML
// ═══════════════════════════════════════════════════════════
function createTrainCard(t) {
  const statusClass = `status-${t.status || 'ACTIVE'}`;

  const routeChips = (t.routes || [])
    .sort((a, b) => a.stopOrder - b.stopOrder)
    .map(r => `
      <span class="badge badge-neutral">
        ${r.stopOrder}. ${escHtml(r.stationCode)}
      </span>
    `).join('');

  const daysText = t.runningDays?.length
    ? '🗓 ' + t.runningDays.map(d => DAY_SHORT[d] || d).join(' • ')
    : '🗓 All Days';

  return `
    <div class="card" style="margin-bottom:12px;">
      <div style="display:flex;justify-content:space-between;gap:16px;flex-wrap:wrap;">

        <div style="flex:1;min-width:0;">
          <div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap;margin-bottom:8px;">
            <strong>${escHtml(t.trainName)}</strong>
            <span class="badge badge-neutral">#${escHtml(t.trainNumber)}</span>
            ${t.trainType
              ? `<span class="badge badge-blue">${t.trainType.replace(/_/g,' ')}</span>`
              : ''}
            <span class="train-status-badge ${statusClass}">${t.status || 'ACTIVE'}</span>
          </div>
          <div style="font-size:.8rem;color:var(--text-muted);margin-bottom:10px;">${daysText}</div>
          <div style="display:flex;flex-wrap:wrap;gap:6px;">${routeChips}</div>
        </div>

        <div class="card-actions">
          <button class="btn btn-ghost btn-sm edit-btn"
                  data-id="${t.id}">✏️ Edit</button>
          <button class="btn btn-outline btn-sm status-btn"
                  data-id="${t.id}"
                  data-name="${escHtml(t.trainName)}">🔄 Status</button>
          <button class="btn btn-danger btn-sm del-btn"
                  data-id="${t.id}">🗑️ Delete</button>
        </div>

      </div>
    </div>`;
}

// ═══════════════════════════════════════════════════════════
//  OPEN ADD MODAL
// ═══════════════════════════════════════════════════════════
function openAdd() {
  _editId = null;

  qs('#tModTitle').textContent = 'Add Train';
  qs('#trainForm').reset();
  qs('#stopList').innerHTML = '';
  qs('#tAlert').innerHTML   = '';

  renderDays(); // re-render so checkboxes are fresh after form.reset()

  openModal('trainMod');
  addStop(); // min 2 stops
  addStop();
}

// ═══════════════════════════════════════════════════════════
//  OPEN EDIT MODAL
// ═══════════════════════════════════════════════════════════
async function openEdit(id) {
  _editId = id;

  qs('#tModTitle').textContent = 'Edit Train';
  qs('#stopList').innerHTML    = '';
  qs('#tAlert').innerHTML      = '';

  renderDays(); // fresh checkboxes

  try {
    const t = await Api.getTrainById(id);

    qs('#tName').value       = t.trainName   || '';
    qs('#tNum').value        = t.trainNumber || '';
    qs('#tTrainType').value  = t.trainType   || '';

    (t.runningDays || []).forEach(d => {
      const cb = qs(`[name="day_${d}"]`);
      if (cb) cb.checked = true;
    });

    const sorted = (t.routes || []).sort((a, b) => a.stopOrder - b.stopOrder);
    sorted.forEach(r => {
      const st = _stations.find(s => s.code === r.stationCode);
      addStop(
        st?.id || '',
        r.stopOrder,
        r.arrivalTime,
        r.departureTime,
        r.distanceFromOrigin ?? 0
      );
    });

    openModal('trainMod');
  } catch (err) {
    Toast.error(err.message);
  }
}

// ═══════════════════════════════════════════════════════════
//  DELETE MODAL
// ═══════════════════════════════════════════════════════════
function openDel(id) {
  _delId = id;
  openModal('tDelMod');
}

async function doDelete() {
  if (!_delId) return;

  const btn = qs('#tDelBtn');
  btn.disabled    = true;
  btn.textContent = 'Deleting…';

  try {
    await Api.deleteTrain(_delId);
    Toast.success('Train deleted successfully');
    closeModal('tDelMod');
    loadTrains(_curPage);
  } catch (err) {
    Toast.error(err.message);
  } finally {
    btn.disabled    = false;
    btn.textContent = 'Delete';
  }
}

// ═══════════════════════════════════════════════════════════
//  STATUS MODAL
// ═══════════════════════════════════════════════════════════
function openStatus(id, name) {
  _statusId = id;
  qs('#sTrainName').textContent = name || `Train #${id}`;
  qs('#sAlert').innerHTML = '';
  openModal('tStatusMod');
}

async function doStatusChange(status) {
  if (!_statusId) return;

  const btns = ['#sBtnActive','#sBtnInactive','#sBtnDelayed']
    .map(s => qs(s));
  btns.forEach(b => { if (b) b.disabled = true; });

  qs('#sAlert').innerHTML = '';

  try {
    await Api.updateTrainStatus(_statusId, status);
    Toast.success(`Status changed to ${status}`);
    closeModal('tStatusMod');
    loadTrains(_curPage);
  } catch (err) {
    qs('#sAlert').innerHTML =
      `<div class="alert alert-error">${escHtml(err.message)}</div>`;
  } finally {
    btns.forEach(b => { if (b) b.disabled = false; });
  }
}

// ═══════════════════════════════════════════════════════════
//  ADD STOP ROW
//  Using document.getElementById() for scoped reads — avoids
//  any conflict with ui.js's qs() signature
// ═══════════════════════════════════════════════════════════
function addStop(stationId = '', order = '', arr = '', dep = '', dist = 0) {
  _stopCount++;
  const n            = _stopCount;
  const defaultOrder = order !== '' ? order
    : qs('#stopList').children.length + 1;

  const div     = document.createElement('div');
  div.id        = `stop_${n}`;
  div.className = 'stop-box';

  div.innerHTML = `
    <div class="stop-box-head">
      <span class="stop-label">Stop ${defaultOrder}</span>
      <button type="button" class="btn-remove-stop"
              onclick="removeStop('stop_${n}')">✕ Remove</button>
    </div>

    <div class="form-row">
      <div class="form-group">
        <label class="form-label">Station</label>
        <select id="ss_${n}" class="form-control" required>
          <option value="">Select Station</option>
          ${_stations.map(s => `
            <option value="${s.id}" ${stationId == s.id ? 'selected' : ''}>
              ${escHtml(s.name)} (${escHtml(s.code)})
            </option>
          `).join('')}
        </select>
      </div>
      <div class="form-group">
        <label class="form-label">Stop Order</label>
        <input id="so_${n}" type="number" class="form-control"
               value="${defaultOrder}" min="1" required />
      </div>
    </div>

    <div class="form-row-3">
      <div class="form-group">
        <label class="form-label">Arrival</label>
        <input id="sa_${n}" type="time" class="form-control"
               value="${arr || ''}" required />
      </div>
      <div class="form-group">
        <label class="form-label">Departure</label>
        <input id="sd_${n}" type="time" class="form-control"
               value="${dep || ''}" required />
      </div>
      <div class="form-group">
        <label class="form-label">Distance (km)</label>
        <input id="sdist_${n}" type="number" class="form-control"
               value="${dist ?? 0}" min="0" />
      </div>
    </div>`;

  qs('#stopList').appendChild(div);
}

function removeStop(boxId) {
  if (qs('#stopList').children.length <= 2) {
    Toast.error('Minimum 2 stops required');
    return;
  }
  document.getElementById(boxId)?.remove();
}

// ═══════════════════════════════════════════════════════════
//  SAVE (Add or Update)
// ═══════════════════════════════════════════════════════════
async function onSave(e) {
  e.preventDefault();

  const stopBoxes = Array.from(
    qs('#stopList').querySelectorAll('[id^="stop_"]')
  );

  if (stopBoxes.length < 2) {
    showFormAlert('Minimum 2 route stops are required.');
    return;
  }

  // Use document.getElementById() — no qs() signature conflict
  const routes = stopBoxes.map(box => {
    const n = box.id.replace('stop_', '');
    return {
      stationId:          +(document.getElementById(`ss_${n}`)?.value    || 0),
      stopOrder:          +(document.getElementById(`so_${n}`)?.value    || 0),
      arrivalTime:          document.getElementById(`sa_${n}`)?.value    || '',
      departureTime:        document.getElementById(`sd_${n}`)?.value    || '',
      distanceFromOrigin: +(document.getElementById(`sdist_${n}`)?.value || 0),
    };
  });

  if (routes.some(r => !r.stationId)) {
    showFormAlert('Please select a station for every stop.');
    return;
  }

  const payload = {
    trainName:   qs('#tName').value.trim(),
    trainNumber: qs('#tNum').value.trim(),
    trainType:   qs('#tTrainType').value,
    runningDays: DAYS.filter(d => qs(`[name="day_${d}"]`)?.checked),
    routes,
  };

  const btn = qs('#tSaveBtn');
  btn.disabled    = true;
  btn.textContent = 'Saving…';
  qs('#tAlert').innerHTML = '';

  try {
    if (_editId) {
      await Api.updateTrain(_editId, payload);
    } else {
      await Api.addTrain(payload);
    }
    Toast.success(_editId ? 'Train updated successfully' : 'Train added successfully');
    closeModal('trainMod');
    loadTrains(_curPage);
  } catch (err) {
    showFormAlert(err.message);
  } finally {
    btn.disabled    = false;
    btn.textContent = 'Save Train';
  }
}

// ─── Form alert ───────────────────────────────────────────
function showFormAlert(msg) {
  qs('#tAlert').innerHTML =
    `<div class="alert alert-error" style="margin-top:12px;">${escHtml(msg)}</div>`;
}