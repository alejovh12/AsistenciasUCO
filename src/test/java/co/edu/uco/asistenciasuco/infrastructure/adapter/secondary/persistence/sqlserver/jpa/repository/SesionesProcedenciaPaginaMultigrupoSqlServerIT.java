package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * PAG-DEC-SESIONES / PAG-COORD-SESIONES: contra SQL Server real, solo lectura, una pagina que mezcla sesiones de varios
 * grupos se resuelve con una unica lectura por ids y devuelve exactamente lo que devolvia la lectura anterior (una por
 * grupo, filtrada a la pagina), incluidas las sesiones canceladas. Sin escrituras ni fixtures: SKIP (assumeTrue)
 * cuando los datos vivos no alcanzan, nunca PASS vacio.
 */
@Tag("integration")
@SpringBootTest
@MockitoBean(types = JwtDecoder.class)
class SesionesProcedenciaPaginaMultigrupoSqlServerIT {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbc;

    /** Hasta {@code porGrupo} sesiones de cada uno de los primeros {@code grupos} grupos con al menos esa cantidad. */
    private List<UUID> paginaMultigrupo(final int grupos, final int porGrupo) {
        final List<UUID> idsGrupo = jdbc.query("""
                SELECT TOP (%d) s.idGrupo FROM dbo.uv_sesion_v2 s
                GROUP BY s.idGrupo HAVING COUNT(*) >= %d ORDER BY COUNT(*) DESC, s.idGrupo
                """.formatted(grupos, porGrupo), (rs, row) -> rs.getObject(1, UUID.class));
        assumeTrue(idsGrupo.size() >= 2, "No hay dos grupos con sesiones suficientes.");
        final var repo = new SesionProcedenciaJpaRepository(entityManager);
        final List<UUID> pagina = new ArrayList<>();
        for (final UUID grupo : idsGrupo) {
            repo.consultarSesionesPorGrupo(grupo).stream().limit(porGrupo)
                    .map(SesionProcedenciaRepositoryProjection::sesion).forEach(pagina::add);
        }
        return pagina;
    }

    @Test
    void una_pagina_de_varios_grupos_por_ids_equivale_a_la_lectura_por_grupo_filtrada_a_la_pagina() {
        final List<UUID> pagina = paginaMultigrupo(5, 2);
        final var repo = new SesionProcedenciaJpaRepository(entityManager);
        final Set<UUID> ids = new LinkedHashSet<>(pagina);

        final List<SesionProcedenciaRepositoryProjection> porIds = repo.consultarSesionesPorIds(ids);

        final Set<UUID> grupos = porIds.stream().map(SesionProcedenciaRepositoryProjection::grupo)
                .collect(Collectors.toSet());
        assertTrue(grupos.size() >= 2, "La pagina de prueba abarca al menos dos grupos.");
        final List<SesionProcedenciaRepositoryProjection> esperado = new ArrayList<>();
        for (final UUID grupo : grupos) {
            repo.consultarSesionesPorGrupo(grupo).stream().filter(p -> ids.contains(p.sesion())).forEach(esperado::add);
        }
        assertEquals(ids.size(), porIds.size(), "Una fila de procedencia por sesion pedida, sin el resto de los grupos.");
        assertEquals(esperado.stream().map(Object::toString).sorted().toList(),
                porIds.stream().map(Object::toString).sorted().toList());
    }

    @Test
    void las_sesiones_canceladas_conservan_su_cancelacion_en_la_lectura_por_ids() {
        final List<UUID> canceladas = jdbc.query("""
                SELECT TOP 5 e.idSesion FROM dbo.uv_sesion_estado e WHERE e.estaCancelada = 1 ORDER BY e.idSesion
                """, (rs, row) -> rs.getObject(1, UUID.class));
        assumeTrue(!canceladas.isEmpty(), "No hay sesiones canceladas en los datos vivos.");
        final var repo = new SesionProcedenciaJpaRepository(entityManager);

        final List<SesionProcedenciaRepositoryProjection> porIds = repo.consultarSesionesPorIds(canceladas);
        final List<SesionProcedenciaRepositoryProjection> porGrupo = new ArrayList<>();
        for (final UUID grupo : porIds.stream().map(SesionProcedenciaRepositoryProjection::grupo)
                .collect(Collectors.toSet())) {
            repo.consultarSesionesPorGrupo(grupo).stream()
                    .filter(p -> canceladas.contains(p.sesion())).forEach(porGrupo::add);
        }

        assertEquals(porGrupo.stream().map(Object::toString).sorted().toList(),
                porIds.stream().map(Object::toString).sorted().toList());
    }
}
