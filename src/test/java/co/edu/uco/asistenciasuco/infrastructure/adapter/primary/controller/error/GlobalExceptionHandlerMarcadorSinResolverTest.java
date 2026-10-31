package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * IDENT-001 (VCE_001): el catalogo de la BD escribe el parametro como {@code {}}. Si el texto resuelto por codigo llega con
 * el marcador sin sustituir, la respuesta usa el mensaje de la excepcion (que ya incluye el dominio). Un texto del catalogo
 * sin marcador sigue teniendo prioridad, como hasta ahora.
 */
class GlobalExceptionHandlerMarcadorSinResolverTest {

    @RestController
    static class ProbeController {
        @GetMapping("/probe/con-parametro")
        void conParametro() {
            throw new ValidationException("VCE_001", "El correo del estudiante debe ser institucional y terminar en @uco.edu.co.");
        }

        @GetMapping("/probe/sin-parametro")
        void sinParametro() {
            throw new ValidationException("VCE_002", "Mensaje de la excepcion.");
        }
    }

    private MockMvc mvc(final String textoDelCatalogo) {
        return MockMvcBuilders.standaloneSetup(new ProbeController())
                .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.of(textoDelCatalogo)))
                .build();
    }

    @Test
    void con_marcador_sin_resolver_usa_el_mensaje_de_la_excepcion() throws Exception {
        mvc("El correo del estudiante debe ser institucional y terminar en {}.")
                .perform(get("/probe/con-parametro"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VCE_001"))
                .andExpect(jsonPath("$.message").value("El correo del estudiante debe ser institucional y terminar en @uco.edu.co."));
    }

    @Test
    void sin_marcador_el_texto_del_catalogo_conserva_la_prioridad() throws Exception {
        mvc("El enlace de verificación no es válido o ya venció.")
                .perform(get("/probe/sin-parametro"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El enlace de verificación no es válido o ya venció."));
    }
}
