package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente;

import co.edu.uco.asistenciasuco.application.exception.business.FeatureUnavailableException;
import co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.primaryports.ResolverSolicitudRevisionAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.primaryports.dto.ResolverSolicitudRevisionAsistenciaDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.primaryports.ConsultarAsignaturasDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.primaryports.dto.AsignaturaDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.primaryports.ConsultarHorariosDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.primaryports.dto.HorarioDocenteDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.request.ResolverReclamoRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DocentePortalControllerTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID RECLAMO = UUID.randomUUID();

    private final ConsultarHorariosDocenteInputPort horarios = mock(ConsultarHorariosDocenteInputPort.class);
    private final ConsultarAsignaturasDocenteInputPort asignaturas = mock(ConsultarAsignaturasDocenteInputPort.class);
    private final ResolverSolicitudRevisionAsistenciaInputPort resolverReclamo =
            mock(ResolverSolicitudRevisionAsistenciaInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final DocentePortalController controller =
            new DocentePortalController(horarios, asignaturas, resolverReclamo, identity);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> java.util.Optional.empty())).build();

    @Test
    void consultarReclamos_no_implementado_lanza_featureUnavailable() {
        assertEquals("FEATURE_UNAVAILABLE",
                assertThrows(FeatureUnavailableException.class, controller::consultarReclamos).getCode());
    }

    @Test
    void resolverReclamo_mapea_cuerpo_de_la_peticion() throws Exception {
        final String body = "{\"accion\":\"APROBAR\",\"respuestaDocente\":\"Aceptada\"}";

        mvc.perform(patch("/api/v1/docente/reclamos/{id}", RECLAMO).contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true));

        final ArgumentCaptor<ResolverSolicitudRevisionAsistenciaDTO> captor =
                ArgumentCaptor.forClass(ResolverSolicitudRevisionAsistenciaDTO.class);
        verify(resolverReclamo).execute(captor.capture());
        assertEquals(RECLAMO, captor.getValue().getSolicitud());
        assertEquals("APROBAR", captor.getValue().getAccion());
        assertEquals("Aceptada", captor.getValue().getRespuestaDocente());
        assertEquals(ACTOR, captor.getValue().getUsuario());
    }

    @Test
    void resolverReclamo_con_peticion_nula_mapea_campos_nulos() {
        controller.resolverReclamo(RECLAMO, null);

        final ArgumentCaptor<ResolverSolicitudRevisionAsistenciaDTO> captor =
                ArgumentCaptor.forClass(ResolverSolicitudRevisionAsistenciaDTO.class);
        verify(resolverReclamo).execute(captor.capture());
        assertNull(captor.getValue().getAccion());
        assertEquals(ACTOR, captor.getValue().getUsuario());
    }

    @Test
    void consultarHorarios_usa_actor_autenticado() throws Exception {
        when(horarios.execute(ACTOR)).thenReturn(List.of(new HorarioDocenteDTO(
                UUID.randomUUID(), ACTOR, UUID.randomUUID(), "MAT-01", "Calculo", "G1", "LUNES",
                LocalTime.of(8, 0), LocalTime.of(10, 0), 25)));

        mvc.perform(get("/api/v1/docente/horarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos[0].nombreMateria").value("Calculo"));
        verify(horarios).execute(ACTOR);
    }

    @Test
    void cancelarSesion_no_implementado_lanza_featureUnavailable() {
        assertEquals("FEATURE_UNAVAILABLE",
                assertThrows(FeatureUnavailableException.class, () -> controller.cancelarSesion(RECLAMO)).getCode());
    }

    @Test
    void consultarAsignaturas_usa_actor_autenticado() throws Exception {
        when(asignaturas.execute(ACTOR)).thenReturn(List.of(new AsignaturaDocenteDTO(
                UUID.randomUUID(), "Calculo", UUID.randomUUID(), "Grupo 1", UUID.randomUUID(), "Ingenieria")));

        mvc.perform(get("/api/v1/docente/asignaturas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos[0].nombreAsignatura").value("Calculo"));
        verify(asignaturas).execute(ACTOR);
    }

    @Test
    void resolverReclamoRequest_usa_respuesta_como_fallback_de_respuestaDocente() {
        final ResolverReclamoRequest request = new ResolverReclamoRequest();
        request.setRespuesta("Fallback");

        assertEquals("Fallback", request.getRespuestaDocente());

        request.setRespuestaDocente("Directa");
        assertEquals("Directa", request.getRespuestaDocente());
    }
}
