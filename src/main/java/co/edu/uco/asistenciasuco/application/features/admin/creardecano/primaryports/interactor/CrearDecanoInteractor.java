package co.edu.uco.asistenciasuco.application.features.admin.creardecano.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.usuario.correoinstitucional.CorreoInstitucionalPolicy;
import co.edu.uco.asistenciasuco.application.features.admin.creardecano.primaryports.CrearDecanoInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.creardecano.primaryports.dto.CrearDecanoDTO;
import co.edu.uco.asistenciasuco.application.features.admin.creardecano.primaryports.dto.CrearDecanoResultadoDTO;
import co.edu.uco.asistenciasuco.application.features.admin.creardecano.primaryports.mapper.CrearDecanoMapper;
import co.edu.uco.asistenciasuco.application.features.admin.creardecano.usecase.CrearDecanoUseCase;

import java.util.Objects;

public final class CrearDecanoInteractor implements CrearDecanoInputPort {
    private final CrearDecanoUseCase useCase;
    private final CorreoInstitucionalPolicy correoInstitucional;
    public CrearDecanoInteractor(final CrearDecanoUseCase useCase, final CorreoInstitucionalPolicy correoInstitucional) {
        this.useCase = Objects.requireNonNull(useCase, "CrearDecanoUseCase es obligatorio.");
        this.correoInstitucional = Objects.requireNonNull(correoInstitucional, "CorreoInstitucionalPolicy es obligatorio.");
    }
    @Override
    public CrearDecanoResultadoDTO execute(final CrearDecanoDTO dto) {
        // DEC-073: decanos, coordinadores y docentes usan el correo institucional (VCE_008). Sin correo (p. ej. vincular
        // un docente que ya existe) no hay nada que validar.
        if (dto != null && dto.correo() != null && !dto.correo().isBlank()) {
            correoInstitucional.exigirPersonal(dto.correo());
        }
        return CrearDecanoMapper.toResultadoDto(useCase.execute(CrearDecanoMapper.toDomain(dto)));
    }
}
