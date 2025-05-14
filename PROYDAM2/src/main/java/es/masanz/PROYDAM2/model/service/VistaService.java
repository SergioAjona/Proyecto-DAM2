package es.masanz.PROYDAM2.model.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.cloud.FirestoreClient;
import es.masanz.PROYDAM2.model.entity.Bicicleta;
import es.masanz.PROYDAM2.model.entity.Usuario;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class VistaService {

    private FirebaseService fb = new FirebaseService();

    public String registerUser(Usuario usuario) throws Exception {
        UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                .setEmail(usuario.getEmail())
                .setPassword(usuario.getContrasena());

        UserRecord userRecord = FirebaseAuth.getInstance().createUser(request);
        String uid = userRecord.getUid();

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("nombre", usuario.getNombre());
        userMap.put("email", usuario.getEmail());

        fb.getFirestore().collection("usuarios").document(uid).set(userMap);

        return uid;
    }

    public String loginUser(String email, String password) throws IOException, JSONException {
        String apiKey = "AIzaSyCjzd8E1YA34ayRIrENxIEP9ifQAD83xns";
        URL url = new URL("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + apiKey);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String payload = String.format("{\"email\":\"%s\",\"password\":\"%s\",\"returnSecureToken\":true}", email, password);
        OutputStream os = conn.getOutputStream();
        os.write(payload.getBytes());
        os.flush();

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String response = in.lines().collect(Collectors.joining());
        in.close();

        // Extraer el idToken y localId (uid)
        JSONObject jsonResponse = new JSONObject(response);
        String idToken = jsonResponse.getString("idToken");
        String uid = jsonResponse.getString("localId");

        return uid;
    }

    // Método para obtener todos los productos de Firestore
    public List<Bicicleta> filtrarProductos(String tipo, Double precioMax, List<String> marca, boolean soloEnStock) {
        CollectionReference productosRef = fb.getFirestore().collection("bicicletas");
        Query query = productosRef;

        // Si no se aplica ningún filtro, se devuelven todos los productos
        if (tipo != null && !tipo.isEmpty()) {
            query = query.whereEqualTo("tipo", tipo);
        }
        if (precioMax != null && precioMax > 0) {
            query = query.whereLessThanOrEqualTo("precio", precioMax);
        }
        if (marca != null && !marca.isEmpty()) {
            query = query.whereIn("marca", marca);
        }
        if (soloEnStock) {
            query = query.whereGreaterThan("stock", 0);
        }

        // Recuperar las bicicletas filtradas
        List<Bicicleta> bicicletas = new ArrayList<>();
        try {
            // Ejecutar la consulta
            QuerySnapshot snapshot = query.get().get();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                Bicicleta bicicleta = doc.toObject(Bicicleta.class);
                bicicleta.setId(doc.getId());
                bicicletas.add(bicicleta);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return bicicletas;
    }

    public List<Bicicleta> obtenerProductos() {
        Firestore db = FirestoreClient.getFirestore();
        List<Bicicleta> bicicletas = new ArrayList<>();

        try {
            // Accedemos a la colección de productos (bicicletas)
            QuerySnapshot querySnapshot = db.collection("bicicletas").get().get();

            // Iteramos sobre los documentos de la colección
            for (QueryDocumentSnapshot document : querySnapshot) {
                Bicicleta bicicleta = document.toObject(Bicicleta.class);
                bicicleta.setId(document.getId());  // Seteamos el ID del producto
                bicicletas.add(bicicleta);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return bicicletas;
    }

    public Bicicleta findById(String id) {
        try {
            Firestore db = FirestoreClient.getFirestore();
            DocumentSnapshot document = db.collection("bicicletas").document(id).get().get();

            if (document.exists()) {
                Bicicleta bicicleta = document.toObject(Bicicleta.class);
                if (bicicleta != null) {
                    bicicleta.setId(document.getId());
                }
                return bicicleta;
            } else {
                throw new RuntimeException("Producto no encontrado con ID: " + id);
            }

        } catch (Exception e) {
            throw new RuntimeException("Error al obtener el producto: " + e.getMessage(), e);
        }
    }

    public void sumarStock(String id) {
        Bicicleta bicicleta = findById(id);
        int nuevoStock = bicicleta.getStock() + 1;
        DocumentReference docRef = fb.getFirestore().collection("bicicletas").document(id);
        docRef.update("stock", nuevoStock);
    }

    public void restarStock(String id) {

        Bicicleta bicicleta = findById(id);
        if (bicicleta.getStock() > 0) {
            int nuevoStock = bicicleta.getStock() - 1;
            DocumentReference docRef = fb.getFirestore().collection("bicicletas").document(id);
            docRef.update("stock", nuevoStock);
        } else {
            throw new RuntimeException("Stock no disponible para restar");
        }
    }

    public void agregarProductoAlCarrito(String uid, String productoId) {
        DocumentReference usuarioRef = fb.getFirestore().collection("usuarios").document(uid);

        try {
            ApiFuture<DocumentSnapshot> future = usuarioRef.get();
            DocumentSnapshot document = future.get();

            if (document.exists()) {
                // Obtener carrito existente o inicializar uno nuevo
                List<String> carrito = (List<String>) document.get("carrito");

                if (carrito == null) {
                    carrito = new ArrayList<>();
                }

                carrito.add(productoId); // Agregar producto al carrito

                // Actualizar el documento
                ApiFuture<WriteResult> writeResult = usuarioRef.update("carrito", carrito);
                System.out.println("Carrito actualizado: " + writeResult.get().getUpdateTime());

            } else {
                // Si el documento no existe, lo creamos con el producto en el carrito
                Map<String, Object> nuevoUsuario = Map.of("carrito", List.of(productoId));
                ApiFuture<WriteResult> writeResult = usuarioRef.set(nuevoUsuario);
                System.out.println("Documento creado: " + writeResult.get().getUpdateTime());
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error al agregar el producto al carrito: " + e.getMessage());
        }
    }
}