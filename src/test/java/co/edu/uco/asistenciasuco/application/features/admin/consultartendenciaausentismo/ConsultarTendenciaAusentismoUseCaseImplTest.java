package co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.usecase.domain.ConsultarTendenciaAusentismoDomain;
import co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.usecase.domain.PuntoTendenciaDomain;
import co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.usecase.impl.ConsultarTendenciaAusentismoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AdminIndicadoresQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PuntoTendenciaProjection;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsultarTendenciaAusentismoUseCaseImplTest {

    private static final UUID ACTOR = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PERIODO = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private final AdminIndicadoresQueryPort queryPort = mock(AdminIndicadoresQueryPort.class);
    private final ConsultarTendenciaAusentismoUseCaseImpl useCase = new ConsultarTendenciaAusentismoUseCaseImpl(queryPort);

    private static List<PuntoTendenciaProjection> puntos() {
        return List.of(
                new PuntoTendenciaProjection(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 8), 20, 3, new BigDecimal("15.00")),
                new PuntoTendenciaProjection(LocalDate.of(2026, 3, 9), LocalDate.of(2026, 3, 15), 5, 0, new BigDecimal("0.00")),
                new PuntoTendenciaProjection(LocalDate.of(2026, 3, 16), LocalDate.of(2026, 3, 22), 0, 0, null));
    }

    @Test
    void T_BE_01_devuelve_los_puntos_del_puerto_en_orden_y_sin_recalcular() {
        when(queryPort.consultarTendencia(ACTOR, PERIODO, "MES")).thenReturn(puntos());

        final List<PuntoTendenciaDomain> r = useCase.execute(new ConsultarTendenciaAusentismoDomain(ACTOR, PERIODO, "MES"));

        assertEquals(3, r.size());
        assertEquals(LocalDate.of(2026, 3, 2), r.get(0).inicioIntervalo());
        assertEquals(LocalDate.of(2026, 3, 8), r.get(0).finIntervalo());
        assertEquals(20, r.get(0).sesionesEjecutadas());
        assertEquals(3, r.get(0).faltasSinJustaCausa());
        assertEquals(new BigDecimal("15.00"), r.get(0).porcentajeAusentismo());
        assertEquals(new BigDecimal("0.00"), r.get(1).porcentajeAusentismo());
    }

    @Test
    void T_BE_03_un_porcentaje_nulo_no_se_convierte_en_cero() {
        when(queryPort.consultarTendencia(ACTOR, null, "SEMANA")).thenReturn(puntos());

        assertNull(useCase.execute(new ConsultarTendenciaAusentismoDomain(ACTOR, null, "SEMANA")).get(2).porcentajeAusentismo());
    }

    @Test
    void sin_granularidad_o_vacia_se_usa_semana() {
        when(queryPort.consultarTendencia(ACTOR, null, "SEMANA")).thenReturn(List.of());

        useCase.execute(new ConsultarTendenciaAusentismoDomain(ACTOR, null, null));
        useCase.execute(new ConsultarTendenciaAusentismoDomain(ACTOR, null, "  "));

        verify(queryPort, org.mockito.Mockito.times(2)).consultarTendencia(ACTOR, null, "SEMANA");
    }

    @Test
    void la_granularidad_se_normaliza_a_mayusculas() {
        when(queryPort.consultarTendencia(ACTOR, null, "MES")).thenReturn(List.of());

        assertTrue(useCase.execute(new ConsultarTendenciaAusentismoDomain(ACTOR, null, " mes ")).isEmpty());

        verify(queryPort).consultarTendencia(ACTOR, null, "MES");
    }

    @Test
    void una_granularidad_invalida_es_error_de_validacion_sin_tocar_la_db() {
        assertThrows(ValidationException.class, () -> useCase.execute(new ConsultarTendenciaAusentismoDomain(ACTOR, null, "ANIO")));
        verifyNoInteractions(queryPort);
    }

    @Test
    void el_dominio_es_obligatorio() {
        assertThrows(ValidationException.class, () -> useCase.execute(null));
    }

    @Test
    void el_puerto_es_obligatorio() {
        assertThrows(NullPointerException.class, () -> new ConsultarTendenciaAusentismoUseCaseImpl(null));
    }
}
