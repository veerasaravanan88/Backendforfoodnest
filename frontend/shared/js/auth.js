// Authentication & Route Protection Helper for AdvanceFoodnest
import { apiRequest, fetchCsrfToken, getCartTotalCount, AUTH_KEYS } from './api.js?v=2';
import { showToast } from './toast.js?v=2';

let currentUser = null;

export function getBaseUrl() {
  const path = window.location.pathname;
  const idx = path.indexOf('/frontend/');
  if (idx !== -1) {
    return path.substring(0, idx + '/frontend/'.length);
  }
  // On deployed domains (Netlify, Vercel, Railway, etc.), root path is '/'
  // This avoids double-folder navigation (e.g. /restaurant/restaurant/login.html)
  return '/';
}

export function saveAuthSession(user, token = null, role = null) {
  if (!user) return;
  const targetRole = role || user.role || 'CUSTOMER';
  const userJson = JSON.stringify(user);

  let userKey = AUTH_KEYS.CUSTOMER_USER;
  let tokenKey = AUTH_KEYS.CUSTOMER_TOKEN;
  if (targetRole === 'RESTAURANT') {
    userKey = AUTH_KEYS.RESTAURANT_USER;
    tokenKey = AUTH_KEYS.RESTAURANT_TOKEN;
  } else if (targetRole === 'DELIVERY_MAN') {
    userKey = AUTH_KEYS.RIDER_USER;
    tokenKey = AUTH_KEYS.RIDER_TOKEN;
  }

  try {
    localStorage.setItem(userKey, userJson);
    sessionStorage.setItem(userKey, userJson);
    if (token) {
      localStorage.setItem(tokenKey, token);
      sessionStorage.setItem(tokenKey, token);
    }
  } catch (e) {
    console.debug('Failed to save auth session:', e);
  }
}

export function getStoredUser(role = null) {
  let targetRole = role;
  if (!targetRole) {
    const path = window.location.pathname;
    if (path.includes('/restaurant/')) targetRole = 'RESTAURANT';
    else if (path.includes('/deliveryman/')) targetRole = 'DELIVERY_MAN';
    else targetRole = 'CUSTOMER';
  }

  let userKey = AUTH_KEYS.CUSTOMER_USER;
  if (targetRole === 'RESTAURANT') userKey = AUTH_KEYS.RESTAURANT_USER;
  else if (targetRole === 'DELIVERY_MAN') userKey = AUTH_KEYS.RIDER_USER;

  try {
    const raw = localStorage.getItem(userKey) || sessionStorage.getItem(userKey);
    if (raw) return JSON.parse(raw);
  } catch (e) {
    console.debug('Storage read error:', e);
  }
  return null;
}

export function clearAuthSession(role = null) {
  let targetRole = role;
  if (!targetRole) {
    const path = window.location.pathname;
    if (path.includes('/restaurant/')) targetRole = 'RESTAURANT';
    else if (path.includes('/deliveryman/')) targetRole = 'DELIVERY_MAN';
    else targetRole = 'CUSTOMER';
  }

  let userKey = AUTH_KEYS.CUSTOMER_USER;
  let tokenKey = AUTH_KEYS.CUSTOMER_TOKEN;
  if (targetRole === 'RESTAURANT') {
    userKey = AUTH_KEYS.RESTAURANT_USER;
    tokenKey = AUTH_KEYS.RESTAURANT_TOKEN;
  } else if (targetRole === 'DELIVERY_MAN') {
    userKey = AUTH_KEYS.RIDER_USER;
    tokenKey = AUTH_KEYS.RIDER_TOKEN;
  }

  try {
    localStorage.removeItem(userKey);
    localStorage.removeItem(tokenKey);
    sessionStorage.removeItem(userKey);
    sessionStorage.removeItem(tokenKey);
  } catch (e) {
    console.debug('Storage clear error:', e);
  }

  if (currentUser && currentUser.role === targetRole) {
    currentUser = null;
  }
}

export async function checkAuth(expectedRole = null) {
  let targetRole = expectedRole;
  if (!targetRole) {
    const path = window.location.pathname;
    if (path.includes('/restaurant/')) targetRole = 'RESTAURANT';
    else if (path.includes('/deliveryman/')) targetRole = 'DELIVERY_MAN';
    else targetRole = 'CUSTOMER';
  }

  try {
    await fetchCsrfToken();
    const res = await apiRequest('/auth/me', { role: targetRole });
    if (res.success && res.data) {
      if (res.data.role === targetRole) {
        saveAuthSession(res.data, res.data.token, targetRole);
        currentUser = res.data;
        return currentUser;
      }
      // Strict role isolation: do not overwrite session if roles differ
    } else if (res && (res.status === 401 || res.status === 403)) {
      // Backend rejected the session (token expired or invalidated)
      clearAuthSession(targetRole);
      currentUser = null;
      return null;
    }
  } catch (e) {
    console.debug('Auth check bypassed:', e);
  }

  // Fallback to locally stored session ONLY on network disconnection, not on rejection
  const stored = getStoredUser(targetRole);
  if (stored && stored.role === targetRole) {
    currentUser = stored;
    return currentUser;
  }

  clearAuthSession(targetRole);
  currentUser = null;
  return null;
}

export function getCurrentUser() {
  return currentUser;
}

export async function requireAuth(expectedRole = null) {
  const base = getBaseUrl();
  let role = expectedRole;
  if (!role) {
    const path = window.location.pathname;
    if (path.includes('/restaurant/')) role = 'RESTAURANT';
    else if (path.includes('/deliveryman/')) role = 'DELIVERY_MAN';
    else role = 'CUSTOMER';
  }

  const user = await checkAuth(role);
  if (!user || (role !== 'CUSTOMER' && user.role !== role)) {
    if (role === 'RESTAURANT') {
      window.location.href = `${base}restaurant/login.html?error=restaurant_required`;
      return null;
    }
    if (role === 'DELIVERY_MAN') {
      window.location.href = `${base}deliveryman/login.html?error=rider_required`;
      return null;
    }
    // Customer
    window.location.href = `${base}customer/login.html?redirect=${encodeURIComponent(window.location.href)}`;
    return null;
  }

  return user;
}

export async function logoutUser(targetRole = null) {
  const base = getBaseUrl();
  let role = targetRole;
  if (!role) {
    const path = window.location.pathname;
    if (path.includes('/restaurant/')) role = 'RESTAURANT';
    else if (path.includes('/deliveryman/')) role = 'DELIVERY_MAN';
    else role = 'CUSTOMER';
  }

  try {
    await apiRequest('/auth/logout', { method: 'POST', role });
  } catch (e) {
    console.debug('Logout request bypassed:', e);
  }

  clearAuthSession(role);

  if (role === 'RESTAURANT') {
    showToast('Restaurant partner logged out', 'info');
    setTimeout(() => {
      window.location.href = `${base}restaurant/login.html`;
    }, 350);
  } else if (role === 'DELIVERY_MAN') {
    showToast('Delivery partner logged out', 'info');
    setTimeout(() => {
      window.location.href = `${base}deliveryman/login.html`;
    }, 350);
  } else {
    showToast('Logged out successfully', 'info');
    setTimeout(() => {
      window.location.href = `${base}index.html`;
    }, 350);
  }
}

export function renderNavbar(activeNav = '') {
  const navContainer = document.getElementById('navbar-container');
  if (!navContainer) return;

  const base = getBaseUrl();
  const path = window.location.pathname;

  let sectionRole = 'CUSTOMER';
  if (path.includes('/restaurant/')) sectionRole = 'RESTAURANT';
  else if (path.includes('/deliveryman/')) sectionRole = 'DELIVERY_MAN';

  const user = (currentUser && currentUser.role === sectionRole) ? currentUser : getStoredUser(sectionRole);
  const storedRestaurant = getStoredUser('RESTAURANT');
  const storedRider = getStoredUser('DELIVERY_MAN');
  const cartCount = getCartTotalCount();

  let userSection = `
    <div style="display: flex; align-items: center; gap: 8px;">
      <a href="${base}customer/login.html" id="nav-login-btn" class="btn btn-outline btn-sm">Log In</a>
      <a href="${base}customer/register.html" id="nav-signup-btn" class="btn btn-primary btn-sm">Sign Up</a>
      <div style="position: relative; display: inline-block;">
        <button id="nav-partner-btn" class="btn btn-outline btn-sm" style="font-size: 0.8rem; padding: 6px 10px; cursor: pointer;">
          Partners ▾
        </button>
        <div id="nav-partner-dropdown" style="display: none; position: absolute; right: 0; top: 100%; margin-top: 6px; background: #FFF; border: 1px solid var(--border); border-radius: var(--radius-md); box-shadow: var(--shadow-lg); min-width: 195px; z-index: 1000; padding: 6px 0;">
          ${storedRestaurant ? `
            <a href="${base}restaurant/dashboard.html" style="display: block; padding: 8px 16px; font-size: 0.85rem; color: var(--primary); font-weight: 700;">🏪 Kitchen (${storedRestaurant.fullName || 'Active'}) &rarr;</a>
          ` : `
            <a href="${base}restaurant/login.html" style="display: block; padding: 8px 16px; font-size: 0.85rem; color: var(--dark); font-weight: 500;">👨‍🍳 Restaurant Login</a>
          `}
          <a href="${base}restaurant/register.html" style="display: block; padding: 8px 16px; font-size: 0.85rem; color: var(--dark); font-weight: 500;">🏪 Register Restaurant</a>
          <div style="height: 1px; background: var(--border); margin: 4px 0;"></div>
          ${storedRider ? `
            <a href="${base}deliveryman/dashboard.html" style="display: block; padding: 8px 16px; font-size: 0.85rem; color: #10B981; font-weight: 700;">🛵 Rider Hub (${storedRider.fullName || 'Active'}) &rarr;</a>
          ` : `
            <a href="${base}deliveryman/login.html" style="display: block; padding: 8px 16px; font-size: 0.85rem; color: var(--dark); font-weight: 500;">🛵 Rider Login</a>
          `}
          <a href="${base}deliveryman/register.html" style="display: block; padding: 8px 16px; font-size: 0.85rem; color: var(--dark); font-weight: 500;">🚴 Register as Rider</a>
        </div>
      </div>
    </div>
  `;

  if (user) {
    let dashboardUrl = `${base}customer/dashboard.html`;
    let roleBadge = 'Customer';
    if (user.role === 'RESTAURANT') {
      dashboardUrl = `${base}restaurant/dashboard.html`;
      roleBadge = 'Restaurant';
    } else if (user.role === 'DELIVERY_MAN') {
      dashboardUrl = `${base}deliveryman/dashboard.html`;
      roleBadge = 'Rider';
    }

    userSection = `
      <div style="display: flex; align-items: center; gap: 12px;">
        <a href="${dashboardUrl}" class="btn btn-outline btn-sm" style="display: flex; align-items: center; gap: 6px;">
          <span>👤</span>
          <span>${user.fullName || user.email}</span>
          <span class="status-badge status-accepted" style="font-size: 0.7rem; padding: 2px 6px;">${roleBadge}</span>
        </a>
        <button id="logout-btn" class="btn btn-outline btn-sm" style="color: var(--danger); border-color: rgba(239, 68, 68, 0.3);">
          Logout
        </button>
      </div>
    `;
  }

  navContainer.innerHTML = `
    <nav class="navbar">
      <div class="container navbar-inner">
        <a href="${base}index.html" class="brand">
          <div class="brand-badge">🍲</div>
          <span>Advance<span class="highlight">Foodnest</span></span>
        </a>

        <ul class="nav-links">
          <li><a href="${base}index.html" class="nav-link ${activeNav === 'home' ? 'active' : ''}">Home</a></li>
          <li><a href="${base}customer/restaurants.html" class="nav-link ${activeNav === 'restaurants' ? 'active' : ''}">Restaurants</a></li>
          ${user && user.role === 'CUSTOMER' ? `
            <li><a href="${base}customer/orders.html" class="nav-link ${activeNav === 'orders' ? 'active' : ''}">My Orders</a></li>
          ` : ''}
          ${user && user.role === 'RESTAURANT' ? `
            <li><a href="${base}restaurant/dashboard.html" class="nav-link ${activeNav === 'dashboard' ? 'active' : ''}">Dashboard</a></li>
            <li><a href="${base}restaurant/dishes.html" class="nav-link ${activeNav === 'dishes' ? 'active' : ''}">Dishes</a></li>
            <li><a href="${base}restaurant/orders.html" class="nav-link ${activeNav === 'orders' ? 'active' : ''}">Orders</a></li>
          ` : ''}
          ${user && user.role === 'DELIVERY_MAN' ? `
            <li><a href="${base}deliveryman/dashboard.html" class="nav-link ${activeNav === 'dashboard' ? 'active' : ''}">Rider Hub</a></li>
            <li><a href="${base}deliveryman/deliveries.html" class="nav-link ${activeNav === 'deliveries' ? 'active' : ''}">Deliveries</a></li>
          ` : ''}
        </ul>

        <div class="nav-actions">
          <a href="${base}customer/cart.html" class="btn btn-outline btn-sm cart-badge-wrap" title="View Cart">
            <span>🛒</span>
            <span>Cart</span>
            <span class="cart-counter" id="nav-cart-count" style="${cartCount > 0 ? '' : 'display: none;'}">${cartCount}</span>
          </a>
          ${userSection}
        </div>
      </div>
    </nav>
  `;

  // Partner dropdown listener
  const partnerBtn = document.getElementById('nav-partner-btn');
  const partnerDropdown = document.getElementById('nav-partner-dropdown');
  if (partnerBtn && partnerDropdown) {
    partnerBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      const isVisible = partnerDropdown.style.display === 'block';
      partnerDropdown.style.display = isVisible ? 'none' : 'block';
    });
    document.addEventListener('click', () => {
      partnerDropdown.style.display = 'none';
    });
  }

  // Logout listener
  const logoutBtn = document.getElementById('logout-btn');
  if (logoutBtn) {
    logoutBtn.addEventListener('click', () => logoutUser(sectionRole));
  }

  // Hook Swiggy Drawer to Nav Login / Signup buttons if not on dedicated auth pages
  const isDedicatedAuthPage = window.location.pathname.includes('/login.html') || window.location.pathname.includes('/register.html');
  if (!isDedicatedAuthPage && !user) {
    const loginBtn = document.getElementById('nav-login-btn');
    const signupBtn = document.getElementById('nav-signup-btn');
    if (loginBtn) {
      loginBtn.addEventListener('click', (e) => {
        e.preventDefault();
        openAuthDrawer('login');
      });
    }
    if (signupBtn) {
      signupBtn.addEventListener('click', (e) => {
        e.preventDefault();
        openAuthDrawer('signup');
      });
    }
  }

  // Cross-tab and reactive cart badge update listener
  const updateNavCartBadge = () => {
    const counter = document.getElementById('nav-cart-count');
    if (counter) {
      const count = getCartTotalCount();
      counter.textContent = count;
      counter.style.display = count > 0 ? 'flex' : 'none';
      if (count > 0) {
        counter.style.animation = 'none';
        counter.offsetHeight; /* trigger reflow */
        counter.style.animation = 'badgePulse 0.4s ease';
      }
    }
  };

  window.addEventListener('cart-updated', updateNavCartBadge);
  window.addEventListener('storage', (e) => {
    if (!e.key || e.key === 'foodnest_cart') updateNavCartBadge();
  });
  window.addEventListener('focus', updateNavCartBadge);
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') updateNavCartBadge();
  });
}

// ==========================================
// SWIGGY-STYLE SLIDE-OUT AUTH DRAWER
// ==========================================
let authDrawerInitialized = false;

export function openAuthDrawer(initialTab = 'login') {
  ensureAuthDrawer();
  const backdrop = document.getElementById('swiggy-auth-backdrop');
  const drawer = document.getElementById('swiggy-auth-drawer');
  if (backdrop && drawer) {
    switchAuthDrawerTab(initialTab);
    backdrop.classList.add('active');
    drawer.classList.add('active');
  }
}

export function closeAuthDrawer() {
  const backdrop = document.getElementById('swiggy-auth-backdrop');
  const drawer = document.getElementById('swiggy-auth-drawer');
  if (backdrop && drawer) {
    backdrop.classList.remove('active');
    drawer.classList.remove('active');
  }
}

function switchAuthDrawerTab(tab) {
  const loginView = document.getElementById('drawer-login-view');
  const signupView = document.getElementById('drawer-signup-view');
  const loginTab = document.getElementById('drawer-tab-login');
  const signupTab = document.getElementById('drawer-tab-signup');
  const alertBox = document.getElementById('drawer-auth-alert');
  if (alertBox) alertBox.style.display = 'none';

  if (tab === 'login') {
    if (loginView) loginView.style.display = 'block';
    if (signupView) signupView.style.display = 'none';
    if (loginTab) {
      loginTab.style.color = 'var(--primary)';
      loginTab.style.borderBottom = '3px solid var(--primary)';
    }
    if (signupTab) {
      signupTab.style.color = 'var(--text-muted)';
      signupTab.style.borderBottom = 'none';
    }
  } else {
    if (loginView) loginView.style.display = 'none';
    if (signupView) signupView.style.display = 'block';
    if (signupTab) {
      signupTab.style.color = 'var(--primary)';
      signupTab.style.borderBottom = '3px solid var(--primary)';
    }
    if (loginTab) {
      loginTab.style.color = 'var(--text-muted)';
      loginTab.style.borderBottom = 'none';
    }
  }
}

function ensureAuthDrawer() {
  if (authDrawerInitialized) return;
  authDrawerInitialized = true;

  const base = getBaseUrl();
  const markup = `
    <div id="swiggy-auth-backdrop" class="swiggy-drawer-backdrop"></div>
    <div id="swiggy-auth-drawer" class="swiggy-drawer">
      <button id="swiggy-drawer-close" class="swiggy-drawer-close" title="Close">&times;</button>
      
      <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 20px;">
        <span style="font-size: 1.8rem;">🍲</span>
        <h3 style="font-size: 1.4rem; font-weight: 800; margin: 0;">Advance<span class="highlight">Foodnest</span></h3>
      </div>

      <!-- Tab Switcher -->
      <div style="display: flex; border-bottom: 2px solid var(--border); margin-bottom: 20px;">
        <button id="drawer-tab-login" type="button" style="flex: 1; padding: 10px 0; background: none; border: none; font-weight: 700; font-size: 1rem; color: var(--primary); border-bottom: 3px solid var(--primary); cursor: pointer;">
          Log In
        </button>
        <button id="drawer-tab-signup" type="button" style="flex: 1; padding: 10px 0; background: none; border: none; font-weight: 700; font-size: 1rem; color: var(--text-muted); cursor: pointer;">
          Sign Up
        </button>
      </div>

      <!-- Alert Box -->
      <div id="drawer-auth-alert" style="display: none; padding: 10px 14px; border-radius: var(--radius-md); font-size: 0.85rem; font-weight: 500; margin-bottom: 16px;"></div>

      <!-- LOGIN VIEW -->
      <div id="drawer-login-view">
        <!-- 1-Click Demo Fill Selector -->
        <div style="background: #FFF7ED; border: 1px solid #FFEDD5; border-radius: var(--radius-md); padding: 10px 12px; margin-bottom: 18px;">
          <div style="font-size: 0.72rem; font-weight: 700; text-transform: uppercase; color: var(--primary); margin-bottom: 6px;">
            ⚡ Quick 1-Click Login (Auto-Fill & Sign In)
          </div>
          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 6px;">
            <button type="button" id="drawer-demo-customer" class="btn btn-outline btn-sm" style="font-size: 0.75rem; padding: 6px 8px; background: #FFF;">
              👤 Customer
            </button>
            <button type="button" id="drawer-demo-fastfoods" class="btn btn-outline btn-sm" style="font-size: 0.75rem; padding: 6px 8px; background: #FFF;">
              🍔 Fastfoods
            </button>
            <button type="button" id="drawer-demo-foodies" class="btn btn-outline btn-sm" style="font-size: 0.75rem; padding: 6px 8px; background: #FFF;">
              🍛 Foodies
            </button>
            <button type="button" id="drawer-demo-rider" class="btn btn-outline btn-sm" style="font-size: 0.75rem; padding: 6px 8px; background: #FFF;">
              🛵 Rider
            </button>
          </div>
        </div>

        <form id="drawer-login-form">
          <div class="form-group" style="margin-bottom: 12px;">
            <label class="form-label" style="font-size: 0.82rem;" for="drawer-role">Sign In As</label>
            <select id="drawer-role" class="form-select" style="padding: 9px 12px; font-size: 0.88rem;">
              <option value="CUSTOMER">Customer Account</option>
              <option value="RESTAURANT">Restaurant Partner</option>
              <option value="DELIVERY_MAN">Delivery Partner (Rider)</option>
            </select>
          </div>

          <div class="form-group" style="margin-bottom: 12px;">
            <label class="form-label" style="font-size: 0.82rem;" for="drawer-email">Email Address</label>
            <input type="email" id="drawer-email" class="form-input" style="padding: 9px 12px; font-size: 0.88rem;" required placeholder="name@example.com">
          </div>

          <div class="form-group" style="margin-bottom: 16px;">
            <label class="form-label" style="font-size: 0.82rem;" for="drawer-password">Password</label>
            <input type="password" id="drawer-password" class="form-input" style="padding: 9px 12px; font-size: 0.88rem;" required placeholder="••••••••">
          </div>

          <button type="submit" id="drawer-login-submit" class="btn btn-primary btn-full" style="font-weight: 700; padding: 11px;">
            Sign In
          </button>
        </form>

        <div style="text-align: center; margin-top: 16px; font-size: 0.82rem; color: var(--text-muted);">
          Looking for full portals?
          <a href="${base}restaurant/login.html" style="color: var(--dark); font-weight: 600;">Restaurant</a> |
          <a href="${base}deliveryman/login.html" style="color: var(--dark); font-weight: 600;">Rider</a>
        </div>
      </div>

      <!-- SIGNUP VIEW (1-STEP WITH AUTO LOGIN) -->
      <div id="drawer-signup-view" style="display: none;">
        <form id="drawer-signup-form">
          <div class="form-group" style="margin-bottom: 12px;">
            <label class="form-label" style="font-size: 0.82rem;" for="drawer-signup-role">I want to register as</label>
            <select id="drawer-signup-role" class="form-select" style="padding: 9px 12px; font-size: 0.88rem;">
              <option value="CUSTOMER">Hungry Customer</option>
              <option value="RESTAURANT">Restaurant Partner</option>
              <option value="DELIVERY_MAN">Delivery Partner (Rider)</option>
            </select>
          </div>

          <div class="form-group" style="margin-bottom: 12px;">
            <label class="form-label" style="font-size: 0.82rem;" for="drawer-signup-name">Full Name / Business Name</label>
            <input type="text" id="drawer-signup-name" class="form-input" style="padding: 9px 12px; font-size: 0.88rem;" required minlength="2" placeholder="e.g. Maya Krishnan">
          </div>

          <div class="form-group" style="margin-bottom: 12px;">
            <label class="form-label" style="font-size: 0.82rem;" for="drawer-signup-email">Email Address</label>
            <input type="email" id="drawer-signup-email" class="form-input" style="padding: 9px 12px; font-size: 0.88rem;" required placeholder="name@example.com">
          </div>

          <div class="form-group" style="margin-bottom: 12px;">
            <label class="form-label" style="font-size: 0.82rem;" for="drawer-signup-phone">Phone Number</label>
            <input type="tel" id="drawer-signup-phone" class="form-input" style="padding: 9px 12px; font-size: 0.88rem;" required minlength="7" placeholder="e.g. 9876543210">
          </div>

          <div class="form-group" style="margin-bottom: 12px;">
            <label class="form-label" style="font-size: 0.82rem;" for="drawer-signup-password">Password (min 6 chars)</label>
            <input type="password" id="drawer-signup-password" class="form-input" style="padding: 9px 12px; font-size: 0.88rem;" required minlength="6" placeholder="••••••••">
          </div>

          <div class="form-group" style="margin-bottom: 16px;">
            <label class="form-label" style="font-size: 0.82rem;" for="drawer-signup-address">Address</label>
            <input type="text" id="drawer-signup-address" class="form-input" style="padding: 9px 12px; font-size: 0.88rem;" required placeholder="Street / Locality, City">
          </div>

          <button type="submit" id="drawer-signup-submit" class="btn btn-primary btn-full" style="font-weight: 700; padding: 11px;">
            Create Account & Sign In
          </button>
        </form>
      </div>
    </div>
  `;

  document.body.insertAdjacentHTML('beforeend', markup);

  // Close handlers
  document.getElementById('swiggy-drawer-close')?.addEventListener('click', closeAuthDrawer);
  document.getElementById('swiggy-auth-backdrop')?.addEventListener('click', closeAuthDrawer);

  // Tab switchers
  document.getElementById('drawer-tab-login')?.addEventListener('click', () => switchAuthDrawerTab('login'));
  document.getElementById('drawer-tab-signup')?.addEventListener('click', () => switchAuthDrawerTab('signup'));

  function setDrawerAlert(msg, type = 'error') {
    const alert = document.getElementById('drawer-auth-alert');
    if (!alert) return;
    if (!msg) {
      alert.style.display = 'none';
      return;
    }
    alert.style.display = 'block';
    if (type === 'error') {
      alert.style.background = '#FEE2E2';
      alert.style.border = '1px solid #FCA5A5';
      alert.style.color = '#991B1B';
    } else if (type === 'success') {
      alert.style.background = '#DCFCE7';
      alert.style.border = '1px solid #86EFAC';
      alert.style.color = '#166534';
    }
    alert.innerHTML = msg;
  }

  // Quick Demo Buttons
  const roleSelect = document.getElementById('drawer-role');
  const emailInput = document.getElementById('drawer-email');
  const passwordInput = document.getElementById('drawer-password');

  async function executeDrawerLogin(role, email, password) {
    const submitBtn = document.getElementById('drawer-login-submit');
    setDrawerAlert('', '');
    submitBtn.disabled = true;
    submitBtn.textContent = 'Signing in...';

    let endpoint = '/auth/customer/login';
    let targetDash = `${base}customer/dashboard.html`;
    if (role === 'RESTAURANT') {
      endpoint = '/auth/restaurant/login';
      targetDash = `${base}restaurant/dashboard.html`;
    } else if (role === 'DELIVERY_MAN') {
      endpoint = '/auth/deliveryman/login';
      targetDash = `${base}deliveryman/dashboard.html`;
    }

    try {
      const res = await apiRequest(endpoint, {
        method: 'POST',
        body: JSON.stringify({ email, password })
      });

      if (res.success) {
        saveAuthSession(res.data, res.data?.token, role);
        setDrawerAlert('✅ Signed in! Redirecting...', 'success');
        showToast('Signed in successfully!', 'success');
        setTimeout(() => {
          window.location.href = targetDash;
        }, 300);
      } else {
        const err = res.message || 'Login failed';
        setDrawerAlert(`❌ ${err}`, 'error');
        showToast(err, 'error');
        submitBtn.disabled = false;
        submitBtn.textContent = 'Sign In';
      }
    } catch (e) {
      setDrawerAlert('❌ Cannot reach backend server. Please verify your connection.', 'error');
      submitBtn.disabled = false;
      submitBtn.textContent = 'Sign In';
    }
  }

  document.getElementById('drawer-demo-customer')?.addEventListener('click', () => {
    roleSelect.value = 'CUSTOMER';
    emailInput.value = 'customer@foodnest.com';
    passwordInput.value = 'password123';
    executeDrawerLogin('CUSTOMER', 'customer@foodnest.com', 'password123');
  });

  document.getElementById('drawer-demo-fastfoods')?.addEventListener('click', () => {
    roleSelect.value = 'RESTAURANT';
    emailInput.value = 'fastfoods@foodnest.com';
    passwordInput.value = 'password123';
    executeDrawerLogin('RESTAURANT', 'fastfoods@foodnest.com', 'password123');
  });

  document.getElementById('drawer-demo-foodies')?.addEventListener('click', () => {
    roleSelect.value = 'RESTAURANT';
    emailInput.value = 'foodies@foodnest.com';
    passwordInput.value = 'password123';
    executeDrawerLogin('RESTAURANT', 'foodies@foodnest.com', 'password123');
  });

  document.getElementById('drawer-demo-rider')?.addEventListener('click', () => {
    roleSelect.value = 'DELIVERY_MAN';
    emailInput.value = 'rider1@foodnest.com';
    passwordInput.value = 'password123';
    executeDrawerLogin('DELIVERY_MAN', 'rider1@foodnest.com', 'password123');
  });

  document.getElementById('drawer-login-form')?.addEventListener('submit', (e) => {
    e.preventDefault();
    executeDrawerLogin(roleSelect.value, emailInput.value.trim(), passwordInput.value);
  });

  document.getElementById('drawer-login-submit')?.addEventListener('click', (e) => {
    e.preventDefault();
    executeDrawerLogin(roleSelect.value, emailInput.value.trim(), passwordInput.value);
  });

  // Drawer Sign Up Form
  document.getElementById('drawer-signup-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const signupBtn = document.getElementById('drawer-signup-submit');
    setDrawerAlert('', '');
    signupBtn.disabled = true;
    signupBtn.textContent = 'Creating account & logging in...';

    const role = document.getElementById('drawer-signup-role').value;
    const name = document.getElementById('drawer-signup-name').value.trim();
    const email = document.getElementById('drawer-signup-email').value.trim();
    const phone = document.getElementById('drawer-signup-phone').value.trim();
    const password = document.getElementById('drawer-signup-password').value;
    const address = document.getElementById('drawer-signup-address').value.trim();

    let endpoint = '/auth/customer/register';
    let payload = { fullName: name, email, phone, password, address };
    let targetDash = `${base}customer/dashboard.html`;

    if (role === 'RESTAURANT') {
      endpoint = '/auth/restaurant/register';
      payload = { name, ownerName: name, email, phone, password, address, cuisineType: 'Multi-Cuisine' };
      targetDash = `${base}restaurant/dashboard.html`;
    } else if (role === 'DELIVERY_MAN') {
      endpoint = '/auth/deliveryman/register';
      payload = { fullName: name, email, phone, password, vehicleType: 'Motorbike (Bike)' };
      targetDash = `${base}deliveryman/dashboard.html`;
    }

    try {
      const res = await apiRequest(endpoint, {
        method: 'POST',
        body: JSON.stringify(payload)
      });

      if (res.success) {
        saveAuthSession(res.data, res.data?.token, role);
        setDrawerAlert('✅ Account created! Logging you in...', 'success');
        showToast('Account created successfully!', 'success');
        setTimeout(() => {
          window.location.href = targetDash;
        }, 300);
      } else {
        const err = res.message || 'Registration failed';
        setDrawerAlert(`❌ ${err}`, 'error');
        showToast(err, 'error');
        signupBtn.disabled = false;
        signupBtn.textContent = 'Create Account & Sign In';
      }
    } catch (e) {
      setDrawerAlert('❌ Cannot reach backend server. Please verify your connection.', 'error');
      signupBtn.disabled = false;
      signupBtn.textContent = 'Create Account & Sign In';
    }
  });
}

