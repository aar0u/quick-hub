import { formatFileSize, escapeFilename } from './utils.js';
import { checkFile, addFile, fetchList } from './api/fileApi.js';

const elements = {
  fileInput: document.getElementById('fileInput'),
  message: document.getElementById('message'),
  folder: document.getElementById('folder'),
  fileList: document
    .getElementById('fileList')
    .getElementsByTagName('tbody')[0],
};

let dirname = '';

async function updateList() {
  try {
    elements.message.textContent = '';
    elements.message.style.color = '';

    const result = await fetchList(dirname);

    if (result.status !== 'success') {
      elements.message.textContent = result.message;
      elements.message.style.color = 'red';
    }

    elements.folder.textContent = result.data.folder;
    elements.fileList.innerHTML = '';

    result.data.files.forEach((file) => {
      elements.fileList.appendChild(createFileRow(file));
    });
  } catch (error) {
    console.error('Error:', error);
    elements.message.textContent = error.message;
    elements.message.style.color = 'red';
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
  const fileName = escapeFilename(file.path);
  const href =
    file.type === 'directory' ? `#` : `/file/${fileName}`;

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

  (async () => {
    try { (await import('./fileActions.js')).pushButton(td, file); }
    catch (e) { /* Ignore error */ }
  })();

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
