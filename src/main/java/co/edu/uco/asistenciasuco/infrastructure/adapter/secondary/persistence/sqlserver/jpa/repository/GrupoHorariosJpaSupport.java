package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.FranjaHorarioRepositoryProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvHorarioEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper.CoreViewJpaProjectionMapper;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Franjas del horario semanal de un conjunto de grupos (vista {@code uv_horario}), ordenadas de lunes a domingo y por
 * hora. Lo comparten {@code GET /grupos} ({@link GrupoJpaRepository}) y {@code GET /coordinador/grupos}
 * ({@link AsistenciaProgramaJpaRepository}); el ambito de los grupos ya lo aplico el SP que los listo.
 */
final class GrupoHorariosJpaSupport {

    private static final List<String> ORDEN_DIAS = List.of("LU", "MA", "MI", "JU", "VI", "SA", "DO");
    private static final int MAX_PARAMETROS_IN = 1000;
    private static final Comparator<FranjaHorarioRepositoryProjection> ORDEN = Comparator
            .comparingInt((FranjaHorarioRepositoryProjection franja) -> ORDEN_DIAS.indexOf(franja.codigoDia()))
            .thenComparing(FranjaHorarioRepositoryProjection::horaInicio);

    private GrupoHorariosJpaSupport() {
    }

    /** Franjas ordenadas por grupo; un grupo sin horario no aparece en el mapa. */
    static Map<UUID, List<FranjaHorarioRepositoryProjection>> franjasPorGrupo(final EntityManager entityManager,
                                                                              final Collection<UUID> idsGrupo) {
        final Map<UUID, List<FranjaHorarioRepositoryProjection>> porGrupo = new HashMap<>();
        final List<UUID> ids = List.copyOf(idsGrupo);
        for (int desde = 0; desde < ids.size(); desde += MAX_PARAMETROS_IN) {
            entityManager.createQuery(GrupoJpaRepository.HQL_HORARIOS_DE_GRUPOS, UvHorarioEntity.class)
                    .setParameter("idsGrupo", ids.subList(desde, Math.min(desde + MAX_PARAMETROS_IN, ids.size())))
                    .getResultList().stream()
                    .map(CoreViewJpaProjectionMapper::toFranjaHorario)
                    .forEach(franja -> porGrupo.computeIfAbsent(franja.idGrupo(), id -> new ArrayList<>()).add(franja));
        }
        porGrupo.values().forEach(franjas -> franjas.sort(ORDEN));
        return porGrupo;
    }
}
