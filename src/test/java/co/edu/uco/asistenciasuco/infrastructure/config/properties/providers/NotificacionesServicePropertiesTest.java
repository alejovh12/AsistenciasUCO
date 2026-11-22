package co.edu.uco.asistenciasuco.infrastructure.config.properties.providers;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificacionesServicePropertiesTest {

    private static NotificacionesServiceProperties con(final String url, final String clave, final Duration conexion,
                                                       final Duration lectura, final String plataforma) {
        return new NotificacionesServiceProperties(url, clave, conexion, lectura, null, null, false, plataforma);
    }

    @Test
    void sin_url_el_servicio_no_esta_configurado_y_los_valores_por_defecto_son_los_del_contrato() {
        final NotificacionesServiceProperties props = con(null, null, null, null, null);

        assertFalse(props.configurado());
        assertEquals("", props.url());
        assertEquals("", props.apiKey());
        assertEquals(Duration.ofSeconds(2), props.connectTimeout());
        assertEquals(Duration.ofSeconds(10), props.readTimeout());
        assertEquals("asistencias", props.aplicacion());
        assertEquals("es", props.idioma());
    }

    @Test
    void normaliza_la_url_y_recorta_la_clave() {
        final NotificacionesServiceProperties props = con(" http://notificaciones:8080/ ", " k ", null, null, "https://app.uco.edu.co/");

        assertTrue(props.configurado());
        assertEquals("http://notificaciones:8080", props.url());
        assertEquals("k", props.apiKey());
        assertEquals("https://app.uco.edu.co", props.platformUrl());
    }

    @Test
    void rechaza_una_url_que_no_es_http() {
        assertThrows(IllegalArgumentException.class, () -> con("ftp://x", "k", null, null, null));
        assertThrows(IllegalArgumentException.class, () -> con("http://x", "k", null, null, "localhost:4200"));
    }

    @Test
    void rechaza_tiempos_de_espera_no_positivos() {
        assertThrows(IllegalArgumentException.class, () -> con("http://x", "k", Duration.ZERO, null, null));
        assertThrows(IllegalArgumentException.class, () -> con("http://x", "k", null, Duration.ofSeconds(-1), null));
    }

    @Test
    void el_toString_no_expone_la_clave() {
        final String texto = con("http://x", "clave-secreta", null, null, null).toString();

        assertFalse(texto.contains("clave-secreta"));
        assertTrue(texto.contains("[PROTECTED]"));
    }
}
