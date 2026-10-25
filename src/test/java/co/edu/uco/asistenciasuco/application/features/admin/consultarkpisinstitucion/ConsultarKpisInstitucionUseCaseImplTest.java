package co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.usecase.domain.ConsultarKpisInstitucionDomain;
import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.usecase.domain.KpisInstitucionDomain;
import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.usecase.impl.ConsultarKpisInstitucionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AdminIndicadoresQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.KpisInstitucionProjection;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsultarKpisInstitucionUseCaseImplTest {

    private static final UUID ACTOR = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID INSTITUCION = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PERIODO = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private final AdminIndicadoresQueryPort queryPort = mock(AdminIndicadoresQueryPort.class);
    private final ConsultarKpisInstitucionUseCaseImpl useCase = new ConsultarKpisInstitucionUseCaseImpl(queryPort);

    private static KpisInstitucionProjection kpis() {
        return new KpisInstitucionProjection(INSTITUCION, "Universidad", PERIODO, "2026-2", 3, 4, 6, 2, 1, 3, new BigDecimal("50.00"),
                35, 6, new BigDecimal("17.14"), new BigDecimal("82.86"), 12, 11, new BigDecimal("91.67"), 6, 7, 1, 2,
                new BigDecimal("15.00"), new BigDecimal("20.00"));
    }

    @Test
    void T_BE_01_devuelve_las_cifras_del_puerto_sin_recalcular() {
        when(queryPort.consultarKpis(ACTOR, PERIODO)).thenReturn(kpis());

        final KpisInstitucionDomain r = useCase.execute(new ConsultarKpisInstitucionDomain(ACTOR, PERIODO));

        assertEquals(INSTITUCION, r.idInstitucion());
        assertEquals("Universidad", r.nombreInstitucion());
        assertEquals(3, r.totalFacultades());
        assertEquals(4, r.totalProgramas());
        assertEquals(6, r.totalEstudiantes());
        assertEquals(new BigDecimal("50.00"), r.tasaRetencion());
        assertEquals(35, r.sesionesEjecutadas());
        assertEquals(6, r.faltasSinJustaCausa());
        assertEquals(new BigDecimal("17.14"), r.porcentajeAusentismo());
        assertEquals(new BigDecimal("82.86"), r.porcentajeAsistencia());
        assertEquals(12, r.sesionesVencidas());
        assertEquals(11, r.sesionesConRegistro());
        assertEquals(new BigDecimal("91.67"), r.porcentajeCumplimiento());
        assertEquals(6, r.estudiantesConMatriculaActiva());
        assertEquals(7, r.estudiantesRegistrados());
        assertEquals(1, r.docentesConGrupoActivo());
        assertEquals(2, r.docentesRegistrados());
        assertEquals(new BigDecimal("15.00"), r.umbralAdvertencia());
        assertEquals(new BigDecimal("20.00"), r.umbralPerdida());
        assertEquals("2026-2", r.nombrePeriodoAcademico());
    }

    @Test
    void T_BE_03_conserva_los_nulos_en_vez_de_inventar_valores() {
        when(queryPort.consultarKpis(ACTOR, null)).thenReturn(new KpisInstitucionProjection(INSTITUCION, "Universidad", null, null, 1, 1, 0, 0, 0, 0,
                null, 0, 0, null, null, 0, 0, null, 0, 0, 0, 0, new BigDecimal("15.00"), new BigDecimal("20.00")));

        final KpisInstitucionDomain r = useCase.execute(new ConsultarKpisInstitucionDomain(ACTOR, null));

        assertNull(r.tasaRetencion());
        assertNull(r.porcentajeAusentismo());
        assertNull(r.porcentajeAsistencia());
        assertNull(r.porcentajeCumplimiento());
        assertNull(r.idPeriodoAcademico());
    }

    @Test
    void el_dominio_es_obligatorio() {
        assertThrows(ValidationException.class, () -> useCase.execute(null));
    }

    @Test
    void consulta_con_el_actor_y_el_periodo_indicados() {
        when(queryPort.consultarKpis(ACTOR, PERIODO)).thenReturn(kpis());

        useCase.execute(new ConsultarKpisInstitucionDomain(ACTOR, PERIODO));

        verify(queryPort).consultarKpis(ACTOR, PERIODO);
    }

    @Test
    void el_puerto_es_obligatorio() {
        assertThrows(NullPointerException.class, () -> new ConsultarKpisInstitucionUseCaseImpl(null));
    }
}
