package es.masanz.PROYDAM2.model.entity;

import java.util.ArrayList;
import java.util.List;

public class Usuario {
    private String id;

    private String nombre;
    private String apellido;
    private String contrasena;
    private String dni;
    private String email;
    private int telefono;
    private String ciudad;
    private List<Bicicleta> carrito = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getTelefono() {
        return telefono;
    }

    public void setTelefono(int telefono) {
        this.telefono = telefono;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public List<Bicicleta> getCarrito() {
        return carrito;
    }

    public void setCarrito(List<Bicicleta> carrito) {
        this.carrito = carrito;
    }
}