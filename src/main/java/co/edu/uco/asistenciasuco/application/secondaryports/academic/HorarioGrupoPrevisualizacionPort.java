package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PrevisualizacionHorarioGrupoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.FranjaHorarioRepositoryDTO;

import java.util.List;
import java.util.UUID;

/** SES-GEN-01: previsualizacion de solo lectura del horario propuesto de un grupo (nuevo o existente). No escribe estado de negocio. */
public interface HorarioGrupoPrevisualizacionPort {

    /**
     * @param idGrupo nulo para un grupo nuevo (entonces idAsignatura, idPeriodoAcademico e idDocente son obligatorios)
     * @throws RuntimeException de la capa de persistencia con el codigo de la DB (VAL_*, SEC_*, ASI_010, PER_010, HOR_004, ...) cuando el
     *         insumo es invalido, el recurso no existe o esta fuera del ambito del ejecutor; los conflictos del horario NO lanzan: vienen como datos
     */
    PrevisualizacionHorarioGrupoProjection previsualizar(UUID idGrupo, UUID idAsignatura, UUID idPeriodoAcademico, UUID idDocente,
                                                         List<FranjaHorarioRepositoryDTO> franjas, UUID usuarioEjecutor);
}
