const qs   = (sel, ctx = document) => ctx.querySelector(sel);
const qsa  = (sel, ctx = document) => [...ctx.querySelectorAll(sel)];
const show = el => el?.classList.remove('hidden');
const hide = el => el?.classList.add('hidden');

function setText(id, val) {
  const el = document.getElementById(id);
  if (el) el.textContent = val ?? '—';
}

function cloneTpl(id) {
  return document.getElementById(id).content.cloneNode(true);
}

function fmtDate(d) {
  if (!d) return '—';
  return new Date(d).toLocaleDateString('en-IN',
    { day: '2-digit', month: 'short', year: 'numeric' });
}

function fmtDateTime(d) {
  if (!d) return '—';
  return new Date(d).toLocaleString('en-IN',
    { day: '2-digit', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit' });
}

function fmtMoney(n) {
  return `₹${parseFloat(n || 0).toLocaleString('en-IN',
    { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

const Toast = (() => {
  let wrap = null;
  const icons = { success: '✅', error: '❌', warning: '⚠️', info: 'ℹ️' };

  function _getWrap() {
    if (!wrap) {
      wrap = document.createElement('div');
      wrap.className = 'toast-wrap';
      document.body.appendChild(wrap);
    }
    return wrap;
  }

  function _show(msg, type, ms = 3500) {
    const el = document.createElement('div');
    el.className = `toast ${type}`;
    el.innerHTML = `<span class="toast-icon">${icons[type]}</span>
                    <span class="toast-msg">${msg}</span>`;
    _getWrap().appendChild(el);
    setTimeout(() => {
      el.classList.add('out');
      setTimeout(() => el.remove(), 260);
    }, ms);
  }

  return {
    success: m => _show(m, 'success'),
    error:   m => _show(m, 'error'),
    warning: m => _show(m, 'warning'),
    info:    m => _show(m, 'info'),
  };
})();

const Loader = {
  _el: null,
  show(text = 'Loading...') {
    if (this._el) return;
    const wrap = document.createElement('div');
    wrap.className = 'page-loader';
    wrap.innerHTML = `<div class="spinner spinner-lg"></div>
                      <div class="page-loader-text">${text}</div>`;
    document.body.appendChild(wrap);
    this._el = wrap;
  },
  hide() { this._el?.remove(); this._el = null; },
};

function btnLoad(btn, loading, text = '') {
  if (loading) {
    btn._orig    = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = `<span class="spinner"></span>${text ? ` ${text}` : ''}`;
  } else {
    if (btn._orig !== undefined) btn.innerHTML = btn._orig;
    btn.disabled = false;
  }
}

const STATUS_COLORS = {
  ACTIVE:          'mint',
  INACTIVE:        'red',
  DELAYED:         'yellow',
  CANCELLED:       'red',
  CONFIRMED:       'mint',
  WAITLISTED:      'yellow',
  PENDING_PAYMENT: 'yellow',
  EXPIRED:         'red',
  SUCCESS:         'mint',
  FAILED:          'red',
  REFUNDED:        'blue',
  PENDING:         'yellow',
};

function statusBadge(status, el) {
  if (!el) return;
  el.className   = `badge badge-${STATUS_COLORS[status] || 'neutral'}`;
  el.textContent = status;
}

function _statusBadgeHtml(status) {
  return `<span class="badge badge-${STATUS_COLORS[status] || 'neutral'}">${status}</span>`;
}


function openModal(id)  { document.getElementById(id)?.classList.add('open'); }
function closeModal(id) { document.getElementById(id)?.classList.remove('open'); }


document.addEventListener('click', e => {
  const btn = e.target.closest('[data-close]');
  if (btn) closeModal(btn.dataset.close);
});

function paginate(totalPages, current, fnName) {
  const el = document.getElementById('pagination');
  if (!el) return;
  if (totalPages <= 1) { el.innerHTML = ''; return; }

  el.innerHTML = '';
  const add = (label, page, active = false, disabled = false) => {
    const btn = document.createElement('button');
    btn.className   = `page-btn${active ? ' active' : ''}`;
    btn.textContent = label;
    btn.disabled    = disabled;
    if (!disabled) btn.onclick = () => window[fnName](page);
    el.appendChild(btn);
  };

  if (current > 0) add('‹', current - 1);
  for (let i = 0; i < totalPages; i++) add(i + 1, i, i === current);
  if (current < totalPages - 1) add('›', current + 1);
}

function renderNavUser() {
  const u  = Auth.getUser();
  const el = document.getElementById('navUser');
  if (!u || !el) return;

  el.innerHTML = `
    <div style="display:flex; align-items:center; gap:8px;">
      <div class="avatar">${u.username.slice(0, 2).toUpperCase()}</div>
      <div style="color:white; font-weight:600;">${u.username}</div>
    </div>`;

  const logoutBtn = document.getElementById('logoutBtn');
  if (logoutBtn) {
  
    const newBtn = logoutBtn.cloneNode(true);
    logoutBtn.parentNode.replaceChild(newBtn, logoutBtn);
    newBtn.addEventListener('click', () => {
      const modal = document.getElementById('logoutModal');
      if (modal) openModal('logoutModal');
      else Auth.logout();
    });
  }

  const confirmBtn = document.getElementById('confirmLogoutBtn');
  if (confirmBtn) {
    const newConfirm = confirmBtn.cloneNode(true);
    confirmBtn.parentNode.replaceChild(newConfirm, confirmBtn);
    newConfirm.addEventListener('click', () => {
      closeModal('logoutModal');
      Auth.logout();
    });
  }
}