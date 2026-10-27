package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DestinatarioRetiroProjection;

import java.util.List;
import java.util.UUID;

/**
 * HU051: retiro de un estudiante del grupo por el docente titular. La titularidad, el periodo vigente y la matricula
 * activa los valida el provider DB; un grupo ajeno es indistinguible de uno inexistente.
 */
public interface RetiroEstudianteGrupoPort {

    void retirarEstudiante(UUID idGrupo, UUID idEstudiante, String motivo, UUID idUsuarioEjecutor);

    /** Estudiante, docente titular y coordinador del programa; vacio si el ejecutor no es el titular del grupo. */
    List<DestinatarioRetiroProjection> consultarDestinatarios(UUID idGrupo, UUID idEstudiante, UUID idUsuarioEjecutor);
}
