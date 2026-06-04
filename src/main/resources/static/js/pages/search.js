if (!Auth.requireAuth()) throw '';
renderNavUser();

const _area   = document.getElementById('resultsArea');
const _params = new URLSearchParams(location.search);

const today = new Date().toISOString().split('T')[0];
const _dateEl = qs('#travelDate');
_dateEl.min   = today;
_dateEl.value = _params.get('date') || today;

// ── Coach config ──────────────────────────────────────────────
const COACH_CFG = {
  SLEEPER: { label: 'Sleeper (SL)',      color: '#4F8EF7' },
  AC_3:    { label: 'AC 3 Tier (3A)',    color: '#00D4AA' },
  AC_2:    { label: 'AC 2 Tier (2A)',    color: '#8B5CF6' },
  AC_1:    { label: 'AC 1st Class (1A)', color: '#FBBF24' },
};

// ══════════════════════════════════════════════════════════════
//  STATION AUTOCOMPLETE
// ══════════════════════════════════════════════════════════════
function makeAutocomplete(opts) {
  const { inputEl, listEl, codeEl, badgeEl, clearEl } = opts;

  let debounceTimer = null;
  let selectedCode  = '';
  let activeIdx     = -1;
  let items         = [];

  // Pre-fill from URL params
  const prefill = opts.prefill;
  if (prefill) {
    inputEl.value   = prefill;
    codeEl.value    = prefill;
    badgeEl.textContent = prefill;
    selectedCode    = prefill;
    clearEl.classList.add('visible');
    // Try to resolve display name
    Api.searchStations(prefill).then(res => {
      const match = res.find(s => s.code === prefill.toUpperCase());
      if (match) {
        inputEl.value       = match.name;
        codeEl.value        = match.code;
        badgeEl.textContent = match.code;
        selectedCode        = match.code;
      }
    }).catch(() => {});
  }

  function showList(html) {
    listEl.innerHTML = html;
    listEl.classList.add('open');
    activeIdx = -1;
    items = Array.from(listEl.querySelectorAll('.ac-item'));
  }

  function hideList() {
    listEl.classList.remove('open');
    listEl.innerHTML = '';
    activeIdx = -1;
    items = [];
  }

  function selectStation(code, name) {
    selectedCode        = code;
    inputEl.value       = name;
    codeEl.value        = code;
    badgeEl.textContent = code;
    clearEl.classList.add('visible');
    hideList();
  }

  function clearSelection() {
    selectedCode        = '';
    inputEl.value       = '';
    codeEl.value        = '';
    badgeEl.textContent = '—';
    clearEl.classList.remove('visible');
    inputEl.focus();
  }

  async function search(q) {
    if (q.length < 1) { hideList(); return; }

    showList(`<div class="ac-loading"><span class="spinner"></span> Searching...</div>`);

    try {
      const results = await Api.searchStations(q);
      if (!results.length) {
        showList(`<div class="ac-empty">No stations found for "${q}"</div>`);
        return;
      }
      const html = results.slice(0, 8).map((s, i) => `
        <div class="ac-item" data-code="${s.code}" data-name="${s.name}" data-idx="${i}">
          <span class="ac-code">${s.code}</span>
          <span class="ac-name">${s.name}</span>
          ${s.city ? `<span class="ac-city">${s.city}</span>` : ''}
        </div>`).join('');
      showList(html);

      // Click handlers
      listEl.querySelectorAll('.ac-item').forEach(el => {
        el.addEventListener('mousedown', e => {
          e.preventDefault();
          selectStation(el.dataset.code, el.dataset.name);
        });
      });
    } catch (e) {
      showList(`<div class="ac-empty">Error loading stations</div>`);
    }
  }

  // Input events
  inputEl.addEventListener('input', () => {
    const q = inputEl.value.trim();
    if (selectedCode && inputEl.value !== '') {
      // User typed after selection — clear selection
      if (inputEl.value !== codeEl.value) {
        selectedCode = '';
        codeEl.value = '';
        badgeEl.textContent = '—';
        clearEl.classList.remove('visible');
      }
    }
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => search(q), 280);
  });

  // Keyboard nav
  inputEl.addEventListener('keydown', e => {
    if (!items.length) return;
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      activeIdx = Math.min(activeIdx + 1, items.length - 1);
      items.forEach((el, i) => el.classList.toggle('active', i === activeIdx));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      activeIdx = Math.max(activeIdx - 1, 0);
      items.forEach((el, i) => el.classList.toggle('active', i === activeIdx));
    } else if (e.key === 'Enter' && activeIdx >= 0) {
      e.preventDefault();
      const el = items[activeIdx];
      selectStation(el.dataset.code, el.dataset.name);
    } else if (e.key === 'Escape') {
      hideList();
    }
  });

  inputEl.addEventListener('blur', () => {
    // small delay so mousedown on item fires first
    setTimeout(hideList, 150);
  });

  inputEl.addEventListener('focus', () => {
    const q = inputEl.value.trim();
    if (q.length >= 1 && !selectedCode) search(q);
  });

  clearEl.addEventListener('click', clearSelection);

  return {
    getCode: () => selectedCode || codeEl.value || inputEl.value.trim().toUpperCase(),
    isSelected: () => !!selectedCode,
    getDisplayName: () => inputEl.value,
  };
}

// Init both autocompletes
const fromAC = makeAutocomplete({
  inputEl:  qs('#fromInput'),
  listEl:   qs('#fromList'),
  codeEl:   qs('#fromCode'),
  badgeEl:  qs('#fromBadge'),
  clearEl:  qs('#fromClear'),
  prefill:  _params.get('from') || '',
});

const toAC = makeAutocomplete({
  inputEl:  qs('#toInput'),
  listEl:   qs('#toList'),
  codeEl:   qs('#toCode'),
  badgeEl:  qs('#toBadge'),
  clearEl:  qs('#toClear'),
  prefill:  _params.get('to') || '',
});

// ══════════════════════════════════════════════════════════════
//  SEARCH RESULTS
// ══════════════════════════════════════════════════════════════
function showInitState() {
  _area.innerHTML = `
    <div class="empty" style="margin-top:24px;">
      <div class="empty-icon">🚉</div>
      <h3 class="empty-title">Ready to search</h3>
      <p class="empty-text">Type station name above and select from suggestions</p>
    </div>`;
}

function _routeTime(routes, code, type) {
  const r = (routes || []).find(r => r.stationCode === code);
  return r ? (type === 'dep' ? r.departureTime : r.arrivalTime) : '—';
}

function goToBook(train, from, to, date) {
  sessionStorage.setItem('tms_booking', JSON.stringify({
    trainId: train.id, trainName: train.trainName,
    trainNumber: train.trainNumber, trainType: train.trainType,
    fromStationCode: from, toStationCode: to, travelDate: date,
  }));
  location.href = 'book.html';
}

// Inline availability panel (IRCTC style)
function renderAvailPanel(container, data) {
  if (!data.coaches || !data.coaches.length) {
    container.innerHTML = `<div style="padding:10px;font-size:.82rem;color:var(--text-muted);">No coaches configured.</div>`;
    return;
  }
  const COLS = '2fr 100px 140px';
  const rows = data.coaches.map(c => {
    const cfg        = COACH_CFG[c.coachType] || { label: c.coachType, color: '#64748B' };
    const avail      = c.availableSeats;
    const availColor = avail === 0 ? '#F87171' : avail <= 5 ? '#FBBF24' : '#00D4AA';
    const availTxt   = avail === 0 ? 'NOT AVBL' : avail <= 5 ? `AVBL ${avail}` : `AVBL ${avail}`;
    let wlBadge = '';
    if (c.waitlistedCount > 0) {
      wlBadge = `<span style="font-size:.72rem;font-weight:700;color:#FBBF24;
        background:rgba(251,191,36,0.1);border:1px solid rgba(251,191,36,0.3);
        padding:1px 7px;border-radius:4px;margin-left:6px;">WL#${c.waitlistedCount}</span>`;
    }
    return `<div style="display:grid;grid-template-columns:${COLS};align-items:center;
      padding:10px 14px;border-bottom:1px solid var(--border);font-size:.82rem;gap:0;">
      <div style="font-weight:600;color:var(--text-primary);">${cfg.label}</div>
      <div style="color:var(--text-muted);text-align:center;">${c.totalSeats}</div>
      <div style="display:flex;align-items:center;justify-content:center;gap:4px;">
        <span style="font-weight:700;color:${availColor};">${availTxt}</span>${wlBadge}
      </div>
    </div>`;
  }).join('');

  container.innerHTML = `
    <div style="display:grid;grid-template-columns:${COLS};padding:8px 14px;
      background:var(--bg-overlay);font-size:.72rem;font-weight:600;color:var(--text-muted);
      letter-spacing:.05em;text-transform:uppercase;gap:0;
      border-radius:var(--r-md) var(--r-md) 0 0;">
      <span>Class</span>
      <span style="text-align:center;">Total</span>
      <span style="text-align:center;">Availability</span>
    </div>${rows}`;
}

function makeTrainCard(train, from, to, date) {
  const card = document.createElement('article');
  card.className = 'train-card';
  card.style.cssText = 'margin-bottom:10px; padding:16px 20px;';

  const fromTime = _routeTime(train.routes, from, 'dep');
  const toTime   = _routeTime(train.routes, to, 'arr');

  card.innerHTML = `
    <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:12px;">
      <div>
        <span class="train-name" style="font-size:.95rem;">${train.trainName}</span>
        <span style="font-family:var(--mono);font-size:.78rem;color:var(--text-muted);margin-left:8px;">#${train.trainNumber}</span>
        <span style="font-size:.72rem;color:var(--blue);background:var(--blue-subtle);
          border:1px solid var(--blue-border);padding:1px 7px;border-radius:99px;margin-left:6px;">
          ${train.trainType || ''}
        </span>
      </div>
      <span id="st-${train.id}"></span>
    </div>
    <div style="display:flex;align-items:center;gap:10px;margin-bottom:14px;">
      <div style="text-align:center;min-width:48px;">
        <div style="font-family:var(--mono);font-size:1.1rem;font-weight:700;color:var(--blue);">${from}</div>
        <div style="font-size:.75rem;font-weight:600;color:var(--text-primary);">${fromTime}</div>
      </div>
      <div style="flex:1;display:flex;flex-direction:column;align-items:center;gap:3px;">
        <div style="width:100%;height:1px;background:linear-gradient(90deg,var(--blue),var(--mint));"></div>
        <div style="font-size:.68rem;color:var(--text-muted);">${(train.routes||[]).length} stops</div>
      </div>
      <div style="text-align:center;min-width:48px;">
        <div style="font-family:var(--mono);font-size:1.1rem;font-weight:700;color:var(--mint);">${to}</div>
        <div style="font-size:.75rem;font-weight:600;color:var(--text-primary);">${toTime}</div>
      </div>
      <button class="btn btn-primary js-book-btn" style="margin-left:16px;white-space:nowrap;">Book Now →</button>
    </div>
    <div style="border-top:1px solid var(--border);padding-top:10px;display:flex;align-items:center;justify-content:space-between;">
      <button class="js-avail-btn" style="background:none;border:none;cursor:pointer;
        font-size:.8rem;font-weight:600;color:var(--blue);display:flex;align-items:center;gap:5px;padding:0;">
        <span class="js-avail-arrow">▶</span> Check Availability
      </button>
      <span style="font-size:.72rem;color:var(--text-muted);">${fmtDate(date)}</span>
    </div>
    <div class="js-avail-panel" style="display:none;margin-top:10px;
      border:1px solid var(--border);border-radius:var(--r-md);overflow:hidden;">
      <div class="js-avail-content">
        <div style="padding:14px;font-size:.82rem;color:var(--text-muted);display:flex;gap:8px;align-items:center;">
          <span class="spinner"></span> Loading...
        </div>
      </div>
    </div>`;

  statusBadge(train.status, card.querySelector(`#st-${train.id}`));
  card.querySelector('.js-book-btn').addEventListener('click', () => goToBook(train, from, to, date));

  const availBtn     = card.querySelector('.js-avail-btn');
  const availPanel   = card.querySelector('.js-avail-panel');
  const availContent = card.querySelector('.js-avail-content');
  const arrow        = card.querySelector('.js-avail-arrow');
  let loaded = false;

  availBtn.addEventListener('click', async () => {
    const isOpen = availPanel.style.display !== 'none';
    if (isOpen) { availPanel.style.display = 'none'; arrow.textContent = '▶'; return; }
    availPanel.style.display = 'block';
    arrow.textContent = '▼';
    if (!loaded) {
      try {
        const data = await Api.getTrainSeatAvailability(train.id, date);
        renderAvailPanel(availContent, data);
        loaded = true;
      } catch (err) {
        availContent.innerHTML = `<div style="padding:10px;font-size:.82rem;color:var(--error);">❌ ${err.message}</div>`;
      }
    }
  });

  return card;
}

async function doSearch() {
  const from = fromAC.getCode();
  const to   = toAC.getCode();
  const date = _dateEl.value;

  if (!from || from === '—') { Toast.warning('Please select From station'); qs('#fromInput').focus(); return; }
  if (!to   || to   === '—') { Toast.warning('Please select To station');   qs('#toInput').focus();   return; }
  if (!date) { Toast.warning('Select travel date'); return; }

  const btn = qs('#searchBtn');
  btnLoad(btn, true, 'Searching...');
  _area.innerHTML = `<div class="empty" style="margin-top:24px;"><div class="spinner spinner-lg"></div></div>`;

  try {
    const trains = await Api.searchTrains(from, to, date);

    _area.innerHTML = `
      <div class="page-head-row" style="margin-top:8px;margin-bottom:14px;">
        <div>
          <h2 class="page-title" style="margin-bottom:2px;">${fromAC.getDisplayName()} → ${toAC.getDisplayName()}</h2>
          <p class="page-sub">Travel date: ${fmtDate(date)}</p>
        </div>
        <span class="badge badge-blue" style="font-size:.8rem;padding:5px 14px;">
          ${trains.length} train${trains.length !== 1 ? 's' : ''} found
        </span>
      </div>
      <div id="trainsList"></div>`;

    const list = qs('#trainsList');
    if (!trains.length) {
      list.innerHTML = `
        <div class="empty">
          <div class="empty-icon">🚫</div>
          <h3 class="empty-title">No trains found</h3>
          <p class="empty-text">No active trains between ${fromAC.getDisplayName()} and ${toAC.getDisplayName()} on ${fmtDate(date)}</p>
        </div>`;
      return;
    }
    trains.forEach(t => list.appendChild(makeTrainCard(t, from, to, date)));
  } catch (err) {
    _area.innerHTML = `<div class="alert alert-error" style="margin-top:16px;">❌ ${err.message}</div>`;
  } finally {
    btnLoad(btn, false);
  }
}

showInitState();
qs('#searchBtn').addEventListener('click', doSearch);
if (_params.get('from') && _params.get('to')) doSearch();