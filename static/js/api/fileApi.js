export async function fetchList(dirname) {
  const response = await fetch('/files/list', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ dirname }),
  });
  return response.json();
}

export async function checkFile(dirname, filename) {
  const response = await fetch('/files/check', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ dirname, filename }),
  });
  return response.json();
}

export async function addFile(dirname, file, onProgress) {
  const formData = new FormData();
  const metadata = JSON.stringify({
    dirname,
    filename: file.name,
    fileSize: file.size,
  });

  formData.append('metadata', metadata);
  formData.append('files', file);

  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open('POST', '/files/add', true);
    
    // Add metadata to header
    xhr.setRequestHeader('X-File-Metadata', metadata);

    xhr.upload.onprogress = (e) => {
      if (e.lengthComputable && onProgress) {
        const percentComplete = (e.loaded / e.total) * 100;
        onProgress(percentComplete);
      }
    };

    xhr.onload = () => {
      try {
        const response = JSON.parse(xhr.responseText);
        resolve(response);
      } catch (error) {
        reject(new Error('Invalid response format'));
      }
    };

    xhr.onerror = () => {
      reject(new Error('Network error'));
    };

    xhr.send(formData);
  });
}
