package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.mapper;

import co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.primaryports.dto.ActualizarSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.primaryports.dto.CerrarSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.dto.ConsultarSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.dto.CrearSesionDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.request.CerrarSesionRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.request.ConsultarSesionRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.request.CrearSesionRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.request.ActualizarSesionRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.HttpTemporalParser;

import java.util.Objects;
import java.util.UUID;

public final class SesionHttpMapper {

    private SesionHttpMapper() {
    }

    public static CrearSesionDTO toApplicationDTO(final CrearSesionRequest request, final UUID usuarioEjecutor) {
        Objects.requireNonNull(request, "El request HTTP para crear sesion es obligatorio.");
        return new CrearSesionDTO(
                request.getGrupo(),
                request.getNombre(),
                HttpTemporalParser.parseLocalDateTime(request.getFechaHoraInicio(), "fechaHoraInicio"),
                HttpTemporalParser.parseLocalDateTime(request.getFechaHoraFin(), "fechaHoraFin"),
                usuarioEjecutor
        );
    }

    public static ConsultarSesionDTO toApplicationDTO(final ConsultarSesionRequest request) {
        Objects.requireNonNull(request, "El request HTTP para consultar sesion es obligatorio.");
        return new ConsultarSesionDTO(request.getSesion());
    }

    public static CerrarSesionDTO toApplicationDTO(final CerrarSesionRequest request, final UUID usuarioEjecutor) {
        Objects.requireNonNull(request, "El request HTTP para cerrar sesion es obligatorio.");
        return new CerrarSesionDTO(request.getSesion(), usuarioEjecutor, request.getObservacionCierre(), usuarioEjecutor);
    }

    public static ActualizarSesionDTO toApplicationDTO(
            final UUID sesionId,
            final ActualizarSesionRequest request,
            final UUID usuarioEjecutor
    ) {
        Objects.requireNonNull(request, "El request HTTP para actualizar sesion es obligatorio.");
        return new ActualizarSesionDTO(
                sesionId,
                request.getNombre(),
                HttpTemporalParser.parseLocalDateTime(request.getFechaHoraInicio(), "fechaHoraInicio"),
                HttpTemporalParser.parseLocalDateTime(request.getFechaHoraFin(), "fechaHoraFin"),
                usuarioEjecutor
        );
    }
}
