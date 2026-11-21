package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

/**
 * Orden de {@code usp_admitir_estudiante} (CULM-001 D.4). La clave ya no viaja: la gestiona la identidad externa (SEG-11).
 */
public record AdmisionEstudianteCommand(
        UUID tipoIdentificacionId,
        Integer numeroIdentificacion,
        String primerNombre,
        String segundoNombre,
        String primerApellido,
        String segundoApellido,
        String correo,
        UUID idPrograma,
        UUID usuarioEjecutor
) {

    @Override
    public String toString() {
        return "AdmisionEstudianteCommand[idPrograma=" + idPrograma + ", usuarioEjecutor=" + usuarioEjecutor + "]";
    }
}
