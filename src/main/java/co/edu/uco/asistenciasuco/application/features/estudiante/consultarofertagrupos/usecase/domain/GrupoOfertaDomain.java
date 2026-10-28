package co.edu.uco.asistenciasuco.application.features.estudiante.consultarofertagrupos.usecase.domain;

import java.time.LocalDate;
import java.util.UUID;

/**
 * estadoSolicitud: PENDIENTE o EN_ESPERA si el estudiante tiene una solicitud abierta; null si no.
 * tipoAsignatura: ESPECIFICA (propia del programa), BASICA o INSTITUCIONAL (transversales, {@code transversal = true}).
 */
public record GrupoOfertaDomain(
        UUID idGrupo,
        Integer codigoGrupo,
        String nombreGrupo,
        UUID idAsignatura,
        String codigoAsignatura,
        String nombreAsignatura,
        String nombreDocente,
        UUID idPeriodoAcademico,
        String nombrePeriodoAcademico,
        LocalDate fechaInicioPeriodo,
        LocalDate fechaFinPeriodo,
        String estadoPeriodo,
        String horario,
        int capacidadMaxima,
        int estudiantesActivos,
        int cuposDisponibles,
        boolean matriculado,
        UUID idSolicitudAbierta,
        String estadoSolicitud,
        String tipoAsignatura,
        boolean transversal
) {
}
