const fileInput = document.getElementById('fileInput');
const uploadButton = document.getElementById('uploadButton');
const refreshButton = document.getElementById('refreshButton');
const uploadStatus = document.getElementById('message');
const folder = document.getElementById('folder');
const fetchStatus = document.getElementById('fetchStatus');
const fileListTable = document
  .getElementById('fileList')
  .getElementsByTagName('tbody')[0];

let dirname = '';

async function handleFileUpload() {
  uploadStatus.textContent = '';
  uploadStatus.style.color = '';

  const file = fileInput.files[0];
  if (!file) return;

  try {
    const checkResponse = await fetch('/files/check', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        dirname,
        filename: file.name,
      }),
    });

    const checkResult = await checkResponse.json();
    if (checkResult.status !== 'success') {
      uploadStatus.textContent = `${checkResult.message} (${checkResponse.status})`;
      return;
    }

    const formData = new FormData();
    formData.append(
      'metadata',
      JSON.stringify({
        dirname,
        filename: file.name,
        fileSize: file.size,
      })
    );
    formData.append('file', file);

    const xhr = new XMLHttpRequest();
    xhr.open('POST', '/files/add', true);

    xhr.upload.onprogress = function (e) {
      if (e.lengthComputable) {
        const percentComplete = (e.loaded / e.total) * 100;
        uploadStatus.textContent = percentComplete.toFixed(2) + '%';
      }
    };

    xhr.onload = function () {
      const jsonResponse = JSON.parse(xhr.responseText);
      uploadStatus.textContent = `${jsonResponse.message} (${xhr.status})`;
      fetchFileList();
    };

    xhr.send(formData);
  } catch (error) {
    uploadStatus.textContent = error.message;
    uploadStatus.style.color = 'red';
  }
}

function formatFileSize(bytes) {
  if (!bytes) return '';
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
  if (bytes < 1024 * 1024 * 1024)
    return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
  return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB';
}

async function fetchFileList() {
  try {
    fetchStatus.textContent = '';
    fetchStatus.style.color = '';

    const response = await fetch('/files/list', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ dirname }),
    });
    const json = await response.json();
    fileListTable.innerHTML = '';

    folder.textContent = json.data.folder;
    if (json.status && json.status !== 'success') {
      fetchStatus.textContent = json.message;
      fetchStatus.style.color = 'red';
    } else {
      fetchStatus.textContent = '';
      fetchStatus.style.color = '';
    }

    json.data.files.forEach((file) => {
      const tr = document.createElement('tr');
      const fileNameCell = document.createElement('td');
      const fileSizeCell = document.createElement('td');
      const fileTimeCell = document.createElement('td');

      const downloadLink = document.createElement('a');

      let fileName = encodeURIComponent(file.path);
      if (file.type === 'directory') {
        downloadLink.href = `/files/${fileName}`;
        downloadLink.onclick = function (event) {
          event.preventDefault();
          dirname = file.path;
          fetchFileList();
        };
      } else {
        downloadLink.href = `/files/download/${fileName}`;
      }
      downloadLink.textContent = file.name;
      downloadLink.className = 'download-link';
      fileNameCell.appendChild(downloadLink);
      fileSizeCell.textContent = formatFileSize(file.size);
      fileTimeCell.textContent = file.uploadTime;

      tr.appendChild(fileNameCell);
      tr.appendChild(fileSizeCell);
      tr.appendChild(fileTimeCell);

      fileListTable.appendChild(tr);
    });
  } catch (error) {
    console.error('Error:', error);
    fetchStatus.textContent = error.message;
    fetchStatus.style.color = 'red';
  }
}

uploadButton.addEventListener('click', handleFileUpload);
refreshButton.addEventListener('click', fetchFileList);

fetchFileList();
