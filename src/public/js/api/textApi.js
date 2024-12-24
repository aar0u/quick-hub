export async function fetchList() {
  const response = await fetch('/text/list');
  const json = await response.json();
  return json.data;
}

export async function addText(text) {
  const response = await fetch('/text/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ text }),
  });
  return response.json();
}
