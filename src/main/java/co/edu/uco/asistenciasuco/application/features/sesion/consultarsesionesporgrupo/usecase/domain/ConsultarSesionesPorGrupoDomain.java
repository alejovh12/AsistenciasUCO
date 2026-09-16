package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupo.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.grupo.exception.GrupoErrorCode;
import co.edu.uco.asistenciasuco.application.features.sesion.exception.SesionErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.UUID;

/**
 * Dominio de la operacion consultar sesiones por grupo.
 */
public final class ConsultarSesionesPorGrupoDomain {

    private final UUID grupo;
    private final UUID usuarioEjecutor;

    public ConsultarSesionesPorGrupoDomain(final UUID grupo, final UUID usuarioEjecutor) {
        if (ObjectHelper.isNull(grupo)) {
            throw new ValidationException(GrupoErrorCode.ERR_GRUPO_REQUERIDO);
        }
        if (ObjectHelper.isNull(usuarioEjecutor)) {
            throw new ValidationException(SesionErrorCode.ERR_DOCENTE_REQUERIDO);
        }
        this.grupo = grupo;
        this.usuarioEjecutor = usuarioEjecutor;
    }

    public UUID getGrupo() {
        return grupo;
    }

    public UUID getUsuarioEjecutor() {
        return usuarioEjecutor;
    }
}
