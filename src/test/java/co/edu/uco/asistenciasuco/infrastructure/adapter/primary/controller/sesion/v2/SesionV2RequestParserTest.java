package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.v2;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.RequestValidationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SesionV2RequestParserTest {

    @Test
    void normaliza_offset_a_instante_utc_sin_perder_precision_de_siete_digitos() {
        final UUID grupo = UUID.randomUUID();

        final SesionV2RequestParser.CrearSesionV2 parsed = SesionV2RequestParser.parseCrear(Map.of(
                "grupo", grupo.toString(),
                "nombre", "UTC exacto",
                "fechaHoraInicio", "2042-07-15T16:00:00.1234567+02:00",
                "fechaHoraFin", "2042-07-15T17:30:00.7654321+02:00"
        ));

        assertEquals(grupo, parsed.grupo());
        assertEquals(LocalDateTime.parse("2042-07-15T14:00:00.123456700"), parsed.inicioUtc());
        assertEquals(LocalDateTime.parse("2042-07-15T15:30:00.765432100"), parsed.finUtc());
    }

    @Test
    void agrega_errores_de_campos_desconocidos_y_formato_sin_reflejar_el_valor_hostil() {
        final String hostile = "2042-07-15T16:00:00+99:99";
        final UUID grupo = UUID.randomUUID();
        final UUID usuarioEjecutor = UUID.randomUUID();
        final Map<String, Object> invalidRequest = Map.of(
                "grupo", grupo.toString(),
                "nombre", "UTC",
                "fechaHoraInicio", hostile,
                "fechaHoraFin", "2042-07-15T17:00:00Z",
                "usuarioEjecutor", usuarioEjecutor.toString()
        );

        final RequestValidationException exception = assertThrows(RequestValidationException.class,
                () -> SesionV2RequestParser.parseCrear(invalidRequest));

        assertEquals(2, exception.getValidationResult().issues().size());
        assertTrue(exception.getValidationResult().issues().stream()
                .anyMatch(issue -> issue.field().equals("fechaHoraInicio")));
        assertTrue(exception.getValidationResult().issues().stream()
                .anyMatch(issue -> issue.field().equals("usuarioEjecutor")));
        assertTrue(exception.getValidationResult().issues().stream()
                .noneMatch(issue -> issue.message().contains(hostile)));
    }

    @Test
    void parsea_actualizacion_y_rechaza_request_nulo() {
        final SesionV2RequestParser.ActualizarSesionV2 parsed = SesionV2RequestParser.parseActualizar(Map.of(
                "nombre", "Sesion editada",
                "fechaHoraInicio", "2042-07-15T16:00:00.1234567+02:00",
                "fechaHoraFin", "2042-07-15T17:30:00.7654321+02:00"
        ));
        assertEquals("Sesion editada", parsed.nombre());
        assertEquals(LocalDateTime.parse("2042-07-15T14:00:00.123456700"), parsed.inicioUtc());
        assertThrows(RequestValidationException.class, () -> SesionV2RequestParser.parseCrear(null));
        assertThrows(RequestValidationException.class, () -> SesionV2RequestParser.parseActualizar(null));
    }

    @Test
    void agrega_errores_para_tipos_longitudes_uuid_y_instantes_invalidos() {
        final Map<String, Object> invalid = new HashMap<>();
        invalid.put("grupo", "00000000-0000-0000-0000-000000000000");
        invalid.put("nombre", " ");
        invalid.put("fechaHoraInicio", 123);
        invalid.put("fechaHoraFin", "2042-07-15T17:30:00.12345678Z");
        assertThrows(RequestValidationException.class, () -> SesionV2RequestParser.parseCrear(invalid));

        invalid.put("grupo", 123);
        invalid.put("nombre", "x".repeat(51));
        assertThrows(RequestValidationException.class, () -> SesionV2RequestParser.parseCrear(invalid));

        final Map<String, Object> missing = new HashMap<>();
        missing.put("grupo", null);
        missing.put("nombre", null);
        missing.put("fechaHoraInicio", null);
        missing.put("fechaHoraFin", null);
        assertThrows(RequestValidationException.class, () -> SesionV2RequestParser.parseCrear(missing));
    }

    @Test
    void DEC_072_rechaza_crear_una_sesion_que_termina_al_dia_siguiente_en_la_zona_del_cliente() {
        final Map<String, Object> cruzaMedianoche = Map.of(
                "grupo", UUID.randomUUID().toString(),
                "nombre", "Test nueva clase",
                "fechaHoraInicio", "2042-11-13T20:30:00-05:00",
                "fechaHoraFin", "2042-11-14T10:00:00-05:00");

        final RequestValidationException exception = assertThrows(RequestValidationException.class,
                () -> SesionV2RequestParser.parseCrear(cruzaMedianoche));

        assertTrue(exception.getValidationResult().issues().stream()
                .anyMatch(issue -> issue.field().equals("fechaHoraFin") && issue.message().contains("medianoche")));
    }

    @Test
    void DEC_072_el_mismo_dia_se_mide_con_el_offset_del_cliente_aunque_en_utc_cambie_la_fecha() {
        // 20:30 a 22:00 en Bogota son 01:30 y 03:00 UTC del dia siguiente: sigue siendo el mismo dia para el cliente.
        final SesionV2RequestParser.CrearSesionV2 parsed = SesionV2RequestParser.parseCrear(Map.of(
                "grupo", UUID.randomUUID().toString(),
                "nombre", "Nocturna",
                "fechaHoraInicio", "2042-11-13T20:30:00-05:00",
                "fechaHoraFin", "2042-11-13T22:00:00-05:00"));

        assertEquals(LocalDateTime.parse("2042-11-14T01:30:00"), parsed.inicioUtc());
    }

    @Test
    void DEC_072_editar_conserva_los_instantes_de_una_sesion_antigua_que_cruza_la_medianoche() {
        final SesionV2RequestParser.ActualizarSesionV2 parsed = SesionV2RequestParser.parseActualizar(Map.of(
                "nombre", "Renombrada",
                "fechaHoraInicio", "2042-11-13T22:30:00-05:00",
                "fechaHoraFin", "2042-11-14T00:35:00-05:00"));

        assertEquals("Renombrada", parsed.nombre());
    }

    @Test
    void DEC_073_rechaza_un_nombre_con_html() {
        final Map<String, Object> conHtml = Map.of(
                "grupo", UUID.randomUUID().toString(),
                "nombre", "<script>alert(1)</script>",
                "fechaHoraInicio", "2042-11-13T08:00:00-05:00",
                "fechaHoraFin", "2042-11-13T10:00:00-05:00");

        final RequestValidationException exception = assertThrows(RequestValidationException.class,
                () -> SesionV2RequestParser.parseCrear(conHtml));

        assertTrue(exception.getValidationResult().issues().stream().anyMatch(issue -> issue.field().equals("nombre")));
    }
}
