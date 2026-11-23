package co.edu.uco.notificaciones.application.service;

import co.edu.uco.notificaciones.application.model.TipoPlantillaAdmin;
import co.edu.uco.notificaciones.application.model.VistaPrevia;
import co.edu.uco.notificaciones.domain.ErrorNegocio;
import co.edu.uco.notificaciones.domain.VersionPlantilla;
import co.edu.uco.notificaciones.soporte.Datos;
import co.edu.uco.notificaciones.soporte.MetricasFake;
import co.edu.uco.notificaciones.soporte.PlantillaRepoFake;
import co.edu.uco.notificaciones.soporte.RelojFijo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GestionPlantillasTest {

    private PlantillaRepoFake repo;
    private GestionPlantillas gestion;

    @BeforeEach
    void preparar() {
        repo = Datos.repoConTipoSaludo();
        repo.tipo("SIN_MARCADORES", false);
        final var renderizador = new RenderizadorPlantillas(repo, new MetricasFake(), "", "{{contenido}}{{pie}}");
        gestion = new GestionPlantillas(repo, renderizador, new RelojFijo(Datos.AHORA));
    }

    @Test
    void consultaTiposConDescripcionMarcadoresEHistorial() {
        final List<TipoPlantillaAdmin> tipos = gestion.consultar(null);
        assertEquals(2, tipos.size());
        final TipoPlantillaAdmin saludo = tipos.getFirst();
        assertEquals("SALUDO", saludo.tipo());
        assertEquals("Descripcion de SALUDO", saludo.descripcion());
        assertEquals("es", saludo.idioma());
        assertEquals(1, saludo.versionActiva());
        assertEquals("Hola {nombre}", saludo.asuntoActivo());
        assertEquals(3, saludo.marcadores().size());
        assertEquals(1, saludo.versiones().size());
        final TipoPlantillaAdmin sinVersion = tipos.get(1);
        assertEquals(0, sinVersion.versionActiva());
        assertEquals("", sinVersion.asuntoActivo());
        assertEquals(Datos.AHORA, sinVersion.actualizadoEn());
    }

    @Test
    void crearVersionValidaYQuedaActiva() {
        final VersionPlantilla v = gestion.crearVersion(" SALUDO ", "es", " Nuevo {nombre} ", " Cuerpo {nombre} ", " admin ");
        assertEquals(2, v.version());
        assertTrue(v.activa());
        assertEquals("Nuevo {nombre}", v.asunto());
        assertEquals("admin", v.creadoPor());
        assertFalse(repo.versiones("SALUDO", "es").getLast().activa());
        assertEquals("sin autor", gestion.crearVersion("SALUDO", null, "Otro {nombre}", "c {nombre}", "").creadoPor() == null ? "sin autor" : "con autor");
    }

    @Test
    void crearVersionRechazaTipoDesconocidoInternoOContenidoInvalido() {
        assertEquals("TIPO_NO_ENCONTRADO", assertThrows(ErrorNegocio.class, () -> gestion.crearVersion("NO_EXISTE", "es", "a", "b", null)).codigo());
        assertEquals("TIPO_NO_ENCONTRADO", assertThrows(ErrorNegocio.class, () -> gestion.crearVersion(null, "es", "a", "b", null)).codigo());
        assertEquals("TIPO_NO_ENCONTRADO", assertThrows(ErrorNegocio.class, () -> gestion.crearVersion("PRUEBA_CANAL_NOTIFICACION", "es", "a", "b", null)).codigo());
        assertEquals("PLANTILLA_MARCADOR_OBLIGATORIO_AUSENTE", assertThrows(ErrorNegocio.class, () -> gestion.crearVersion("SALUDO", "es", "a", "sin marcador", null)).codigo());
        assertEquals("PLANTILLA_ASUNTO_INVALIDO", assertThrows(ErrorNegocio.class, () -> gestion.crearVersion("SALUDO", "es", null, "{nombre}", null)).codigo());
        assertEquals("PLANTILLA_SIN_MARCADORES", assertThrows(ErrorNegocio.class, () -> gestion.crearVersion("SIN_MARCADORES", "es", "a", "b", null)).codigo());
    }

    @Test
    void activarVersionExistenteYRechazarInexistente() {
        gestion.crearVersion("SALUDO", "es", "Dos {nombre}", "c {nombre}", null);
        final VersionPlantilla activada = gestion.activarVersion("SALUDO", "es", 1);
        assertEquals(1, activada.version());
        assertTrue(activada.activa());
        assertEquals("VERSION_NO_ENCONTRADA", assertThrows(ErrorNegocio.class, () -> gestion.activarVersion("SALUDO", "es", 99)).codigo());
        assertEquals("TIPO_NO_ENCONTRADO", assertThrows(ErrorNegocio.class, () -> gestion.activarVersion("NADA", "es", 1)).codigo());
    }

    @Test
    void vistaPreviaUsaLosEjemplosDeLosMarcadores() {
        final VistaPrevia v = gestion.vistaPrevia("SALUDO", null, "Hola {nombre}", "Cuerpo {nombre} {nota}");
        assertEquals("preview", v.origen());
        assertEquals("Hola ejemplo-nombre", v.asunto());
        assertTrue(v.textoPlano().contains("ejemplo-nota"));
        assertTrue(v.html().contains("ejemplo-nombre"));
    }

    @Test
    void vistaPreviaSinMarcadoresUsaMuestrasBasicas() {
        final VistaPrevia v = gestion.vistaPrevia("SIN_MARCADORES", "es", "Grupo {grupo}", "Estudiante {estudiante}");
        assertEquals("Grupo G01 de Ingeniería de Software", v.asunto());
    }

    @Test
    void vistaPreviaConMarcadorSinEjemploUsaLaMuestraDeRespaldo() {
        repo.tipo("RARO", false, new co.edu.uco.notificaciones.domain.Marcador("porcentaje", "d", " ", false));
        assertEquals("Avance 15.5", gestion.vistaPrevia("RARO", "es", "Avance {porcentaje}", "x").asunto());
        assertEquals("TIPO_NO_ENCONTRADO", assertThrows(ErrorNegocio.class, () -> gestion.vistaPrevia("NO_EXISTE", "es", "a", "b")).codigo());
    }
}
