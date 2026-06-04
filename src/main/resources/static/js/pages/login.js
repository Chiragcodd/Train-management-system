// Auth.redirectIfLoggedIn();

// qs('#loginForm').addEventListener('submit', async e => {
//   e.preventDefault();

//   const btn = qs('#loginBtn');
//   const box = qs('#alertBox');
//   box.innerHTML = '';

//   const username = qs('#username').value.trim();
//   const password = qs('#password').value;

//   if (!username || !password) {
//     box.innerHTML = '<div class="alert alert-error">Enter username and password</div>';
//     return;
//   }

//   btnLoad(btn, true, 'Signing in...');

//   try {
//     const res = await Api.login({ username, password });
//     Auth.save(res.token, res.username, res.role, null);
//     Toast.success(`Welcome, ${res.username}!`);
//     setTimeout(() => {
//       window.location.href = res.role === 'ROLE_ADMIN'
//         ? '/admin/dashboard.html'
//         : '/dashboard.html';
//     }, 500);
//   } catch (err) {
//     let msg = err.message || 'Login failed';
//     if (msg.toLowerCase().includes('bad credentials') || msg.includes('401'))
//       msg = 'Invalid username or password.';
//     else if (msg.includes('Cannot connect'))
//       msg = 'Server unreachable — is backend running?';
//     else if (msg.includes('Too many') || msg.includes('Too Many'))
//       msg = 'Too many failed login attempts. Please try again later.';
//     box.innerHTML = `<div class="alert alert-error">❌ ${msg}</div>`;
//   } finally {
//     btnLoad(btn, false);
//   }
// });


Auth.redirectIfLoggedIn();

qs('#loginForm').addEventListener('submit', async e => {
  e.preventDefault();

  const btn = qs('#loginBtn');
  const box = qs('#alertBox');
  box.innerHTML = '';

  const username = qs('#username').value.trim();
  const password = qs('#password').value;

  if (!username || !password) {
    box.innerHTML = '<div class="alert alert-error">❌ Enter username and password</div>';
    return;
  }

  btnLoad(btn, true, 'Signing in...');

  try {
    const res = await Api.login({ username, password });
    Auth.save(res.token, res.username, res.role, null);
    Toast.success(`Welcome, ${res.username}!`);
    setTimeout(() => {
      window.location.href = res.role === 'ROLE_ADMIN'
        ? '/admin/dashboard.html'
        : '/dashboard.html';
    }, 500);
  } catch (err) {
    let msg = err.message || 'Login failed';

    if (msg.toLowerCase().includes('bad credentials') || msg.includes('401'))
      msg = 'Invalid username or password.';
    else if (msg.includes('Cannot connect'))
      msg = 'Server unreachable — is backend running?';
    else if (msg.includes('Too many') || msg.includes('Too Many'))
      msg = 'Too many failed login attempts. Please try again later.';
    else if (msg.includes('disabled') || msg.includes('Disabled'))
      msg = 'Your account has been disabled. Please contact support.';
    // For validation errors (e.g. blank fields sent), show them as-is
    // They are already formatted with • by api.js

    const formatted = msg.replace(/\n/g, '<br>');
    box.innerHTML = `<div class="alert alert-error" style="white-space:pre-line;">❌ ${formatted}</div>`;
  } finally {
    btnLoad(btn, false);
  }
});