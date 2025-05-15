package es.masanz.PROYDAM2.model.entity;

public class Mensaje {
    private String id;
    private String usuarioId;
    private String usuarioEmail;
    private String asunto;
    private String contenido;
    private String timestamp;

    public void setId(String id) {
	    this.id = id;
    }

    public String getId() {
	    return id;
    }

    public void setUsuarioId(String usuarioId) {
	    this.usuarioId = usuarioId;
    }

    public String getUsuarioId() {
	    return usuarioId;
    }

    public void setUsuarioEmail(String usuarioEmail) {
	    this.usuarioEmail = usuarioEmail;
    }

    public String getUsuarioEmail() {
	    return usuarioEmail;
    }

    public void setAsunto(String asunto) {
	    this.asunto = asunto;
    }

    public String getAsunto() {
	    return asunto;
    }

    public void setContenido(String contenido) {
	    this.contenido = contenido;
    }

    public String getContenido() {
	    return contenido;
    }

    public void setTimestamp(String timestamp) {
	    this.timestamp = timestamp;
    }

    public String getTimestamp() {
	    return timestamp;
    }
}