package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion;

import co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.primaryports.ActualizarSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.primaryports.dto.ActualizarSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.primaryports.CerrarSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.ConsultarSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.dto.ConsultarSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.dto.SesionConsultadaDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.CrearSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.dto.CrearSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.primaryports.GenerarSesionesGrupoInputPort;
import co.edu.uco.asistenciasuco.application.exception.business.FeatureUnavailableException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.AuthenticatedUserProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SesionControllerContractTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID GROUP = UUID.randomUUID();
    private static final UUID SESSION = UUID.randomUUID();
    private static final LocalDateTime START = LocalDateTime.of(2026, 9, 14, 8, 0);
    private final CrearSesionInputPort create = mock(CrearSesionInputPort.class);
    private final ConsultarSesionInputPort query = mock(ConsultarSesionInputPort.class);
    private final CerrarSesionInputPort close = mock(CerrarSesionInputPort.class);
    private final ActualizarSesionInputPort update = mock(ActualizarSesionInputPort.class);
    private final GenerarSesionesGrupoInputPort generate = mock(GenerarSesionesGrupoInputPort.class);
    private final AuthenticatedUserProvider identity = () -> ACTOR;
    private final SesionController controller = new SesionController(create, query, close, update, generate, identity);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @Test
    void createAcceptsLegacyNameAndRoomAndPassesParsedDatesAndActor() throws Exception {
        mvc.perform(post("/api/v1/sesiones").contentType("application/json")
                        .content("""
                                {"grupo":"%s","nombre":"Tema válido","descripcion":"Descripción suficiente",
                                 "fechaHoraInicio":"2026-09-14T08:00","fechaHoraFin":"2026-09-14T09:00",
                                 "room":"A101","tipo":"PRESENCIAL"}
                                """.formatted(GROUP)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exitoso").value(true));
        final var captor = ArgumentCaptor.forClass(CrearSesionDTO.class);
        verify(create).execute(captor.capture());
        assertEquals(GROUP, captor.getValue().getGrupo());
        assertEquals("Tema válido", captor.getValue().getTema());
        assertEquals("A101", captor.getValue().getAula());
        assertEquals(START, captor.getValue().getFechaHoraInicio());
        assertEquals(START.plusHours(1), captor.getValue().getFechaHoraFin());
        assertEquals(ACTOR, captor.getValue().getDocente());
        assertEquals("PRESENCIAL", captor.getValue().getTipo());
    }

    @Test
    void queryByPathAndLegacyBodyPreserveCompleteSessionResponse() throws Exception {
        when(query.execute(any(ConsultarSesionDTO.class))).thenReturn(new SesionConsultadaDTO(SESSION, GROUP,
                "Sesión 1", 1, "S01", 2, "G01", "Grupo 1", START, START.plusHours(1)));

        mvc.perform(get("/api/v1/sesiones/{id}", SESSION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.sesion").value(SESSION.toString()))
                .andExpect(jsonPath("$.datos.grupo").value(GROUP.toString()))
                .andExpect(jsonPath("$.datos.nombre").value("Sesión 1"))
                .andExpect(jsonPath("$.datos.numero").value(1))
                .andExpect(jsonPath("$.datos.codigo").value("S01"))
                .andExpect(jsonPath("$.datos.numeroSemana").value(2))
                .andExpect(jsonPath("$.datos.codigoGrupo").value("G01"))
                .andExpect(jsonPath("$.datos.nombreGrupo").value("Grupo 1"))
                .andExpect(jsonPath("$.datos.fechaHoraInicio").value("2026-09-14T08:00:00"))
                .andExpect(jsonPath("$.datos.fechaHoraFin").value("2026-09-14T09:00:00"));
        mvc.perform(post("/api/v1/sesiones/consultas").contentType("application/json")
                        .content("{\"sesion\":\"" + SESSION + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.datos.sesion").value(SESSION.toString()));
        final var captor = ArgumentCaptor.forClass(ConsultarSesionDTO.class);
        verify(query, org.mockito.Mockito.times(2)).execute(captor.capture());
        captor.getAllValues().forEach(dto -> assertEquals(SESSION, dto.getSesion()));
    }

    @Test
    void updateCloseAndGeneratePassPathBodyAndActor() throws Exception {
        mvc.perform(put("/api/v1/sesiones/{id}", SESSION).contentType("application/json")
                        .content("""
                                {"tema":"Tema actualizado","fechaHoraInicio":"2026-09-14T08:00",
                                 "fechaHoraFin":"2026-09-14T10:00","room":"B202","descripcion":"Cambio de aula"}
                                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.exitoso").value(true));
        final var updateCaptor = ArgumentCaptor.forClass(ActualizarSesionDTO.class);
        verify(update).execute(updateCaptor.capture());
        assertEquals(SESSION, updateCaptor.getValue().getSesion());
        assertEquals("Tema actualizado", updateCaptor.getValue().getNombre());
        assertEquals(START.plusHours(2), updateCaptor.getValue().getFechaHoraFin());
        assertEquals("B202", updateCaptor.getValue().getAula());
        assertEquals(ACTOR, updateCaptor.getValue().getDocente());

        mvc.perform(post("/api/v1/sesiones/cierres").contentType("application/json")
                        .content("{\"sesion\":\"" + SESSION + "\",\"observacionCierre\":\"Finalizada\"}"))
                .andExpect(status().isOk());
        final var closeCaptor = ArgumentCaptor.forClass(co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.primaryports.dto.CerrarSesionDTO.class);
        verify(close).execute(closeCaptor.capture());
        assertEquals(SESSION, closeCaptor.getValue().getSesion());
        assertEquals(ACTOR, closeCaptor.getValue().getDocente());

        mvc.perform(post("/api/v1/sesiones/grupo/{id}/generacion", GROUP))
                .andExpect(status().isCreated());
        final var generateCaptor = ArgumentCaptor.forClass(co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.primaryports.dto.GenerarSesionesGrupoDTO.class);
        verify(generate).execute(generateCaptor.capture());
        assertEquals(GROUP, generateCaptor.getValue().getGrupo());
    }

    @Test
    void invalidBodyIsRejectedBeforeCallingPorts() throws Exception {
        mvc.perform(post("/api/v1/sesiones").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/sesiones/consultas").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/sesiones/cierres").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/sesiones/{id}", SESSION).contentType("application/json")
                        .content("{\"fechaHoraInicio\":\"no-es-iso\"}"))
                .andExpect(status().isBadRequest());
        org.mockito.Mockito.verifyNoInteractions(create, query, close, update);
    }

    @Test
    void groupListingRemainsUnavailableUntilPublicDbContractExists() {
        assertThrows(FeatureUnavailableException.class, () -> controller.consultarSesionesPorGrupo(GROUP));
    }
}
