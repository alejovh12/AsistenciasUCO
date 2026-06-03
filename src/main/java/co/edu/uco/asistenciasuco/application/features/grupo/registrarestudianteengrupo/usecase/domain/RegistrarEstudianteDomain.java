package co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.domain;

import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

import java.util.UUID;

/**
 * Dominio de la operacion registrar estudiante en grupo.
 */
public final class RegistrarEstudianteDomain {

    private final UUID estudiante;
    private final UUID grupo;
    private final UUID idCorrelacion;

    public RegistrarEstudianteDomain(
            final UUID estudiante,
            final UUID grupo,
            final UUID idCorrelacion
    ) {
        validarEstudiante(estudiante);
        validarGrupo(grupo);
        validarIdCorrelacion(idCorrelacion);

        this.estudiante = estudiante;
        this.grupo = grupo;
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

    public UUID getIdCorrelacion() {
        return idCorrelacion;
    }
}
