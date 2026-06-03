package co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.domain;

import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

import java.util.UUID;

/**
 * Dominio de la operacion consultar asistencias por grupo.
 */
public final class ConsultarAsistenciasPorGrupoDomain {

    private final UUID grupo;
    private final UUID sesion;
    private final UUID idCorrelacion;

    public ConsultarAsistenciasPorGrupoDomain(
            final UUID grupo,
            final UUID sesion,
            final UUID idCorrelacion
    ) {
        validarGrupo(grupo);
        validarIdCorrelacion(idCorrelacion);

        this.grupo = grupo;
        this.sesion = sesion;
        this.idCorrelacion = idCorrelacion;
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

    public UUID getGrupo() {
        return grupo;
    }

    public UUID getSesion() {
        return sesion;
    }

    public UUID getIdCorrelacion() {
        return idCorrelacion;
    }
}
