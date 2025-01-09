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
  formData.append(
    'metadata',
    JSON.stringify({
      dirname,
      filename: file.name,
      fileSize: file.size,
    })
  );
  formData.append('file', file);

  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open('POST', '/files/add', true);

    xhr.upload.onprogress = (e) => {
      if (e.lengthComputable && onProgress) {
        const percentComplete = (e.loaded / e.total) * 100;
        onProgress(percentComplete);
      }
    };

    xhr.onload = () => resolve(JSON.parse(xhr.responseText));
    xhr.onerror = () => reject(new Error('Upload failed'));
    xhr.send(formData);
  });
}
