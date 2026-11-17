package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.catalog.MensajeUsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.features.docente.common.dto.ResumenGruposDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarresumengrupos.primaryports.ConsultarResumenGruposDocenteInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** CIERRE-W3I: {@code GET /api/v1/docente/resumen-grupos}. */
class DocenteResumenGruposControllerTest {

    private static final UUID ACTOR = UUID.randomUUID();

    private final ConsultarResumenGruposDocenteInputPort resumen = mock(ConsultarResumenGruposDocenteInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new DocenteResumenGruposController(resumen, identity))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> java.util.Optional.empty())).build();

    @Test
    void devuelve_los_totales_del_docente_autenticado_en_el_sobre_estandar() throws Exception {
        when(resumen.execute(new ConsultarResumenGruposDocenteInputPort.Consulta(ACTOR)))
                .thenReturn(new ResumenGruposDocenteDTO(2, 4, 50, new BigDecimal("8.00")));

        mvc.perform(get("/api/v1/docente/resumen-grupos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.gruposHabilitados").value(2))
                .andExpect(jsonPath("$.datos.estudiantesActivos").value(4))
                .andExpect(jsonPath("$.datos.cupoTotal").value(50))
                .andExpect(jsonPath("$.datos.tasaOcupacion").value(8.00));
    }

    @Test
    void la_tasa_de_ocupacion_nula_viaja_como_null() throws Exception {
        when(resumen.execute(new ConsultarResumenGruposDocenteInputPort.Consulta(ACTOR)))
                .thenReturn(new ResumenGruposDocenteDTO(0, 0, 0, null));

        mvc.perform(get("/api/v1/docente/resumen-grupos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.cupoTotal").value(0))
                .andExpect(jsonPath("$.datos.tasaOcupacion").doesNotExist());
    }

    @Test
    void un_rol_distinto_de_docente_recibe_403() throws Exception {
        when(resumen.execute(new ConsultarResumenGruposDocenteInputPort.Consulta(ACTOR)))
                .thenThrow(new ForbiddenException(MensajeUsuarioErrorCode.ACC_APP_002));

        mvc.perform(get("/api/v1/docente/resumen-grupos")).andExpect(status().isForbidden());
    }

    @Test
    void el_constructor_exige_sus_dependencias() {
        assertThrows(NullPointerException.class, () -> new DocenteResumenGruposController(null, identity));
        assertThrows(NullPointerException.class, () -> new DocenteResumenGruposController(resumen, null));
    }
}
