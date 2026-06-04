const Auth = {

  // ── Token Expiry Check ────────────────────────────────────
  isTokenExpired() {
    const token = this.getToken();
    if (!token) return true;
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.exp * 1000 < Date.now();
    } catch {
      return true;
    }
  },

  // ── Getters ──────────────────────────────────────────────
  getToken() { return localStorage.getItem(CONFIG.TOKEN_KEY); },
  getUser()  { return JSON.parse(localStorage.getItem(CONFIG.USER_KEY) || 'null'); },

  isLoggedIn() {
    if (!this.getToken()) return false;
    if (this.isTokenExpired()) {
      this.logout();
      return false;
    }
    return true;
  },

  isAdmin() {
    const u = this.getUser();
    return !!(u?.roles?.includes('ROLE_ADMIN') || u?.role === 'ROLE_ADMIN');
  },

  // ── Save after login ──────────────────────────────────────
  save(token, username, role, userId = null) {
    localStorage.setItem(CONFIG.TOKEN_KEY, token);
    localStorage.setItem(CONFIG.USER_KEY, JSON.stringify({
      username,
      role,
      roles: [role],
      userId,
    }));
  },

  // ── Save userId once resolved ─────────────────────────────
  cacheUserId(id) {
    const u = this.getUser();
    if (!u) return;
    u.userId = id;
    localStorage.setItem(CONFIG.USER_KEY, JSON.stringify(u));
  },

  // ── Logout ───────────────────────────────────────────────
  logout() {
    localStorage.removeItem(CONFIG.TOKEN_KEY);
    localStorage.removeItem(CONFIG.USER_KEY);

    // FIX: index.html ya register.html pe already hain toh redirect mat karo
    // Warna infinite loop ban jaata hai!
    const path = window.location.pathname;
    const onLoginPage = path === '/'
                     || path.includes('index.html')
                     || path.includes('register.html');

    if (!onLoginPage) {
      window.location.href = '/index.html';
    }
  },

  // ── Route Guards ─────────────────────────────────────────
  requireAuth() {
    if (this.isLoggedIn()) return true;
    window.location.href = '/index.html';
    return false;
  },

  requireAdmin() {
    if (!this.isLoggedIn()) { window.location.href = '/index.html'; return false; }
    if (!this.isAdmin())    { window.location.href = '/dashboard.html'; return false; }
    return true;
  },

  redirectIfLoggedIn() {
    if (!this.isLoggedIn()) return;
    window.location.href = this.isAdmin()
      ? '/admin/dashboard.html'
      : '/dashboard.html';
  },
};