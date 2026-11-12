package co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.usecase.domain;


import co.edu.uco.asistenciasuco.application.features.asistencia.exception.AsistenciaErrorCode;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.usuario.exception.UsuarioErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import co.edu.uco.asistenciasuco.crosscutting.util.TextHelper;

import java.util.List;
import java.util.UUID;

/**
 * Dominio de la operacion solicitar revision de asistencia.
 */
public final class SolicitarRevisionAsistenciaDomain {

    private final UUID sesion;
    private final String categoria;
    private final String justificacion;
    private final List<UUID> soportes;
    private final UUID usuario;
    private final UUID toma;

    /** Reclamo de la sesion (legado, sin toma). */
    public SolicitarRevisionAsistenciaDomain(
            final UUID sesion,
            final String categoria,
            final String justificacion,
            final List<UUID> soportes,
            final UUID usuario
    ) {
        this(sesion, null, categoria, justificacion, soportes, usuario);
    }

    /** PC (puntos de control): {@code toma} nula es el reclamo de la sesion; con toma, el reclamo es de esa toma. */
    public SolicitarRevisionAsistenciaDomain(
            final UUID sesion,
            final UUID toma,
            final String categoria,
            final String justificacion,
            final List<UUID> soportes,
            final UUID usuario
    ) {
        validarSesion(sesion);
        validarUsuario(usuario);
        this.categoria = validarTextoObligatorio(categoria, AsistenciaErrorCode.ERR_CATEGORIA_REVISION_REQUERIDA);
        this.justificacion = validarTextoObligatorio(justificacion, AsistenciaErrorCode.ERR_JUSTIFICACION_REVISION_REQUERIDA);
        this.soportes = validarSoportes(soportes);

        this.sesion = sesion;
        this.toma = toma;
        this.usuario = usuario;
    }

    private void validarSesion(final UUID sesion) {
        if (ObjectHelper.isNull(sesion)) {
            throw new ValidationException(AsistenciaErrorCode.ERR_SESION_ASISTENCIA_REQUERIDA);
        }
    }

    private void validarUsuario(final UUID usuario) {
        if (ObjectHelper.isNull(usuario)) {
            throw new ValidationException(UsuarioErrorCode.ERR_USUARIO_REQUERIDO);
        }
    }

    private List<UUID> validarSoportes(final List<UUID> soportes) {
        if (ObjectHelper.isNull(soportes)) {
            return List.of();
        }
        if (soportes.stream().anyMatch(ObjectHelper::isNull)) {
            throw new ValidationException(AsistenciaErrorCode.ERR_SOPORTE_RECLAMO_INVALIDO);
        }
        return List.copyOf(soportes);
    }

    private String validarTextoObligatorio(final String valor, final AsistenciaErrorCode errorCode) {
        final String normalizado = TextHelper.trim(valor);
        if (TextHelper.isNullOrBlank(normalizado)) {
            throw new ValidationException(errorCode);
        }
        if (!TextHelper.hasLengthBetween(normalizado, 1, 300)) {
            throw new ValidationException(AsistenciaErrorCode.ERR_MOTIVO_REVISION_LONGITUD_INVALIDA);
        }
        return normalizado;
    }

    public UUID getSesion() {
        return sesion;
    }

    /** PC (puntos de control): toma reclamada; nulo es el reclamo de la sesion completa (legado). */
    public UUID getToma() {
        return toma;
    }

    public String getCategoria() {
        return categoria;
    }

    public String getJustificacion() {
        return justificacion;
    }

    /**
     * fileId opacos de 0 a N soportes (copia inmutable, en el orden recibido): SQL valida cantidad, repetidos, propiedad y estado
     * de cada uno antes de vincularlos.
     */
    public List<UUID> getSoportes() {
        return soportes;
    }

    public UUID getUsuario() {
        return usuario;
    }

}
