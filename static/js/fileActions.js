import { escapeFilename, showToast } from './utils.js';

function pushButton(td, file) {
  const url = `${window.location.protocol}//${window.location.hostname}${window.location.port
    ? ':' + window.location.port : ''}/file/get/${escapeFilename(file.path)}`;

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

    // Visual feedback
    button.style.opacity = '0.5';
    button.disabled = true;
    setTimeout(() => {
      button.style.opacity = '1';
      button.disabled = false;
    }, 500);

    const key = 'pushUrl';
    const defaultValue = 'http://192.168.31.204:9978/action';
    let pushUrl = localStorage.getItem(key);
    if (!pushUrl) {
      localStorage.setItem(key, defaultValue);
      pushUrl = defaultValue;
    }

    const controller = new AbortController();
    const timeoutId = setTimeout(() => { controller.abort(); }, 2000);
    fetch(pushUrl, {
      method: 'POST',
      mode: 'no-cors',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      body: new URLSearchParams({ do: 'push', url }).toString(),
      signal: controller.signal,
    }).then(response => {
      clearTimeout(timeoutId);
      showToast(`Send to ${pushUrl}`);
    }).catch(error => {
      clearTimeout(timeoutId);
      playMedia(url);
    }).finally(() => {
      button.disabled = false; // Ensure the button is enabled
    });
  };

  if (isStreamingMedia(file.path)) {
    td.prepend(button);
  }
}

function isStreamingMedia(path) {
  const streamingMediaExtensions = [
    // Video
    'mp4', 'webm', 'ogg', 'mov', 'avi', 'mkv', 'flv', 'm3u8', 'ts',
    // Audio
    'mp3', 'wav', 'ogg', 'm4a', 'aac', 'flac', 'wma'
  ];

  return streamingMediaExtensions.some(extension => path.endsWith(`.${extension}`));
}

function playMedia(url) {
  let playerContainer = document.getElementById('mediaPlayerContainer');
  if (!playerContainer) {
    playerContainer = document.createElement('div');
    playerContainer.id = 'mediaPlayerContainer';
    playerContainer.style.position = 'fixed';
    playerContainer.style.top = '50%';
    playerContainer.style.left = '50%';
    playerContainer.style.transform = 'translate(-50%, -50%)';
    playerContainer.style.zIndex = '1000';
    playerContainer.style.backgroundColor = 'black';
    playerContainer.style.padding = '0';
    playerContainer.style.width = '80%';
    playerContainer.style.height = 'auto';
    playerContainer.style.maxHeight = '90vh';
    playerContainer.style.overflow = 'auto';

    const video = document.createElement('video');
    video.src = url;
    video.controls = true;
    video.style.width = '100%';
    video.style.height = 'auto';
    video.autoplay = true;

    const closeButton = document.createElement('button');
    closeButton.innerText = '✖ Close';
    closeButton.style.position = 'absolute';
    closeButton.style.top = '20px';
    closeButton.style.right = '20px';
    closeButton.style.color = 'white';
    closeButton.style.border = 'none';
    closeButton.style.borderRadius = '5px';
    closeButton.style.padding = '10px 15px';
    closeButton.style.cursor = 'pointer';
    closeButton.style.fontSize = '16px';

    closeButton.onclick = () => {
      document.body.removeChild(playerContainer);
      video.pause();
      video.src = ''; // Clean video source
    };

    playerContainer.appendChild(video);
    playerContainer.appendChild(closeButton);
    document.body.appendChild(playerContainer);
  } else {
    // If player exists, update video source and play again
    const video = playerContainer.querySelector('video');
    video.src = url;
    video.play();
  }
}

export { pushButton };
