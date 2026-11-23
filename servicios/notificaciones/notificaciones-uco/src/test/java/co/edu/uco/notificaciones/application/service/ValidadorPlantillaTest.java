package co.edu.uco.notificaciones.application.service;

import co.edu.uco.notificaciones.domain.ErrorNegocio;
import co.edu.uco.notificaciones.domain.Marcador;
import org.junit.jupiter.api.Test;

import java.util.List;

import static co.edu.uco.notificaciones.soporte.Datos.marcador;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidadorPlantillaTest {

    private final List<Marcador> marcadores = List.of(marcador("nombre", true), marcador("nota", false));

    private String codigo(final String asunto, final String cuerpo) {
        return assertThrows(ErrorNegocio.class, () -> ValidadorPlantilla.validar(marcadores, asunto, cuerpo)).codigo();
    }

    @Test
    void aceptaPlantillaValida() {
        assertDoesNotThrow(() -> ValidadorPlantilla.validar(marcadores, "Hola {nombre}", "Cuerpo {nota}"));
        assertDoesNotThrow(() -> ValidadorPlantilla.validar(marcadores, "Asunto", "Cuerpo con {nombre} y sin llaves extra"));
    }

    @Test
    void rechazaAsuntoYCuerpoInvalidos() {
        assertEquals("PLANTILLA_ASUNTO_INVALIDO", codigo(null, "{nombre}"));
        assertEquals("PLANTILLA_ASUNTO_INVALIDO", codigo(" ", "{nombre}"));
        assertEquals("PLANTILLA_ASUNTO_INVALIDO", codigo("x".repeat(201), "{nombre}"));
        assertEquals("PLANTILLA_CUERPO_INVALIDO", codigo("a", null));
        assertEquals("PLANTILLA_CUERPO_INVALIDO", codigo("a", ""));
        assertEquals("PLANTILLA_CUERPO_INVALIDO", codigo("a", "x".repeat(4001)));
    }

    @Test
    void rechazaTipoSinMarcadores() {
        assertEquals("PLANTILLA_SIN_MARCADORES",
                assertThrows(ErrorNegocio.class, () -> ValidadorPlantilla.validar(List.of(), "a", "b")).codigo());
    }

    @Test
    void rechazaMarcadorObligatorioAusente() {
        assertEquals("PLANTILLA_MARCADOR_OBLIGATORIO_AUSENTE", codigo("Asunto", "Sin el nombre {nota}"));
    }

    @Test
    void rechazaMarcadorNoPermitido() {
        assertEquals("PLANTILLA_MARCADOR_NO_PERMITIDO", codigo("{nombre}", "Hola {desconocido}"));
    }

    @Test
    void rechazaLlavesSueltas() {
        assertEquals("PLANTILLA_LLAVES_INVALIDAS", codigo("{nombre}", "Hola { mundo"));
        assertEquals("PLANTILLA_LLAVES_INVALIDAS", codigo("{nombre}", "Hola } mundo"));
    }
}
