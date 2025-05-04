package es.masanz.PROYDAM2.model.service;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import es.masanz.PROYDAM2.model.entity.CarritoItem;
import es.masanz.PROYDAM2.model.entity.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class UsuarioService {

    @Autowired
    private FirebaseService fb;

    public String registrarUsuario(String token, Usuario datosUsuario) throws Exception {
        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
        String id = decodedToken.getUid();

        // Verificar que el ID no sea nulo o vacío
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("El ID del usuario es inválido.");
        }

        Firestore db = fb.getFirestore();
        DocumentReference userRef = db.collection("usuarios").document(id);

        if (userRef.get().get().exists()) {
            throw new IllegalStateException("Usuario ya registrado");
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("id", id);
        userData.put("email", decodedToken.getEmail());
        userData.put("nombre", datosUsuario.getNombre());
        userData.put("apellido", datosUsuario.getApellido());
        userData.put("contrasena", datosUsuario.getContrasena());
        userData.put("telefono", datosUsuario.getTelefono());
        userData.put("dni", datosUsuario.getDni());
        userData.put("ciudad", datosUsuario.getCiudad());

        userRef.set(userData);

        return "Usuario registrado correctamente en Firestore";
    }

    public String loginUsuario(String token) throws Exception {
        try {
            // Verificar el token
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
            String uid = decodedToken.getUid();

            // Buscar al usuario en Firestore (síncrono)
            DocumentReference userRef = fb.getFirestore().collection("usuarios").document(uid);
            DocumentSnapshot documentSnapshot = userRef.get().get(); // Utiliza .get() para bloquear hasta que obtenga el documento

            if (documentSnapshot.exists()) {
                // Usuario encontrado, obtén los detalles
                String contrasena = documentSnapshot.getString("contrasena");
                String email = documentSnapshot.getString("email");

                // Puedes devolver una respuesta más detallada si es necesario
                return "Login exitoso. Usuario: (" + email + ")";
            } else {
                // El usuario no existe en Firestore
                return "El usuario no se encuentra en Firestore.";
            }
        } catch (Exception e) {
            // Manejo de excepciones en caso de error durante el proceso
            throw new Exception("Error en el login: " + e.getMessage());
        }
    }

    public void agregarAlCarrito(String token, CarritoItem item) throws Exception {
        String uid = FirebaseAuth.getInstance().verifyIdToken(token).getUid();
        DocumentReference carritoRef = fb.getFirestore().collection("usuarios").document(uid)
                .collection("carrito").document(item.getId());
        Map<String, Object> datos = new HashMap<>();
        datos.put("marca", item.getMarca());
        datos.put("modelo", item.getModelo());
        datos.put("precio", item.getPrecio());

        carritoRef.set(datos);
    }

    public void limpiarCarrito(String token) throws Exception {
        String uid = FirebaseAuth.getInstance().verifyIdToken(token).getUid();
        CollectionReference carritoRef = fb.getFirestore().collection("usuarios").document(uid).collection("carrito");

        // Borrar todos los documentos del carrito
        for (DocumentSnapshot doc : carritoRef.get().get().getDocuments()) {
            doc.getReference().delete();
        }
    }
}
