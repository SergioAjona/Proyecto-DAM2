import { auth } from './firebase-config.js';
import { createUserWithEmailAndPassword } from "https://www.gstatic.com/firebasejs/10.11.0/firebase-auth.js";
import { signInWithEmailAndPassword } from "https://www.gstatic.com/firebasejs/10.11.0/firebase-auth.js";

// GESTIONAR BOTONES DE SESIÓN
// Referencias a los botones
const abrirLoginBtn = document.getElementById("abrirLogin");
const abrirRegistroBtn = document.getElementById("abrirRegistro");
const cerrarSesionBtn = document.getElementById("cerrarSesion");

// Función para actualizar los botones según el estado de autenticación
function actualizarEstadoAutenticacion() {
    const usuario = auth.currentUser; // Verificar si hay un usuario autenticado

    if (usuario) {
        // Si el usuario está autenticado, mostrar el botón de "Cerrar Sesión" y ocultar los otros
        cerrarSesionBtn.style.display = "inline-block";  // Mostrar "Cerrar Sesión"
        abrirLoginBtn.style.display = "none";           // Ocultar "Iniciar Sesión"
        abrirRegistroBtn.style.display = "none";        // Ocultar "Registrarse"
    } else {
        // Si el usuario no está autenticado, mostrar "Iniciar Sesión" y "Registrarse"
        cerrarSesionBtn.style.display = "none";         // Ocultar "Cerrar Sesión"
        abrirLoginBtn.style.display = "inline-block";  // Mostrar "Iniciar Sesión"
        abrirRegistroBtn.style.display = "inline-block";// Mostrar "Registrarse"
    }
}

// Detectar el cambio en el estado de autenticación
auth.onAuthStateChanged(function(user) {
    actualizarEstadoAutenticacion();
});

// Lógica para cerrar sesión
cerrarSesionBtn.addEventListener("click", async () => {
    try {
        await auth.signOut(); // Cerrar sesión en Firebase
        alert("Has cerrado sesión correctamente.");
    } catch (error) {
        console.error("Error al cerrar sesión: ", error);
    }
});

// REGISTRO
document.getElementById("formRegistro").addEventListener("submit", async (e) => {
  e.preventDefault();  // Evitar redirección predeterminada

  // Obtener valores del formulario
  const nombre = document.getElementById("nombre").value;
  const apellido = document.getElementById("apellido").value;
  const email = document.getElementById("email").value;
  const contrasena = document.getElementById("contrasena").value;
  const dni = document.getElementById("dni").value;
  const telefono = document.getElementById("telefono").value;
  const ciudad = document.getElementById("ciudad").value;

  try {
    // Crear el usuario con email y contraseña en Firebase Authentication
    const userCredential = await createUserWithEmailAndPassword(auth, email, contrasena);
    const user = userCredential.user;

    // Obtener el ID token
    const idToken = await user.getIdToken();

    // Enviar los datos al backend para registrar al usuario en Firestore
    const response = await fetch("http://localhost/registro", {  // Asegúrate de que la URL esté correcta
      method: "POST",
      headers: {
        "Authorization": `Bearer ${idToken}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        nombre: nombre,
        apellido: apellido,
        contrasena: contrasena,
        dni: dni,
        telefono: telefono,
        ciudad: ciudad
      })
    });

    // Verificar si la respuesta fue exitosa
    if (response.ok) {
      // Confirmación de que el usuario se registró correctamente
      alert("Usuario registrado correctamente");

      // Llamar a la función que actualiza el estado de los botones
      actualizarEstadoAutenticacion();

      // Cerrar el modal de registro
      document.getElementById("modalRegistro").style.display = "none";
    } else {
      // Si la respuesta no es ok, mostrar el mensaje de error del backend
      const errorMessage = await response.text();
      alert("Error en el registro: " + errorMessage);
    }

  } catch (error) {
    console.error(error);
    alert("Error en el registro: " + error.message);
  }
});


// INICIO DE SESIÓN
document.getElementById("formLogin").addEventListener("submit", async (e) => {
    e.preventDefault();  // Evitar redirección predeterminada

    // Obtener valores del formulario
    const email = document.getElementById("emailLogin").value;
    const contrasena = document.getElementById("contrasenaLogin").value;

    try {
        const userCredential = await signInWithEmailAndPassword(auth, email, contrasena);
        const user = userCredential.user;

        // Obtener el ID token
        const idToken = await user.getIdToken();

        // Enviar el token al backend para realizar el login
        const response = await fetch('http://localhost/login', {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${idToken}`,
            'Content-Type': 'application/json',
          }
        });

        const data = await response.text();
        alert(data); // Mostrar mensaje de éxito

        // Llamar a la función para actualizar los botones
        actualizarEstadoAutenticacion();

        // Cerrar el modal de registro
        document.getElementById("modalLogin").style.display = "none";
    } catch (error) {
      console.error('Error en el inicio de sesión:', error);
      alert('Error en el inicio de sesión: ' + error.message);
    }
});