package co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.primaryports;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.primaryports.dto.SesionMateriaEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.primaryports.dto.SesionesMateriaEstudiantePaginaDTO;
import java.util.List; import java.util.UUID;

public interface ConsultarSesionesMateriaEstudianteInputPort {
    default List<SesionMateriaEstudianteDTO> execute(UUID actorUsuarioId, UUID idAsignatura) {
        return execute(actorUsuarioId, idAsignatura, null, null);
    }

    /** {@code pagina} (1 por defecto) y {@code tamano} (PAGINACION/TAMANO_MAXIMO por defecto) son opcionales. */
    default List<SesionMateriaEstudianteDTO> execute(UUID actorUsuarioId, UUID idAsignatura, Integer pagina, Integer tamano) {
        return consultarPagina(actorUsuarioId, idAsignatura, null, pagina, tamano).sesiones();
    }

    /**
     * CIERRE-W2B: pagina de sesiones en el orden de la base (inicio, numero, id) con el total real; {@code idGrupo} opcional
     * limita a un grupo del estudiante.
     */
    SesionesMateriaEstudiantePaginaDTO consultarPagina(UUID actorUsuarioId, UUID idAsignatura, UUID idGrupo, Integer pagina, Integer tamano);
}
