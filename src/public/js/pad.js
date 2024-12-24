const messageDiv = document.getElementById('message');

function fetchList() {
  fetch('/text/list')
    .then((response) => response.json())
    .then((json) => {
      const historyList = document.getElementById('history-list');
      historyList.innerHTML = ''; // Clear history list
      json.data.forEach((item) => {
        const listItem = document.createElement('li');
        listItem.textContent = `${new Date(item.timestamp).toLocaleString(
          'en-SG',
          { dateStyle: 'short', timeStyle: 'short', hour12: false }
        )}: ${item.text}`;
        listItem.addEventListener('click', () => {
          const textArea = document.getElementById('text-area');
          textArea.value = item.text;
          textArea.select();
          document.execCommand('copy');
          messageDiv.textContent = 'Copied to clipboard';
        });
        historyList.prepend(listItem);
      });
    })
    .catch((error) => {
      console.error('Error loading history:', error);
    });
}

function saveText() {
  messageDiv.textContent = '';
  messageDiv.style.color = '';
  const text = document.getElementById('text-area').value;
  fetch('/text/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ text: text }),
  })
    .then((response) => response.json())
    .then((data) => {
      if (data.status === 'success') {
        messageDiv.textContent = data.message;
        fetchList(); // Reload history list to show the latest item
      } else {
        messageDiv.textContent = data.message;
        messageDiv.style.color = 'red';
      }
    })
    .catch((error) => {
      console.error(
        'There has been a problem with your fetch operation:',
        error
      );
    });
}

document.getElementById('saveButton').addEventListener('click', saveText);
document.getElementById('refreshButton').addEventListener('click', fetchList);
document.addEventListener('keydown', (event) => {
  if (event.ctrlKey && event.key == 'Enter') {
    saveText();
  }
});
document.addEventListener('DOMContentLoaded', fetchList);
