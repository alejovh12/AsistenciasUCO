package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsistenciasPorGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.AsistenciaRepositoryProjection;

import java.util.List;

/**
 * Abstraccion INTERNA de Infrastructure (no es un Application Port) para la unica query de
 * asistencia que puede resolverse con una tecnologia distinta a JDBC (LB-002.1). Permite que el
 * adapter hibrido delegue la query sin conocer JPA.
 */
@FunctionalInterface
public interface AsistenciaQueryPersistence {

    List<AsistenciaRepositoryProjection> consultarAsistenciasPorGrupo(ConsultarAsistenciasPorGrupoRepositoryDTO dto);
}
