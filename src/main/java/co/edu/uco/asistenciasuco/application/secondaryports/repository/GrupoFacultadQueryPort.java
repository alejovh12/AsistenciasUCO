package co.edu.uco.asistenciasuco.application.secondaryports.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.GrupoRepositoryProjection;

import java.util.List;
import java.util.UUID;

/**
 * Puerto secundario de lectura de grupos con ambito de facultad (portal del decano). Es independiente de
 * {@link GrupoRepositoryPort}, cuyo listado es institucional y no se abre a roles con ambito acotado.
 */
public interface GrupoFacultadQueryPort {

    /** Grupos de las asignaturas de los programas que pertenecen a la facultad indicada. */
    List<GrupoRepositoryProjection> consultarGruposPorFacultad(UUID idFacultad);
}
