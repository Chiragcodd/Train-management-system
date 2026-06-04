if (!Auth.requireAdmin()) throw '';
renderNavUser();

let _editId = null, _delId = null;

qs('#addStationBtn').addEventListener('click', openAdd);
qs('#stForm').addEventListener('submit', onSave);
qs('#delBtn').addEventListener('click', doDelete);

// ── Load all stations ─────────────────────────────────────────
async function loadStations() {
  try {
    const stations = await Api.getAllStations();
    const tbody = qs('#stBody');
    const empty = qs('#stEmpty');
    tbody.innerHTML = '';

    if (!stations.length) { show(empty); return; }
    hide(empty);
    stations.forEach(s => tbody.appendChild(makeRow(s)));
  } catch (err) {
    qs('#stBody').innerHTML =
      `<tr><td colspan="4"><div class="alert alert-error">${err.message}</div></td></tr>`;
  }
}

// ── Build table row ───────────────────────────────────────────
function makeRow(s) {
  const frag = cloneTpl('tpl-station-row');
  frag.querySelector('[data-f="idx"]').textContent  = s.id;
  frag.querySelector('[data-f="name"]').textContent = s.name;
  frag.querySelector('[data-f="code"]').textContent = s.code;
  frag.querySelector('[data-f="city"]').textContent = s.city || '—';

  frag.querySelector('.btn-edit').addEventListener('click', () =>
    openEdit(s.id, s.name, s.code, s.city));
  frag.querySelector('.btn-del').addEventListener('click', () =>
    openDel(s.id));

  return frag;
}

function openAdd() {
  _editId = null;
  qs('#stForm').reset();
  qs('#stAlert').innerHTML = '';
  setText('stModTitle', 'Add Station');
  openModal('stMod');
}

function openEdit(id, name, code, city) {
  _editId = id;
  qs('#stName').value = name;
  qs('#stCode').value = code;
  if (qs('#stCity')) qs('#stCity').value = city || '';
  qs('#stAlert').innerHTML = '';
  setText('stModTitle', 'Edit Station');
  openModal('stMod');
}

function openDel(id) { _delId = id; openModal('delMod'); }

async function onSave(e) {
  e.preventDefault();
  const btn   = qs('#stSaveBtn');
  const alert = qs('#stAlert');

  alert.innerHTML = '';
  btnLoad(btn, true, 'Saving...');

  try {
    const payload = {
      name: qs('#stName').value.trim(),
      code: qs('#stCode').value.trim().toUpperCase(),
      city: qs('#stCity')?.value.trim() || '',
    };

    if (_editId) {
      await Api.updateStation(_editId, payload);
    } else {
      await Api.addStation(payload);
    }

    Toast.success(_editId ? 'Station updated!' : 'Station added!');
    
    btnLoad(btn, false);   // ✅ IMPORTANT FIX

    closeModal('stMod');
    loadStations();

  } catch (err) {
    alert.innerHTML = `<div class="alert alert-error">❌ ${err.message}</div>`;
    btnLoad(btn, false);
  }
}

async function doDelete() {
  const btn = qs('#delBtn');
  btnLoad(btn, true, 'Deleting...');
  try {
    await Api.deleteStation(_delId);
    Toast.success('Station deleted!');
    closeModal('delMod');
    loadStations();
  } catch (err) {
    Toast.error(err.message);
    btnLoad(btn, false);
  }
}

loadStations();