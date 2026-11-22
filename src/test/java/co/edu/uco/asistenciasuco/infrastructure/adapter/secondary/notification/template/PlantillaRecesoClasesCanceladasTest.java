package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template;

import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** DEC-081: plantillas por defecto del aviso de clases canceladas por un dia de receso; espejo del seed de la BD (migracion 68). */
class PlantillaRecesoClasesCanceladasTest {

    private static final String ASUNTO = "Clases canceladas por receso académico ({fecha})";

    private final PlantillaPorDefectoCatalogo catalogo = new PlantillaPorDefectoCatalogo();

    private void verificar(final String tipo, final String persona, final String cuerpo) {
        assertTrue(catalogo.listarTiposConocidos().contains(tipo));
        final PlantillaNotificacion plantilla = catalogo.obtener(tipo, "es").orElseThrow();

        assertEquals(ASUNTO, plantilla.asunto());
        assertEquals(cuerpo, plantilla.cuerpo());
        final List<String> marcadores = List.of(persona, "fecha", "motivo", "clases");
        assertEquals(marcadores.size(), plantilla.marcadores().size());
        for (final String marcador : marcadores) {
            assertTrue(plantilla.marcadores().stream().anyMatch(m -> m.nombre().equals(marcador) && m.obligatorio()), marcador);
            assertTrue(plantilla.cuerpo().contains("{" + marcador + "}"), marcador);
        }
    }

    @Test
    void la_plantilla_del_estudiante_dice_que_las_clases_no_cuentan_como_inasistencia() {
        verificar(Notificacion.TIPO_RECESO_CLASES_CANCELADAS_ESTUDIANTE, "estudiante",
                "Hola {estudiante}. La institución declaró receso académico el {fecha}. Motivo: {motivo}. "
                        + "Estas clases quedaron canceladas y no cuentan como inasistencia: {clases}.");
    }

    @Test
    void la_plantilla_del_docente_dice_que_no_requieren_toma_de_asistencia() {
        verificar(Notificacion.TIPO_RECESO_CLASES_CANCELADAS_DOCENTE, "docente",
                "Hola {docente}. La institución declaró receso académico el {fecha}. Motivo: {motivo}. "
                        + "Estas clases suyas quedaron canceladas y no requieren toma de asistencia: {clases}.");
    }
}