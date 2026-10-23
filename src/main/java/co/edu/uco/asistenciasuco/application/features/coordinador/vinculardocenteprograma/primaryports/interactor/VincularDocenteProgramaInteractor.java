package co.edu.uco.asistenciasuco.application.features.coordinador.vinculardocenteprograma.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.DocenteProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.mapper.DocenteProgramaMapper;
import co.edu.uco.asistenciasuco.application.features.coordinador.vinculardocenteprograma.primaryports.VincularDocenteProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.vinculardocenteprograma.primaryports.dto.VincularDocenteProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.vinculardocenteprograma.usecase.VincularDocenteProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.vinculardocenteprograma.usecase.domain.VincularDocenteProgramaDomain;

import java.util.Objects;

public final class VincularDocenteProgramaInteractor implements VincularDocenteProgramaInputPort {

    private final VincularDocenteProgramaUseCase useCase;

    public VincularDocenteProgramaInteractor(final VincularDocenteProgramaUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public DocenteProgramaDTO execute(final VincularDocenteProgramaDTO dto) {
        return DocenteProgramaMapper.toDTO(useCase.execute(new VincularDocenteProgramaDomain(
                dto.actorUsuarioId(), dto.idDocente(), dto.idUsuario(), dto.idTipoIdentificacion(),
                dto.numeroIdentificacion(), dto.primerNombre(), dto.segundoNombre(), dto.primerApellido(),
                dto.segundoApellido(), dto.correo(), dto.password(), dto.idTipoVinculacion())));
    }
}
