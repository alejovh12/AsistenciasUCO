package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.mapper;

import co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.primaryports.dto.CerrarSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.dto.ConsultarSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.dto.CrearSesionDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.request.CerrarSesionRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.request.ConsultarSesionRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.request.CrearSesionRequest;

import java.util.Objects;

public final class SesionHttpMapper {

    private SesionHttpMapper() {
    }

    public static CrearSesionDTO toApplicationDTO(final CrearSesionRequest request) {
        Objects.requireNonNull(request, "El request HTTP para crear sesion es obligatorio.");
        return new CrearSesionDTO(request.getGrupo(), request.getTema(), request.getDescripcion());
    }

    public static ConsultarSesionDTO toApplicationDTO(final ConsultarSesionRequest request) {
        Objects.requireNonNull(request, "El request HTTP para consultar sesion es obligatorio.");
        return new ConsultarSesionDTO(request.getSesion());
    }

    public static CerrarSesionDTO toApplicationDTO(final CerrarSesionRequest request) {
        Objects.requireNonNull(request, "El request HTTP para cerrar sesion es obligatorio.");
        return new CerrarSesionDTO(request.getSesion(), request.getObservacionCierre());
    }
}
