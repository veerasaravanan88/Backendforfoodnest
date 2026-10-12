// Authentication & Route Protection Helper for AdvanceFoodnest
import { apiRequest, fetchCsrfToken, getCartTotalCount } from './api.js';
import { showToast } from './toast.js';

let currentUser = null;

export async function checkAuth() {
  await fetchCsrfToken();
  const res = await apiRequest('/auth/me');
  if (res.success && res.data) {
    currentUser = res.data;
    return currentUser;
  }
  currentUser = null;
  return null;
}

export function getCurrentUser() {
  return currentUser;
}

export async function requireAuth(expectedRole = null) {
  const user = await checkAuth();
  if (!user) {
    let redirect = '/customer/login.html';
    if (expectedRole === 'RESTAURANT') redirect = '/restaurant/login.html';
    if (expectedRole === 'DELIVERY_MAN') redirect = '/deliveryman/login.html';
    window.location.href = redirect;
    return null;
  }

  if (expectedRole && user.role !== expectedRole) {
    alert(`Access denied: This section is for ${expectedRole} accounts only.`);
    if (user.role === 'CUSTOMER') window.location.href = '/customer/dashboard.html';
    else if (user.role === 'RESTAURANT') window.location.href = '/restaurant/dashboard.html';
    else if (user.role === 'DELIVERY_MAN') window.location.href = '/deliveryman/dashboard.html';
    else window.location.href = '/';
    return null;
  }

  return user;
}

export async function logoutUser() {
  const res = await apiRequest('/auth/logout', { method: 'POST' });
  currentUser = null;
  showToast('Logged out successfully', 'info');
  setTimeout(() => {
    window.location.href = '/';
  }, 500);
}

export function renderNavbar(activeNav = '') {
  const navContainer = document.getElementById('navbar-container');
  if (!navContainer) return;

  const user = currentUser;
  const cartCount = getCartTotalCount();

  let userSection = `
    <a href="/customer/login.html" class="btn btn-outline btn-sm">Log In</a>
    <a href="/customer/register.html" class="btn btn-primary btn-sm">Sign Up</a>
  `;

  if (user) {
    let dashboardUrl = '/customer/dashboard.html';
    let roleBadge = 'Customer';
    if (user.role === 'RESTAURANT') {
      dashboardUrl = '/restaurant/dashboard.html';
      roleBadge = 'Restaurant';
    } else if (user.role === 'DELIVERY_MAN') {
      dashboardUrl = '/deliveryman/dashboard.html';
      roleBadge = 'Rider';
    }

    userSection = `
      <div style="display: flex; align-items: center; gap: 14px;">
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
        <a href="/" class="brand">
          <div class="brand-badge">🍲</div>
          <span>Advance<span class="highlight">Foodnest</span></span>
        </a>

        <ul class="nav-links">
          <li><a href="/" class="nav-link ${activeNav === 'home' ? 'active' : ''}">Home</a></li>
          <li><a href="/customer/restaurants.html" class="nav-link ${activeNav === 'restaurants' ? 'active' : ''}">Restaurants</a></li>
          ${user && user.role === 'CUSTOMER' ? `
            <li><a href="/customer/orders.html" class="nav-link ${activeNav === 'orders' ? 'active' : ''}">My Orders</a></li>
          ` : ''}
          ${user && user.role === 'RESTAURANT' ? `
            <li><a href="/restaurant/dashboard.html" class="nav-link ${activeNav === 'dashboard' ? 'active' : ''}">Dashboard</a></li>
            <li><a href="/restaurant/dishes.html" class="nav-link ${activeNav === 'dishes' ? 'active' : ''}">Dishes</a></li>
            <li><a href="/restaurant/orders.html" class="nav-link ${activeNav === 'orders' ? 'active' : ''}">Orders</a></li>
          ` : ''}
          ${user && user.role === 'DELIVERY_MAN' ? `
            <li><a href="/deliveryman/dashboard.html" class="nav-link ${activeNav === 'dashboard' ? 'active' : ''}">Rider Hub</a></li>
            <li><a href="/deliveryman/deliveries.html" class="nav-link ${activeNav === 'deliveries' ? 'active' : ''}">Deliveries</a></li>
          ` : ''}
        </ul>

        <div class="nav-actions">
          <a href="/customer/cart.html" class="btn btn-outline btn-sm cart-badge-wrap" title="View Cart">
            <span>🛒</span>
            <span>Cart</span>
            <span class="cart-counter" id="nav-cart-count" style="${cartCount > 0 ? '' : 'display: none;'}">${cartCount}</span>
          </a>
          ${userSection}
        </div>
      </div>
    </nav>
  `;

  const logoutBtn = document.getElementById('logout-btn');
  if (logoutBtn) {
    logoutBtn.addEventListener('click', logoutUser);
  }

  window.addEventListener('cart-updated', (e) => {
    const counter = document.getElementById('nav-cart-count');
    if (counter) {
      const count = getCartTotalCount();
      counter.textContent = count;
      counter.style.display = count > 0 ? 'flex' : 'none';
    }
  });
}
