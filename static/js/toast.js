const TOAST_DURATION = 3000;

const container = document.createElement('div');
container.className = 'toast-container';
document.body.appendChild(container);

export function showToast(message = '', options = {}) {
  const {
    type = 'info',
    key,
    duration,
  } = options;

  let toast;

  if (key) {
    toast = container.querySelector(`[data-toast-key="${key}"]`);
    if (!toast) {
      toast = document.createElement('div');
      toast.dataset.toastKey = key;
      container.appendChild(toast);
    }
  } else {
    toast = document.createElement('div');
    container.appendChild(toast);
  }

  toast.className = `toast toast-${type}`;
  toast.textContent = message;

  requestAnimationFrame(() => {
    toast.classList.add('toast-show');
  });

  const finalDuration = duration ?? (key ? 0 : TOAST_DURATION);
  if (!finalDuration || finalDuration <= 0) return;

  setTimeout(() => {
    toast.classList.remove('toast-show');
    toast.addEventListener('transitionend', () => {
      toast.remove();
    });
  }, finalDuration);
}

export function closeToastByKey(key) {
  const toast = container.querySelector(`[data-toast-key="${key}"]`);
  if (!toast) return;

  toast.classList.remove('toast-show');
  toast.addEventListener('transitionend', () => {
    toast.remove();
  }, { once: true });
}

export function showInfo(message) {
  showToast(message, { type: 'info' });
}

export function showSuccess(message) {
  showToast(message, { type: 'success' });
}

export function showError(message) {
  showToast(message, { type: 'error' });
}
