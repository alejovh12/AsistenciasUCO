package co.edu.uco.asistenciasuco.application.features.estudiante.consultarresumenmaterias.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.catalog.MensajeUsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarresumenmaterias.usecase.ConsultarResumenMateriasEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarresumenmaterias.usecase.domain.ResumenMateriasEstudianteDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ResumenMateriasEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResumenMateriasEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;

import java.util.Objects;
import java.util.UUID;

/**
 * CIERRE-W2B: resumen del tablero del estudiante autenticado. La base calcula las cuatro cifras sobre las mismas materias que el
 * listado ({@code usp_consultar_materias_estudiante}); aqui solo se valida que el usuario sea estudiante (403 si no) y se
 * transporta el resultado. Nada se suma ni se clasifica en este nivel.
 */
public final class ConsultarResumenMateriasEstudianteUseCaseImpl implements ConsultarResumenMateriasEstudianteUseCase {

    private final InstitutionalScopePort scopePort;
    private final ResumenMateriasEstudianteQueryPort queryPort;

    public ConsultarResumenMateriasEstudianteUseCaseImpl(final InstitutionalScopePort scopePort,
                                                         final ResumenMateriasEstudianteQueryPort queryPort) {
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
        this.queryPort = Objects.requireNonNull(queryPort, "ResumenMateriasEstudianteQueryPort es obligatorio.");
    }

    @Override
    public ResumenMateriasEstudianteDomain execute(final UUID actorUsuarioId) {
        scopePort.findEstudianteIdByUsuario(actorUsuarioId)
                .orElseThrow(() -> new ForbiddenException(MensajeUsuarioErrorCode.ACC_APP_004));
        final ResumenMateriasEstudianteProjection fila = queryPort.consultarResumen(actorUsuarioId);
        return new ResumenMateriasEstudianteDomain(fila.totalMaterias(), fila.materiasEnRiesgo(), fila.totalHorasFalta(),
                fila.totalCreditos());
    }
}
