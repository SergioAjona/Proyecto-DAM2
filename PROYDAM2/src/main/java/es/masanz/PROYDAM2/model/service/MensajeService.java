package es.masanz.PROYDAM2.model.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;

import es.masanz.PROYDAM2.model.entity.Mensaje;

@Service
public class MensajeService {

    @Autowired
    private JavaMailSender mailSender;

    private FirebaseService fb = new FirebaseService();

    public void sendResponseEmail(String para, String asunto, String cuerpo) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom("baick.noreply@gmail.com");
        mensaje.setTo(para);
        mensaje.setSubject(asunto);
        mensaje.setText(cuerpo);
        mailSender.send(mensaje);
    }

    public void guardarMensaje(Mensaje mensaje) throws ExecutionException, InterruptedException {
        DocumentReference docRef = fb.getFirestore().collection("mensajes").document();
        mensaje.setId(docRef.getId());
        mensaje.setTimestamp(Timestamp.now().toString());
        docRef.set(mensaje).get();
    }

    public List<Mensaje> getAllMensajes() throws ExecutionException, InterruptedException {
        ApiFuture<QuerySnapshot> future = fb.getFirestore().collection("mensajes").orderBy("timestamp", Query.Direction.DESCENDING).get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        List<Mensaje> mensajes = new ArrayList<>();
        for (QueryDocumentSnapshot doc : documents) {
            Mensaje mensaje = doc.toObject(Mensaje.class);
            mensajes.add(mensaje);
        }
        return mensajes;
    }

    public Mensaje getMensajeById(String id) throws ExecutionException, InterruptedException {
        DocumentSnapshot doc = fb.getFirestore().collection("mensajes").document(id).get().get();
        if (doc.exists()) {
            return doc.toObject(Mensaje.class);
        }
        return null;
    }

    public void eliminarMensajeById(String id) throws ExecutionException, InterruptedException {
        fb.getFirestore().collection("mensajes").document(id).delete().get();
    }

}