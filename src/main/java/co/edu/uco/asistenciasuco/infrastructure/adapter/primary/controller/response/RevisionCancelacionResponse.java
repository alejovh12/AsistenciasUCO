package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** CULM-001 C.1 (contrato 3.2): revision de cancelacion por inasistencia para las bandejas del coordinador y del decano. */
public record RevisionCancelacionResponse(
        UUID id,
        String estado,
        String nombreEstado,
        boolean requiereAccionDocente,
        boolean requiereAccionResolutor,
        boolean esResolutorActual,
        List<String> accionesPermitidas,
        String perfilResolutorActual,
        Instant fechaEscalamientoUtc,
        Persona estudiante,
        String estadoMatricula,
        Referencia grupo,
        Referencia asignatura,
        Persona docente,
        Programa programa,
        BigDecimal porcentajeCreacion,
        BigDecimal porcentajeActual,
        BigDecimal umbralPerdida,
        BigDecimal horasFalta,
        BigDecimal horasDictadas,
        Instant fechaCreacionUtc,
        String decisionDocente,
        String motivoDocente,
        Instant fechaDecisionDocenteUtc,
        String decisionResolucion,
        String motivoResolucion,
        Resolutor resolutor,
        Instant fechaResolucionUtc
) {

    public record Persona(UUID id, String nombreCompleto) {
    }

    public record Referencia(UUID id, Integer codigo, String nombre) {
    }

    public record Programa(UUID id, String nombre) {
    }

    public record Resolutor(String nombreCompleto, String perfil) {
    }
}
