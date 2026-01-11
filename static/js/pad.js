import { formatDate } from './utils.js';
import { fetchList, addText } from './api/textApi.js';
import { showInfo, showError, showSuccess } from './toast.js';

const textArea = document.getElementById('text-area');
const historyList = document.getElementById('history-list');

async function updateList() {
  try {
    const data = await fetchList();
    historyList.innerHTML = '';
    data.forEach((item) => {
      const listItem = document.createElement('li');
      listItem.textContent = `${formatDate(item.timestamp)}: ${item.text}`;
      listItem.addEventListener('click', () => handleCopy(item.text));
      historyList.prepend(listItem);
    });
  } catch (error) {
    console.error(error);
  }
}

async function handleSave() {
  try {
    const result = await addText(textArea.value);
    if (result.status === 'success') {
      showSuccess(result.message);
      await updateList();
    } else {
      showError(result.message);
    }
  } catch (error) {
    console.error(error);
    showError(error.message);
  }
}

function handleCopy(text) {
  textArea.value = text;
  textArea.select();
  document.execCommand('copy');
  showInfo('Copied');
}

document.getElementById('saveButton').addEventListener('click', handleSave);
document.getElementById('refreshButton').addEventListener('click', updateList);
document.addEventListener('keydown', (event) => {
  if (event.ctrlKey && event.key === 'Enter') handleSave();
});
document.addEventListener('DOMContentLoaded', updateList);
