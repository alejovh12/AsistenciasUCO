package co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.primaryports.RegistrarDocenteDesdeUsuarioInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.primaryports.dto.RegistrarDocenteDesdeUsuarioDTO;
import co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.primaryports.dto.RegistrarDocenteDesdeUsuarioResultadoDTO;
import co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.primaryports.mapper.RegistrarDocenteDesdeUsuarioMapper;
import co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.usecase.RegistrarDocenteDesdeUsuarioUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.usecase.domain.RegistrarDocenteDesdeUsuarioDomain;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

/**
 * Interactor del puerto de entrada para registrar docente desde usuario.
 */
public final class RegistrarDocenteDesdeUsuarioInteractor implements RegistrarDocenteDesdeUsuarioInputPort {

    private final RegistrarDocenteDesdeUsuarioUseCase useCase;

    public RegistrarDocenteDesdeUsuarioInteractor(final RegistrarDocenteDesdeUsuarioUseCase useCase) {
        if (ObjectHelper.isNull(useCase)) {
            throw new CrosscuttingException("El caso de uso RegistrarDocenteDesdeUsuarioUseCase es obligatorio.");
        }
        this.useCase = useCase;
    }

    @Override
    public RegistrarDocenteDesdeUsuarioResultadoDTO execute(final RegistrarDocenteDesdeUsuarioDTO dto) {
        final RegistrarDocenteDesdeUsuarioDomain domain = RegistrarDocenteDesdeUsuarioMapper.toDomain(dto);
        return RegistrarDocenteDesdeUsuarioMapper.toDTO(useCase.execute(domain));
    }
}
