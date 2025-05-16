package es.masanz.PROYDAM2.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;

import es.masanz.PROYDAM2.model.entity.Bicicleta;
import es.masanz.PROYDAM2.model.entity.Usuario;
import es.masanz.PROYDAM2.model.service.VistaService;
import jakarta.servlet.http.HttpSession;

@Controller
public class VistaController {

    @Autowired
    private VistaService vistaService;

    @GetMapping("/registro")
    public String mostrarFormularioRegistro(Model model) {
        model.addAttribute("Usuario", new Usuario());
        return "registro";
    }

    @PostMapping("/registro")
    public String registrarUsuario(@ModelAttribute("Usuario") Usuario usuario, Model model) {
        try {
            vistaService.registrarUsuario(usuario);
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("error", "Error al registrar usuario: " + e.getMessage());
            return "registro";
        }
    }

    @GetMapping("/login")
    public String mostrarFormularioLogin(Model model,
                                         @RequestParam(value = "error", required = false) String error) {
        if (error != null) {
            model.addAttribute("error", "Credenciales inválidas");
        }

        model.addAttribute("Usuario", new Usuario());
        return "login";
    }

    @PostMapping("/login")
    public String login(@ModelAttribute Usuario usuario, HttpSession session, Model model) {
        try {
            // Login con Firebase (esto ya lo tienes)
            String uid = vistaService.logearUsuario(usuario.getEmail(), usuario.getContrasena());

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
            @RequestParam(required = false, defaultValue = "false") boolean stock,
            Model model, HttpSession session) {
        String uid = (String) session.getAttribute("uid");

        if (uid != null && "admin".equals(session.getAttribute("rol"))) {
            model.addAttribute("admin", true);
        }

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

        if (stock) {
            model.addAttribute("stock", stock);
        }

        List<Bicicleta> bicicletas = vistaService.filtrarProductos(tipo, precioMax, marca, stock);
        model.addAttribute("bicicletas", bicicletas);

        return "catalogo";
    }

    @GetMapping("/")
    public String galeria(Model model) {
        model.addAttribute("bicicletas", vistaService.obtenerProductos());
        return "index";
    }

    @GetMapping("/pago")
    public String pago(HttpSession session, Model model) throws Exception {
        String uid = (String) session.getAttribute("uid");

        if (uid == null) {
            return "redirect:/login";
        }

        Firestore fb = FirestoreClient.getFirestore();
        DocumentReference usuarioRef = fb.collection("usuarios").document(uid);

        List<Map<String, Object>> carritoConDetalles = vistaService.obtenerCarritoConDetalles(uid);
        double total = vistaService.calcularTotalCarrito(carritoConDetalles);

        model.addAttribute("carrito", carritoConDetalles);
        model.addAttribute("total", total);
        model.addAttribute("usuarioNombre", usuarioRef.get().get().get("nombre") + " " + usuarioRef.get().get().get("apellido"));
        model.addAttribute("usuarioEmail", usuarioRef.get().get().get("email"));

        return "pago";
    }

    @GetMapping("/catalogo/{id}")
    public String verProducto(@PathVariable String id, Model model) throws ExecutionException, InterruptedException {
        Bicicleta bicicleta = vistaService.findById(id);
        model.addAttribute("bicicleta", bicicleta);

        Firestore db = FirestoreClient.getFirestore();
        DocumentReference biciRef = db.collection("bicicletas").document(id);

        ApiFuture<DocumentSnapshot> futureBici = biciRef.get();
        DocumentSnapshot docBici = futureBici.get();

        Long stock = docBici.getLong("stock");
        if (stock == null || stock <= 0) {
            model.addAttribute("unidades", false);
        } else {
            model.addAttribute("unidades", true);
        }
        return "vistaProducto";
    }

    @PostMapping("/catalogo/{id}/sumar")
    public String sumarStock(@PathVariable String id, HttpSession session) {
        String uid = (String) session.getAttribute("uid");

        if (uid != null && "admin".equals(session.getAttribute("rol"))) {
            try {
                vistaService.sumarStock(id);
            } catch (Exception e) {
                e.printStackTrace();
                return "error";
            }
            return "redirect:/catalogo";
        }
        return "error";
    }

    @PostMapping("/catalogo/{id}/restar")
    public String restarStock(@PathVariable String id, HttpSession session) {
        String uid = (String) session.getAttribute("uid");

        if (uid != null && "admin".equals(session.getAttribute("rol"))) {
            try {
                vistaService.restarStock(id);
            } catch (Exception e) {
                e.printStackTrace();
                return "error";
            }
            return "redirect:/catalogo";
        }
        return "error";
    }

    @GetMapping("/carrito")
    public String verCarrito(HttpSession session, Model model) throws Exception {
        String uid = (String) session.getAttribute("uid");
        if (uid == null) {
            return "redirect:/login";
        }

        List<Map<String, Object>> carritoConDetalles = vistaService.obtenerCarritoConDetalles(uid);
        model.addAttribute("carrito", carritoConDetalles);
        return "carrito";
    }

    @PostMapping("/carrito/agregar")
    public String agregarProductoAlCarrito(@RequestParam("productoId") String productoId,
                                           HttpSession session,
                                           RedirectAttributes redirectAttributes, Model model) {
        String uid = (String) session.getAttribute("uid");

        if (uid == null) {
            return "redirect:/login";
        }

        try {
            vistaService.agregarProductoAlCarrito(uid, productoId);
            vistaService.restarStock(productoId);
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
            vistaService.eliminarProductoDelCarrito(uid, productoId);
            vistaService.sumarStock(productoId);
            redirectAttributes.addFlashAttribute("mensaje", "Producto eliminado del carrito.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar del carrito: " + e.getMessage());
        }

        return "redirect:/carrito";
    }

    @PostMapping("/pago/confirmar")
    public String vaciarCarrito(HttpSession session, RedirectAttributes redirectAttributes) {
        String uid = (String) session.getAttribute("uid");

        if (uid == null) {
            return "redirect:/login";
        }

        try {
            vistaService.vaciarCarrito(uid);
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
    public String verSoporte(HttpSession session, Model model) {
        String rol = (String) session.getAttribute("rol");
        if (rol == null || !rol.equals("admin")) {
            return "soporte";
        }

        model.addAttribute("admin", true);
        return "soporte";
    }
}
