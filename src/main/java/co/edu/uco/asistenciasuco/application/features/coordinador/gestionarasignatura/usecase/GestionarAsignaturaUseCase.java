package co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.usecase;

import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.usecase.domain.AsignaturaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.usecase.domain.EstadoAsignaturaDomain;
import java.util.UUID;

public interface GestionarAsignaturaUseCase {

    void crear(AsignaturaDomain domain);

    void actualizar(AsignaturaDomain domain);

    /** HU074: {@code estaActivaDeseada} {@code null} alterna el estado actual. */
    EstadoAsignaturaDomain toggleEstado(UUID asignaturaId, Boolean estaActivaDeseada, UUID usuarioEjecutor);
}
