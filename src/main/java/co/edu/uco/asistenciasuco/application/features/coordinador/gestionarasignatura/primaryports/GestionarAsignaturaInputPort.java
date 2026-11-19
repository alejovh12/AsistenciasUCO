package co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.primaryports;

import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.primaryports.dto.EstadoAsignaturaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.primaryports.dto.GuardarAsignaturaDTO;
import java.util.UUID;

public interface GestionarAsignaturaInputPort {

    void crear(GuardarAsignaturaDTO dto);

    void actualizar(GuardarAsignaturaDTO dto);

    /**
     * HU074: activa o inactiva la asignatura y devuelve el estado en que quedo con el mensaje del catalogo.
     *
     * @param estaActivaDeseada estado pedido; {@code null} alterna el estado actual.
     */
    EstadoAsignaturaDTO toggleEstado(UUID asignaturaId, Boolean estaActivaDeseada, UUID usuarioEjecutor);
}
