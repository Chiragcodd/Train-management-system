if (!Auth.requireAdmin()) throw '';
renderNavUser();

let _delId = null, _curPage = 0;

qs('#uDelBtn').addEventListener('click', doDelete);

// ── Load users ────────────────────────────────────────────────
async function loadUsers(page = 0) {
  _curPage = page;

  const el = qs('#userTable');
  el.innerHTML = '<div class="empty"><div class="spinner spinner-lg"></div></div>';

  try {
    const data  = await Api.getAllUsers(page);
    const users = data.content || [];

    setText('totalU', `${data.totalElements || users.length} users`);

    if (!users.length) {
      el.innerHTML = '<div class="empty"><div class="empty-title">No users</div></div>';
      return;
    }

    el.innerHTML = `
      <table>
        <thead>
          <tr>
            <th>#</th><th>Name</th><th>Username</th>
            <th>Email</th><th>Mobile</th><th>Role</th><th>Action</th>
          </tr>
        </thead>
        <tbody>
          ${users.map((u, i) => `
            <tr>
              <td style="color:var(--text-muted);">${page * 10 + i + 1}</td>
              <td>
                <div style="display:flex;align-items:center;gap:10px;">
                  <div style="width:30px;height:30px;border-radius:50%;flex-shrink:0;
                    background:linear-gradient(135deg,var(--blue),var(--mint));
                    display:flex;align-items:center;justify-content:center;
                    font-size:.7rem;font-weight:700;color:#fff;">
                    ${(u.name || u.username || '?')[0].toUpperCase()}
                  </div>
                  <strong>${u.name}</strong>
                </div>
              </td>
              <td style="font-family:var(--mono);color:var(--text-secondary);">
                @${u.username}
              </td>
              <td>${u.email}</td>
              <td>${u.mobileNumber || '—'}</td>
              <td>
                ${(u.roles || []).map(r =>
                  `<span class="badge badge-${r.includes('ADMIN') ? 'blue' : 'mint'}">
                    ${r.replace('ROLE_', '')}
                  </span>`).join(' ')}
              </td>
              <td>
                <button class="btn btn-danger btn-sm del-btn" data-id="${u.id}">🗑️</button>
              </td>
            </tr>`).join('')}
        </tbody>
      </table>`;

    // Event delegation
    el.onclick = e => {
      const btn = e.target.closest('.del-btn');
      if (btn) { _delId = btn.dataset.id; openModal('uDelMod'); }
    };

    paginate(data.totalPages || 1, page, 'loadUsers');

  } catch (err) {
    el.innerHTML = `<div class="alert alert-error">❌ ${err.message}</div>`;
  }
}

async function doDelete() {
  const btn = qs('#uDelBtn');
  btnLoad(btn, true, 'Deleting...');
  try {
    await Api.deleteUser(_delId);
    Toast.success('User deleted!');
    closeModal('uDelMod');
    loadUsers(_curPage);
  } catch (err) {
    Toast.error(err.message);
    btnLoad(btn, false);
  }
}

loadUsers();