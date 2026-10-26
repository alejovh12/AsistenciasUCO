package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarperiodoacademico.primaryports.GestionarPeriodoAcademicoInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarperiodoacademico.primaryports.dto.GuardarPeriodoAcademicoDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU096/HU097/HU098: contrato HTTP de alta, modificacion y eliminacion de periodos academicos. */
class AdminPeriodoAcademicoControllerTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID PERIODO = UUID.randomUUID();
    private static final String BODY = "{\"nombre\":\"2027-1\",\"fechaInicio\":\"2027-02-01\",\"fechaFin\":\"2027-06-30\"}";

    private final GestionarPeriodoAcademicoInputPort periodos = mock(GestionarPeriodoAcademicoInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AdminPeriodoAcademicoController(periodos, identity))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> java.util.Optional.empty())).build();

    @Test
    void crear_y_actualizar_mapean_cuerpo_ruta_y_actor() throws Exception {
        mvc.perform(post("/api/v1/admin/periodos-academicos").contentType("application/json").content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.exitoso").value(true));
        mvc.perform(put("/api/v1/admin/periodos-academicos/{id}", PERIODO).contentType("application/json").content(BODY))
                .andExpect(status().isOk());

        final ArgumentCaptor<GuardarPeriodoAcademicoDTO> creado = ArgumentCaptor.forClass(GuardarPeriodoAcademicoDTO.class);
        verify(periodos).crear(creado.capture());
        assertNull(creado.getValue().idPeriodo());
        assertEquals("2027-1", creado.getValue().nombre());
        assertEquals(LocalDate.of(2027, 2, 1), creado.getValue().fechaInicio());
        assertEquals(LocalDate.of(2027, 6, 30), creado.getValue().fechaFin());
        assertEquals(ACTOR, creado.getValue().usuario());

        final ArgumentCaptor<GuardarPeriodoAcademicoDTO> actualizado = ArgumentCaptor.forClass(GuardarPeriodoAcademicoDTO.class);
        verify(periodos, times(1)).actualizar(actualizado.capture());
        assertEquals(PERIODO, actualizado.getValue().idPeriodo());
    }

    @Test
    void eliminar_delega_con_el_actor_y_preserva_el_conflicto() throws Exception {
        mvc.perform(delete("/api/v1/admin/periodos-academicos/{id}", PERIODO))
                .andExpect(status().isOk()).andExpect(jsonPath("$.exitoso").value(true));
        verify(periodos).eliminar(PERIODO, ACTOR);

        final UUID conGrupos = UUID.randomUUID();
        doThrow(new ConflictException("El período académico tiene grupos o sesiones asociados."))
                .when(periodos).eliminar(conGrupos, ACTOR);
        mvc.perform(delete("/api/v1/admin/periodos-academicos/{id}", conGrupos))
                .andExpect(status().isConflict());
    }

    @Test
    void cuerpo_ausente_responde_400() throws Exception {
        mvc.perform(post("/api/v1/admin/periodos-academicos").contentType("application/json"))
                .andExpect(status().isBadRequest());
    }
}
