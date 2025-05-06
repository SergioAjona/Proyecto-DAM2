package es.masanz.PROYDAM2.controller;

import es.masanz.PROYDAM2.model.entity.Bicicleta;
import es.masanz.PROYDAM2.model.service.BicicletaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

@Controller
public class BicicletaController {

    @Autowired
    private BicicletaService bicicletaService;

    @GetMapping("/catalogo")
    public String mostrarCatalogo(
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) Double precioMax,
            @RequestParam(required = false) List<String> marca,
            @RequestParam(required = false, defaultValue = "false") boolean soloEnStock,
            Model model) {

        // Si no hay filtros, todos los productos se muestran
        if (marca != null) {
            model.addAttribute("marcas", marca);
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

        List<Bicicleta> bicicletas = bicicletaService.filtrarProductos(tipo, precioMax, marca, soloEnStock);
        model.addAttribute("bicicletas", bicicletas);

        return "catalogo";
    }

    @GetMapping("/")
    public String galeria(Model model) {
        model.addAttribute("bicicletas", bicicletaService.obtenerProductos());
        return "index"; // nombre del HTML Thymeleaf
    }
}
