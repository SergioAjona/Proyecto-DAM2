// firebase-config.js
import { getAuth } from "https://www.gstatic.com/firebasejs/10.11.0/firebase-auth.js";
import { initializeApp } from "https://www.gstatic.com/firebasejs/10.11.0/firebase-app.js";
// TODO: Add SDKs for Firebase products that you want to use
// https://firebase.google.com/docs/web/setup#available-libraries

// Your web app's Firebase configuration
const firebaseConfig = {
  apiKey: "AIzaSyCjzd8E1YA34ayRIrENxIEP9ifQAD83xns",
  authDomain: "proyectodam2-37d0e.firebaseapp.com",
  databaseURL: "https://proyectodam2-37d0e-default-rtdb.europe-west1.firebasedatabase.app",
  projectId: "proyectodam2-37d0e",
  storageBucket: "proyectodam2-37d0e.firebasestorage.app",
  messagingSenderId: "56781774013",
  appId: "1:56781774013:web:80aac9646632513ca8abd4"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
