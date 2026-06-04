// Auth.redirectIfLoggedIn();

// qs('#regForm').addEventListener('submit', async e => {
//   e.preventDefault();

//   const btn = qs('#regBtn');
//   const box = qs('#alertBox');
//   box.innerHTML = '';
//   btnLoad(btn, true, 'Creating account...');

//   const payload = {
//     name:         qs('#name').value.trim(),
//     email:        qs('#email').value.trim(),
//     username:     qs('#username').value.trim(),
//     password:     qs('#password').value,
//     mobileNumber: qs('#mobile').value.trim(),
//   };

//   if (!payload.name || !payload.email || !payload.username ||
//       !payload.password || !payload.mobileNumber) {
//     box.innerHTML = '<div class="alert alert-error">All fields are required</div>';
//     btnLoad(btn, false);
//     return;
//   }

//   try {
//     await Api.register(payload);
//     box.innerHTML = '<div class="alert alert-success">✅ Account created! Redirecting...</div>';
//     setTimeout(() => { location.href = 'index.html'; }, 1500);
//   } catch (err) {
//     const msg = err.message.replace(/\n/g, '<br>');
//     box.innerHTML = `<div class="alert alert-error" style="white-space:pre-line;">❌ ${msg}</div>`;
//     btnLoad(btn, false);
//   }
// });

Auth.redirectIfLoggedIn();

qs('#regForm').addEventListener('submit', async e => {
  e.preventDefault();

  const btn = qs('#regBtn');
  const box = qs('#alertBox');
  box.innerHTML = '';
  btnLoad(btn, true, 'Creating account...');

  const payload = {
    name:         qs('#name').value.trim(),
    email:        qs('#email').value.trim(),
    username:     qs('#username').value.trim(),
    password:     qs('#password').value,
    mobileNumber: qs('#mobile').value.trim(),
  };

  if (!payload.name || !payload.email || !payload.username ||
      !payload.password || !payload.mobileNumber) {
    box.innerHTML = '<div class="alert alert-error">❌ All fields are required</div>';
    btnLoad(btn, false);
    return;
  }

  try {
    await Api.register(payload);
    box.innerHTML = '<div class="alert alert-success">✅ Account created! Redirecting...</div>';
    setTimeout(() => { location.href = 'index.html'; }, 1500);
  } catch (err) {
    // err.message already has formatted validation errors (one per line with •)
    const msg = err.message.replace(/\n/g, '<br>');
    box.innerHTML = `<div class="alert alert-error" style="white-space:pre-line;">❌ ${msg}</div>`;
    btnLoad(btn, false);
  }
});