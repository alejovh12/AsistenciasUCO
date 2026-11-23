package co.edu.uco.notificaciones.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModeloDominioTest {

    @Test
    void estadosTerminalesSonEntregadaYFallidaDefinitiva() {
        for (final EstadoEnvio e : EstadoEnvio.values()) {
            assertEquals(e == EstadoEnvio.ENTREGADA || e == EstadoEnvio.FALLIDA_DEFINITIVA, e.esTerminal());
            assertFalse(e.nombre().isBlank());
        }
    }

    @Test
    void notificacionNormalizaColeccionesNulas() {
        final Notificacion n = new Notificacion(null, "a", "k", "T", "es", null, null, null, null, EstadoEnvio.PENDIENTE, 0, null, null, null,
                null, null, null, null, null, null, null);
        assertTrue(n.destinatarios().isEmpty());
        assertTrue(n.datos().isEmpty());
    }

    @Test
    void correoSalienteValidaYNormaliza() {
        final CorreoSaliente c = new CorreoSaliente(" a@uco.edu.co ", null, " a@uco.edu.co ", null, "As", "txt", "<p/>", " <id@x> ");
        assertEquals("a@uco.edu.co", c.fromAddress());
        assertEquals("", c.fromName());
        assertTrue(c.bccAddresses().isEmpty());
        assertEquals("<id@x>", c.messageId());
        assertThrows(NullPointerException.class, () -> new CorreoSaliente(null, "", "a", List.of(), "a", "b", "c", "d"));
        assertThrows(NullPointerException.class, () -> new CorreoSaliente("a", "", "a", List.of(), null, "b", "c", "d"));
    }

    @Test
    void contenidoRenderizadoExigeCampos() {
        assertThrows(NullPointerException.class, () -> new ContenidoRenderizado(null, "t", "h", "o"));
        assertEquals("o", new ContenidoRenderizado("a", "t", "h", "o").origen());
    }

    @Test
    void errorNegocioExponeCategoriaYCodigo() {
        final ErrorNegocio e = ErrorNegocio.conflicto("C", "m");
        assertEquals(ErrorNegocio.Categoria.CONFLICTO, e.categoria());
        assertEquals("C", e.codigo());
        assertEquals(ErrorNegocio.Categoria.VALIDACION, ErrorNegocio.validacion("V", "m").categoria());
        assertEquals(ErrorNegocio.Categoria.NO_ENCONTRADO, ErrorNegocio.noEncontrado("N", "m").categoria());
    }
}
