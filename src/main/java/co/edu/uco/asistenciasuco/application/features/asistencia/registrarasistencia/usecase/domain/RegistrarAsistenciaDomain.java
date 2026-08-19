package co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistencia.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.ErrorCode;
import co.edu.uco.asistenciasuco.application.exception.ValidationException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import co.edu.uco.asistenciasuco.crosscutting.helpers.TextHelper;

import java.util.UUID;

/**
 * Dominio de la operacion registrar asistencia.
 */
public final class RegistrarAsistenciaDomain {

    private final UUID estudiante;
    private final UUID grupo;
    private final UUID sesion;
    private final boolean presente;
    private final String observacion;

    public RegistrarAsistenciaDomain(
            final UUID estudiante,
            final UUID grupo,
            final UUID sesion,
            final Boolean presente,
            final String observacion
    ) {
        validarEstudiante(estudiante);
        validarGrupo(grupo);
        validarSesion(sesion);

        this.presente = validarPresente(presente);
        this.observacion = normalizarObservacion(observacion);

        validarObservacionSegunAsistencia(this.presente, this.observacion);

        this.estudiante = estudiante;
        this.grupo = grupo;
        this.sesion = sesion;
    }

    private void validarEstudiante(final UUID estudiante) {
        if (ObjectHelper.isNull(estudiante)) {
            throw new ValidationException(ErrorCode.ERR_ESTUDIANTE_ID_REQUERIDO);
        }
    }

    private void validarGrupo(final UUID grupo) {
        if (ObjectHelper.isNull(grupo)) {
            throw new ValidationException(ErrorCode.ERR_GRUPO_REQUERIDO);
        }
    }

    private void validarSesion(final UUID sesion) {
        if (ObjectHelper.isNull(sesion)) {
            throw new ValidationException(ErrorCode.ERR_SESION_REQUERIDA);
        }
    }

    private boolean validarPresente(final Boolean presente) {
        if (ObjectHelper.isNull(presente)) {
            throw new ValidationException(ErrorCode.ERR_PRESENTE_REQUERIDO);
        }
        return presente;
    }

    private String normalizarObservacion(final String observacion) {
        final String observacionNormalizada = TextHelper.trim(observacion);

        if (TextHelper.isNullOrBlank(observacionNormalizada)) {
            return null;
        }

        if (!TextHelper.hasLengthBetween(observacionNormalizada, 5, 250)) {
            throw new ValidationException(ErrorCode.ERR_OBSERVACION_ASISTENCIA_LONGITUD_INVALIDA);
        }

        return observacionNormalizada;
    }

    private void validarObservacionSegunAsistencia(final boolean presente, final String observacion) {
        if (!presente && TextHelper.isNullOrBlank(observacion)) {
            throw new ValidationException(ErrorCode.ERR_OBSERVACION_ASISTENCIA_REQUERIDA);
        }
    }

    public UUID getEstudiante() {
        return estudiante;
    }

    public UUID getGrupo() {
        return grupo;
    }

    public UUID getSesion() {
        return sesion;
    }

    public boolean isPresente() {
        return presente;
    }

    public String getObservacion() {
        return observacion;
    }

}
