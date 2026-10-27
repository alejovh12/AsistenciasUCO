package co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.primaryports.ConsultarKpisInstitucionInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.primaryports.dto.ConsultarKpisInstitucionDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.primaryports.dto.KpisInstitucionDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.primaryports.mapper.ConsultarKpisInstitucionMapper;
import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.usecase.ConsultarKpisInstitucionUseCase;
import java.util.Objects;

public class ConsultarKpisInstitucionInteractor implements ConsultarKpisInstitucionInputPort {

    private final ConsultarKpisInstitucionUseCase useCase;

    public ConsultarKpisInstitucionInteractor(final ConsultarKpisInstitucionUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public KpisInstitucionDTO execute(final ConsultarKpisInstitucionDTO dto) {
        return ConsultarKpisInstitucionMapper.toDto(useCase.execute(ConsultarKpisInstitucionMapper.toDomain(dto)));
    }
}
