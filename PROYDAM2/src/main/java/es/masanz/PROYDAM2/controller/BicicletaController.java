package es.masanz.PROYDAM2.controller;

import es.masanz.PROYDAM2.model.service.BicicletaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BicicletaController {

    @Autowired
    private BicicletaService bicicletaService;

    // Ruta para mostrar los productos
    @GetMapping("/catalogo")
    public String mostrarProductos(Model model) {
        model.addAttribute("bicicletas", bicicletaService.obtenerProductos());
        return "catalogo";  // Nombre de la vista Thymeleaf
    }

    @GetMapping("/")
    public String galeria(Model model) {
        model.addAttribute("bicicletas", bicicletaService.obtenerProductos());
        return "index"; // nombre del HTML Thymeleaf
    }
}
