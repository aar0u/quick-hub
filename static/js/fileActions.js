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

export { pushButton };
