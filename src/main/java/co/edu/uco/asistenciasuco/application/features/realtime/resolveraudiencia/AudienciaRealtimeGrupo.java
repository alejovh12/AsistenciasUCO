package co.edu.uco.asistenciasuco.application.features.realtime.resolveraudiencia;

/** Audiencias de un usuario sobre un grupo (todas falsas = sin acceso). */
public record AudienciaRealtimeGrupo(boolean docenteTitular, boolean estudianteGrupo, boolean ambitoGrupo) {

    public boolean tieneAlguna() {
        return docenteTitular || estudianteGrupo || ambitoGrupo;
    }
}
