import { formatFileSize } from './utils/formatters.js';
import { checkFile, addFile, fetchList } from './api/fileApi.js';

const elements = {
  fileInput: document.getElementById('fileInput'),
  message: document.getElementById('message'),
  folder: document.getElementById('folder'),
  listStatus: document.getElementById('fetchStatus'),
  fileList: document
    .getElementById('fileList')
    .getElementsByTagName('tbody')[0],
};

let dirname = '';

async function updateList() {
  try {
    elements.listStatus.textContent = '';
    elements.listStatus.style.color = '';

    const result = await fetchList(dirname);

    if (result.status !== 'success') {
      elements.listStatus.textContent = result.message;
      elements.listStatus.style.color = 'red';
    }

    elements.folder.textContent = result.data.folder;
    elements.fileList.innerHTML = '';

    result.data.files.forEach((file) => {
      elements.fileList.appendChild(createFileRow(file));
    });
  } catch (error) {
    console.error('Error:', error);
    elements.listStatus.textContent = error.message;
    elements.listStatus.style.color = 'red';
  }
}

async function handleUpload() {
  elements.message.textContent = '';
  elements.message.style.color = '';

  const file = elements.fileInput.files[0];
  if (!file) return;

  try {
    const checkResult = await checkFile(dirname, file.name);
    if (checkResult.status !== 'success') {
      elements.message.textContent = checkResult.message;
      elements.message.style.color = 'red';
      return;
    }

    await addFile(dirname, file, (progress) => {
      elements.message.textContent = progress.toFixed(2) + '%';
    }).then((res) => (elements.message.textContent = res.message));
    await updateList();
  } catch (error) {
    elements.message.textContent = error.message;
    elements.message.style.color = 'red';
  }
}

function createFileRow(file) {
  const tr = document.createElement('tr');
  const fileName = file.path.replace(/#/g, '%23');
  const href =
    file.type === 'directory'
      ? `/file/${fileName}`
      : `/file/get/${fileName}`;

  const td = document.createElement('td');
  const a = document.createElement('a');
  a.href = href;
  a.className = file.name === '..' ? 'parent-dir' : '';
  a.textContent = file.name;
  a.addEventListener('click', (event) => {
    event.preventDefault();
  });

  td.appendChild(a);
  tr.appendChild(td);
  tr.appendChild(document.createElement('td')).textContent = formatFileSize(file.size) || '-';
  tr.appendChild(document.createElement('td')).textContent = file.uploadTime || '-';

  // Testing
  pushButton(td, file)

  if (file.type === 'directory') {
    tr.addEventListener('click', (e) => {
      e.preventDefault();
      dirname = file.path;
      updateList();
    });
  } else {
    tr.addEventListener('click', () => {
      window.location.href = href;
    });
  }

  return tr;
}

document.getElementById('uploadButton').addEventListener('click', handleUpload);
document.getElementById('refreshButton').addEventListener('click', updateList);
document.addEventListener('DOMContentLoaded', updateList);

// Testing
function pushButton(td, file) {
  const url = `${window.location.protocol}//${window.location.hostname}${window.location.port
    ? ':' + window.location.port : ''}/file/get/${file.path.replace(/#/g, '%23')}`;

  const button = document.createElement('span');
  button.innerHTML = `
<svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
  <path d="M14 13.9633H16V7.96331H10V9.96331H12.5858L7.25623 15.2929L8.67044 16.7071L14 11.3775V13.9633Z" fill="currentColor" />
  <path
    d="M23 19C23 21.2091 21.2091 23 19 23H5C2.79086 23 1 21.2091 1 19V5C1 2.79086 2.79086 1 5 1H19C21.2091 1 23 2.79086 23 5V19ZM19 21H5C3.89543 21 3 20.1046 3 19V5C3 3.89543 3.89543 3 5 3H19C20.1046 3 21 3.89543 21 5V19C21 20.1046 20.1046 21 19 21Z"
    fill-rule="evenodd" clip-rule="evenodd" fill="currentColor" />
</svg>
`;
  button.onclick = (event) => {
    event.stopPropagation(); // Prevent the row click event

    const params = new URLSearchParams();
    params.append('do', 'push');
    params.append('url', url);

    const key = 'pushUrl';
    const defaultValue = 'http://192.168.31.204:9978/action';
    let pushUrl = localStorage.getItem(key);
    if (!pushUrl) {
      localStorage.setItem(key, defaultValue);
      pushUrl = defaultValue;
    }

    fetch(pushUrl, {
      method: 'POST',
      mode: 'no-cors',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      body: params.toString(),
    }).then(response => {
      console.log(response); // Response is opaque
    }).catch(error => {
      console.error('Fetch failed', error);
    });
  };

  if (file.type !== 'directory') {
    td.prepend(button);
  }
}
