package es.masanz.PROYDAM2.controller;

import es.masanz.PROYDAM2.model.entity.CarritoItem;
import es.masanz.PROYDAM2.model.entity.Usuario;
import es.masanz.PROYDAM2.model.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    private String limpiarToken(String token) {
        return token.replace("Bearer ", "");
    }

    @PostMapping("/registro")
    public ResponseEntity<String> registrar(@RequestHeader("Authorization") String token,
                                            @RequestBody Usuario datosUsuario) {
        try {
            // Llamar al servicio para registrar al usuario en Firestore
            String respuesta = usuarioService.registrarUsuario(limpiarToken(token), datosUsuario);
            return ResponseEntity.ok(respuesta);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public String login(@RequestHeader("Authorization") String token) {
        try {
            return usuarioService.loginUsuario(limpiarToken(token));
        } catch (Exception e) {
            return "Error en el inicio de sesión: " + e.getMessage();
        }
    }

    @PostMapping("/carrito/agregar")
    public ResponseEntity<String> agregarAlCarrito(@RequestHeader("Authorization") String token,
                                                   @RequestBody CarritoItem item) {
        try {
            usuarioService.agregarAlCarrito(limpiarToken(token), item);
            return ResponseEntity.ok("Producto agregado al carrito");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    @DeleteMapping("/carrito/limpiar")
    public ResponseEntity<String> limpiarCarrito(@RequestHeader("Authorization") String token) {
        try {
            usuarioService.limpiarCarrito(limpiarToken(token));
            return ResponseEntity.ok("Carrito limpiado");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }
}
