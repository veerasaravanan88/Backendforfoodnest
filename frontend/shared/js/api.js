// API & Client Helper for AdvanceFoodnest
import { showToast } from './toast.js';

export const PRODUCTION_API_URL = 'https://backendforfoodnest-production.up.railway.app/api';

export function getApiBase() {
  // 1. Allow explicit override via localStorage (useful for local debugging)
  try {
    const customApi = localStorage.getItem('foodnest_api_base');
    if (customApi) return customApi;
  } catch (e) {}

  // 2. Allow override via window.__API_BASE__
  if (typeof window !== 'undefined' && window.__API_BASE__) {
    return window.__API_BASE__;
  }

  // 3. If running locally on localhost or 127.0.0.1
  if (typeof window !== 'undefined') {
    const isLocalhost = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1';
    if (isLocalhost) {
      if (window.location.port === '8080') {
        return '/api';
      }
      return 'http://localhost:8080/api';
    }
  }

  // 4. Deployed production backend
  return PRODUCTION_API_URL;
}

export const API_BASE = getApiBase();

function getCookie(name) {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) return parts.pop().split(';').shift();
  return null;
}

let cachedCsrfToken = null;

export async function fetchCsrfToken() {
  try {
    const res = await fetch(`${API_BASE}/auth/csrf`, { credentials: 'include' });
    if (res.ok) {
      const json = await res.json();
      if (json.data && json.data.token) {
        cachedCsrfToken = json.data.token;
      }
    }
  } catch (e) {
    console.debug('CSRF token fetch bypassed:', e);
  }
}

export const AUTH_KEYS = {
  CUSTOMER_USER: 'foodnest_customer_user',
  CUSTOMER_TOKEN: 'foodnest_customer_token',
  RESTAURANT_USER: 'foodnest_restaurant_user',
  RESTAURANT_TOKEN: 'foodnest_restaurant_token',
  RIDER_USER: 'foodnest_rider_user',
  RIDER_TOKEN: 'foodnest_rider_token'
};

export function getStoredToken(role = null) {
  let targetRole = role;
  if (!targetRole) {
    const path = window.location.pathname;
    if (path.includes('/restaurant/')) targetRole = 'RESTAURANT';
    else if (path.includes('/deliveryman/')) targetRole = 'DELIVERY_MAN';
    else targetRole = 'CUSTOMER';
  }

  let tokenKey = AUTH_KEYS.CUSTOMER_TOKEN;
  if (targetRole === 'RESTAURANT') tokenKey = AUTH_KEYS.RESTAURANT_TOKEN;
  else if (targetRole === 'DELIVERY_MAN') tokenKey = AUTH_KEYS.RIDER_TOKEN;

  try {
    const val = localStorage.getItem(tokenKey) || sessionStorage.getItem(tokenKey);
    if (val) return val;
    // Strict isolation: NEVER fall back to another role's token!
  } catch (e) {
    console.debug('Storage read bypassed:', e);
  }
  return null;
}

export async function apiRequest(endpoint, options = {}) {
  const url = endpoint.startsWith('http') ? endpoint : `${API_BASE}${endpoint}`;
  const method = (options.method || 'GET').toUpperCase();

  const headers = {
    'Accept': 'application/json',
    ...(options.headers || {})
  };

  if (options.body && !(options.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json';
  }

  // Attach CSRF Token for state-changing methods
  if (['POST', 'PUT', 'DELETE', 'PATCH'].includes(method)) {
    const cookieToken = getCookie('XSRF-TOKEN');
    let token = cookieToken || cachedCsrfToken;
    if (!token) {
      await fetchCsrfToken();
      token = cachedCsrfToken;
    }
    if (token) {
      headers['X-XSRF-TOKEN'] = decodeURIComponent(token);
    }
  }

  // Resolve role-specific token for isolated multi-portal authentication
  let targetRole = options.role;
  if (!targetRole) {
    if (endpoint.includes('/restaurant/') || window.location.pathname.includes('/restaurant/')) {
      targetRole = 'RESTAURANT';
    } else if (endpoint.includes('/deliveryman/') || window.location.pathname.includes('/deliveryman/')) {
      targetRole = 'DELIVERY_MAN';
    } else if (endpoint.includes('/customer/') || endpoint.includes('/orders') || endpoint.includes('/cart') || window.location.pathname.includes('/customer/')) {
      targetRole = 'CUSTOMER';
    }
  }

  const authToken = options.token || getStoredToken(targetRole);
  if (authToken) {
    headers['Authorization'] = `Bearer ${authToken}`;
    headers['X-Auth-Token'] = authToken;
  }

  const fetchOptions = {
    method,
    headers,
    credentials: 'include',
    ...options
  };

  try {
    const response = await fetch(url, fetchOptions);

    const contentType = response.headers.get('content-type');
    let data;
    if (contentType && contentType.includes('application/json')) {
      data = await response.json();
    } else {
      const text = await response.text();
      data = { success: response.ok, message: text };
    }

    if (!response.ok) {
      let defaultMsg = `Request failed with status ${response.status}`;
      if (response.status === 401) defaultMsg = 'Authentication required. Please log in.';
      else if (response.status === 403) defaultMsg = 'You do not have permission for this action.';
      const errorMsg = data.message || defaultMsg;
      return { success: false, status: response.status, message: errorMsg, errors: data.data };
    }

    return { success: true, status: response.status, data: data.data !== undefined ? data.data : data, message: data.message };
  } catch (err) {
    console.error('Network/API Error:', err);
    return {
      success: false,
      status: 0,
      message: 'Cannot connect to AdvanceFoodnest backend. Please check your internet connection or backend server status.'
    };
  }
}

// ==========================
// ==========================
// CART STORAGE HELPER (Multi-tier with localStorage, sessionStorage & Cookies)
// ==========================
const CART_STORAGE_KEY = 'foodnest_cart';

function setCartCookie(cart) {
  try {
    const serialized = encodeURIComponent(JSON.stringify(cart));
    document.cookie = `${CART_STORAGE_KEY}=${serialized}; path=/; max-age=604800; SameSite=Lax`;
  } catch (e) {}
}

function getCartCookie() {
  try {
    const val = getCookie(CART_STORAGE_KEY);
    if (!val) return null;
    const parsed = JSON.parse(decodeURIComponent(val));
    if (parsed && typeof parsed === 'object' && Array.isArray(parsed.items)) return parsed;
  } catch (e) {}
  return null;
}

export function getCart() {
  let cart = null;

  // 1. Try localStorage
  try {
    const raw = localStorage.getItem(CART_STORAGE_KEY);
    if (raw) {
      const parsed = JSON.parse(raw);
      if (parsed && typeof parsed === 'object' && Array.isArray(parsed.items)) {
        cart = parsed;
      }
    }
  } catch (e) {}

  // 2. Fallback to sessionStorage if empty
  if (!cart || !cart.items || cart.items.length === 0) {
    try {
      const rawSession = sessionStorage.getItem(CART_STORAGE_KEY);
      if (rawSession) {
        const parsed = JSON.parse(rawSession);
        if (parsed && typeof parsed === 'object' && Array.isArray(parsed.items) && parsed.items.length > 0) {
          cart = parsed;
          try { localStorage.setItem(CART_STORAGE_KEY, rawSession); } catch(e) {}
        }
      }
    } catch (e) {}
  }

  // 3. Fallback to Cookie if still empty
  if (!cart || !cart.items || cart.items.length === 0) {
    const cookieCart = getCartCookie();
    if (cookieCart && Array.isArray(cookieCart.items) && cookieCart.items.length > 0) {
      cart = cookieCart;
      try { localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(cart)); } catch(e) {}
    }
  }

  if (!cart || typeof cart !== 'object') {
    cart = { restaurantId: null, restaurantName: '', items: [] };
  }
  if (!Array.isArray(cart.items)) {
    cart.items = [];
  }
  return cart;
}

export function saveCart(cart) {
  try {
    if (!cart || typeof cart !== 'object') {
      cart = { restaurantId: null, restaurantName: '', items: [] };
    }
    if (!Array.isArray(cart.items)) {
      cart.items = [];
    }
    const jsonStr = JSON.stringify(cart);
    localStorage.setItem(CART_STORAGE_KEY, jsonStr);
    try { sessionStorage.setItem(CART_STORAGE_KEY, jsonStr); } catch (e) {}
    setCartCookie(cart);

    window.dispatchEvent(new CustomEvent('cart-updated', { detail: cart }));
  } catch (e) {
    console.debug('Failed to save cart to localStorage:', e);
  }
}

export function clearCart() {
  const empty = { restaurantId: null, restaurantName: '', items: [] };
  saveCart(empty);
  try {
    document.cookie = `${CART_STORAGE_KEY}=; path=/; max-age=0; SameSite=Lax`;
  } catch (e) {}
  return empty;
}

export function promptRestaurantSwitch(oldRestName, newRestName) {
  return new Promise((resolve) => {
    // If modal already exists, remove it
    const existing = document.getElementById('switch-restaurant-modal');
    if (existing) existing.remove();

    const overlay = document.createElement('div');
    overlay.id = 'switch-restaurant-modal';
    overlay.style.cssText = `
      position: fixed; inset: 0; background: rgba(0,0,0,0.6); backdrop-filter: blur(4px);
      z-index: 99999; display: flex; align-items: center; justify-content: center; padding: 20px;
    `;

    const cartUrl = window.location.pathname.includes('/customer/') ? './cart.html' : './customer/cart.html';

    overlay.innerHTML = `
      <div style="background: #FFF; border-radius: 20px; max-width: 440px; width: 100%; padding: 28px; box-shadow: 0 20px 50px rgba(0,0,0,0.3); text-align: center; border: 1px solid var(--border); animation: popIn 0.25s cubic-bezier(0.16, 1, 0.3, 1);">
        <div style="width: 58px; height: 58px; background: rgba(255,107,0,0.12); color: var(--primary); border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 1.8rem; margin: 0 auto 16px;">
          🍽️
        </div>
        <h3 style="font-size: 1.25rem; font-weight: 800; color: var(--dark); margin-bottom: 8px;">
          Replace items in cart?
        </h3>
        <p style="color: var(--text-muted); font-size: 0.92rem; line-height: 1.5; margin-bottom: 22px;">
          Your cart currently has dishes from <strong style="color: var(--dark);">"${oldRestName || 'another restaurant'}"</strong>.<br>
          AdvanceFoodnest delivers each order from one kitchen at a time. Would you like to discard them and order from <strong style="color: var(--primary);">"${newRestName || 'this restaurant'}"</strong> instead?
        </p>

        <div style="display: flex; flex-direction: column; gap: 10px;">
          <button id="modal-btn-confirm-switch" class="btn btn-primary btn-full" style="padding: 12px; font-weight: 800; font-size: 0.95rem; box-shadow: 0 4px 12px rgba(255,107,0,0.3);">
            Yes, Discard & Order from "${newRestName || 'New Restaurant'}" ➔
          </button>
          <a href="${cartUrl}" class="btn btn-outline btn-full" style="padding: 10px; font-weight: 700; font-size: 0.9rem; text-decoration: none; display: block; text-align: center;">
            Keep & View Current Cart ("${oldRestName || 'Current'}") 🛒
          </a>
          <button id="modal-btn-cancel-switch" class="btn btn-sm" style="background: none; border: none; color: var(--text-muted); cursor: pointer; padding: 6px; font-weight: 600;">
            Never mind, keep browsing
          </button>
        </div>
      </div>
    `;

    document.body.appendChild(overlay);

    document.getElementById('modal-btn-confirm-switch').onclick = () => {
      overlay.remove();
      resolve(true);
    };

    document.getElementById('modal-btn-cancel-switch').onclick = () => {
      overlay.remove();
      resolve(false);
    };

    overlay.onclick = (e) => {
      if (e.target === overlay) {
        overlay.remove();
        resolve(false);
      }
    };
  });
}

export async function addToCart(dish, quantity = 1) {
  const cart = getCart();

  const dishRestId = dish.restaurantId ? String(dish.restaurantId) : null;
  const currentRestId = cart.restaurantId ? String(cart.restaurantId) : null;

  // Only prompt to switch restaurant IF the cart actually HAS items from a DIFFERENT restaurant!
  if (cart.items.length > 0 && currentRestId && dishRestId && currentRestId !== dishRestId) {
    const confirmClear = await promptRestaurantSwitch(cart.restaurantName, dish.restaurantName);
    if (!confirmClear) return false;
    cart.restaurantId = dish.restaurantId;
    cart.restaurantName = dish.restaurantName;
    cart.items = [];
  } else {
    cart.restaurantId = dish.restaurantId;
    cart.restaurantName = dish.restaurantName || cart.restaurantName;
  }

  const targetDishId = dish.dishId || dish.id;
  const priceVal = parseFloat(dish.price || dish.unitPrice || 0);
  const existing = cart.items.find(i => String(i.dishId) === String(targetDishId) || String(i.id) === String(targetDishId));
  if (existing) {
    existing.quantity = (existing.quantity || 1) + quantity;
    if (!existing.price) existing.price = priceVal;
  } else {
    cart.items.push({
      dishId: targetDishId,
      id: targetDishId,
      name: dish.name,
      price: priceVal,
      imageUrl: dish.imageUrl,
      quantity
    });
  }

  saveCart(cart);
  const cartUrl = window.location.pathname.includes('/customer/') ? './cart.html' : './customer/cart.html';
  showToast(`Added "${dish.name}" to cart! <a href="${cartUrl}" style="color: #FFF; background: var(--primary); padding: 3px 8px; border-radius: var(--radius-sm); margin-left: 8px; font-weight: 700; text-decoration: none; font-size: 0.85rem; display: inline-block;">View Cart 🛒</a>`, 'success');
  return true;
}

export async function addMultipleToCart(itemsToAdd) {
  if (!Array.isArray(itemsToAdd) || itemsToAdd.length === 0) return false;
  const cart = getCart();

  const firstItem = itemsToAdd[0].dish || itemsToAdd[0];
  const dishRestId = firstItem.restaurantId ? String(firstItem.restaurantId) : null;
  const currentRestId = cart.restaurantId ? String(cart.restaurantId) : null;

  if (cart.items.length > 0 && currentRestId && dishRestId && currentRestId !== dishRestId) {
    const confirmClear = await promptRestaurantSwitch(cart.restaurantName, firstItem.restaurantName);
    if (!confirmClear) return false;
    cart.restaurantId = firstItem.restaurantId;
    cart.restaurantName = firstItem.restaurantName;
    cart.items = [];
  } else if (!cart.restaurantId && dishRestId) {
    cart.restaurantId = firstItem.restaurantId;
    cart.restaurantName = firstItem.restaurantName || cart.restaurantName;
  }

  let totalAddedCount = 0;
  itemsToAdd.forEach(entry => {
    const dish = entry.dish || entry;
    const qty = entry.quantity || 1;
    if (qty <= 0) return;

    const targetDishId = dish.dishId || dish.id;
    const priceVal = parseFloat(dish.price || dish.unitPrice || 0);
    const existing = cart.items.find(i => String(i.dishId) === String(targetDishId) || String(i.id) === String(targetDishId));
    if (existing) {
      existing.quantity = (existing.quantity || 0) + qty;
      if (!existing.price) existing.price = priceVal;
    } else {
      cart.items.push({
        dishId: targetDishId,
        id: targetDishId,
        name: dish.name,
        price: priceVal,
        imageUrl: dish.imageUrl,
        quantity: qty
      });
    }
    totalAddedCount += qty;
  });

  saveCart(cart);
  const cartUrl = window.location.pathname.includes('/customer/') ? './cart.html' : './customer/cart.html';
  const dishCount = itemsToAdd.length;
  showToast(`Added ${dishCount} dish${dishCount > 1 ? 'es' : ''} (${totalAddedCount} total items) to cart! <a href="${cartUrl}" style="color: #FFF; background: var(--primary); padding: 3px 8px; border-radius: var(--radius-sm); margin-left: 8px; font-weight: 700; text-decoration: none; font-size: 0.85rem; display: inline-block;">View Cart 🛒</a>`, 'success');
  return true;
}

export function updateCartQuantity(dishId, quantity) {
  const cart = getCart();
  const index = cart.items.findIndex(i => String(i.dishId) === String(dishId) || String(i.id) === String(dishId));
  if (index !== -1) {
    if (quantity <= 0) {
      cart.items.splice(index, 1);
    } else {
      cart.items[index].quantity = quantity;
    }

    if (cart.items.length === 0) {
      cart.restaurantId = null;
      cart.restaurantName = '';
    }

    saveCart(cart);
  }
  return cart;
}

export function getCartTotalCount() {
  try {
    const cart = getCart();
    if (!cart || !Array.isArray(cart.items)) return 0;
    return cart.items.reduce((sum, item) => sum + (item.quantity || 0), 0);
  } catch (e) {
    return 0;
  }
}
