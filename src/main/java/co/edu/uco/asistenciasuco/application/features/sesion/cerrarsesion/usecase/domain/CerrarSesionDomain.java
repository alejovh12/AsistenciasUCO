package co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.usecase.domain;


import co.edu.uco.asistenciasuco.application.features.sesion.exception.SesionErrorCode;
import co.edu.uco.asistenciasuco.application.features.usuario.exception.UsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import co.edu.uco.asistenciasuco.crosscutting.util.TextHelper;

import java.util.UUID;

/**
 * Dominio de la operacion cerrar sesion.
 */
public final class CerrarSesionDomain {

    private final UUID sesion;
    private final UUID docente;
    private final String observacionCierre;
    private final UUID usuarioEjecutor;

    public CerrarSesionDomain(
            final UUID sesion,
            final UUID docente,
            final String observacionCierre,
            final UUID usuarioEjecutor
    ) {
        validarSesion(sesion);
        validarDocente(docente);
        validarUsuarioEjecutor(usuarioEjecutor);
        this.observacionCierre = validarObservacionCierre(observacionCierre);

        this.docente = docente;
        this.usuarioEjecutor = usuarioEjecutor;
        this.sesion = sesion;
    }

    private void validarSesion(final UUID sesion) {
        if (ObjectHelper.isNull(sesion)) {
            throw new ValidationException(SesionErrorCode.ERR_SESION_REQUERIDA);
        }
    }

    private void validarDocente(final UUID docente) {
        if (ObjectHelper.isNull(docente)) {
            throw new ValidationException(SesionErrorCode.ERR_DOCENTE_REQUERIDO);
        }
    }

    private void validarUsuarioEjecutor(final UUID usuarioEjecutor) {
        if (ObjectHelper.isNull(usuarioEjecutor)) {
            throw new ValidationException(UsuarioErrorCode.ERR_USUARIO_REQUERIDO);
        }
    }

    private String validarObservacionCierre(final String observacionCierre) {
        final String observacionNormalizada = TextHelper.trim(observacionCierre);

        if (TextHelper.isNullOrBlank(observacionNormalizada)) {
            throw new ValidationException(SesionErrorCode.ERR_OBSERVACION_CIERRE_REQUERIDA);
        }

        if (!TextHelper.hasLengthBetween(observacionNormalizada, 10, 250)) {
            throw new ValidationException(SesionErrorCode.ERR_OBSERVACION_CIERRE_LONGITUD_INVALIDA);
        }

        return observacionNormalizada;
    }

    public UUID getSesion() {
        return sesion;
    }

    public UUID getDocente() {
        return docente;
    }

    public String getObservacionCierre() {
        return observacionCierre;
    }

    public UUID getUsuarioEjecutor() {
        return usuarioEjecutor;
    }

}
