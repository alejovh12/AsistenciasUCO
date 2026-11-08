package co.edu.uco.asistenciasuco.application.secondaryports.report;

import java.util.List;
import java.util.UUID;

public interface ReporteAsistenciaQueryPort {

    List<ReporteAsistenciaRow> consultarReporteAsistenciaGrupo(UUID grupoId);

    /**
     * CULM-001 D.9-a (F-TRX-021): la base de datos valida que el grupo exista y este en el ambito del ejecutor antes de generar el
     * archivo; un grupo inexistente o ajeno lanza {@code ForbiddenException} (403, SEC_002).
     */
    void validarAccesoGrupo(UUID usuarioEjecutor, UUID grupoId);
}
