package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.mapper;

import co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.primaryports.dto.AsignarDocenteAGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.primaryports.dto.ConsultarAsignacionesAcademicasDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.primaryports.dto.ConsultarDocentePorIdDTO;
import co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.primaryports.dto.RegistrarDocenteDesdeUsuarioDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.request.AsignarDocenteAGrupoRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.request.ConsultarAsignacionesAcademicasDocenteRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.request.ConsultarDocentePorIdRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.request.RegistrarDocenteDesdeUsuarioRequest;

import java.util.Objects;

public final class DocenteHttpMapper {

    private DocenteHttpMapper() {
    }

    public static ConsultarDocentePorIdDTO toApplicationDTO(final ConsultarDocentePorIdRequest request) {
        Objects.requireNonNull(request, "El request HTTP para consultar docente por id es obligatorio.");
        return new ConsultarDocentePorIdDTO(request.getDocente());
    }

    public static ConsultarAsignacionesAcademicasDocenteDTO toApplicationDTO(
            final ConsultarAsignacionesAcademicasDocenteRequest request
    ) {
        Objects.requireNonNull(request, "El request HTTP para consultar asignaciones de docente es obligatorio.");
        return new ConsultarAsignacionesAcademicasDocenteDTO(request.getDocente());
    }

    public static RegistrarDocenteDesdeUsuarioDTO toApplicationDTO(final RegistrarDocenteDesdeUsuarioRequest request) {
        Objects.requireNonNull(request, "El request HTTP para registrar docente desde usuario es obligatorio.");
        return new RegistrarDocenteDesdeUsuarioDTO(request.getUsuario());
    }

    public static AsignarDocenteAGrupoDTO toApplicationDTO(final AsignarDocenteAGrupoRequest request) {
        Objects.requireNonNull(request, "El request HTTP para asignar docente a grupo es obligatorio.");
        return new AsignarDocenteAGrupoDTO(request.getDocente(), request.getGrupo());
    }
}
