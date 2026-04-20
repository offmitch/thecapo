// Scene setup FIRST (important)
const scene = new THREE.Scene();

const camera = new THREE.PerspectiveCamera(
  75,
  window.innerWidth / window.innerHeight,
  0.1,
  1000,
);

const renderer = new THREE.WebGLRenderer({
  canvas: document.getElementById("bg"),
  alpha: true,
});

renderer.setSize(window.innerWidth, window.innerHeight);
renderer.setClearColor(0x000000, 0);
camera.position.z = 5;

// ✅ Add light (VERY important for models)
const light = new THREE.AmbientLight(0xffffff, 1);
scene.add(light);

// Optional directional light (adds depth)
const dirLight = new THREE.DirectionalLight(0xffffff, 1);
dirLight.position.set(5, 5, 5);
scene.add(dirLight);

// ✅ Load 3D model
let model;

const loader = new THREE.GLTFLoader();
loader.load(
  "headphones.glb",
  function (gltf) {
    model = gltf.scene;

    // 🔥 Bigger
    model.scale.set(15, 15, 15);

    // 📍 Position (adjust if needed)
    model.position.set(0, 0, 0);

    // 🎯 Tilt forward (rotate on X axis)
    model.rotation.x = Math.PI / 3; // 45 degrees forward

    scene.add(model);
  },
  undefined,
  function (error) {
    console.error("Error loading model:", error);
  },
);

let time = 0;

function animate() {
  requestAnimationFrame(animate);

  time += 0.05;

  if (model) {
    model.rotation.z += 0.0025;
  }

  renderer.render(scene, camera);
}

animate();

window.addEventListener("resize", () => {
  camera.aspect = window.innerWidth / window.innerHeight;
  camera.updateProjectionMatrix();
  renderer.setSize(window.innerWidth, window.innerHeight);
});

//user input handling
let selectedType = "song";

const dropdown = document.getElementById("dropdown");
const selected = document.getElementById("selected");
const options = document.getElementById("options");
const input = document.getElementById("input");

// Toggle dropdown
dropdown.addEventListener("click", () => {
  dropdown.classList.toggle("open");
});

options.querySelectorAll("div").forEach((option) => {
  option.addEventListener("click", (e) => {
    e.stopPropagation();

    selectedType = option.getAttribute("data-value");
    selected.innerHTML = option.innerHTML + ' <span class="arrow">▾</span>';

    // ✅ update placeholder here
    if (selectedType === "song") {
      input.placeholder = "Which song should we match?";
    } else {
      input.placeholder = "What type of mood you in?";
    }

    dropdown.classList.remove("open");
  });
});

// Close if clicking outside
document.addEventListener("click", (e) => {
  if (!dropdown.contains(e.target)) {
    dropdown.classList.remove("open");
  }
});

async function getRecommendation() {
  const input = document.getElementById("input").value;
  const type = selectedType;
  const resultDiv = document.getElementById("result");

  if (!input) {
    resultDiv.innerText = "Please enter something.";
    return;
  }

  resultDiv.classList.add("show");
  
  resultDiv.innerHTML = "<p>Finding your vibe...</p>";

  resultDiv.offsetHeight;

  try {
    const response = await fetch(
      `https://thecapo-backend.onrender.com/api/recommend?${type}=${input}`
      // `http://localhost:8080/api/recommend?${type}=${encodeURIComponent(input)}`
    );

    if (!response.ok) {
      throw new Error("Server error");
    }

    const data = await response.json();
    console.log("Response from server:", data);

    

    if (data.error) {
      resultDiv.innerText = data.error;
      return;
    }

    resultDiv.innerHTML = `
      <div class="song-result">
        <img src="${data.imageUrl}" alt="Album cover" />
        <div class="song-text">
          ${data.recommendation}
        </div>
      </div>
    `;
  } catch (error) {
    console.error(error);
    resultDiv.innerText = "Error connecting to server.";
  }
}