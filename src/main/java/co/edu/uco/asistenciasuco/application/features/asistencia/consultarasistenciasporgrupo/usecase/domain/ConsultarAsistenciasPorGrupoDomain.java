package co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.domain;


import co.edu.uco.asistenciasuco.application.features.grupo.exception.GrupoErrorCode;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

import java.util.UUID;

/**
 * Dominio de la operacion consultar asistencias por grupo.
 */
public final class ConsultarAsistenciasPorGrupoDomain {

    private final UUID grupo;
    private final UUID sesion;

    public ConsultarAsistenciasPorGrupoDomain(
            final UUID grupo,
            final UUID sesion
    ) {
        validarGrupo(grupo);

        this.grupo = grupo;
        this.sesion = sesion;
    }

    private void validarGrupo(final UUID grupo) {
        if (ObjectHelper.isNull(grupo)) {
            throw new ValidationException(GrupoErrorCode.ERR_GRUPO_REQUERIDO);
        }
    }

    public UUID getGrupo() {
        return grupo;
    }

    public UUID getSesion() {
        return sesion;
    }

}
