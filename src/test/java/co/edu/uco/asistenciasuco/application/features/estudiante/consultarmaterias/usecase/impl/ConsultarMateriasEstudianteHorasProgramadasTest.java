package co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.asistencia.common.dto.IndicadoresAsistenciaDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.primaryports.dto.MateriaEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.primaryports.mapper.ConsultarMateriasEstudianteMapper;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.usecase.domain.MateriaEstudianteDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.HorasProgramadasEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.MateriaEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.HorasProgramadasEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.MateriaEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ResumenAsistenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HU019: las materias del estudiante llevan la carga horaria programada de su grupo (HP-UC-01..04). */
class ConsultarMateriasEstudianteHorasProgramadasTest {

    private static final UUID ACTOR = UUID.randomUUID();

    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final MateriaEstudianteQueryPort queryPort = mock(MateriaEstudianteQueryPort.class);
    private final ResumenAsistenciaQueryPort resumenPort = mock(ResumenAsistenciaQueryPort.class);
    private final HorasProgramadasEstudianteQueryPort horasPort = mock(HorasProgramadasEstudianteQueryPort.class);
    private final ConsultarMateriasEstudianteUseCaseImpl useCase =
            new ConsultarMateriasEstudianteUseCaseImpl(scopePort, queryPort, resumenPort, horasPort);

    @Test
    void HP_UC_01_cada_materia_lleva_las_horas_y_sesiones_programadas_de_su_grupo() {
        final UUID grupoConCarga = UUID.randomUUID();
        final UUID grupoSinFila = UUID.randomUUID();
        when(scopePort.findEstudianteIdByUsuario(ACTOR)).thenReturn(Optional.of(UUID.randomUUID()));
        when(queryPort.consultarMateriasEstudiante(ACTOR)).thenReturn(List.of(
                new MateriaEstudianteProjection(UUID.randomUUID(), "Calculo", grupoConCarga, "Grupo 1", 3),
                new MateriaEstudianteProjection(UUID.randomUUID(), "Fisica", grupoSinFila, "Grupo 2", 4)));
        final IndicadoresAsistenciaDTO indicadores = new IndicadoresAsistenciaDTO(10, new BigDecimal("20.00"),
                new BigDecimal("16.00"), new BigDecimal("3.00"), new BigDecimal("1.00"), BigDecimal.ZERO,
                new BigDecimal("15.00"), new BigDecimal("80.00"), "RIESGO", new BigDecimal("15.00"), new BigDecimal("20.00"));
        when(resumenPort.consultarResumenEstudiante(ACTOR)).thenReturn(List.of(
                new ResumenAsistenciaQueryPort.FilaResumen(grupoConCarga, UUID.randomUUID(), "Ana", "A", indicadores)));
        when(horasPort.consultarHorasProgramadas(ACTOR)).thenReturn(List.of(
                new HorasProgramadasEstudianteProjection(grupoConCarga, 24, new BigDecimal("48.00")),
                new HorasProgramadasEstudianteProjection(UUID.randomUUID(), 9, new BigDecimal("18.00"))));

        final List<MateriaEstudianteDomain> materias = useCase.execute(ACTOR);

        assertEquals(new BigDecimal("48.00"), materias.get(0).horasProgramadas());
        assertEquals(24, materias.get(0).sesionesProgramadas());
        assertEquals(indicadores, materias.get(0).indicadores());
        assertEquals(3, materias.get(0).creditos());
        // Sin fila de carga horaria: nulo, nunca cero inventado. Los indicadores siguen su propia regla.
        assertNull(materias.get(1).horasProgramadas());
        assertNull(materias.get(1).sesionesProgramadas());
        assertEquals(0, materias.get(1).indicadores().sesionesEjecutadas());
        assertEquals(4, materias.get(1).creditos());

        final MateriaEstudianteDTO dto = ConsultarMateriasEstudianteMapper.toDTO(materias.get(0));
        assertEquals(new BigDecimal("48.00"), dto.horasProgramadas());
        assertEquals(24, dto.sesionesProgramadas());
        assertEquals(new BigDecimal("20.00"), dto.indicadores().horasDictadas());
    }

    @Test
    void HP_UC_02_sin_materias_no_consulta_las_horas_programadas() {
        when(scopePort.findEstudianteIdByUsuario(ACTOR)).thenReturn(Optional.of(UUID.randomUUID()));
        when(queryPort.consultarMateriasEstudiante(ACTOR)).thenReturn(List.of());

        assertEquals(List.of(), useCase.execute(ACTOR));

        verify(horasPort, never()).consultarHorasProgramadas(any());
    }

    @Test
    void HP_UC_03_el_constructor_sin_puerto_deja_la_carga_en_nulo_y_el_puerto_no_puede_ser_nulo() {
        when(scopePort.findEstudianteIdByUsuario(ACTOR)).thenReturn(Optional.of(UUID.randomUUID()));
        when(queryPort.consultarMateriasEstudiante(ACTOR)).thenReturn(List.of(
                new MateriaEstudianteProjection(UUID.randomUUID(), "Calculo", UUID.randomUUID(), "Grupo 1")));
        when(resumenPort.consultarResumenEstudiante(ACTOR)).thenReturn(List.of());

        final MateriaEstudianteDomain materia =
                new ConsultarMateriasEstudianteUseCaseImpl(scopePort, queryPort, resumenPort).execute(ACTOR).getFirst();

        assertNull(materia.horasProgramadas());
        assertNull(materia.sesionesProgramadas());
        assertThrows(NullPointerException.class,
                () -> new ConsultarMateriasEstudianteUseCaseImpl(scopePort, queryPort, resumenPort, null));
    }

    @Test
    void HP_UC_04_los_constructores_cortos_del_dominio_y_del_dto_dejan_la_carga_en_nulo() {
        final IndicadoresAsistenciaDTO sinSesiones = IndicadoresAsistenciaDTO.sinSesiones(null, null);
        final MateriaEstudianteDomain dominio = new MateriaEstudianteDomain(UUID.randomUUID(), "X", UUID.randomUUID(), "G", sinSesiones, 3);
        final MateriaEstudianteDTO dto = new MateriaEstudianteDTO(UUID.randomUUID(), "X", UUID.randomUUID(), "G", sinSesiones, 3);

        assertNull(dominio.horasProgramadas());
        assertNull(dominio.sesionesProgramadas());
        assertNull(dto.horasProgramadas());
        assertNull(dto.sesionesProgramadas());
        assertEquals(3, dto.creditos());
        assertNull(new MateriaEstudianteDomain(UUID.randomUUID(), "X", UUID.randomUUID(), "G").creditos());
    }
}
