package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.SoporteReclamoProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * CULM-001 D.2 (CONTRACT D 3.2): soportesJson de usp_consultar_detalle_solicitud_revision_docente y
 * usp_consultar_solicitudes_revision_estudiante es un arreglo [{idArchivo, nombreOriginal, tipoContenido, tamanioBytes}]
 * ordenado por adjuncion ('[]' sin soportes).
 */
class SoportesReclamoJsonTest {

    private static final UUID UNO = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID DOS = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void lee_los_soportes_en_el_orden_recibido() {
        final String json = "[{\"idArchivo\":\"" + UNO + "\",\"nombreOriginal\":\"uno.pdf\",\"tipoContenido\":\"application/pdf\",\"tamanioBytes\":1234},"
                + "{\"idArchivo\":\"" + DOS.toString().toUpperCase() + "\",\"nombreOriginal\":\"dos.png\",\"tipoContenido\":\"image/png\",\"tamanioBytes\":99}]";

        assertEquals(List.of(new SoporteReclamoProjection(UNO, "uno.pdf", "application/pdf", 1234L),
                new SoporteReclamoProjection(DOS, "dos.png", "image/png", 99L)), SoportesReclamoJson.leer(json));
    }

    @Test
    void sin_soportes_devuelve_una_lista_vacia() {
        assertEquals(List.of(), SoportesReclamoJson.leer(null));
        assertEquals(List.of(), SoportesReclamoJson.leer(""));
        assertEquals(List.of(), SoportesReclamoJson.leer("   "));
        assertEquals(List.of(), SoportesReclamoJson.leer("[]"));
    }

    @Test
    void los_campos_opcionales_ausentes_quedan_nulos_o_en_cero() {
        final List<SoporteReclamoProjection> soportes = SoportesReclamoJson.leer("[{\"idArchivo\":\"" + UNO + "\"}]");

        assertEquals(List.of(new SoporteReclamoProjection(UNO, null, null, 0L)), soportes);
    }

    @Test
    void la_lista_leida_es_inmutable() {
        final List<SoporteReclamoProjection> soportes = SoportesReclamoJson.leer("[{\"idArchivo\":\"" + UNO + "\"}]");

        assertThrows(UnsupportedOperationException.class, () -> soportes.add(new SoporteReclamoProjection(DOS, "x", "y", 1L)));
    }

    @Test
    void un_json_que_no_es_un_arreglo_de_soportes_es_un_fallo_tecnico() {
        assertThrows(DatabaseOperationException.class, () -> SoportesReclamoJson.leer("{\"idArchivo\":\"" + UNO + "\"}"));
        assertThrows(DatabaseOperationException.class, () -> SoportesReclamoJson.leer("[not json"));
        assertThrows(DatabaseOperationException.class, () -> SoportesReclamoJson.leer("[{\"nombreOriginal\":\"sin-id.pdf\"}]"));
        assertThrows(DatabaseOperationException.class, () -> SoportesReclamoJson.leer("[{\"idArchivo\":\"no-es-uuid\"}]"));
        assertThrows(DatabaseOperationException.class, () -> SoportesReclamoJson.leer("[7]"));
    }
}
