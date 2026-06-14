if (!Auth.requireAuth()) throw '';
renderNavUser();

const _user = Auth.getUser();
let _userId = null;

async function loadProfile() {
  try {
    const me = await Api.get('/api/auth/me');
    _userId = me.id;

    document.getElementById('profileAvatar').textContent =
      me.name.slice(0, 2).toUpperCase();
    document.getElementById('profileName').textContent     = me.name;
    document.getElementById('profileUsername').textContent = '@' + me.username;

    const isAdmin = (me.roles || []).includes('ROLE_ADMIN');
    document.getElementById('profileRole').innerHTML = isAdmin
      ? '<span class="badge badge-blue">Admin</span>'
      : '<span class="badge badge-mint">User</span>';

    document.getElementById('pName').value     = me.name     || '';
    document.getElementById('pEmail').value    = me.email    || '';
    document.getElementById('pMobile').value   = me.mobileNumber || '';
    document.getElementById('pUsername').value = me.username || '';

  } catch (err) {
    document.getElementById('profileAlert').innerHTML =
      `<div class="alert alert-error">❌ ${err.message}</div>`;
  }
}

// ── Update Profile ───────────────────────────────────────────
document.getElementById('updateProfileBtn').addEventListener('click', async () => {
  const alertEl = document.getElementById('profileAlert');
  alertEl.innerHTML = '';

  const name     = document.getElementById('pName').value.trim();
  const email    = document.getElementById('pEmail').value.trim();
  const mobile   = document.getElementById('pMobile').value.trim();
  const username = document.getElementById('pUsername').value.trim();

  if (!name || !email || !mobile || !username) {
    alertEl.innerHTML =
      '<div class="alert alert-error">❌ All fields are required</div>';
    return;
  }

  const btn = document.getElementById('updateProfileBtn');
  btnLoad(btn, true, 'Saving...');

  try {
    const oldUsername = _user.username;
    await Api.updateUser(_userId, { name, email, username, mobileNumber: mobile });

    alertEl.innerHTML =
      '<div class="alert alert-success">✅ Profile updated successfully!</div>';

    if (username !== oldUsername) {
      alertEl.innerHTML =
        '<div class="alert alert-success">✅ Username changed — logging out...</div>';
      setTimeout(() => Auth.logout(), 1500);
    } else {
      await loadProfile();
    }
  } catch (err) {
    const msg = err.message.replace(/\n/g, '<br>');
    alertEl.innerHTML =
      `<div class="alert alert-error" style="white-space:pre-line;">❌ ${msg}</div>`;
  } finally {
    btnLoad(btn, false);
  }
});

// ── Change Password ──────────────────────────────────────────
document.getElementById('changePassBtn').addEventListener('click', async () => {
  const alertEl = document.getElementById('passwordAlert');
  alertEl.innerHTML = '';

  const currentPass = document.getElementById('pCurrentPass').value;
  const newPass     = document.getElementById('pNewPass').value;
  const confirmPass = document.getElementById('pConfirmPass').value;

  if (!currentPass || !newPass || !confirmPass) {
    alertEl.innerHTML =
      '<div class="alert alert-error">❌ All fields are required</div>';
    return;
  }

  if (newPass !== confirmPass) {
    alertEl.innerHTML =
      '<div class="alert alert-error">❌ Passwords do not match</div>';
    return;
  }

  if (newPass === currentPass) {
    alertEl.innerHTML =
      '<div class="alert alert-error">❌ New password cannot be same as current password</div>';
    return;
  }

  const passRegex =
    /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/;
  if (!passRegex.test(newPass)) {
    alertEl.innerHTML =
      '<div class="alert alert-error">❌ Password must be 8+ chars with uppercase, lowercase, number & special character</div>';
    return;
  }

  const btn = document.getElementById('changePassBtn');
  btnLoad(btn, true, 'Changing...');

  try {
    await Api.changePassword(_userId, {
      currentPassword: currentPass,
      newPassword:     newPass
    });

    alertEl.innerHTML =
      '<div class="alert alert-success">✅ Password changed! Please login again.</div>';

    document.getElementById('pCurrentPass').value = '';
    document.getElementById('pNewPass').value     = '';
    document.getElementById('pConfirmPass').value = '';

    setTimeout(() => Auth.logout(), 2000);

  } catch (err) {
    let msg = err.message;
    if (msg.includes('incorrect') || msg.includes('Current password'))
      msg = 'Current password is incorrect';
    alertEl.innerHTML = `<div class="alert alert-error">❌ ${msg}</div>`;
  } finally {
    btnLoad(btn, false);
  }
});

loadProfile();