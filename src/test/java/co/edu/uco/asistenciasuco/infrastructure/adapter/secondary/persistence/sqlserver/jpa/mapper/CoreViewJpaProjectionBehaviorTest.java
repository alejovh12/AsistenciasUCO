package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.GrupoRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionRepositoryProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvGrupoEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvSesionEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.mapping.JdbcBaselineValueMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * QUALITY-PR15 JPA02: full projections, not merely conversion helper branches.
 * The old JDBC value converter is a test-only oracle, not a runtime dependency.
 */
class CoreViewJpaProjectionBehaviorTest {

    @Test
    void sesion_con_codigo_y_fechas_convierte_los_instantes_a_utc() {
        final UvSesionEntity row = mock(UvSesionEntity.class);
        final UUID sesion = UUID.randomUUID();
        final UUID grupo = UUID.randomUUID();
        when(row.id()).thenReturn(sesion);
        when(row.idGrupo()).thenReturn(grupo);
        when(row.nombre()).thenReturn("Evaluación");
        when(row.numero()).thenReturn(7);
        when(row.codigoGrupo()).thenReturn(508);
        when(row.fechaHoraInicio()).thenReturn(Date.from(Instant.parse("2026-10-08T14:00:00Z")));
        when(row.fechaHoraFin()).thenReturn(Date.from(Instant.parse("2026-10-08T15:30:00Z")));
        final SesionRepositoryProjection projection = CoreViewJpaProjectionMapper.toSesion(row);
        assertEquals(sesion, projection.getSesion());
        assertEquals(grupo, projection.getGrupo());
        assertEquals("Evaluación", projection.getNombre());
        assertEquals(7, projection.getNumero());
        assertEquals("508", projection.getCodigoGrupo());
        assertEquals(LocalDateTime.parse("2026-10-08T14:00:00"), projection.getFechaHoraInicio());
        assertEquals(LocalDateTime.parse("2026-10-08T15:30:00"), projection.getFechaHoraFin());
    }

    @Test
    void sesion_sin_codigo_grupo_preserva_null_conforme_al_oraculo_jdbc() {
        final UvSesionEntity row = mock(UvSesionEntity.class);
        assertNull(JdbcBaselineValueMapper.toString(null));
        assertNull(CoreViewJpaProjectionMapper.toSesion(row).getCodigoGrupo(),
                "La ausencia en DB no es la cadena literal 'null'.");
    }

    @Test
    void grupo_habilitado_si_y_solo_si_flag_es_uno() {
        final UvGrupoEntity row = mock(UvGrupoEntity.class);
        when(row.codigo()).thenReturn(300);
        when(row.grupoEstaHablitado()).thenReturn(1);
        GrupoRepositoryProjection projection = CoreViewJpaProjectionMapper.toGrupo(row);
        assertEquals("300", projection.getCodigo());
        assertTrue(projection.isGrupoHabilitado());
        when(row.grupoEstaHablitado()).thenReturn(0);
        assertFalse(CoreViewJpaProjectionMapper.toGrupo(row).isGrupoHabilitado());
        when(row.grupoEstaHablitado()).thenReturn(2);
        assertFalse(CoreViewJpaProjectionMapper.toGrupo(row).isGrupoHabilitado());
    }

    @Test
    void grupo_sin_codigo_preserva_null_como_el_oraculo_de_paridad() {
        final UvGrupoEntity row = mock(UvGrupoEntity.class);
        assertNull(CoreViewJpaProjectionMapper.toGrupo(row).getCodigo());
    }
}
