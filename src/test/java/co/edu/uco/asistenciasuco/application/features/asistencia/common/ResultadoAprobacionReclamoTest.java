package co.edu.uco.asistenciasuco.application.features.asistencia.common;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ResultadoAprobacionReclamoTest {

    @Test
    void traduce_entre_el_contrato_http_y_los_codigos_de_la_base_de_datos() {
        assertEquals("AN", ResultadoAprobacionReclamo.PRESENTE.codigo());
        assertEquals("EX", ResultadoAprobacionReclamo.JUSTIFICADA.codigo());
        assertEquals(Optional.of(ResultadoAprobacionReclamo.PRESENTE), ResultadoAprobacionReclamo.desdeNombre(" presente "));
        assertEquals(Optional.of(ResultadoAprobacionReclamo.JUSTIFICADA), ResultadoAprobacionReclamo.desdeNombre("JUSTIFICADA"));
        assertEquals(Optional.empty(), ResultadoAprobacionReclamo.desdeNombre("AN"));
        assertEquals(Optional.empty(), ResultadoAprobacionReclamo.desdeNombre(null));
        assertEquals("PRESENTE", ResultadoAprobacionReclamo.nombreDesdeCodigo("an"));
        assertEquals("JUSTIFICADA", ResultadoAprobacionReclamo.nombreDesdeCodigo(" EX "));
        assertNull(ResultadoAprobacionReclamo.nombreDesdeCodigo("SJC"));
        assertNull(ResultadoAprobacionReclamo.nombreDesdeCodigo(null));
    }
}
