import { formatDate } from './utils.js';
import { fetchList, addText } from './api/textApi.js';

const messageDiv = document.getElementById('message');
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
    console.error('Error loading history:', error);
  }
}

async function handleSave() {
  messageDiv.textContent = '';
  messageDiv.style.color = '';
  try {
    const result = await addText(textArea.value);
    messageDiv.textContent = result.message;
    if (result.status === 'success') {
      await updateList();
    } else {
      messageDiv.style.color = 'red';
    }
  } catch (error) {
    console.error('Error saving text:', error);
    messageDiv.textContent = error.message;
    messageDiv.style.color = 'red';
  }
}

function handleCopy(text) {
  textArea.value = text;
  textArea.select();
  document.execCommand('copy');
  messageDiv.textContent = 'Copied to clipboard';
}

document.getElementById('saveButton').addEventListener('click', handleSave);
document.getElementById('refreshButton').addEventListener('click', updateList);
document.addEventListener('keydown', (event) => {
  if (event.ctrlKey && event.key === 'Enter') handleSave();
});
document.addEventListener('DOMContentLoaded', updateList);
