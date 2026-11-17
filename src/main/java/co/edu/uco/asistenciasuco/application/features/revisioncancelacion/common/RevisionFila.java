package co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RevisionFila(UUID id, String estado, String nombreEstado, boolean requiereAccionDocente, boolean requiereAccionResolutor,
                    boolean esResolutorActual, List<String> accionesPermitidas, String perfilResolutorActual,
                    Instant fechaEscalamientoUtc, UUID estudianteId, String nombreEstudiante, String estadoMatricula,
                    UUID grupoId, int codigoGrupo, String nombreGrupo, UUID asignaturaId, String nombreAsignatura,
                    UUID docenteId, String nombreDocente, UUID programaId, String nombrePrograma,
                    BigDecimal porcentajeCreacion, BigDecimal porcentajeActual, BigDecimal umbralPerdida, BigDecimal horasFalta,
                    BigDecimal horasDictadas, Instant fechaCreacionUtc, String decisionDocente, String motivoDocente,
                    Instant fechaDecisionDocenteUtc, String decisionResolucion, String motivoResolucion, String nombreResolutor,
                    String perfilResolucion, Instant fechaResolucionUtc, String codigoPermanenciaActual) {
    /** Forma previa a CIERRE-W3I (sin el nivel de permanencia). */
    public RevisionFila(final UUID id, final String estado, final String nombreEstado, final boolean requiereAccionDocente, final boolean requiereAccionResolutor, final boolean esResolutorActual, final List<String> accionesPermitidas, final String perfilResolutorActual, final Instant fechaEscalamientoUtc, final UUID estudianteId, final String nombreEstudiante, final String estadoMatricula, final UUID grupoId, final int codigoGrupo, final String nombreGrupo, final UUID asignaturaId, final String nombreAsignatura, final UUID docenteId, final String nombreDocente, final UUID programaId, final String nombrePrograma, final BigDecimal porcentajeCreacion, final BigDecimal porcentajeActual, final BigDecimal umbralPerdida, final BigDecimal horasFalta, final BigDecimal horasDictadas, final Instant fechaCreacionUtc, final String decisionDocente, final String motivoDocente, final Instant fechaDecisionDocenteUtc, final String decisionResolucion, final String motivoResolucion, final String nombreResolutor, final String perfilResolucion, final Instant fechaResolucionUtc) {
        this(id, estado, nombreEstado, requiereAccionDocente, requiereAccionResolutor, esResolutorActual, accionesPermitidas, perfilResolutorActual, fechaEscalamientoUtc, estudianteId, nombreEstudiante, estadoMatricula, grupoId, codigoGrupo, nombreGrupo, asignaturaId, nombreAsignatura, docenteId, nombreDocente, programaId, nombrePrograma, porcentajeCreacion, porcentajeActual, umbralPerdida, horasFalta, horasDictadas, fechaCreacionUtc, decisionDocente, motivoDocente, fechaDecisionDocenteUtc, decisionResolucion, motivoResolucion, nombreResolutor, perfilResolucion, fechaResolucionUtc, null);
    }
}
