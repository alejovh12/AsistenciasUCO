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
                    String perfilResolucion, Instant fechaResolucionUtc) {
}
