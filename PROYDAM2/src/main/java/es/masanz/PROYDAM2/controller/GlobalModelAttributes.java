package es.masanz.PROYDAM2.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import es.masanz.PROYDAM2.model.service.VistaService;

import org.springframework.ui.Model;
import jakarta.servlet.http.HttpSession;

@ControllerAdvice
public class GlobalModelAttributes {

    @Autowired
    private VistaService vistaService;

    @ModelAttribute
    public void addAttributes(Model model, HttpSession session) {
        String uid = (String) session.getAttribute("uid");

        if (uid != null) {
            try {
                int cantidad = vistaService.obtenerCarritoConDetalles(uid).size();
                model.addAttribute("cantidadCarrito", cantidad);
            } catch (Exception e) {
                model.addAttribute("cantidadCarrito", 0);
            }
        } else {
            model.addAttribute("cantidadCarrito", 0);
        }
    }
}
