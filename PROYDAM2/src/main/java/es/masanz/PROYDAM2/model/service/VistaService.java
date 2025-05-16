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

    public String registrarUsuario(Usuario usuario) throws Exception {
        UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                .setEmail(usuario.getEmail())
                .setPassword(usuario.getContrasena());

        UserRecord userRecord = FirebaseAuth.getInstance().createUser(request);
        String uid = userRecord.getUid();

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", uid);
        userMap.put("nombre", usuario.getNombre());
        userMap.put("apellido", usuario.getApellido());
        userMap.put("email", usuario.getEmail());
        userMap.put("contrasena", usuario.getContrasena());
        userMap.put("ciudad", usuario.getCiudad());
        userMap.put("dni", usuario.getDni());
        userMap.put("telefono", usuario.getTelefono());

        fb.getFirestore().collection("usuarios").document(uid).set(userMap);

        return uid;
    }

    public String logearUsuario(String email, String password) throws IOException, JSONException {
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

        JSONObject jsonResponse = new JSONObject(response);
        String idToken = jsonResponse.getString("idToken");
        String uid = jsonResponse.getString("localId");

        return uid;
    }

    public List<Bicicleta> filtrarProductos(String tipo, Double precioMax, List<String> marca, boolean stock) {
        CollectionReference productosRef = fb.getFirestore().collection("bicicletas");
        Query query = productosRef;

        if (tipo != null && !tipo.isEmpty()) {
            query = query.whereEqualTo("tipo", tipo);
        }
        if (precioMax != null && precioMax > 0) {
            query = query.whereLessThanOrEqualTo("precio", precioMax);
        }
        if (marca != null && !marca.isEmpty()) {
            query = query.whereIn("marca", marca);
        }
        if (stock) {
            query = query.whereGreaterThan("stock", 0);
        }

        List<Bicicleta> bicicletas = new ArrayList<>();
        try {
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
        DocumentReference biciRef = fb.getFirestore().collection("bicicletas").document(productoId);

        try {
            ApiFuture<DocumentSnapshot> future = usuarioRef.get();
            DocumentSnapshot document = future.get();

            ApiFuture<DocumentSnapshot> futureBici = biciRef.get();
            DocumentSnapshot docBici = futureBici.get();

            Long stock = docBici.getLong("stock");
            if (stock == null || stock <= 0) {
                throw new RuntimeException("Error al agregar el producto al carrito: ");
            } else {
                if (document.exists()) {
                    List<String> carrito = (List<String>) document.get("carrito");

                    if (carrito == null) {
                        carrito = new ArrayList<>();
                    }

                    carrito.add(productoId);

                    ApiFuture<WriteResult> writeResult = usuarioRef.update("carrito", carrito);
                    System.out.println("Carrito actualizado: " + writeResult.get().getUpdateTime());

                } else {
                    Map<String, Object> nuevoUsuario = Map.of("carrito", List.of(productoId));
                    ApiFuture<WriteResult> writeResult = usuarioRef.set(nuevoUsuario);
                    System.out.println("Documento creado: " + writeResult.get().getUpdateTime());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error al agregar el producto al carrito: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> obtenerCarritoConDetalles(String uid) throws Exception {
        DocumentSnapshot usuarioDoc = fb.getFirestore().collection("usuarios").document(uid).get().get();

        List<Map<String, Object>> carritoConDetalles = new ArrayList<>();

        if (!usuarioDoc.exists()) {
            return carritoConDetalles;
        }

        List<String> carritoIds = (List<String>) usuarioDoc.get("carrito");
        if (carritoIds != null) {
            for (String productoId : carritoIds) {
                if (productoId != null) {
                    DocumentSnapshot productoDoc = fb.getFirestore().collection("bicicletas").document(productoId).get().get();
                    if (productoDoc.exists()) {
                        Map<String, Object> producto = productoDoc.getData();
                        producto.put("id", productoId);
                        carritoConDetalles.add(producto);
                    }
                }
            }
        }

        return carritoConDetalles;
    }

    public void eliminarProductoDelCarrito(String uid, String productoId) throws Exception {
        DocumentReference usuarioRef = fb.getFirestore().collection("usuarios").document(uid);

        DocumentSnapshot snapshot = usuarioRef.get().get();
        List<String> carrito = (List<String>) snapshot.get("carrito");

        if (carrito != null && !carrito.isEmpty()) {
            for (String producto : carrito) {
                if (producto.equals(productoId)) {
                    carrito.remove(producto);
                    break;
                }
            }

            usuarioRef.update("carrito", carrito);
        }
    }

    public void vaciarCarrito(String uid) throws Exception {
        DocumentReference usuarioRef = fb.getFirestore().collection("usuarios").document(uid);
    
        Map<String, Object> actualizacion = new HashMap<>();
        actualizacion.put("carrito", new ArrayList<>());
    
        usuarioRef.update(actualizacion);
    }
    
    public double calcularTotalCarrito(List<Map<String, Object>> carritoConDetalles) {
        double total = 0.0;
    
        for (Map<String, Object> producto : carritoConDetalles) {
            if (producto != null && producto.get("precio") != null) {
                try {
                    Object precioObj = producto.get("precio");
                    double precio = 0.0;
    
                    if (precioObj instanceof Number) {
                        precio = ((Number) precioObj).doubleValue();
                    } else if (precioObj instanceof String) {
                        precio = Double.parseDouble((String) precioObj);
                    }
    
                    total += precio;
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException("Error al agregar el precio del producto: " + e.getMessage());
                }
            }
        }
    
        return total;
    }
}