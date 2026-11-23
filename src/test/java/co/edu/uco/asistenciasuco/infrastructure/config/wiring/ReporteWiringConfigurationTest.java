package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Regresion P3 D2: la zona del Excel sale de TIEMPO/ZONA_HORARIA_IANA, con America/Bogota de respaldo. */
class ReporteWiringConfigurationTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    @Test
    void usa_la_zona_del_parametro() {
        final ParameterCatalogPort parametros = mock(ParameterCatalogPort.class);
        when(parametros.getParameter("TIEMPO", "ZONA_HORARIA_IANA")).thenReturn(Optional.of(" Europe/Madrid "));

        assertEquals(ZoneId.of("Europe/Madrid"), ReporteWiringConfiguration.zonaInstitucional(parametros));
    }

    @Test
    void sin_catalogo_parametro_vacio_o_invalido_usa_bogota() {
        final ParameterCatalogPort parametros = mock(ParameterCatalogPort.class);
        assertEquals(BOGOTA, ReporteWiringConfiguration.zonaInstitucional(null));

        when(parametros.getParameter("TIEMPO", "ZONA_HORARIA_IANA")).thenReturn(Optional.empty());
        assertEquals(BOGOTA, ReporteWiringConfiguration.zonaInstitucional(parametros));

        when(parametros.getParameter("TIEMPO", "ZONA_HORARIA_IANA")).thenReturn(Optional.of("  "));
        assertEquals(BOGOTA, ReporteWiringConfiguration.zonaInstitucional(parametros));

        when(parametros.getParameter("TIEMPO", "ZONA_HORARIA_IANA")).thenReturn(Optional.of("No/Existe"));
        assertEquals(BOGOTA, ReporteWiringConfiguration.zonaInstitucional(parametros));

        when(parametros.getParameter("TIEMPO", "ZONA_HORARIA_IANA")).thenThrow(new ParameterCatalogPort.ParameterCatalogException("caido"));
        assertEquals(BOGOTA, ReporteWiringConfiguration.zonaInstitucional(parametros));
    }
}
