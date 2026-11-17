package co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase;

import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.domain.SesionMateriaEstudianteDomain;
import java.util.List; import java.util.UUID;

public interface ConsultarSesionesMateriaEstudianteUseCase {
    /** Primera pagina con el tamano por defecto (PAGINACION/TAMANO_MAXIMO). */
    default List<SesionMateriaEstudianteDomain> execute(UUID actorUsuarioId, UUID idAsignatura) {
        return execute(actorUsuarioId, idAsignatura, null, null);
    }

    /** {@code pagina} (1 por defecto) y {@code tamano} (PAGINACION/TAMANO_MAXIMO por defecto) son opcionales. */
    default List<SesionMateriaEstudianteDomain> execute(UUID actorUsuarioId, UUID idAsignatura, Integer pagina, Integer tamano) {
        return consultarPagina(actorUsuarioId, idAsignatura, null, pagina, tamano).elementos();
    }

    /**
     * CIERRE-W2B: pagina de sesiones ordenada en la base (inicio, numero, id) con el total real de filas. {@code idGrupo}
     * (opcional) limita a un grupo en el que el estudiante esta inscrito; {@code pagina} y {@code tamano} son opcionales.
     */
    PaginaResultado<SesionMateriaEstudianteDomain> consultarPagina(UUID actorUsuarioId, UUID idAsignatura, UUID idGrupo,
                                                                   Integer pagina, Integer tamano);
}
