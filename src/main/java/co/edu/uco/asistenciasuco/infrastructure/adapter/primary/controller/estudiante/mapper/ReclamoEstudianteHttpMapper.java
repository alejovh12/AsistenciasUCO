package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante.mapper;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.primaryports.dto.ReclamoEstudianteDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante.response.ReclamoEstudianteResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.HttpUtcInstantCodec;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** Adapta el DTO de Application a la representacion HTTP, codificando los instantes UTC con el codec UTC v2. */
public final class ReclamoEstudianteHttpMapper {

    private ReclamoEstudianteHttpMapper() {
    }

    public static ReclamoEstudianteResponse toResponse(final ReclamoEstudianteDTO dto) {
        final ReclamoEstudianteDTO.SoporteAdjuntoDTO soporte = dto.soporteAdjunto();
        return new ReclamoEstudianteResponse(
                dto.id(),
                dto.nombre(),
                dto.fechaSolicitud(),
                dto.estadoSolicitud(),
                dto.justificacionSolicitud(),
                dto.justificacionRespuesta(),
                dto.sesionId(),
                dto.sesionNumero(),
                utc(dto.fechaSesionUtc()),
                dto.procedenciaTemporalSesion(),
                dto.grupoId(),
                dto.grupoCodigo(),
                dto.grupoNombre(),
                dto.materiaId(),
                dto.materiaCodigo(),
                dto.materiaNombre(),
                soporte == null ? null : new ReclamoEstudianteResponse.SoporteAdjuntoResponse(
                        soporte.fileId(), soporte.nombre(), soporte.tipo(), soporte.tamanioBytes(),
                        utc(soporte.fechaSubidaUtc()), soporte.url())
        );
    }

    /** Instant UTC -> {@code yyyy-MM-ddTHH:mm:ss[.fffffff]Z} sin pasar por la zona de la JVM. */
    private static String utc(final Instant instant) {
        return instant == null
                ? null
                : HttpUtcInstantCodec.fromUtcDatabaseDateTime(LocalDateTime.ofInstant(instant, ZoneOffset.UTC));
    }
}
