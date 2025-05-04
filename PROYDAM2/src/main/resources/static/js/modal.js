const abrirLogin = document.getElementById("abrirLogin");
const abrirRegistro = document.getElementById("abrirRegistro");
const modalLogin = document.getElementById("modalLogin");
const modalRegistro = document.getElementById("modalRegistro");
const cerrarLogin = document.getElementById("cerrarLogin");
const cerrarRegistro = document.getElementById("cerrarRegistro");

// Abrir modales
abrirLogin.onclick = () => modalLogin.style.display = "block";
abrirRegistro.onclick = () => modalRegistro.style.display = "block";

// Cerrar modales
cerrarLogin.onclick = () => modalLogin.style.display = "none";
cerrarRegistro.onclick = () => modalRegistro.style.display = "none";

// Cerrar al hacer clic fuera del contenido
window.onclick = (e) => {
  if (e.target === modalLogin) modalLogin.style.display = "none";
  if (e.target === modalRegistro) modalRegistro.style.display = "none";
};