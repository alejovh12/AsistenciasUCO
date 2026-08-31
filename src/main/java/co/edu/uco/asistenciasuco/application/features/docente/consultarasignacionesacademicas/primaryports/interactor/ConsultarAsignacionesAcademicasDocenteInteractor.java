package co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.primaryports.ConsultarAsignacionesAcademicasDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.primaryports.dto.ConsultarAsignacionesAcademicasDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.primaryports.dto.DocenteAsignacionAcademicaDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.primaryports.mapper.ConsultarAsignacionesAcademicasDocenteMapper;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.usecase.ConsultarAsignacionesAcademicasDocenteUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.usecase.domain.ConsultarAsignacionesAcademicasDocenteDomain;

import java.util.List;
import java.util.Objects;

/**
 * Interactor del puerto de entrada para consultar asignaciones academicas de un docente.
 */
public final class ConsultarAsignacionesAcademicasDocenteInteractor
        implements ConsultarAsignacionesAcademicasDocenteInputPort {

    private final ConsultarAsignacionesAcademicasDocenteUseCase useCase;

    public ConsultarAsignacionesAcademicasDocenteInteractor(
            final ConsultarAsignacionesAcademicasDocenteUseCase useCase
    ) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso ConsultarAsignacionesAcademicasDocenteUseCase es obligatorio.");
    }

    @Override
    public List<DocenteAsignacionAcademicaDTO> execute(final ConsultarAsignacionesAcademicasDocenteDTO dto) {
        final ConsultarAsignacionesAcademicasDocenteDomain domain =
                ConsultarAsignacionesAcademicasDocenteMapper.toDomain(dto);
        return ConsultarAsignacionesAcademicasDocenteMapper.toDTOs(useCase.execute(domain));
    }
}