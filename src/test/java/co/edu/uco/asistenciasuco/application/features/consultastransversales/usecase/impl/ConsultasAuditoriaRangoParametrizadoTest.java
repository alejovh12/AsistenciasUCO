package co.edu.uco.asistenciasuco.application.features.consultastransversales.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.FiltroAuditoria;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.Pagina;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** PAR-06: el rango maximo de la auditoria sale de AUDITORIA/RANGO_MAXIMO_DIAS; sin el parametro rige el respaldo de 90 dias. */
class ConsultasAuditoriaRangoParametrizadoTest {

    private static final UUID USUARIO = UUID.randomUUID();
    private static final Instant AHORA = Instant.parse("2026-11-04T15:00:00Z");
    private static final OffsetDateTime HASTA = AHORA.atOffset(ZoneOffset.UTC);

    private final ConsultasTransversalesQueryPort port = mock(ConsultasTransversalesQueryPort.class);
    private final ParameterCatalogPort parametros = mock(ParameterCatalogPort.class);
    private final ConsultasTransversalesUseCaseImpl useCase =
            new ConsultasTransversalesUseCaseImpl(port, Clock.fixed(AHORA, ZoneOffset.UTC), parametros);

    @Test
    void el_parametro_acota_el_rango_de_la_consulta() {
        when(parametros.getParameter("AUDITORIA", "RANGO_MAXIMO_DIAS")).thenReturn(Optional.of("30"));

        final ValidationException error = assertThrows(ValidationException.class,
                () -> useCase.consultarAuditoria(USUARIO, filtro(31), 1, 10));
        assertTrue(error.getMessage().contains("30 dias"), error.getMessage());
    }

    @Test
    void sin_parametro_rige_el_respaldo_de_90_dias() {
        when(parametros.getParameter("AUDITORIA", "RANGO_MAXIMO_DIAS")).thenReturn(Optional.empty());
        when(port.consultarAuditoria(any(), any(), anyInt(), anyInt())).thenReturn(new Pagina<>(List.of(), 0));

        useCase.consultarAuditoria(USUARIO, filtro(90), 1, 10);
        assertThrows(ValidationException.class, () -> useCase.consultarAuditoria(USUARIO, filtro(91), 1, 10));
    }

    @Test
    void valores_invalidos_o_fuera_de_rango_usan_el_respaldo() {
        when(parametros.getParameter("AUDITORIA", "RANGO_MAXIMO_DIAS")).thenReturn(Optional.of("0"));
        assertEquals(Duration.ofDays(90), useCase.rangoAuditoriaMaximo());
        when(parametros.getParameter("AUDITORIA", "RANGO_MAXIMO_DIAS")).thenReturn(Optional.of("5000"));
        assertEquals(Duration.ofDays(90), useCase.rangoAuditoriaMaximo());
        when(parametros.getParameter("AUDITORIA", "RANGO_MAXIMO_DIAS")).thenReturn(Optional.of("noventa"));
        assertEquals(Duration.ofDays(90), useCase.rangoAuditoriaMaximo());
        when(parametros.getParameter("AUDITORIA", "RANGO_MAXIMO_DIAS")).thenReturn(Optional.of(" 180 "));
        assertEquals(Duration.ofDays(180), useCase.rangoAuditoriaMaximo());
        assertEquals(Duration.ofDays(90),
                new ConsultasTransversalesUseCaseImpl(port, Clock.fixed(AHORA, ZoneOffset.UTC)).rangoAuditoriaMaximo());
    }

    private static FiltroAuditoria filtro(final int dias) {
        return new FiltroAuditoria(HASTA.minusDays(dias), HASTA, null, null, null, null, null, true, null, false);
    }
}
