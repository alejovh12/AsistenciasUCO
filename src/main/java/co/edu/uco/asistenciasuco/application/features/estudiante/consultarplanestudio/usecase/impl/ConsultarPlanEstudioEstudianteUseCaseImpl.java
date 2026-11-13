package co.edu.uco.asistenciasuco.application.features.estudiante.consultarplanestudio.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.catalog.MensajeUsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarplanestudio.usecase.ConsultarPlanEstudioEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarplanestudio.usecase.domain.AsignaturaPlanEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarplanestudio.usecase.domain.PlanEstudioEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarplanestudio.usecase.domain.SemestrePlanEstudianteDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.PlanEstudioEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsignaturaPlanEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * HU018: estructura curricular por semestres del estudiante autenticado. Arma, a partir de las filas planas del SP, los
 * planes activos de sus programas con sus semestres y asignaturas, en el orden que entrega el SP (programa, plan,
 * semestre, caracter y nombre). El ambito (solo los programas del propio estudiante) lo aplica el SP; aqui solo se exige
 * que el ejecutor sea un estudiante.
 */
public final class ConsultarPlanEstudioEstudianteUseCaseImpl implements ConsultarPlanEstudioEstudianteUseCase {

    /** Regla de John (2026-10-27): ESP es propia del programa; BAS e INS son transversales. La misma de la oferta (HU008). */
    private static final Map<String, String> TIPO_ASIGNATURA = Map.of("ESP", "ESPECIFICA", "BAS", "BASICA", "INS", "INSTITUCIONAL");

    private final InstitutionalScopePort scopePort;
    private final PlanEstudioEstudianteQueryPort port;

    public ConsultarPlanEstudioEstudianteUseCaseImpl(final InstitutionalScopePort scopePort, final PlanEstudioEstudianteQueryPort port) {
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
        this.port = Objects.requireNonNull(port, "PlanEstudioEstudianteQueryPort es obligatorio.");
    }

    @Override
    public List<PlanEstudioEstudianteDomain> execute(final UUID actorUsuarioId) {
        scopePort.findEstudianteIdByUsuario(actorUsuarioId)
                .orElseThrow(() -> new ForbiddenException(MensajeUsuarioErrorCode.ACC_APP_004));
        // LinkedHashMap: conserva el orden del SP.
        final Map<UUID, List<AsignaturaPlanEstudianteProjection>> porPlan = new LinkedHashMap<>();
        for (final AsignaturaPlanEstudianteProjection fila : port.consultarPlanEstudio(actorUsuarioId)) {
            porPlan.computeIfAbsent(fila.idPlanEstudio(), plan -> new ArrayList<>()).add(fila);
        }
        return porPlan.values().stream().map(ConsultarPlanEstudioEstudianteUseCaseImpl::toPlan).toList();
    }

    private static PlanEstudioEstudianteDomain toPlan(final List<AsignaturaPlanEstudianteProjection> filas) {
        final AsignaturaPlanEstudianteProjection cabecera = filas.getFirst();
        final Map<UUID, List<AsignaturaPlanEstudianteProjection>> porSemestre = new LinkedHashMap<>();
        for (final AsignaturaPlanEstudianteProjection fila : filas) {
            porSemestre.computeIfAbsent(fila.idSemestrePlanEstudio(), semestre -> new ArrayList<>()).add(fila);
        }
        return new PlanEstudioEstudianteDomain(cabecera.idPlanEstudio(), cabecera.inpPlanEstudio(), cabecera.idPrograma(),
                cabecera.nombrePrograma(),
                porSemestre.values().stream().map(ConsultarPlanEstudioEstudianteUseCaseImpl::toSemestre).toList());
    }

    /** Un semestre sin asignaturas llega en una fila sin {@code idAsignatura}: queda con la lista vacia. */
    private static SemestrePlanEstudianteDomain toSemestre(final List<AsignaturaPlanEstudianteProjection> filas) {
        final AsignaturaPlanEstudianteProjection primera = filas.getFirst();
        return new SemestrePlanEstudianteDomain(primera.idSemestrePlanEstudio(), primera.numeroSemestre(),
                filas.stream()
                        .filter(fila -> fila.idAsignatura() != null)
                        .map(ConsultarPlanEstudioEstudianteUseCaseImpl::toAsignatura)
                        .toList());
    }

    private static AsignaturaPlanEstudianteDomain toAsignatura(final AsignaturaPlanEstudianteProjection fila) {
        return new AsignaturaPlanEstudianteDomain(fila.idAsignatura(), fila.codigoAsignatura(), fila.nombreAsignatura(),
                fila.creditos(),
                fila.codigoComponente() == null ? null : TIPO_ASIGNATURA.getOrDefault(fila.codigoComponente(), fila.codigoComponente()),
                fila.codigoCaracter(), fila.nombreCaracter(), fila.cursando(),
                fila.cursando() ? fila.idGrupoCursando() : null,
                fila.cursando() ? fila.nombreGrupoCursando() : null);
    }
}
