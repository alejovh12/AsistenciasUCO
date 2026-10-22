package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import java.util.UUID;

public interface TipoProgramaCommandPort {

    void crearTipoPrograma(CrearTipoProgramaCommand command);

    void actualizarTipoPrograma(ActualizarTipoProgramaCommand command);

    void cambiarEstadoTipoPrograma(CambiarEstadoTipoProgramaCommand command);

    record CrearTipoProgramaCommand(String codigo, String nombre, UUID actorUsuarioId) {
    }

    record ActualizarTipoProgramaCommand(UUID id, String nombre, UUID actorUsuarioId) {
    }

    record CambiarEstadoTipoProgramaCommand(UUID id, boolean activo, UUID actorUsuarioId) {
    }
}
