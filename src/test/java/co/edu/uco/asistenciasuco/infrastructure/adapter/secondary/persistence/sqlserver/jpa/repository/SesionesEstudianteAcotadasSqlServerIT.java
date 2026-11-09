package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsistenciaSesionEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionProcedenciaRepositoryProjection;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * PAG-EST-SESIONES: prueba contra SQL Server real, solo lectura, de las lecturas complementarias acotadas por ids de
 * sesion. No escribe datos ni usa fixtures: se apoya en datos vivos y declara SKIP (assumeTrue) cuando no hay datos
 * suficientes, nunca PASS vacio.
 */
@Tag("integration")
@SpringBootTest
@MockitoBean(types = JwtDecoder.class)
class SesionesEstudianteAcotadasSqlServerIT {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbc;

    private record Par(UUID estudiante, UUID asignatura) { }

    /** Par estudiante/asignatura con asistencia en al menos dos sesiones distintas. */
    private Par parConVariasSesiones() {
        final List<Par> pares = jdbc.query("""
                SELECT TOP 1 eg.idEstudiante, g.idAsignatura
                FROM dbo.uv_asistencia a
                INNER JOIN dbo.uv_estudiante_grupo eg ON eg.id = a.idEstudianteGrupo
                INNER JOIN dbo.uv_grupo g ON g.id = eg.idGrupo
                GROUP BY eg.idEstudiante, g.idAsignatura
                HAVING COUNT(DISTINCT a.idSesion) >= 2
                ORDER BY COUNT(DISTINCT a.idSesion) DESC, eg.idEstudiante
                """, (rs, row) -> new Par(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class)));
        assumeTrue(!pares.isEmpty(), "No hay estudiante con asistencia en dos o mas sesiones.");
        return pares.getFirst();
    }

    @Test
    void la_asistencia_acotada_a_una_sesion_devuelve_solo_esa_sesion_y_es_subconjunto_de_la_lectura_completa() {
        final Par par = parConVariasSesiones();
        final var repo = new AsistenciaSesionEstudianteJpaRepository(entityManager);
        final List<AsistenciaSesionEstudianteProjection> completa =
                repo.consultarAsistenciaSesiones(par.estudiante(), par.asignatura());
        final Set<UUID> sesiones = completa.stream().map(AsistenciaSesionEstudianteProjection::idSesion)
                .collect(Collectors.toSet());
        assertTrue(sesiones.size() >= 2);
        final UUID una = sesiones.iterator().next();

        final List<AsistenciaSesionEstudianteProjection> acotada =
                repo.consultarAsistenciaSesiones(par.estudiante(), par.asignatura(), Set.of(una));

        assertTrue(!acotada.isEmpty());
        assertTrue(acotada.stream().allMatch(h -> una.equals(h.idSesion())), "Solo filas de la sesion pedida.");
        assertEquals(completa.stream().filter(h -> una.equals(h.idSesion())).count(), acotada.size());
        assertTrue(acotada.size() < completa.size(), "La lectura acotada recupera menos filas que la completa.");
    }

    @Test
    void la_asistencia_acotada_con_todos_los_ids_coincide_con_la_lectura_completa() {
        final Par par = parConVariasSesiones();
        final var repo = new AsistenciaSesionEstudianteJpaRepository(entityManager);
        final List<AsistenciaSesionEstudianteProjection> completa =
                repo.consultarAsistenciaSesiones(par.estudiante(), par.asignatura());
        final Set<UUID> todas = completa.stream().map(AsistenciaSesionEstudianteProjection::idSesion).collect(Collectors.toSet());

        final List<AsistenciaSesionEstudianteProjection> acotada =
                repo.consultarAsistenciaSesiones(par.estudiante(), par.asignatura(), todas);

        assertEquals(completa.stream().map(Object::toString).sorted().toList(),
                acotada.stream().map(Object::toString).sorted().toList());
    }

    @Test
    void ids_de_sesion_ajenos_al_estudiante_no_devuelven_filas() {
        final Par par = parConVariasSesiones();
        final var repo = new AsistenciaSesionEstudianteJpaRepository(entityManager);
        final List<UUID> ajenas = jdbc.query("""
                SELECT TOP 5 a.idSesion
                FROM dbo.uv_asistencia a
                INNER JOIN dbo.uv_estudiante_grupo eg ON eg.id = a.idEstudianteGrupo
                WHERE eg.idEstudiante <> ?
                  AND NOT EXISTS (
                      SELECT 1 FROM dbo.uv_asistencia a2
                      INNER JOIN dbo.uv_estudiante_grupo eg2 ON eg2.id = a2.idEstudianteGrupo
                      WHERE eg2.idEstudiante = ? AND a2.idSesion = a.idSesion)
                """, (rs, row) -> rs.getObject(1, UUID.class), par.estudiante(), par.estudiante());
        assumeTrue(!ajenas.isEmpty(), "No hay sesiones de otros estudiantes para el caso negativo.");

        assertTrue(repo.consultarAsistenciaSesiones(par.estudiante(), par.asignatura(), ajenas).isEmpty());
        assertTrue(repo.consultarAsistenciaSesiones(UUID.randomUUID(), par.asignatura(),
                Set.copyOf(ajenas)).isEmpty());
    }

    @Test
    void la_procedencia_por_ids_coincide_con_la_del_grupo_filtrada_a_esos_ids_y_no_trae_el_resto_del_grupo() {
        final List<UUID> grupos = jdbc.query("""
                SELECT TOP 1 s.idGrupo FROM dbo.uv_sesion_v2 s
                GROUP BY s.idGrupo HAVING COUNT(*) >= 3 ORDER BY COUNT(*) DESC
                """, (rs, row) -> rs.getObject(1, UUID.class));
        assumeTrue(!grupos.isEmpty(), "No hay grupo con tres o mas sesiones.");
        final var repo = new SesionProcedenciaJpaRepository(entityManager);
        final List<SesionProcedenciaRepositoryProjection> delGrupo = repo.consultarSesionesPorGrupo(grupos.getFirst());
        final Set<UUID> dos = delGrupo.stream().limit(2).map(SesionProcedenciaRepositoryProjection::sesion)
                .collect(Collectors.toSet());

        final List<SesionProcedenciaRepositoryProjection> porIds = repo.consultarSesionesPorIds(dos);

        assertEquals(2, porIds.size());
        assertTrue(porIds.size() < delGrupo.size());
        assertEquals(delGrupo.stream().filter(p -> dos.contains(p.sesion())).toList(), porIds);
    }
}
