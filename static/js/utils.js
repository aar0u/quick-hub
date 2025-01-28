function formatDate(timestamp) {
  return new Date(timestamp).toLocaleString('en-SG', {
    dateStyle: 'short',
    timeStyle: 'short',
    hour12: false,
  });
}

function formatFileSize(bytes) {
  // Check if input is a valid number and greater than or equal to 0
  if (typeof bytes !== 'number' || isNaN(bytes) || bytes < 0) {
    return '';
  }

  if (bytes === 0) return '0 B';

  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(1024));

  return parseFloat((bytes / Math.pow(1024, i)).toFixed(2)) + ' ' + sizes[i];
}

function escapeFilename(filename) {
  return filename.replace(/%/g, '%25').replace(/\+/g, '%2B').replace(/\?/g, '%3F').replace(/\^/g, '%5E')
    .replace(/ /g, '%20').replace(/#/g, '%23').replace(/\$/, '%24').replace(/&/g, '%26');
}

function showToast(message, duration = 3000) {
  const toast = document.createElement('div');
  toast.textContent = message;
  toast.style.position = 'fixed';
  toast.style.bottom = '20px';
  toast.style.left = '50%';
  toast.style.transform = 'translateX(-50%)';
  toast.style.backgroundColor = 'rgba(0, 0, 0, 0.8)';
  toast.style.color = 'white';
  toast.style.padding = '10px 20px';
  toast.style.borderRadius = '5px';
  toast.style.boxShadow = '0 2px 5px rgba(0, 0, 0, 0.3)';
  toast.style.fontSize = '14px';
  toast.style.zIndex = '9999';
  toast.style.opacity = '0';
  toast.style.transition = 'opacity 0.3s';

  document.body.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '1';
  }, 10);

  setTimeout(() => {
    toast.style.opacity = '0';
    setTimeout(() => {
      document.body.removeChild(toast);
    }, 300);
  }, duration);
}

export { formatDate, formatFileSize, escapeFilename, showToast };
