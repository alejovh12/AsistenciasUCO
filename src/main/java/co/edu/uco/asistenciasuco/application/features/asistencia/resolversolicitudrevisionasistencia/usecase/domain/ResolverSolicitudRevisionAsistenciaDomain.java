package co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.asistencia.exception.AsistenciaErrorCode;
import co.edu.uco.asistenciasuco.application.features.usuario.exception.UsuarioErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import co.edu.uco.asistenciasuco.crosscutting.util.TextHelper;

import java.util.UUID;

public final class ResolverSolicitudRevisionAsistenciaDomain {

    private final UUID solicitud;
    private final String accion;
    private final String respuestaDocente;
    private final UUID usuario;

    public ResolverSolicitudRevisionAsistenciaDomain(
            final UUID solicitud,
            final String accion,
            final String respuestaDocente,
            final UUID usuario
    ) {
        if (ObjectHelper.isNull(solicitud)) {
            throw new ValidationException(AsistenciaErrorCode.ERR_SOLICITUD_REVISION_REQUERIDA);
        }
        if (ObjectHelper.isNull(usuario)) {
            throw new ValidationException(UsuarioErrorCode.ERR_USUARIO_REQUERIDO);
        }
        final String accionNormalizada = TextHelper.trim(accion);
        if (TextHelper.isNullOrBlank(accionNormalizada)) {
            throw new ValidationException(AsistenciaErrorCode.ERR_ACCION_REVISION_REQUERIDA);
        }
        this.solicitud = solicitud;
        this.accion = accionNormalizada;
        this.respuestaDocente = TextHelper.trim(respuestaDocente);
        this.usuario = usuario;
    }

    public UUID getSolicitud() {
        return solicitud;
    }

    public String getAccion() {
        return accion;
    }

    public String getRespuestaDocente() {
        return respuestaDocente;
    }

    public UUID getUsuario() {
        return usuario;
    }
}
