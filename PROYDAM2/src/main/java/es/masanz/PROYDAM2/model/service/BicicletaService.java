package es.masanz.PROYDAM2.model.service;

import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import es.masanz.PROYDAM2.model.entity.Bicicleta;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BicicletaService {

    private FirebaseService fb = new FirebaseService();

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
}