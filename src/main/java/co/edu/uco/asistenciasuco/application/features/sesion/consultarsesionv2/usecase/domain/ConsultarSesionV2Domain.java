package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionv2.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.sesion.exception.SesionErrorCode;
import co.edu.uco.asistenciasuco.application.features.usuario.exception.UsuarioErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.UUID;

/**
 * Dominio de la consulta v2 de una sesion.
 */
public final class ConsultarSesionV2Domain {

    private final UUID sesion;
    private final UUID usuarioEjecutor;

    public ConsultarSesionV2Domain(final UUID sesion, final UUID usuarioEjecutor) {
        if (ObjectHelper.isNull(sesion)) {
            throw new ValidationException(SesionErrorCode.ERR_SESION_REQUERIDA);
        }
        if (ObjectHelper.isNull(usuarioEjecutor)) {
            throw new ValidationException(UsuarioErrorCode.ERR_USUARIO_REQUERIDO);
        }
        this.sesion = sesion;
        this.usuarioEjecutor = usuarioEjecutor;
    }

    public UUID getSesion() {
        return sesion;
    }

    public UUID getUsuarioEjecutor() {
        return usuarioEjecutor;
    }
}
