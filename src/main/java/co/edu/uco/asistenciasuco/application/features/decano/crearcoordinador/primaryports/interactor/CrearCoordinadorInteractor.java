package co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.usuario.correoinstitucional.CorreoInstitucionalPolicy;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.primaryports.CrearCoordinadorInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.primaryports.dto.CrearCoordinadorDTO;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.primaryports.dto.CrearCoordinadorResultadoDTO;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.primaryports.mapper.CrearCoordinadorMapper;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.usecase.CrearCoordinadorUseCase;
import java.util.Objects;

public final class CrearCoordinadorInteractor implements CrearCoordinadorInputPort {

    private final CrearCoordinadorUseCase useCase;
    private final CorreoInstitucionalPolicy correoInstitucional;

    public CrearCoordinadorInteractor(final CrearCoordinadorUseCase useCase, final CorreoInstitucionalPolicy correoInstitucional) {
        this.useCase = Objects.requireNonNull(useCase, "CrearCoordinadorUseCase es obligatorio.");
        this.correoInstitucional = Objects.requireNonNull(correoInstitucional, "CorreoInstitucionalPolicy es obligatorio.");
    }

    @Override
    public CrearCoordinadorResultadoDTO execute(final CrearCoordinadorDTO dto) {
        // DEC-073: decanos, coordinadores y docentes usan el correo institucional (VCE_008). Sin correo (p. ej. vincular
        // un docente que ya existe) no hay nada que validar.
        if (dto != null && dto.correo() != null && !dto.correo().isBlank()) {
            correoInstitucional.exigirPersonal(dto.correo());
        }
        return CrearCoordinadorMapper.toResultadoDto(useCase.execute(CrearCoordinadorMapper.toDomain(dto)));
    }
}
