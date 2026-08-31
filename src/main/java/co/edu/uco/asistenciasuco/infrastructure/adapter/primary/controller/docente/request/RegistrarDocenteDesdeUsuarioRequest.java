package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.request;

import java.util.UUID;

public final class RegistrarDocenteDesdeUsuarioRequest {

    private UUID usuario;

    public UUID getUsuario() {
        return usuario;
    }

    public void setUsuario(final UUID usuario) {
        this.usuario = usuario;
    }
}
