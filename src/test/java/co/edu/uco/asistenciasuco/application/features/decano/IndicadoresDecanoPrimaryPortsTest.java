package co.edu.uco.asistenciasuco.application.features.decano;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.primaryports.dto.AforoGruposPaginaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.primaryports.dto.ConsultarAforoGruposDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.primaryports.interactor.ConsultarAforoGruposInteractor;
import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.usecase.ConsultarAforoGruposUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.usecase.domain.AforoGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.usecase.domain.AforoGruposPaginaDomain;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.primaryports.dto.ComparativaProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.primaryports.dto.ConsultarComparativaProgramasDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.primaryports.interactor.ConsultarComparativaProgramasInteractor;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.usecase.ConsultarComparativaProgramasUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.usecase.domain.ComparativaProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.primaryports.dto.ConsultarKpisDecanoDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.primaryports.dto.KpisDecanoDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.primaryports.interactor.ConsultarKpisDecanoInteractor;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.usecase.ConsultarKpisDecanoUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.usecase.domain.ConsultarKpisDecanoDomain;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.usecase.domain.KpisDecanoDomain;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/** Cubre interactores y mappers de las tres consultas del decano con datos completos (sin mockStatic). */
class IndicadoresDecanoPrimaryPortsTest {

    private static final UUID ACTOR = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID FACULTAD = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PERIODO = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID PROGRAMA = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Test
    void kpis_mapea_dto_a_dominio_y_dominio_a_dto() {
        final ConsultarKpisDecanoUseCase useCase = mock(ConsultarKpisDecanoUseCase.class);
        when(useCase.execute(any())).thenReturn(new KpisDecanoDomain(FACULTAD, "Ingenieria", PERIODO, "2026-2", 3, 6, 2, 1, 3, new BigDecimal("50.00"),
                35, 6, new BigDecimal("17.14"), new BigDecimal("82.86"), 3, 55, 7, null, new BigDecimal("15.00"), new BigDecimal("20.00")));

        final KpisDecanoDTO dto = new ConsultarKpisDecanoInteractor(useCase).execute(new ConsultarKpisDecanoDTO(ACTOR, PERIODO));

        final ArgumentCaptor<ConsultarKpisDecanoDomain> captor = ArgumentCaptor.forClass(ConsultarKpisDecanoDomain.class);
        Mockito.verify(useCase).execute(captor.capture());
        assertEquals(ACTOR, captor.getValue().actorUsuarioId());
        assertEquals(PERIODO, captor.getValue().idPeriodoAcademico());
        assertEquals(FACULTAD, dto.idFacultad());
        assertEquals("Ingenieria", dto.nombreFacultad());
        assertEquals(PERIODO, dto.idPeriodoAcademico());
        assertEquals("2026-2", dto.nombrePeriodoAcademico());
        assertEquals(3, dto.totalProgramas());
        assertEquals(6, dto.totalEstudiantes());
        assertEquals(2, dto.estudiantesRegulares());
        assertEquals(1, dto.estudiantesRiesgo());
        assertEquals(3, dto.estudiantesPerdida());
        assertEquals(new BigDecimal("50.00"), dto.tasaRetencion());
        assertEquals(35, dto.sesionesEjecutadas());
        assertEquals(6, dto.faltasSinJustaCausa());
        assertEquals(new BigDecimal("17.14"), dto.porcentajeAusentismo());
        assertEquals(new BigDecimal("82.86"), dto.porcentajeAsistencia());
        assertEquals(3, dto.gruposActivos());
        assertEquals(55, dto.cupoTotal());
        assertEquals(7, dto.matriculadosActivos());
        assertNull(dto.tasaAforo());
        assertEquals(new BigDecimal("15.00"), dto.umbralAdvertencia());
        assertEquals(new BigDecimal("20.00"), dto.umbralPerdida());
        assertThrows(NullPointerException.class, () -> new ConsultarKpisDecanoInteractor(null));
    }

    @Test
    void comparativa_mapea_cada_fila() {
        final ConsultarComparativaProgramasUseCase useCase = mock(ConsultarComparativaProgramasUseCase.class);
        when(useCase.execute(any())).thenReturn(List.of(
                new ComparativaProgramaDomain(PROGRAMA, "Programa 1", true, 5, 1, 2, new BigDecimal("60.00"), 33, 5, new BigDecimal("15.15")),
                new ComparativaProgramaDomain(UUID.randomUUID(), "Programa 4", false, 0, 0, 0, null, 0, 0, null)));

        final List<ComparativaProgramaDTO> dto = new ConsultarComparativaProgramasInteractor(useCase)
                .execute(new ConsultarComparativaProgramasDTO(ACTOR, PERIODO));

        assertEquals(2, dto.size());
        assertEquals(PROGRAMA, dto.get(0).idPrograma());
        assertEquals("Programa 1", dto.get(0).nombrePrograma());
        assertEquals(true, dto.get(0).estaActivoPrograma());
        assertEquals(5, dto.get(0).totalEstudiantes());
        assertEquals(1, dto.get(0).estudiantesRiesgo());
        assertEquals(2, dto.get(0).estudiantesPerdida());
        assertEquals(new BigDecimal("60.00"), dto.get(0).tasaRetencion());
        assertEquals(33, dto.get(0).sesionesEjecutadas());
        assertEquals(5, dto.get(0).faltasSinJustaCausa());
        assertEquals(new BigDecimal("15.15"), dto.get(0).porcentajeAusentismo());
        assertNull(dto.get(1).porcentajeAusentismo());
        assertThrows(NullPointerException.class, () -> new ConsultarComparativaProgramasInteractor(null));
    }

    @Test
    void aforo_mapea_filtros_filas_y_total() {
        final ConsultarAforoGruposUseCase useCase = mock(ConsultarAforoGruposUseCase.class);
        final UUID grupo = UUID.randomUUID();
        final UUID asignatura = UUID.randomUUID();
        when(useCase.execute(any())).thenReturn(new AforoGruposPaginaDomain(List.of(
                new AforoGrupoDomain(grupo, 931001, asignatura, "Asignatura Uno", PROGRAMA, "Programa 1", 10, 5, 5, new BigDecimal("50.00"), true)), 12));

        final AforoGruposPaginaDTO dto = new ConsultarAforoGruposInteractor(useCase)
                .execute(new ConsultarAforoGruposDTO(ACTOR, PERIODO, PROGRAMA, false, 2, 5));

        final ArgumentCaptor<co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.usecase.domain.ConsultarAforoGruposDomain> captor =
                ArgumentCaptor.forClass(co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.usecase.domain.ConsultarAforoGruposDomain.class);
        Mockito.verify(useCase).execute(captor.capture());
        assertEquals(PROGRAMA, captor.getValue().idPrograma());
        assertEquals(false, captor.getValue().soloHabilitados());
        assertEquals(2, captor.getValue().pagina());
        assertEquals(5, captor.getValue().tamano());
        assertEquals(12, dto.totalRegistros());
        assertEquals(1, dto.grupos().size());
        assertEquals(grupo, dto.grupos().get(0).idGrupo());
        assertEquals(931001, dto.grupos().get(0).codigoGrupo());
        assertEquals(asignatura, dto.grupos().get(0).idAsignatura());
        assertEquals("Asignatura Uno", dto.grupos().get(0).nombreAsignatura());
        assertEquals(PROGRAMA, dto.grupos().get(0).idPrograma());
        assertEquals("Programa 1", dto.grupos().get(0).nombrePrograma());
        assertEquals(10, dto.grupos().get(0).cupoMaximo());
        assertEquals(5, dto.grupos().get(0).matriculadosActivos());
        assertEquals(5, dto.grupos().get(0).cuposDisponibles());
        assertEquals(new BigDecimal("50.00"), dto.grupos().get(0).tasaAforo());
        assertEquals(true, dto.grupos().get(0).grupoEstaHabilitado());
        assertThrows(NullPointerException.class, () -> new ConsultarAforoGruposInteractor(null));
    }
}
