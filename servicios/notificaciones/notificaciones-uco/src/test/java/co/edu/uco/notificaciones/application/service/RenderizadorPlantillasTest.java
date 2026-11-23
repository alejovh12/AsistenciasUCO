package co.edu.uco.notificaciones.application.service;

import co.edu.uco.notificaciones.domain.ContenidoRenderizado;
import co.edu.uco.notificaciones.soporte.Datos;
import co.edu.uco.notificaciones.soporte.MetricasFake;
import co.edu.uco.notificaciones.soporte.PlantillaRepoFake;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RenderizadorPlantillasTest {

    private static final String LAYOUT = "<html>{{contenido}}<footer>{{pie}}</footer></html>";

    private PlantillaRepoFake repo;
    private MetricasFake metricas;
    private RenderizadorPlantillas renderizador;

    @BeforeEach
    void preparar() {
        repo = Datos.repoConTipoSaludo();
        metricas = new MetricasFake();
        renderizador = new RenderizadorPlantillas(repo, metricas, "http://plataforma.local", LAYOUT);
    }

    @Test
    void usaLaVersionActivaYSustituyeMarcadores() {
        final ContenidoRenderizado r = renderizador.renderizar("SALUDO", "es",
                Map.of("nombre", "Ana", "enlace", "https://uco.edu.co/x?a=1&b=2", "nota", "<b>hola</b>"), "as", "msg", null);
        assertEquals("activa", r.origen());
        assertEquals("Hola Ana", r.asunto());
        assertTrue(r.html().contains("<p>Hola Ana.</p><p>Abra <a href=\"https://uco.edu.co/x?a=1&amp;b=2\" style=\"color:#0f5132;\">"));
        assertTrue(r.html().contains("Nota: &lt;b&gt;hola&lt;/b&gt;"));
        assertTrue(r.html().contains("<a href=\"http://plataforma.local\""));
        assertTrue(r.textoPlano().contains("Hola Ana.") && r.textoPlano().endsWith("http://plataforma.local"));
        assertTrue(metricas.contiene("notification.template.render.total|origin,activa"));
    }

    @Test
    void elEnlaceNoHttpSeEscapaComoTexto() {
        final ContenidoRenderizado r = renderizador.renderizar("SALUDO", "es", Map.of("nombre", "Ana", "enlace", "javascript:alert(1)"), null, null, null);
        assertFalse(r.html().contains("<a href=\"javascript"));
        assertTrue(r.html().contains("javascript:alert(1)"));
        final ContenidoRenderizado conEspacio = renderizador.renderizar("SALUDO", "es", Map.of("nombre", "Ana", "enlace", "http://a b"), null, null, null);
        assertFalse(conEspacio.html().contains("<a href=\"http://a b"));
    }

    @Test
    void siLaActivaNoEsValidaCaeALaVersionDeFabrica() {
        repo.version("SALUDO", 1, "Hola {nombre}", "Fabrica {nombre}", false);
        repo.versiones.removeIf(v -> v.version() == 1 && v.cuerpo().startsWith("Hola"));
        repo.version("SALUDO", 2, "Asunto {intruso}", "Cuerpo {nombre}", true);
        final ContenidoRenderizado r = renderizador.renderizar("SALUDO", "es", Map.of("nombre", "Ana"), null, null, null);
        assertEquals("defecto", r.origen());
        assertEquals("Hola Ana", r.asunto());
    }

    @Test
    void siFaltaUnDatoObligatorioUsaElTextoGuardado() {
        final ContenidoRenderizado r = renderizador.renderizar("SALUDO", "es", Map.of("nota", "x"), "Asunto\nguardado", "Primera linea\n\nSegunda <b>", null);
        assertEquals("stored", r.origen());
        assertEquals("Asunto guardado", r.asunto());
        assertTrue(r.html().contains("<p>Primera linea</p><p>Segunda &lt;b&gt;</p>"));
    }

    @Test
    void siLaBdFallaUsaElTextoGuardado() {
        repo.fallar = true;
        final ContenidoRenderizado r = renderizador.renderizar("SALUDO", "es", Map.of("nombre", "Ana"), "A", "M", null);
        assertEquals("stored", r.origen());
    }

    @Test
    void tipoVacioYSinTextoGuardadoDevuelveContenidoVacio() {
        final ContenidoRenderizado r = renderizador.renderizar(" ", null, null, null, null, null);
        assertEquals("stored", r.origen());
        assertEquals("", r.asunto());
        assertFalse(r.html().contains("<p>Hola"));
    }

    @Test
    void anteponeElAvisoDelModoDePruebas() {
        final ContenidoRenderizado r = renderizador.renderizar("SALUDO", "es", Map.of("nombre", "Ana"), null, null, "  MODO DE PRUEBAS <x>  ");
        assertTrue(r.textoPlano().startsWith("MODO DE PRUEBAS <x>\n\nHola Ana."));
        assertTrue(r.html().contains("<p>MODO DE PRUEBAS &lt;x&gt;</p><p>Hola Ana.</p>"));
        final ContenidoRenderizado guardado = renderizador.renderizar("NO_EXISTE", "es", Map.of(), "A", "M", "AVISO");
        assertTrue(guardado.html().contains("<p>AVISO</p><p>M</p>"));
    }

    @Test
    void sinUrlDePlataformaElPieNoLlevaEnlace() {
        final RenderizadorPlantillas sinUrl = new RenderizadorPlantillas(repo, metricas, null, LAYOUT);
        final ContenidoRenderizado r = sinUrl.renderizar("SALUDO", "es", Map.of("nombre", "Ana"), null, null, null);
        assertFalse(r.html().contains("Ir a la plataforma"));
        assertTrue(r.textoPlano().endsWith("Sistema de Gestión de Asistencia"));
    }

    @Test
    void recortaYLimpiaElAsunto() {
        repo.versiones.removeIf(v -> v.version() == 1);
        repo.version("SALUDO", 2, "{nombre}", "{nombre}", true);
        final String largo = "x".repeat(250);
        final ContenidoRenderizado r = renderizador.renderizar("SALUDO", "es", Map.of("nombre", "a\r\nb" + largo), null, null, null);
        assertEquals(200, r.asunto().length());
        assertTrue(r.asunto().startsWith("a b"));
    }

    @Test
    void vistaPreviaExplicitaSustituyeSinConsultarElRepositorio() {
        final ContenidoRenderizado r = renderizador.renderizarExplicito("Hola {nombre}", "Cuerpo {nombre} {nota}", Map.of("nombre", "Ana"));
        assertEquals("preview", r.origen());
        assertEquals("Hola Ana", r.asunto());
        final ContenidoRenderizado vacio = renderizador.renderizarExplicito(null, null, null);
        assertEquals("", vacio.asunto());
    }

    @Test
    void escaparHtmlCubreTodosLosCaracteres() {
        assertEquals("&amp;&lt;&gt;&quot;&#39;x", RenderizadorPlantillas.escaparHtml("&<>\"'x"));
        assertEquals("", RenderizadorPlantillas.escaparHtml(null));
        assertEquals("", RenderizadorPlantillas.escaparHtml(""));
    }

    @Test
    void exigeDependencias() {
        assertThrows(NullPointerException.class, () -> new RenderizadorPlantillas(null, metricas, "", LAYOUT));
        assertThrows(NullPointerException.class, () -> new RenderizadorPlantillas(repo, null, "", LAYOUT));
        assertThrows(NullPointerException.class, () -> new RenderizadorPlantillas(repo, metricas, "", null));
    }
}
