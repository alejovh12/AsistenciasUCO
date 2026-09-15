package co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciaautonoma.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.asistencia.exception.AsistenciaErrorCode;
import co.edu.uco.asistenciasuco.application.features.usuario.exception.UsuarioErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import co.edu.uco.asistenciasuco.crosscutting.util.TextHelper;

import java.util.UUID;

public final class RegistrarAsistenciaAutonomaDomain {

    private final UUID sesion;
    private final String codigoVerificacion;
    private final UUID usuario;

    public RegistrarAsistenciaAutonomaDomain(final UUID sesion, final String codigoVerificacion, final UUID usuario) {
        if (ObjectHelper.isNull(sesion)) {
            throw new ValidationException(AsistenciaErrorCode.ERR_SESION_ASISTENCIA_REQUERIDA);
        }
        if (ObjectHelper.isNull(usuario)) {
            throw new ValidationException(UsuarioErrorCode.ERR_USUARIO_REQUERIDO);
        }
        final String codigoNormalizado = TextHelper.trim(codigoVerificacion);
        if (TextHelper.isNullOrBlank(codigoNormalizado)) {
            throw new ValidationException(AsistenciaErrorCode.ERR_CODIGO_VERIFICACION_REQUERIDO);
        }
        this.sesion = sesion;
        this.codigoVerificacion = codigoNormalizado;
        this.usuario = usuario;
    }

    public UUID getSesion() {
        return sesion;
    }

    public String getCodigoVerificacion() {
        return codigoVerificacion;
    }

    public UUID getUsuario() {
        return usuario;
    }
}
