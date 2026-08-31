package co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.entity;






import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.application.features.sesion.exception.SesionErrorCode;
import co.edu.uco.asistenciasuco.application.features.grupo.exception.GrupoErrorCode;
import co.edu.uco.asistenciasuco.application.features.estudiante.exception.EstudianteErrorCode;
import co.edu.uco.asistenciasuco.application.features.asistencia.exception.AsistenciaErrorCode;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import co.edu.uco.asistenciasuco.crosscutting.helpers.TextHelper;

import java.util.UUID;

/**
 * Modelo interno para la consulta de asistencias por grupo.
 */
public final class AsistenciaConsultadaEntity {

    private final UUID asistencia;
    private final UUID estudiante;
    private final UUID grupo;
    private final UUID sesion;
    private final boolean presente;
    private final String observacion;

    public AsistenciaConsultadaEntity(
            final UUID asistencia,
            final UUID estudiante,
            final UUID grupo,
            final UUID sesion,
            final boolean presente,
            final String observacion
    ) {
        validarIdentificador(asistencia, AsistenciaErrorCode.ERR_ASISTENCIA_REQUERIDA);
        validarIdentificador(estudiante, EstudianteErrorCode.ERR_ESTUDIANTE_ID_REQUERIDO);
        validarIdentificador(grupo, GrupoErrorCode.ERR_GRUPO_REQUERIDO);
        validarIdentificador(sesion, SesionErrorCode.ERR_SESION_REQUERIDA);

        this.asistencia = asistencia;
        this.estudiante = estudiante;
        this.grupo = grupo;
        this.sesion = sesion;
        this.presente = presente;
        this.observacion = normalizarObservacion(observacion);
    }

    private void validarIdentificador(final UUID valor, final ErrorDefinition code) {
        if (ObjectHelper.isNull(valor)) {
            throw new ValidationException(code);
        }
    }

    private String normalizarObservacion(final String observacion) {
        final String observacionNormalizada = TextHelper.trim(observacion);

        if (TextHelper.isNullOrBlank(observacionNormalizada)) {
            return null;
        }

        if (!TextHelper.hasLengthBetween(observacionNormalizada, 5, 250)) {
            throw new ValidationException(AsistenciaErrorCode.ERR_OBSERVACION_ASISTENCIA_LONGITUD_INVALIDA);
        }

        return observacionNormalizada;
    }

    public UUID getAsistencia() {
        return asistencia;
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
