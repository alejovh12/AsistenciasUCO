package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupov2.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.grupo.exception.GrupoErrorCode;
import co.edu.uco.asistenciasuco.application.features.usuario.exception.UsuarioErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.UUID;

/**
 * Dominio del listado v2 de sesiones de un grupo.
 */
public final class ConsultarSesionesPorGrupoV2Domain {

    private final UUID grupo;
    private final UUID usuarioEjecutor;

    public ConsultarSesionesPorGrupoV2Domain(final UUID grupo, final UUID usuarioEjecutor) {
        if (ObjectHelper.isNull(grupo)) {
            throw new ValidationException(GrupoErrorCode.ERR_GRUPO_REQUERIDO);
        }
        if (ObjectHelper.isNull(usuarioEjecutor)) {
            throw new ValidationException(UsuarioErrorCode.ERR_USUARIO_REQUERIDO);
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
