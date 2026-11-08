package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PaginaPeriodosAdminProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PeriodoAcademicoProjection;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PeriodoAcademicoQueryPort {

    List<PeriodoAcademicoProjection> consultarPeriodosAcademicos();

    Optional<PeriodoAcademicoProjection> consultarPeriodoAcademicoPorId(UUID idPeriodoAcademico);

    /**
     * CULM-001 D.9-a (F-ADM-030): periodos de la institucion del administrador, paginados y con {@code esVigente}. El SP exige el perfil
     * ADMINISTRADOR (SEC_001, 403) y la paginacion valida (PAG_001, 400).
     */
    PaginaPeriodosAdminProjection consultarPeriodosAdministrador(UUID idUsuarioEjecutor, int pagina, int tamano);
}
