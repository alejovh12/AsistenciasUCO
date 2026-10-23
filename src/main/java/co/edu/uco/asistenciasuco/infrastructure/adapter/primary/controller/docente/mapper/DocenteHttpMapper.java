package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.mapper;

import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.primaryports.dto.ConsultarAsignacionesAcademicasDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.primaryports.dto.ConsultarDocentePorIdDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.request.ConsultarAsignacionesAcademicasDocenteRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.request.ConsultarDocentePorIdRequest;

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


}
