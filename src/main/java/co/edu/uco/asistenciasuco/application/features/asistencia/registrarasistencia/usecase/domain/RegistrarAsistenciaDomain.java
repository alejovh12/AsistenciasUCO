package co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistencia.usecase.domain;

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
    private final UUID idCorrelacion;

    public RegistrarAsistenciaDomain(
            final UUID estudiante,
            final UUID grupo,
            final UUID sesion,
            final Boolean presente,
            final String observacion,
            final UUID idCorrelacion
    ) {
        validarEstudiante(estudiante);
        validarGrupo(grupo);
        validarSesion(sesion);

        this.presente = validarPresente(presente);
        this.observacion = normalizarObservacion(observacion);

        validarObservacionSegunAsistencia(this.presente, this.observacion);
        validarIdCorrelacion(idCorrelacion);

        this.estudiante = estudiante;
        this.grupo = grupo;
        this.sesion = sesion;
        this.idCorrelacion = idCorrelacion;
    }

    private void validarEstudiante(final UUID estudiante) {
        if (ObjectHelper.isNull(estudiante)) {
            throw new IllegalArgumentException("El estudiante es obligatorio.");
        }
    }

    private void validarGrupo(final UUID grupo) {
        if (ObjectHelper.isNull(grupo)) {
            throw new IllegalArgumentException("El grupo es obligatorio.");
        }
    }

    private void validarSesion(final UUID sesion) {
        if (ObjectHelper.isNull(sesion)) {
            throw new IllegalArgumentException("La sesion es obligatoria.");
        }
    }

    private boolean validarPresente(final Boolean presente) {
        if (ObjectHelper.isNull(presente)) {
            throw new IllegalArgumentException("El indicador de presencia es obligatorio.");
        }
        return presente;
    }

    private String normalizarObservacion(final String observacion) {
        final String observacionNormalizada = TextHelper.trim(observacion);

        if (TextHelper.isNullOrBlank(observacionNormalizada)) {
            return null;
        }

        if (!TextHelper.hasLengthBetween(observacionNormalizada, 5, 250)) {
            throw new IllegalArgumentException("La observacion debe tener entre 5 y 250 caracteres.");
        }

        return observacionNormalizada;
    }

    private void validarObservacionSegunAsistencia(final boolean presente, final String observacion) {
        if (!presente && TextHelper.isNullOrBlank(observacion)) {
            throw new IllegalArgumentException("Debe indicar una observacion cuando el estudiante no asiste.");
        }
    }

    private void validarIdCorrelacion(final UUID idCorrelacion) {
        if (ObjectHelper.isNull(idCorrelacion)) {
            throw new IllegalArgumentException("El id de correlacion es obligatorio.");
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

    public UUID getIdCorrelacion() {
        return idCorrelacion;
    }
}
