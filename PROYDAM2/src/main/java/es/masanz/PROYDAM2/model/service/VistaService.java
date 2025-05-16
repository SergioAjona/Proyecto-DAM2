package es.masanz.PROYDAM2.model.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.cloud.FirestoreClient;
import com.lowagie.text.DocumentException;
import es.masanz.PROYDAM2.model.entity.Bicicleta;
import es.masanz.PROYDAM2.model.entity.Usuario;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;

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

    @Autowired
    private SpringTemplateEngine templateEngine;

    private FirebaseService fb = new FirebaseService();
    @Autowired
    private MensajeService mensajeService = new MensajeService();

    public String registrarUsuario(Usuario usuario) throws Exception {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                .setEmail(usuario.getEmail())
                .setPassword(usuario.getContrasena());

        UserRecord userRecord = auth.createUser(request);

        String link = auth.generateEmailVerificationLink(usuario.getEmail());
        mensajeService.enviarVerificacion(usuario.getEmail(), link);

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
        userMap.put("verificado", false);

        fb.getFirestore().collection("usuarios").document(uid).set(userMap);

        return uid;
    }

    public boolean verificarYRegistrarEnFirestore(String email) throws Exception {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        UserRecord userRecord = auth.getUserByEmail(email);

        if (userRecord.isEmailVerified()) {
            String uid = userRecord.getUid();

            // Actualizar campo "verificado" en Firestore
            ApiFuture<WriteResult> writeResult = fb.getFirestore()
                    .collection("usuarios")
                    .document(uid)
                    .update("verificado", true);

            // Asegúrate de que la actualización se realizó correctamente
            writeResult.get();  // Espera a que se complete la actualización

            return true;
        }

        return false;
    }

    public String autenticarUsuario(String email, String password) throws Exception {
        String apiKey = "AIzaSyCjzd8E1YA34ayRIrENxIEP9ifQAD83xns";
        URL url = new URL("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + apiKey);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String payload = String.format(
                "{\"email\":\"%s\",\"password\":\"%s\",\"returnSecureToken\":true}",
                email, password
        );

        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.getBytes());
            os.flush();
        }

        if (conn.getResponseCode() != 200) {
            throw new Exception("Credenciales inválidas.");
        }

        String response;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            response = in.lines().collect(Collectors.joining());
        }

        JSONObject jsonResponse = new JSONObject(response);
        String uid = jsonResponse.getString("localId");

        verificarYRegistrarEnFirestore(email);

        DocumentSnapshot doc = fb.getFirestore().collection("usuarios").document(uid).get().get();
        Boolean estaVerificado = doc.getBoolean("verificado");

        if (estaVerificado == null || !estaVerificado) {
            throw new Exception("Debes verificar tu correo electrónico antes de iniciar sesión.");
        }

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

    public void generarFacturaPdfDesdeDatos(String nombre, String apellido, List<Map<String, Object>> carrito, double total, HttpServletResponse response) throws Exception {
        Context context = new Context();
        context.setVariable("nombre", nombre);
        context.setVariable("apellido", apellido);
        context.setVariable("carrito", carrito);
        context.setVariable("total", total);

        String htmlContent = templateEngine.process("factura", context);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=baick-factura-" + Timestamp.now().toDate().getTime() + ".pdf");

        try (OutputStream outputStream = response.getOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(htmlContent);
            renderer.layout();
            renderer.createPDF(outputStream);
        } catch (DocumentException e) {
            throw new RuntimeException("Error al generar el PDF", e);
        }
    }

}