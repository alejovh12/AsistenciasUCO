package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.support;

import co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.AlertaFila;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common.ResultadoDecision;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common.ResultadoResolucion;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common.RevisionFila;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.AlertaInasistenciaResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.DecisionRevisionCancelacionResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.RevisionCancelacionResponse;

/** CULM-001 C.1: traduccion de las filas de la DB a las respuestas HTTP (sin logica de negocio). */
public final class RevisionCancelacionHttpMapper {

    private RevisionCancelacionHttpMapper() {
    }

    public static RevisionCancelacionResponse toResponse(final RevisionFila f) {
        return new RevisionCancelacionResponse(f.id(), f.estado(), f.nombreEstado(), f.requiereAccionDocente(), f.requiereAccionResolutor(),
                f.esResolutorActual(), f.accionesPermitidas(), f.perfilResolutorActual(), f.fechaEscalamientoUtc(),
                new RevisionCancelacionResponse.Persona(f.estudianteId(), f.nombreEstudiante()), f.estadoMatricula(),
                new RevisionCancelacionResponse.Referencia(f.grupoId(), f.codigoGrupo(), f.nombreGrupo()),
                new RevisionCancelacionResponse.Referencia(f.asignaturaId(), null, f.nombreAsignatura()),
                new RevisionCancelacionResponse.Persona(f.docenteId(), f.nombreDocente()),
                f.programaId() == null ? null : new RevisionCancelacionResponse.Programa(f.programaId(), f.nombrePrograma()),
                f.porcentajeCreacion(), f.porcentajeActual(), f.umbralPerdida(), f.horasFalta(), f.horasDictadas(), f.fechaCreacionUtc(),
                f.decisionDocente(), f.motivoDocente(), f.fechaDecisionDocenteUtc(), f.decisionResolucion(), f.motivoResolucion(),
                f.nombreResolutor() == null ? null : new RevisionCancelacionResponse.Resolutor(f.nombreResolutor(), f.perfilResolucion()),
                f.fechaResolucionUtc());
    }

    public static DecisionRevisionCancelacionResponse toResponse(final ResultadoDecision r) {
        return new DecisionRevisionCancelacionResponse(r.idRevision(), r.estadoAnterior(), r.estadoNuevo(), r.decision(), r.sinCambios(), false,
                r.perfilResolutor(), r.escalamiento(), r.mensajeUsuario());
    }

    public static DecisionRevisionCancelacionResponse toResponse(final ResultadoResolucion r) {
        return new DecisionRevisionCancelacionResponse(r.idRevision(), r.estadoAnterior(), r.estadoNuevo(), r.decision(), r.sinCambios(),
                r.cambioMatricula(), r.perfilResolucion(), false, r.mensajeUsuario());
    }

    public static AlertaInasistenciaResponse toResponse(final AlertaFila f) {
        return new AlertaInasistenciaResponse(f.id(), f.codigoTipo(), f.nombreTipo(), f.codigoEstado(), f.mensaje(), f.porcentajeCruce(),
                f.porcentajeActual(), f.umbral(), f.umbralPerdida(), f.horasFalta(), f.horasDictadas(), f.fechaCruceUtc(), f.leida(), f.fechaLecturaUtc(),
                new AlertaInasistenciaResponse.Referencia(f.grupoId(), f.codigoGrupo(), f.nombreGrupo()),
                new AlertaInasistenciaResponse.Referencia(f.asignaturaId(), null, f.nombreAsignatura()), f.estadoMatricula());
    }
}
