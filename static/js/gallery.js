// Global variables
let images = [];
let currentIndex = 0;

// API configuration
const API_BASE_URL = window.location.protocol + '//' + window.location.host;
const ALLOWED_EXTENSIONS = ['.png', '.jpg', '.jpeg', '.gif', '.webp', '.bmp', '.svg'];

// DOM elements
const gallery = document.getElementById('gallery');
const modal = document.getElementById('modal');
const modalImg = document.getElementById('modalImg');
const closeBtn = document.querySelector('.close');
const prevBtn = document.getElementById('prevBtn');
const nextBtn = document.getElementById('nextBtn');

// Fetch images from external server
async function loadImages() {
    try {
        const response = await fetch(`${API_BASE_URL}/file/list`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ dirname: '' })
        });
        
        const result = await response.json();
        
        // Filter only image files
        if (result.data && result.data.files && Array.isArray(result.data.files)) {
            images = result.data.files
                .filter(file => {
                    // Filter out directories
                    if (file.type !== 'file') {
                        return false;
                    }
                    // Check file extension
                    const ext = '.' + file.name.split('.').pop().toLowerCase();
                    return ALLOWED_EXTENSIONS.includes(ext);
                })
                .map(file => file.name);
        } else {
            images = [];
        }
        
        displayGallery();
    } catch (error) {
        console.error(error);
        gallery.innerHTML = '<p class="no-images">Failed to load images</p>';
    }
}

// Display thumbnail gallery with lazy loading
function displayGallery() {
    if (images.length === 0) {
        gallery.innerHTML = '<p class="no-images">No images</p>';
        return;
    }

    gallery.innerHTML = '';
    
    // Use DocumentFragment for better performance
    const fragment = document.createDocumentFragment();
    
    images.forEach((image, index) => {
        const container = document.createElement('div');
        container.className = 'thumbnail-container';
        
        const img = document.createElement('img');
        // Set 4:3 aspect ratio (width: 267px, height: 200px)
        img.style.width = '100%';
        img.style.height = '100%';
        img.style.objectFit = 'cover';
        img.alt = image;
        img.className = 'thumbnail';
        
        // Use data-src for lazy loading
        img.dataset.src = `${API_BASE_URL}/file/${encodeURIComponent(image)}`;
        img.loading = 'lazy';
        
        // Add loading placeholder
        const loading = document.createElement('div');
        loading.className = 'loading-placeholder';
        loading.innerHTML = '<span>Loading</span>';
        
        img.addEventListener('click', () => openModal(index));
        
        // Hide loading placeholder when image loads
        img.addEventListener('load', () => {
            loading.style.display = 'none';
        });
        
        img.addEventListener('error', () => {
            loading.innerHTML = '<span>Failed</span>';
        });
        
        container.appendChild(img);
        container.appendChild(loading);
        fragment.appendChild(container);
    });
    
    gallery.appendChild(fragment);
    
    // Implement lazy loading with Intersection Observer
    if ('IntersectionObserver' in window) {
        const imageObserver = new IntersectionObserver((entries, observer) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    const img = entry.target;
                    if (img.dataset.src) {
                        img.src = img.dataset.src;
                        img.removeAttribute('data-src');
                        observer.unobserve(img);
                    }
                }
            });
        }, {
            rootMargin: '50px 0px',
            threshold: 0.1
        });
        
        // Observe all images
        document.querySelectorAll('.thumbnail[data-src]').forEach(img => {
            imageObserver.observe(img);
        });
    } else {
        // Fallback for browsers that don't support Intersection Observer
        document.querySelectorAll('.thumbnail[data-src]').forEach(img => {
            img.src = img.dataset.src;
            img.removeAttribute('data-src');
        });
    }
}

// Open modal with full-size image
function openModal(index) {
    currentIndex = index;
    modal.style.display = 'flex';
    modalImg.src = `${API_BASE_URL}/file/${encodeURIComponent(images[currentIndex])}`;
    document.body.style.overflow = 'hidden';
}

// Close modal
function closeModal() {
    modal.style.display = 'none';
    document.body.style.overflow = 'auto';
}

// Navigate to previous image
function showPrevious() {
    currentIndex = (currentIndex - 1 + images.length) % images.length;
    modalImg.src = `${API_BASE_URL}/file/${encodeURIComponent(images[currentIndex])}`;
}

// Navigate to next image
function showNext() {
    currentIndex = (currentIndex + 1) % images.length;
    modalImg.src = `${API_BASE_URL}/file/${encodeURIComponent(images[currentIndex])}`;
}

// Event listeners
closeBtn.addEventListener('click', closeModal);

prevBtn.addEventListener('click', (e) => {
    e.stopPropagation();
    showPrevious();
});

nextBtn.addEventListener('click', (e) => {
    e.stopPropagation();
    showNext();
});

// Close modal when clicking outside the image
modal.addEventListener('click', (e) => {
    if (e.target === modal) {
        closeModal();
    }
});

// Keyboard navigation
document.addEventListener('keydown', (e) => {
    if (modal.style.display === 'flex') {
        if (e.key === 'Escape') {
            closeModal();
        } else if (e.key === 'ArrowLeft') {
            showPrevious();
        } else if (e.key === 'ArrowRight') {
            showNext();
        }
    }
});

// Initialize gallery on page load
loadImages();
