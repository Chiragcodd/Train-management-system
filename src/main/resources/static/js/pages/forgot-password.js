Auth.redirectIfLoggedIn();

let _email = '';

// ── Step 1: OTP bhejo ────────────────────────────────────────
document.getElementById('sendOtpBtn').addEventListener('click', async () => {
  const alertBox = document.getElementById('alertBox');
  alertBox.innerHTML = '';

  const email = document.getElementById('email').value.trim();

  if (!email) {
    alertBox.innerHTML = '<div class="alert alert-error">❌ Email is required</div>';
    return;
  }

  const btn = document.getElementById('sendOtpBtn');
  btnLoad(btn, true, 'Sending OTP...');

  try {
    await Api.sendOtp({ email });
    _email = email;

    alertBox.innerHTML =
      `<div class="alert alert-success">✅ OTP sent to <b>${email}</b> — check your inbox!</div>`;

    // Step 2 dikhao
    document.getElementById('step1').style.display = 'none';
    document.getElementById('step2').style.display = 'block';

  } catch (err) {
    let msg = err.message;
    if (msg.includes('not found') || msg.includes('404'))
      msg = 'No account found with this email address';
    alertBox.innerHTML = `<div class="alert alert-error">❌ ${msg}</div>`;
  } finally {
    btnLoad(btn, false);
  }
});

// ── Step 2: Password reset karo ──────────────────────────────
document.getElementById('resetBtn').addEventListener('click', async () => {
  const alertBox = document.getElementById('alertBox');
  alertBox.innerHTML = '';

  const otp         = document.getElementById('otp').value.trim();
  const newPassword = document.getElementById('newPassword').value;
  const confirmPass = document.getElementById('confirmPassword').value;

  if (!otp || !newPassword || !confirmPass) {
    alertBox.innerHTML =
      '<div class="alert alert-error">❌ All fields are required</div>';
    return;
  }

  if (otp.length !== 6 || !/^\d+$/.test(otp)) {
    alertBox.innerHTML =
      '<div class="alert alert-error">❌ OTP must be 6 digits</div>';
    return;
  }

  if (newPassword !== confirmPass) {
    alertBox.innerHTML =
      '<div class="alert alert-error">❌ Passwords do not match</div>';
    return;
  }

  const passRegex =
    /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/;
  if (!passRegex.test(newPassword)) {
    alertBox.innerHTML =
      '<div class="alert alert-error">❌ Password must be 8+ chars with uppercase, lowercase, number & special character</div>';
    return;
  }

  const btn = document.getElementById('resetBtn');
  btnLoad(btn, true, 'Resetting...');

  try {
    await Api.resetPassword({
      email: _email,
      otp,
      newPassword
    });

    alertBox.innerHTML =
      '<div class="alert alert-success">✅ Password reset successfully! Redirecting to login...</div>';

    setTimeout(() => { location.href = 'index.html'; }, 2000);

  } catch (err) {
    let msg = err.message;
    if (msg.includes('Invalid OTP'))
      msg = 'Invalid OTP — please check and try again';
    else if (msg.includes('expired'))
      msg = 'OTP has expired — please request a new one';
    alertBox.innerHTML = `<div class="alert alert-error">❌ ${msg}</div>`;
  } finally {
    btnLoad(btn, false);
  }
});

// ── Resend OTP ───────────────────────────────────────────────
document.getElementById('resendOtpBtn').addEventListener('click', async () => {
  const alertBox = document.getElementById('alertBox');
  alertBox.innerHTML = '';

  const btn = document.getElementById('resendOtpBtn');
  btnLoad(btn, true, 'Sending...');

  try {
    await Api.sendOtp({ email: _email });
    alertBox.innerHTML =
      '<div class="alert alert-success">✅ New OTP sent! Check your inbox.</div>';
  } catch (err) {
    alertBox.innerHTML =
      `<div class="alert alert-error">❌ ${err.message}</div>`;
  } finally {
    btnLoad(btn, false);
  }
});