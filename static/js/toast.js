const TOAST_DURATION = 3000;

const container = document.createElement('div');
container.className = 'toast-container';
document.body.appendChild(container);

export function showToast(message, type = 'info') {
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.textContent = message;
  container.appendChild(toast);

  requestAnimationFrame(() => {
    toast.classList.add('toast-show');
  });

  setTimeout(() => {
    toast.classList.remove('toast-show');
    toast.addEventListener('transitionend', () => {
      toast.remove();
    });
  }, TOAST_DURATION);
}

export function showInfo(message) {
  showToast(message, 'info');
}

export function showSuccess(message) {
  showToast(message, 'success');
}

export function showError(message) {
  showToast(message, 'error');
}
