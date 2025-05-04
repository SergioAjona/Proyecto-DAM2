package es.masanz.PROYDAM2.model.service;

import com.google.firebase.cloud.FirestoreClient;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import es.masanz.PROYDAM2.model.entity.Bicicleta;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BicicletaService {

    // Método para obtener todos los productos de Firestore
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