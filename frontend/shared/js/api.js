// API & Client Helper for AdvanceFoodnest
import { showToast } from './toast.js';

export function getApiBase() {
  if (window.location.port === '8080') {
    return '/api';
  }
  const hostname = window.location.hostname || 'localhost';
  return `http://${hostname}:8080/api`;
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
    const token = cookieToken || cachedCsrfToken;
    if (token) {
      headers['X-XSRF-TOKEN'] = decodeURIComponent(token);
    }
  }

  const fetchOptions = {
    method,
    headers,
    credentials: 'include',
    ...options
  };

  try {
    const response = await fetch(url, fetchOptions);

    if (response.status === 401) {
      // Unauthorized
      return { success: false, status: 401, message: 'Authentication required. Please log in.' };
    }

    if (response.status === 403) {
      // Forbidden
      return { success: false, status: 403, message: 'You do not have permission for this action.' };
    }

    const contentType = response.headers.get('content-type');
    let data;
    if (contentType && contentType.includes('application/json')) {
      data = await response.json();
    } else {
      const text = await response.text();
      data = { success: response.ok, message: text };
    }

    if (!response.ok) {
      const errorMsg = data.message || `Request failed with status ${response.status}`;
      return { success: false, status: response.status, message: errorMsg, errors: data.data };
    }

    return { success: true, status: response.status, data: data.data !== undefined ? data.data : data, message: data.message };
  } catch (err) {
    console.error('Network/API Error:', err);
    return {
      success: false,
      status: 0,
      message: 'Cannot connect to AdvanceFoodnest backend. Ensure Spring Boot is running on port 8080.'
    };
  }
}

// ==========================
// CART STORAGE HELPER
// ==========================
const CART_STORAGE_KEY = 'foodnest_cart';

export function getCart() {
  try {
    const raw = localStorage.getItem(CART_STORAGE_KEY);
    return raw ? JSON.parse(raw) : { restaurantId: null, restaurantName: '', items: [] };
  } catch (e) {
    return { restaurantId: null, restaurantName: '', items: [] };
  }
}

export function saveCart(cart) {
  localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(cart));
  window.dispatchEvent(new CustomEvent('cart-updated', { detail: cart }));
}

export function clearCart() {
  const empty = { restaurantId: null, restaurantName: '', items: [] };
  saveCart(empty);
  return empty;
}

export function addToCart(dish, quantity = 1) {
  const cart = getCart();

  if (cart.restaurantId && cart.restaurantId !== dish.restaurantId) {
    const confirmClear = confirm(
      `Your cart already contains dishes from "${cart.restaurantName}". Clear your cart to order from "${dish.restaurantName}"?`
    );
    if (!confirmClear) return false;
    cart.restaurantId = dish.restaurantId;
    cart.restaurantName = dish.restaurantName;
    cart.items = [];
  } else if (!cart.restaurantId) {
    cart.restaurantId = dish.restaurantId;
    cart.restaurantName = dish.restaurantName;
  }

  const existing = cart.items.find(i => i.dishId === dish.id);
  if (existing) {
    existing.quantity += quantity;
  } else {
    cart.items.push({
      dishId: dish.id,
      name: dish.name,
      price: dish.price,
      imageUrl: dish.imageUrl,
      quantity
    });
  }

  saveCart(cart);
  showToast(`Added "${dish.name}" to cart!`, 'success');
  return true;
}

export function updateCartQuantity(dishId, quantity) {
  const cart = getCart();
  const index = cart.items.findIndex(i => i.dishId === dishId);
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
  const cart = getCart();
  return cart.items.reduce((sum, item) => sum + item.quantity, 0);
}
