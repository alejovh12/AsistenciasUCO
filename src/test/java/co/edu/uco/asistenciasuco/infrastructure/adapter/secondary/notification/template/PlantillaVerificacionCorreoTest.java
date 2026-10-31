package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IDENT-001 BE-T01: plantilla por defecto de {@code VERIFICACION_CORREO} y renderizado seguro del enlace (vinculo HTML
 * solo para URLs http/https; todo valor sustituido se escapa).
 */
class PlantillaVerificacionCorreoTest {

    private static final String ENLACE = "https://asistencia.uco.edu.co/verificar-correo#token=" + "B".repeat(43);

    private final PlantillaNotificacionRepository sinPlantillaEnBd = (tipo, idioma) -> Optional.empty();

    private PlantillaRenderer renderer() {
        return new PlantillaRenderer(sinPlantillaEnBd, new PlantillaPorDefectoCatalogo(), new SimpleMeterRegistry(),
                "https://asistencia.uco.edu.co");
    }

    @Test
    void el_catalogo_por_defecto_incluye_VERIFICACION_CORREO_con_nombre_y_enlace_obligatorios() {
        final PlantillaNotificacion plantilla = new PlantillaPorDefectoCatalogo().obtener("VERIFICACION_CORREO", "es").orElseThrow();

        assertTrue(plantilla.asunto().toLowerCase().contains("verifique"));
        assertTrue(plantilla.cuerpo().contains("{nombre}"));
        assertTrue(plantilla.cuerpo().contains("{enlace}"));
        assertTrue(plantilla.marcadores().stream().anyMatch(m -> m.nombre().equals("nombre") && m.obligatorio()));
        assertTrue(plantilla.marcadores().stream().anyMatch(m -> m.nombre().equals("enlace") && m.obligatorio()));
    }

    @Test
    void el_enlace_http_se_renderiza_como_vinculo_en_html_y_como_texto_en_el_alterno() {
        final ContenidoRenderizado contenido = renderer().renderizar(
                "VERIFICACION_CORREO", Map.of("nombre", "Ana", "enlace", ENLACE), "asunto", "mensaje");

        assertEquals("default", contenido.origen());
        assertTrue(contenido.html().contains("<a href=\"" + ENLACE + "\""), contenido.html());
        assertTrue(contenido.textoPlano().contains(ENLACE));
        assertTrue(contenido.textoPlano().contains("Ana"));
    }

    @Test
    void un_enlace_que_no_es_http_no_se_convierte_en_vinculo_y_se_escapa() {
        final ContenidoRenderizado contenido = renderer().renderizar(
                "VERIFICACION_CORREO",
                Map.of("nombre", "Ana", "enlace", "javascript:alert(1)\"><script>"),
                "asunto", "mensaje");

        assertFalse(contenido.html().contains("<a href=\"javascript"), contenido.html());
        assertFalse(contenido.html().contains("<script>"), contenido.html());
        assertTrue(contenido.html().contains("&lt;script&gt;"), contenido.html());
    }

    @Test
    void un_nombre_con_html_se_escapa() {
        final ContenidoRenderizado contenido = renderer().renderizar(
                "VERIFICACION_CORREO", Map.of("nombre", "<b>Ana</b>", "enlace", ENLACE), "asunto", "mensaje");

        assertFalse(contenido.html().contains("<b>Ana</b>"));
        assertTrue(contenido.html().contains("&lt;b&gt;Ana&lt;/b&gt;"));
    }

    @Test
    void sin_enlace_cae_al_asunto_y_mensaje_guardados() {
        final ContenidoRenderizado contenido = renderer().renderizar(
                "VERIFICACION_CORREO", Map.of("nombre", "Ana"), "Verifique su correo institucional", "Se registro una cuenta.");

        assertEquals("stored", contenido.origen());
        assertTrue(contenido.textoPlano().contains("Se registro una cuenta."));
    }
}
