package co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.ResultadoAprobacionReclamo;
import co.edu.uco.asistenciasuco.application.features.asistencia.exception.AsistenciaErrorCode;
import co.edu.uco.asistenciasuco.application.features.usuario.exception.UsuarioErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import co.edu.uco.asistenciasuco.crosscutting.util.TextHelper;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class ResolverSolicitudRevisionAsistenciaDomain {

    private static final Set<String> ACCIONES_CONTRACTUALES = Set.of("APROBADA", "RECHAZADA");
    static final int RESPUESTA_RECHAZO_MINIMO = 10;
    static final int RESPUESTA_MAXIMO = 500;

    private final UUID solicitud;
    private final String accion;
    private final String respuestaDocente;
    private final UUID usuario;
    private final ResultadoAprobacionReclamo resultado;

    public ResolverSolicitudRevisionAsistenciaDomain(
            final UUID solicitud,
            final String accion,
            final String respuestaDocente,
            final UUID usuario
    ) {
        this(solicitud, accion, respuestaDocente, null, usuario);
    }

    /**
     * PC (puntos de control): {@code resultado} (PRESENTE o JUSTIFICADA) solo cuenta con APROBADA; nulo o vacio deja que la base de datos
     * aplique JUSTIFICADA. Un valor distinto es 400. Con RECHAZADA se ignora.
     */
    public ResolverSolicitudRevisionAsistenciaDomain(
            final UUID solicitud,
            final String accion,
            final String respuestaDocente,
            final String resultado,
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
        // SES-04: solo APROBADA (justifica la inasistencia: EX, asistio = 0) o RECHAZADA (conserva la asistencia).
        final String accionContractual = accionNormalizada.toUpperCase(Locale.ROOT);
        if (!ACCIONES_CONTRACTUALES.contains(accionContractual)) {
            throw new ValidationException(AsistenciaErrorCode.ERR_ACCION_REVISION_INVALIDA);
        }
        final String respuestaNormalizada = TextHelper.trim(respuestaDocente);
        if (!respuestaValida(accionContractual, respuestaNormalizada)) {
            throw new ValidationException(AsistenciaErrorCode.ERR_RESPUESTA_RECLAMO_LONGITUD_INVALIDA);
        }
        this.solicitud = solicitud;
        this.accion = accionContractual;
        this.respuestaDocente = respuestaNormalizada;
        this.resultado = "APROBADA".equals(accionContractual) ? resultadoValido(resultado) : null;
        this.usuario = usuario;
    }

    /** DEC-073: al rechazar, la respuesta (sin espacios laterales) va de 10 a 500 caracteres; al aprobar es opcional, hasta 500. */
    private static boolean respuestaValida(final String accion, final String respuesta) {
        final int largo = respuesta == null ? 0 : respuesta.length();
        if (largo > RESPUESTA_MAXIMO) {
            return false;
        }
        return !"RECHAZADA".equals(accion) || largo >= RESPUESTA_RECHAZO_MINIMO;
    }

    private static ResultadoAprobacionReclamo resultadoValido(final String resultado) {
        if (TextHelper.isNullOrBlank(resultado)) {
            return null;
        }
        return ResultadoAprobacionReclamo.desdeNombre(resultado)
                .orElseThrow(() -> new ValidationException(AsistenciaErrorCode.ERR_RESULTADO_APROBACION_INVALIDO));
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

    /** PC: resultado de aprobar (PRESENTE o JUSTIFICADA); nulo si no se indico o la accion es RECHAZADA. */
    public String getResultado() {
        return resultado == null ? null : resultado.name();
    }

    /** Codigo de razon para {@code @codigoResultadoAprobacion} (AN o EX); nulo deja el valor por defecto de la base de datos. */
    public String getCodigoResultadoAprobacion() {
        return resultado == null ? null : resultado.codigo();
    }

    public UUID getUsuario() {
        return usuario;
    }
}
