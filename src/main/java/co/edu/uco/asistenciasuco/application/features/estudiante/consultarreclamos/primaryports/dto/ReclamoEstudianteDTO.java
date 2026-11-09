package co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.primaryports.dto;

import co.edu.uco.asistenciasuco.application.features.asistencia.common.dto.CategoriaReclamoDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.dto.SoporteReclamoDTO;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Solicitud de revision propia del estudiante: los mismos campos de la bandeja docente sin los datos del estudiante.
 * {@code fechaSesionUtc} es un instante UTC (nulo si la hora historica de la sesion no tiene procedencia verificable).
 * CULM-001 D.2: {@code categoria} (nula en reclamos anteriores a la categoria persistida) y {@code soportes} (todos, en orden de
 * adjuncion). {@code soporteAdjunto} (el primero) queda deprecado una version.
 */
public record ReclamoEstudianteDTO(
        UUID id,
        String nombre,
        LocalDate fechaSolicitud,
        String estadoSolicitud,
        String justificacionSolicitud,
        String justificacionRespuesta,
        UUID sesionId,
        Integer sesionNumero,
        Instant fechaSesionUtc,
        String procedenciaTemporalSesion,
        UUID grupoId,
        Integer grupoCodigo,
        String grupoNombre,
        UUID materiaId,
        String materiaCodigo,
        String materiaNombre,
        SoporteAdjuntoDTO soporteAdjunto,
        CategoriaReclamoDTO categoria,
        List<SoporteReclamoDTO> soportes
) {

    /** Soporte vinculado: se descarga SIEMPRE por el backend autenticado ({@code url} relativa), nunca por el storage. */
    public record SoporteAdjuntoDTO(
            UUID fileId,
            String nombre,
            String tipo,
            long tamanioBytes,
            Instant fechaSubidaUtc,
            String url
    ) {
    }
}
