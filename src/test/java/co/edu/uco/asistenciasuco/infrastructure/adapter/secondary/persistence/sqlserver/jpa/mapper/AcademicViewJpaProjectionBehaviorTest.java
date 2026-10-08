package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.HorarioDocenteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.InstitucionProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PeriodoAcademicoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PlanEstudioProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvHorarioDocenteEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvInstitucionEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvPeriodoAcademicoEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvPlanEstudioEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * QUALITY-PR15 JPA01: complete academic projections.
 * Academic times are LOCAL HH:mm and must never receive the UTC session converter.
 */
class AcademicViewJpaProjectionBehaviorTest {

    @Test
    void horario_docente_preserva_hora_local_y_campos_academicos() {
        final UvHorarioDocenteEntity row = mock(UvHorarioDocenteEntity.class);
        final UUID docente = UUID.randomUUID();
        final UUID grupo = UUID.randomUUID();
        when(row.idDocente()).thenReturn(docente);
        when(row.idGrupo()).thenReturn(grupo);
        when(row.codigoMateria()).thenReturn("MAT01");
        when(row.dia()).thenReturn("LUNES");
        when(row.horaInicio()).thenReturn("07:30");
        when(row.horaFin()).thenReturn("09:15");
        when(row.totalEstudiantes()).thenReturn(24);
        final HorarioDocenteProjection projection = AcademicViewJpaProjectionMapper.toHorarioDocente(row);
        assertEquals(docente, projection.idDocente());
        assertEquals(grupo, projection.idGrupo());
        assertEquals("MAT01", projection.codigoMateria());
        assertEquals("LUNES", projection.dia());
        assertEquals(LocalTime.of(7, 30), projection.horaInicio());
        assertEquals(LocalTime.of(9, 15), projection.horaFin());
        assertEquals(24, projection.totalEstudiantes());
    }

    @Test
    void horario_sin_hora_no_fabrica_una_hora_por_defecto() {
        final UvHorarioDocenteEntity row = mock(UvHorarioDocenteEntity.class);
        final HorarioDocenteProjection result = AcademicViewJpaProjectionMapper.toHorarioDocente(row);
        assertNull(result.horaInicio());
        assertNull(result.horaFin());
    }

    @Test
    void periodo_proyecta_codigo_numerico_como_texto_y_fechas_sin_desplazamiento() {
        final UvPeriodoAcademicoEntity row = mock(UvPeriodoAcademicoEntity.class);
        final UUID id = UUID.randomUUID();
        when(row.id()).thenReturn(id);
        when(row.codigo()).thenReturn(20262);
        when(row.fechaInicio()).thenReturn(LocalDate.of(2026, 7, 1));
        when(row.fechaFin()).thenReturn(LocalDate.of(2026, 12, 15));
        final PeriodoAcademicoProjection projection = AcademicViewJpaProjectionMapper.toPeriodoAcademico(row);
        assertEquals(id, projection.id());
        assertEquals("20262", projection.codigo());
        assertEquals(LocalDate.of(2026, 7, 1), projection.fechaInicio());
        assertEquals(LocalDate.of(2026, 12, 15), projection.fechaFin());
    }

    @Test
    void institucion_sin_flag_no_se_considera_activa() {
        final UvInstitucionEntity row = mock(UvInstitucionEntity.class);
        when(row.nombre()).thenReturn("UCO");
        final InstitucionProjection projection = AcademicViewJpaProjectionMapper.toInstitucion(row);
        assertEquals("UCO", projection.nombre());
        assertFalse(projection.estaActivaInstitucion());
        when(row.estaActivaInstitucion()).thenReturn(Boolean.TRUE);
        assertTrue(AcademicViewJpaProjectionMapper.toInstitucion(row).estaActivaInstitucion());
    }

    @Test
    void plan_de_estudio_requiere_flag_uno_y_inp_como_texto() {
        final UvPlanEstudioEntity row = mock(UvPlanEstudioEntity.class);
        when(row.inp()).thenReturn(1234);
        when(row.estaActivoPlanEstudio()).thenReturn(1);
        final PlanEstudioProjection projection = AcademicViewJpaProjectionMapper.toPlanEstudio(row);
        assertEquals("1234", projection.inp());
        assertTrue(projection.estaActivoPlanEstudio());
        when(row.estaActivoPlanEstudio()).thenReturn(2);
        assertFalse(AcademicViewJpaProjectionMapper.toPlanEstudio(row).estaActivoPlanEstudio());
    }
}
