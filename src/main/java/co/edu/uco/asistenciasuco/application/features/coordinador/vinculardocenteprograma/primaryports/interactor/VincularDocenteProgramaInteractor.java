package co.edu.uco.asistenciasuco.application.features.coordinador.vinculardocenteprograma.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.usuario.correoinstitucional.CorreoInstitucionalPolicy;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.DocenteProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.mapper.DocenteProgramaMapper;
import co.edu.uco.asistenciasuco.application.features.coordinador.vinculardocenteprograma.primaryports.VincularDocenteProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.vinculardocenteprograma.primaryports.dto.VincularDocenteProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.vinculardocenteprograma.usecase.VincularDocenteProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.vinculardocenteprograma.usecase.domain.VincularDocenteProgramaDomain;

import java.util.Objects;

public final class VincularDocenteProgramaInteractor implements VincularDocenteProgramaInputPort {

    private final VincularDocenteProgramaUseCase useCase;
    private final CorreoInstitucionalPolicy correoInstitucional;

    public VincularDocenteProgramaInteractor(final VincularDocenteProgramaUseCase useCase, final CorreoInstitucionalPolicy correoInstitucional) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
        this.correoInstitucional = Objects.requireNonNull(correoInstitucional, "CorreoInstitucionalPolicy es obligatorio.");
    }

    @Override
    public DocenteProgramaDTO execute(final VincularDocenteProgramaDTO dto) {
        // DEC-073: decanos, coordinadores y docentes usan el correo institucional (VCE_008). Sin correo (p. ej. vincular
        // un docente que ya existe) no hay nada que validar.
        if (dto != null && dto.correo() != null && !dto.correo().isBlank()) {
            correoInstitucional.exigirPersonal(dto.correo());
        }
        return DocenteProgramaMapper.toDTO(useCase.execute(new VincularDocenteProgramaDomain(
                dto.actorUsuarioId(), dto.idDocente(), dto.idUsuario(), dto.idTipoIdentificacion(),
                dto.numeroIdentificacion(), dto.primerNombre(), dto.segundoNombre(), dto.primerApellido(),
                dto.segundoApellido(), dto.correo(), dto.password(), dto.idTipoVinculacion())));
    }
}
