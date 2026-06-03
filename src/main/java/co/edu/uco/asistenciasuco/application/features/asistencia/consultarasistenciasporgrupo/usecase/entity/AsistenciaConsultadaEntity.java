package co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.entity;

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
        validarIdentificador(asistencia, "La asistencia es obligatoria.");
        validarIdentificador(estudiante, "El estudiante es obligatorio.");
        validarIdentificador(grupo, "El grupo es obligatorio.");
        validarIdentificador(sesion, "La sesion es obligatoria.");

        this.asistencia = asistencia;
        this.estudiante = estudiante;
        this.grupo = grupo;
        this.sesion = sesion;
        this.presente = presente;
        this.observacion = normalizarObservacion(observacion);
    }

    private void validarIdentificador(final UUID valor, final String mensaje) {
        if (ObjectHelper.isNull(valor)) {
            throw new IllegalArgumentException(mensaje);
        }
    }

    private String normalizarObservacion(final String observacion) {
        final String observacionNormalizada = TextHelper.trim(observacion);

        if (TextHelper.isNullOrBlank(observacionNormalizada)) {
            return null;
        }

        if (!TextHelper.hasLengthBetween(observacionNormalizada, 5, 250)) {
            throw new IllegalArgumentException("La observacion de la asistencia debe tener entre 5 y 250 caracteres.");
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
