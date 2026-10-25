package co.edu.uco.asistenciasuco.application.features.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.primaryports.dto.ComparativaFacultadDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.primaryports.dto.ConsultarComparativaFacultadesDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.primaryports.interactor.ConsultarComparativaFacultadesInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.usecase.ConsultarComparativaFacultadesUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.usecase.domain.ComparativaFacultadDomain;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.usecase.domain.ConsultarComparativaFacultadesDomain;
import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.primaryports.dto.ConsultarKpisInstitucionDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.primaryports.dto.KpisInstitucionDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.primaryports.interactor.ConsultarKpisInstitucionInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.usecase.ConsultarKpisInstitucionUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.usecase.domain.ConsultarKpisInstitucionDomain;
import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.usecase.domain.KpisInstitucionDomain;
import co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.primaryports.dto.ConsultarTendenciaAusentismoDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.primaryports.dto.PuntoTendenciaDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.primaryports.interactor.ConsultarTendenciaAusentismoInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.usecase.ConsultarTendenciaAusentismoUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.usecase.domain.ConsultarTendenciaAusentismoDomain;
import co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.usecase.domain.PuntoTendenciaDomain;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Cubre interactores y mappers de las tres consultas del administrador con datos completos (sin mockStatic). */
class IndicadoresAdminPrimaryPortsTest {

    private static final UUID ACTOR = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID INSTITUCION = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PERIODO = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID FACULTAD = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Test
    void kpis_mapea_dto_a_dominio_y_dominio_a_dto() {
        final ConsultarKpisInstitucionUseCase useCase = mock(ConsultarKpisInstitucionUseCase.class);
        when(useCase.execute(any())).thenReturn(new KpisInstitucionDomain(INSTITUCION, "Universidad", PERIODO, "2026-2", 3, 4, 6, 2, 1, 3,
                new BigDecimal("50.00"), 35, 6, new BigDecimal("17.14"), new BigDecimal("82.86"), 12, 11, null, 6, 7, 1, 2,
                new BigDecimal("15.00"), new BigDecimal("20.00")));

        final KpisInstitucionDTO dto = new ConsultarKpisInstitucionInteractor(useCase).execute(new ConsultarKpisInstitucionDTO(ACTOR, PERIODO));

        final ArgumentCaptor<ConsultarKpisInstitucionDomain> captor = ArgumentCaptor.forClass(ConsultarKpisInstitucionDomain.class);
        verify(useCase).execute(captor.capture());
        assertEquals(ACTOR, captor.getValue().actorUsuarioId());
        assertEquals(PERIODO, captor.getValue().idPeriodoAcademico());
        assertEquals(INSTITUCION, dto.idInstitucion());
        assertEquals("Universidad", dto.nombreInstitucion());
        assertEquals(PERIODO, dto.idPeriodoAcademico());
        assertEquals("2026-2", dto.nombrePeriodoAcademico());
        assertEquals(3, dto.totalFacultades());
        assertEquals(4, dto.totalProgramas());
        assertEquals(6, dto.totalEstudiantes());
        assertEquals(2, dto.estudiantesRegulares());
        assertEquals(1, dto.estudiantesRiesgo());
        assertEquals(3, dto.estudiantesPerdida());
        assertEquals(new BigDecimal("50.00"), dto.tasaRetencion());
        assertEquals(35, dto.sesionesEjecutadas());
        assertEquals(6, dto.faltasSinJustaCausa());
        assertEquals(new BigDecimal("17.14"), dto.porcentajeAusentismo());
        assertEquals(new BigDecimal("82.86"), dto.porcentajeAsistencia());
        assertEquals(12, dto.sesionesVencidas());
        assertEquals(11, dto.sesionesConRegistro());
        assertNull(dto.porcentajeCumplimiento());
        assertEquals(6, dto.estudiantesConMatriculaActiva());
        assertEquals(7, dto.estudiantesRegistrados());
        assertEquals(1, dto.docentesConGrupoActivo());
        assertEquals(2, dto.docentesRegistrados());
        assertEquals(new BigDecimal("15.00"), dto.umbralAdvertencia());
        assertEquals(new BigDecimal("20.00"), dto.umbralPerdida());
        assertThrows(NullPointerException.class, () -> new ConsultarKpisInstitucionInteractor(null));
    }

    @Test
    void comparativa_mapea_cada_fila() {
        final ConsultarComparativaFacultadesUseCase useCase = mock(ConsultarComparativaFacultadesUseCase.class);
        when(useCase.execute(any())).thenReturn(List.of(
                new ComparativaFacultadDomain(FACULTAD, "Ingenieria", true, 3, 6, 1, 3, new BigDecimal("50.00"), 35, 6, new BigDecimal("17.14")),
                new ComparativaFacultadDomain(UUID.randomUUID(), "Cerrada", false, 0, 0, 0, 0, null, 0, 0, null)));

        final List<ComparativaFacultadDTO> dto = new ConsultarComparativaFacultadesInteractor(useCase)
                .execute(new ConsultarComparativaFacultadesDTO(ACTOR, PERIODO));

        final ArgumentCaptor<ConsultarComparativaFacultadesDomain> captor = ArgumentCaptor.forClass(ConsultarComparativaFacultadesDomain.class);
        verify(useCase).execute(captor.capture());
        assertEquals(PERIODO, captor.getValue().idPeriodoAcademico());
        assertEquals(2, dto.size());
        assertEquals(FACULTAD, dto.get(0).idFacultad());
        assertEquals("Ingenieria", dto.get(0).nombreFacultad());
        assertEquals(true, dto.get(0).estaActivaFacultad());
        assertEquals(3, dto.get(0).totalProgramas());
        assertEquals(6, dto.get(0).totalEstudiantes());
        assertEquals(1, dto.get(0).estudiantesRiesgo());
        assertEquals(3, dto.get(0).estudiantesPerdida());
        assertEquals(new BigDecimal("50.00"), dto.get(0).tasaRetencion());
        assertEquals(35, dto.get(0).sesionesEjecutadas());
        assertEquals(6, dto.get(0).faltasSinJustaCausa());
        assertEquals(new BigDecimal("17.14"), dto.get(0).porcentajeAusentismo());
        assertNull(dto.get(1).porcentajeAusentismo());
        assertThrows(NullPointerException.class, () -> new ConsultarComparativaFacultadesInteractor(null));
    }

    @Test
    void tendencia_mapea_filtros_y_puntos() {
        final ConsultarTendenciaAusentismoUseCase useCase = mock(ConsultarTendenciaAusentismoUseCase.class);
        when(useCase.execute(any())).thenReturn(List.of(
                new PuntoTendenciaDomain(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 8), 20, 3, new BigDecimal("15.00")),
                new PuntoTendenciaDomain(LocalDate.of(2026, 3, 9), LocalDate.of(2026, 3, 15), 0, 0, null)));

        final List<PuntoTendenciaDTO> dto = new ConsultarTendenciaAusentismoInteractor(useCase)
                .execute(new ConsultarTendenciaAusentismoDTO(ACTOR, PERIODO, "MES"));

        final ArgumentCaptor<ConsultarTendenciaAusentismoDomain> captor = ArgumentCaptor.forClass(ConsultarTendenciaAusentismoDomain.class);
        verify(useCase).execute(captor.capture());
        assertEquals(ACTOR, captor.getValue().actorUsuarioId());
        assertEquals(PERIODO, captor.getValue().idPeriodoAcademico());
        assertEquals("MES", captor.getValue().granularidad());
        assertEquals(2, dto.size());
        assertEquals(LocalDate.of(2026, 3, 2), dto.get(0).inicioIntervalo());
        assertEquals(LocalDate.of(2026, 3, 8), dto.get(0).finIntervalo());
        assertEquals(20, dto.get(0).sesionesEjecutadas());
        assertEquals(3, dto.get(0).faltasSinJustaCausa());
        assertEquals(new BigDecimal("15.00"), dto.get(0).porcentajeAusentismo());
        assertNull(dto.get(1).porcentajeAusentismo());
        assertThrows(NullPointerException.class, () -> new ConsultarTendenciaAusentismoInteractor(null));
    }
}
