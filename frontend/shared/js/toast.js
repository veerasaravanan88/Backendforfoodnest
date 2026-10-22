// Toast Notification Helper for AdvanceFoodnest

export function showToast(message, type = 'info', duration = 3800) {
  let container = document.getElementById('toast-container');
  if (!container) {
    container = document.createElement('div');
    container.id = 'toast-container';
    document.body.appendChild(container);
  }

  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.style.position = 'relative';
  toast.style.overflow = 'hidden';
  
  const icon = type === 'success' ? '✓' : type === 'error' ? '✕' : 'ℹ';
  const iconBg = type === 'success' ? '#10B981' : type === 'error' ? '#EF4444' : '#FF6B00';
  
  toast.innerHTML = `
    <div style="width: 26px; height: 26px; border-radius: 50%; background: ${iconBg}; color: #FFF; display: flex; align-items: center; justify-content: center; font-weight: 800; font-size: 0.85rem; flex-shrink: 0; box-shadow: 0 2px 6px rgba(0,0,0,0.15);">
      ${icon}
    </div>
    <div style="flex: 1; font-weight: 500; line-height: 1.4;">${message}</div>
    <div style="position: absolute; bottom: 0; left: 0; height: 3px; background: ${iconBg}; border-radius: 0 0 0 8px; width: 100%; animation: progressShrink ${duration}ms linear forwards;"></div>
  `;

  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateY(12px) scale(0.95)';
    setTimeout(() => toast.remove(), 320);
  }, duration);
}
