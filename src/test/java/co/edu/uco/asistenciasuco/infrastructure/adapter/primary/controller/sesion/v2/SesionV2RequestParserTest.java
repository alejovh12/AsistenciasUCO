package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.v2;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.RequestValidationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
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

        final RequestValidationException exception = assertThrows(RequestValidationException.class,
                () -> SesionV2RequestParser.parseCrear(Map.of(
                        "grupo", UUID.randomUUID().toString(),
                        "nombre", "UTC",
                        "fechaHoraInicio", hostile,
                        "fechaHoraFin", "2042-07-15T17:00:00Z",
                        "usuarioEjecutor", UUID.randomUUID().toString()
                )));

        assertEquals(2, exception.getValidationResult().issues().size());
        assertTrue(exception.getValidationResult().issues().stream()
                .anyMatch(issue -> issue.field().equals("fechaHoraInicio")));
        assertTrue(exception.getValidationResult().issues().stream()
                .anyMatch(issue -> issue.field().equals("usuarioEjecutor")));
        assertTrue(exception.getValidationResult().issues().stream()
                .noneMatch(issue -> issue.message().contains(hostile)));
    }
}
