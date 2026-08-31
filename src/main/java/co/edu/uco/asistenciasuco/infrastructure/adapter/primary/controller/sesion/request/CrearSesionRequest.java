package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.request;

import java.util.UUID;

public final class CrearSesionRequest {

    private UUID grupo;
    private String tema;
    private String descripcion;

    public UUID getGrupo() {
        return grupo;
    }

    public void setGrupo(final UUID grupo) {
        this.grupo = grupo;
    }

    public String getTema() {
        return tema;
    }

    public void setTema(final String tema) {
        this.tema = tema;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(final String descripcion) {
        this.descripcion = descripcion;
    }
}
