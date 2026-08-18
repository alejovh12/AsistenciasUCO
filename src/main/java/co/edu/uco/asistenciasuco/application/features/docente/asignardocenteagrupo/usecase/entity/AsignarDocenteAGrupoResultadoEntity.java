package co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.usecase.entity;

/**
 * Resultado interno de asignar docente a grupo.
 */
public final class AsignarDocenteAGrupoResultadoEntity {

    private final boolean exitoso;
    private final String mensajeUsuario;

    public AsignarDocenteAGrupoResultadoEntity(
            final boolean exitoso,
            final String mensajeUsuario
    ) {
        this.exitoso = exitoso;
        this.mensajeUsuario = mensajeUsuario;
    }

    public boolean isExitoso() {
        return exitoso;
    }

    public String getMensajeUsuario() {
        return mensajeUsuario;
    }

}
