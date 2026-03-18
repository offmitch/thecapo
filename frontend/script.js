const scene = new THREE.Scene();
const camera = new THREE.PerspectiveCamera(
  75,
  window.innerWidth / window.innerHeight,
  0.1,
  1000,
);
const renderer = new THREE.WebGLRenderer({
  canvas: document.getElementById("bg"),
});

async function getRecommendation() {
  const input = document.getElementById("input").value;
  const resultDiv = document.getElementById("result");

  if (!input) {
    resultDiv.innerText = "Please enter something.";
    return;
  }

  resultDiv.innerText = "Loading...";

  try {
    const response = await fetch(
      `http://localhost:8080/api/recommend?song=${input}`,
    );

    const data = await response.json();

    resultDiv.innerText = data.recommendation || "No result found.";
  } catch (error) {
    resultDiv.innerText = "Error connecting to server.";
  }
}

renderer.setSize(window.innerWidth, window.innerHeight);
camera.position.z = 5;

const geometry = new THREE.TorusGeometry(1, 0.3, 16, 100);
const material = new THREE.MeshBasicMaterial({ wireframe: true });
const torus = new THREE.Mesh(geometry, material);

scene.add(torus);

function animate() {
  requestAnimationFrame(animate);

  torus.rotation.x += 0.01;
  torus.rotation.y += 0.01;

  renderer.render(scene, camera);
}

animate();
