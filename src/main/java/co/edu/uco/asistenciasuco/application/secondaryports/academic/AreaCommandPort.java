package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import java.util.UUID;

public interface AreaCommandPort {

    void crearArea(CrearAreaCommand command);

    void actualizarArea(ActualizarAreaCommand command);

    void cambiarEstadoArea(CambiarEstadoAreaCommand command);

    record CrearAreaCommand(String codigo, String nombre, UUID actorUsuarioId) {
    }

    record ActualizarAreaCommand(UUID id, String nombre, UUID actorUsuarioId) {
    }

    record CambiarEstadoAreaCommand(UUID id, boolean activa, UUID actorUsuarioId) {
    }
}
