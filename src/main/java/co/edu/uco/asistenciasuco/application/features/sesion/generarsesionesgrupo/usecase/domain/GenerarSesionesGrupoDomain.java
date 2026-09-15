package co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.grupo.exception.GrupoErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.UUID;

public final class GenerarSesionesGrupoDomain {

    private final UUID grupo;

    public GenerarSesionesGrupoDomain(final UUID grupo) {
        if (ObjectHelper.isNull(grupo)) {
            throw new ValidationException(GrupoErrorCode.ERR_GRUPO_REQUERIDO);
        }
        this.grupo = grupo;
    }

    public UUID getGrupo() {
        return grupo;
    }
}
