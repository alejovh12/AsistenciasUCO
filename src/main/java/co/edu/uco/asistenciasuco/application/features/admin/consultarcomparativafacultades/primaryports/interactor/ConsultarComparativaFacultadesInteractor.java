package co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.primaryports.ConsultarComparativaFacultadesInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.primaryports.dto.ConsultarComparativaFacultadesDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.primaryports.dto.ComparativaFacultadDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.primaryports.mapper.ConsultarComparativaFacultadesMapper;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.usecase.ConsultarComparativaFacultadesUseCase;
import java.util.List;
import java.util.Objects;

public class ConsultarComparativaFacultadesInteractor implements ConsultarComparativaFacultadesInputPort {

    private final ConsultarComparativaFacultadesUseCase useCase;

    public ConsultarComparativaFacultadesInteractor(final ConsultarComparativaFacultadesUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public List<ComparativaFacultadDTO> execute(final ConsultarComparativaFacultadesDTO dto) {
        return ConsultarComparativaFacultadesMapper.toDto(useCase.execute(ConsultarComparativaFacultadesMapper.toDomain(dto)));
    }
}
