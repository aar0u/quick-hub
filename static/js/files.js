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
  const fileName = encodeURIComponent(file.path);
  const href =
    file.type === 'directory'
      ? `/files/${fileName}`
      : `/files/download/${fileName}`;

  tr.innerHTML = `
    <td><a href="${href}" class="${file.type === 'directory' ? 'directory' : ''}">${file.name}</a></td>
    <td>${formatFileSize(file.size)}</td>
    <td>${file.uploadTime}</td>
  `;

  if (file.type === 'directory') {
    tr.querySelector('a').addEventListener('click', (e) => {
      e.preventDefault();
      dirname = file.path;
      updateList();
    });
  }

  return tr;
}

document.getElementById('uploadButton').addEventListener('click', handleUpload);
document.getElementById('refreshButton').addEventListener('click', updateList);
document.addEventListener('DOMContentLoaded', updateList);
