package co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.primaryports.dto.RegistrarDocenteDesdeUsuarioDTO;
import co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.primaryports.dto.RegistrarDocenteDesdeUsuarioResultadoDTO;
import co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.usecase.domain.RegistrarDocenteDesdeUsuarioDomain;
import co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.usecase.entity.RegistrarDocenteDesdeUsuarioResultadoEntity;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

/**
 * Mapper para convertir entre DTOs y modelos internos de registrar docente desde usuario.
 */
public final class RegistrarDocenteDesdeUsuarioMapper {

    private RegistrarDocenteDesdeUsuarioMapper() {
        throw new CrosscuttingException("No es permitido instanciar una clase utilitaria.");
    }

    public static RegistrarDocenteDesdeUsuarioDomain toDomain(final RegistrarDocenteDesdeUsuarioDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para registrar docente desde usuario es obligatorio.");
        }

        return RegistrarDocenteDesdeUsuarioDomain.crear(dto.getUsuario());
    }

    public static RegistrarDocenteDesdeUsuarioResultadoDTO toDTO(
            final RegistrarDocenteDesdeUsuarioResultadoEntity entity
    ) {
        if (ObjectHelper.isNull(entity)) {
            throw new CrosscuttingException("El resultado de registrar docente desde usuario es obligatorio.");
        }

        return new RegistrarDocenteDesdeUsuarioResultadoDTO(
                entity.getDocenteId(),
                entity.isExitoso(),
                entity.getMensajeUsuario()
        );
    }
}
