package es.masanz.PROYDAM2.controller;

import com.google.cloud.firestore.DocumentSnapshot;
import es.masanz.PROYDAM2.model.entity.Mensaje;
import es.masanz.PROYDAM2.model.service.FirebaseService;
import es.masanz.PROYDAM2.model.service.MensajeService;
import jakarta.servlet.http.HttpSession;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class MensajeController {

    @Autowired
    private MensajeService mensajeService;

    FirebaseService fb = new FirebaseService();

    @GetMapping("/admin/mensajes")
    public String verMensajes(HttpSession session, Model model) {
        String rol = (String) session.getAttribute("rol");
        if (rol == null || !rol.equals("admin")) {
            return "error";
        }

        try {
            List<Mensaje> mensajes = mensajeService.getAllMensajes();
            model.addAttribute("mensajes", mensajes);
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "No se pudieron cargar los mensajes");
        }

        return "mensajesAdmin";
    }

    @PostMapping("/admin/mensajes/responder")
    public String responderMensaje(@RequestParam String mensajeId,
                                    @RequestParam String responseText,
                                    HttpSession session) {
        String rol = (String) session.getAttribute("rol");
        if (rol == null || !rol.equals("admin")) {
            return "error";
        }
    
        try {
            Mensaje mensaje = mensajeService.getMensajeById(mensajeId);
            if (mensaje != null) {
                
                mensajeService.sendResponseEmail(
                    mensaje.getUsuarioEmail(),
                    "Respuesta a tu mensaje de soporte",
                    responseText
                );
            
                mensajeService.eliminarMensajeById(mensajeId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    
        return "redirect:/admin/mensajes";
    }

    @PostMapping("/soporte/enviar")
    public String enviarMensaje(@RequestParam("asunto") String asunto, @RequestParam("contenido") String contenido, HttpSession session, Model model) {
        String uid = (String) session.getAttribute("uid");
        if (uid == null) {
            return "redirect:/login";
        }

        try {
            DocumentSnapshot doc = fb.getFirestore().collection("usuarios").document(uid).get().get();
            String email = doc.getString("email");

            Mensaje mensaje = new Mensaje();
            mensaje.setUsuarioId(uid);
            mensaje.setUsuarioEmail(email);
            mensaje.setAsunto(asunto);
            mensaje.setContenido(contenido);

            mensajeService.guardarMensaje(mensaje);

            model.addAttribute("enviado", true);
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", true);
        }

        return "soporte";
    }
}