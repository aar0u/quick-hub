export function formatDate(timestamp) {
  return new Date(timestamp).toLocaleString('en-SG', {
    dateStyle: 'short',
    timeStyle: 'short',
    hour12: false,
  });
}

export function formatFileSize(bytes) {
  // Check if input is a valid number and greater than or equal to 0
  if (typeof bytes !== 'number' || isNaN(bytes) || bytes < 0) {
    return '';
  }

  if (bytes === 0) return '0 B';

  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(1024));

  return parseFloat((bytes / Math.pow(1024, i)).toFixed(2)) + ' ' + sizes[i];
}

export function escapeFilename(filename) {
  return filename.replace(/%/g, '%25').replace(/\+/g, '%2B').replace(/\?/g, '%3F').replace(/\^/g, '%5E')
    .replace(/ /g, '%20').replace(/#/g, '%23').replace(/\$/, '%24').replace(/&/g, '%26');
}
