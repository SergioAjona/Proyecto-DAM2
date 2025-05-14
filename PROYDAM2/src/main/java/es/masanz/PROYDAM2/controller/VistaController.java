package es.masanz.PROYDAM2.controller;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import es.masanz.PROYDAM2.model.entity.Bicicleta;
import es.masanz.PROYDAM2.model.entity.Usuario;
import es.masanz.PROYDAM2.model.service.VistaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Controller
public class VistaController {

    @Autowired
    private VistaService vistaService;

    @GetMapping("/registro")
    public String mostrarFormularioRegistro(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "registro";
    }

    @PostMapping("/registro")
    public String registrarUsuario(@ModelAttribute("Usuario") Usuario usuario, Model model) {
        try {
            vistaService.registerUser(usuario);
            return "redirect:/login?registroExitoso";
        } catch (Exception e) {
            model.addAttribute("error", "Error al registrar usuario: " + e.getMessage());
            return "registro";
        }
    }

    @GetMapping("/login")
    public String mostrarFormularioLogin(Model model,
                                         @RequestParam(value = "error", required = false) String error,
                                         @RequestParam(value = "registroExitoso", required = false) String registroExitoso) {
        if (error != null) {
            model.addAttribute("error", "Credenciales inválidas");
        }
        if (registroExitoso != null) {
            model.addAttribute("mensaje", "Registro exitoso, ahora puedes iniciar sesión");
        }
        model.addAttribute("Usuario", new Usuario());
        return "login";
    }

    @PostMapping("/login")
    public String login(@ModelAttribute Usuario usuario, HttpSession session, Model model) {
        try {
            // Login con Firebase (esto ya lo tienes)
            String uid = vistaService.loginUser(usuario.getEmail(), usuario.getContrasena());

            // Obtener rol desde Firestore
            Firestore db = FirestoreClient.getFirestore();
            DocumentReference userRef = db.collection("usuarios").document(uid);
            DocumentSnapshot snapshot = userRef.get().get();

            if (!snapshot.exists()) {
                model.addAttribute("error", "Usuario no encontrado en Firestore");
                return "login";
            }

            String rol = snapshot.getString("rol");
            if (rol == null) rol = "cliente";

            // Guardar en sesión
            session.setAttribute("uid", uid);
            session.setAttribute("rol", rol);

            return "redirect:/";
        } catch (Exception e) {
            model.addAttribute("error", "Credenciales inválidas o error al autenticar");
            return "login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/catalogo")
    public String mostrarCatalogo(
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) Double precioMax,
            @RequestParam(required = false) List<String> marca,
            @RequestParam(required = false, defaultValue = "false") boolean soloEnStock,
            Model model) {

        // Si no hay filtros, todos los productos se muestran
        if (marca != null) {
            model.addAttribute("marca", marca);
        } else {
            marca = new ArrayList<>(); // Evitar que 'marcas' sea null
        }

        if (tipo != null) {
            model.addAttribute("tipo", tipo);
        }

        if (precioMax != null) {
            model.addAttribute("precioMax", precioMax);
        } else {
            precioMax = 20000.0;
        }

        if (soloEnStock) {
            model.addAttribute("soloEnStock", soloEnStock);
        }

        List<Bicicleta> bicicletas = vistaService.filtrarProductos(tipo, precioMax, marca, soloEnStock);
        model.addAttribute("bicicletas", bicicletas);

        return "catalogo";
    }

    @GetMapping("/")
    public String galeria(Model model) {
        model.addAttribute("bicicletas", vistaService.obtenerProductos());
        return "index"; // nombre del HTML Thymeleaf
    }

    @GetMapping("/pago")
    public String pago(Model model) {
        model.addAttribute("bici", "hola");
        return "pago";
    }

    @GetMapping("/catalogo/{id}")
    public String verProducto(@PathVariable String id, Model model) throws ExecutionException, InterruptedException {
        Bicicleta bicicleta = vistaService.findById(id);
        model.addAttribute("bicicleta", bicicleta);
        return "vistaProducto";
    }

    @PostMapping("/catalogo/{id}/sumar")
    public String sumarStock(@PathVariable String id) {
        try {
            vistaService.sumarStock(id);
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
        return "redirect:/catalogo/" + id;
    }

    @PostMapping("/catalogo/{id}/restar")
    public String restarStock(@PathVariable String id) {
        try {
            vistaService.restarStock(id);
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
        return "redirect:/catalogo/" + id;
    }

    @GetMapping("/carrito")
    public String verCarrito(HttpSession session, Model model) throws Exception {
        String uid = (String) session.getAttribute("uid");
        if (uid == null) {
            return "redirect:/login";
        }

        Firestore db = FirestoreClient.getFirestore();
        DocumentSnapshot usuarioDoc = db.collection("usuarios").document(uid).get().get();

        if (!usuarioDoc.exists()) {
            model.addAttribute("carrito", new ArrayList<>());
            return "carrito";
        }

        List<String> carritoIds = (List<String>) usuarioDoc.get("carrito");
        List<Map<String, Object>> carritoConDetalles = new ArrayList<>();

        if (carritoIds != null) {
            for (String productoId : carritoIds) {
                if (productoId != null) {
                    DocumentSnapshot productoDoc = db.collection("bicicletas").document(productoId).get().get();
                    if (productoDoc.exists()) {
                        Map<String, Object> producto = productoDoc.getData();
                        producto.put("id", productoId);
                        carritoConDetalles.add(producto);
                    }
                }
            }
        }

        model.addAttribute("carrito", carritoConDetalles);
        return "carrito";
    }

    @PostMapping("/carrito/agregar")
    public String agregarProductoAlCarrito(@RequestParam("productoId") String productoId,
                                           HttpSession session,
                                           RedirectAttributes redirectAttributes) {
        String uid = (String) session.getAttribute("uid");

        if (uid == null) {
            return "redirect:/login";
        }

        try {
            Firestore db = FirestoreClient.getFirestore();
            DocumentReference usuarioRef = db.collection("usuarios").document(uid);

            // Agregar el ID del producto al array 'carrito' usando arrayUnion
            usuarioRef.update("carrito", FieldValue.arrayUnion(productoId));

            redirectAttributes.addFlashAttribute("mensaje", "Producto agregado al carrito.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al agregar al carrito: " + e.getMessage());
        }

        return "redirect:/catalogo";
    }

    @PostMapping("/carrito/eliminar")
    public String eliminarProductoDelCarrito(@RequestParam("productoId") String productoId,
                                             HttpSession session,
                                             RedirectAttributes redirectAttributes) {
        String uid = (String) session.getAttribute("uid");

        if (uid == null) {
            return "redirect:/login";
        }

        try {
            Firestore db = FirestoreClient.getFirestore();
            DocumentReference usuarioRef = db.collection("usuarios").document(uid);

            // Eliminar el producto del array usando arrayRemove
            usuarioRef.update("carrito", FieldValue.arrayRemove(productoId));

            redirectAttributes.addFlashAttribute("mensaje", "Producto eliminado del carrito.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar del carrito: " + e.getMessage());
        }

        return "redirect:/carrito";
    }

    @PostMapping("/carrito/vaciar")
    public String vaciarCarrito(HttpSession session, RedirectAttributes redirectAttributes) {
        String uid = (String) session.getAttribute("uid");

        if (uid == null) {
            return "redirect:/login";
        }

        try {
            Firestore db = FirestoreClient.getFirestore();
            DocumentReference usuarioRef = db.collection("usuarios").document(uid);

            // Sobrescribir el campo 'carrito' con un array vacío
            Map<String, Object> actualizacion = new HashMap<>();
            actualizacion.put("carrito", new ArrayList<>());

            usuarioRef.update(actualizacion);

            redirectAttributes.addFlashAttribute("mensaje", "Carrito vaciado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al vaciar el carrito: " + e.getMessage());
        }

        return "redirect:/";
    }

    @GetMapping("/acercade")
    public String verAcercaDe() {
        return "acercade";
    }

    @GetMapping("/soporte")
    public String verSoporte() {
        return "soporte";
    }
}
