package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template;

import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** NOTOMA-001: plantilla por defecto del aviso a la coordinacion de la sesion sin toma; espejo de la plantilla sembrada en la base de datos. */
class PlantillaSesionSinTomaCoordinadorTest {

    private static final List<String> MARCADORES = List.of(
            "coordinador", "docente", "asignatura", "grupo", "fecha", "horaInicio", "horaFin", "enlaceSesiones");

    @Test
    void el_tipo_es_conocido_y_la_plantilla_por_defecto_tiene_los_ocho_marcadores_obligatorios() {
        final PlantillaPorDefectoCatalogo catalogo = new PlantillaPorDefectoCatalogo();
        assertTrue(catalogo.listarTiposConocidos().contains(Notificacion.TIPO_SESION_SIN_TOMA_COORDINADOR));

        final PlantillaNotificacion plantilla = catalogo.obtener(Notificacion.TIPO_SESION_SIN_TOMA_COORDINADOR, "es").orElseThrow();

        assertEquals("Sesión sin toma de asistencia: {asignatura} ({fecha})", plantilla.asunto());
        assertEquals("Hola {coordinador}. El docente {docente} no tomó asistencia en la sesión de {asignatura} del grupo {grupo} del {fecha} "
                + "de {horaInicio} a {horaFin}. Este aviso es solo informativo; puede revisar el grupo en {enlaceSesiones}.", plantilla.cuerpo());
        assertEquals(MARCADORES.size(), plantilla.marcadores().size());
        for (final String marcador : MARCADORES) {
            assertTrue(plantilla.marcadores().stream().anyMatch(m -> m.nombre().equals(marcador) && m.obligatorio()), marcador);
            assertTrue(plantilla.cuerpo().contains("{" + marcador + "}"), marcador);
        }
    }
}
