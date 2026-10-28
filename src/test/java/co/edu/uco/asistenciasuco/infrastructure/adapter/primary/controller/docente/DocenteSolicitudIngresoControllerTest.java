package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.docente.consultarsolicitudesingreso.primaryports.ConsultarSolicitudesIngresoDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarsolicitudesingreso.primaryports.dto.ConsultarSolicitudesIngresoDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarsolicitudesingreso.primaryports.dto.SolicitudIngresoDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarsolicitudesingreso.primaryports.dto.SolicitudesIngresoDocentePaginaDTO;
import co.edu.uco.asistenciasuco.application.features.docente.resolversolicitudingreso.primaryports.ResolverSolicitudIngresoInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.resolversolicitudingreso.primaryports.dto.ResolverSolicitudIngresoDTO;
import co.edu.uco.asistenciasuco.application.features.docente.resolversolicitudingreso.primaryports.dto.ResultadoSolicitudIngresoDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU053-HU055: bandeja del docente y resolucion de solicitudes de ingreso (CTD-01..CTD-03). */
class DocenteSolicitudIngresoControllerTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID SOLICITUD = UUID.randomUUID();

    private final ConsultarSolicitudesIngresoDocenteInputPort consultar = mock(ConsultarSolicitudesIngresoDocenteInputPort.class);
    private final ResolverSolicitudIngresoInputPort resolver = mock(ResolverSolicitudIngresoInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new DocenteSolicitudIngresoController(consultar, resolver, identity))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> java.util.Optional.empty())).build();

    @Test
    void CTD01_bandeja_con_fechas_utc_cupo_y_posicion() throws Exception {
        when(consultar.execute(new ConsultarSolicitudesIngresoDocenteDTO(ACTOR, GRUPO, "E", 1, 20))).thenReturn(
                new SolicitudesIngresoDocentePaginaDTO(List.of(new SolicitudIngresoDocenteDTO(SOLICITUD, UUID.randomUUID(),
                        "Beto Qa", 970005302, "EN_ESPERA", Instant.parse("2026-10-27T13:00:00Z"), null, null, 1, 30, 0)), 3));

        mvc.perform(get("/api/v1/docente/grupos/{grupoId}/solicitudes-ingreso", GRUPO)
                        .param("estado", "E").param("pagina", "1").param("tamano", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.datos[0].id").value(SOLICITUD.toString()))
                .andExpect(jsonPath("$.datos[0].estado").value("EN_ESPERA"))
                .andExpect(jsonPath("$.datos[0].fechaSolicitudUtc").value("2026-10-27T13:00:00Z"))
                .andExpect(jsonPath("$.datos[0].posicionEspera").value(1))
                .andExpect(jsonPath("$.datos[0].cuposDisponibles").value(0));
    }

    @Test
    void CTD02_resolver_devuelve_el_estado_resultante() throws Exception {
        when(resolver.execute(new ResolverSolicitudIngresoDTO(ACTOR, SOLICITUD, "RECHAZAR", "Cupo reservado")))
                .thenReturn(new ResultadoSolicitudIngresoDTO(SOLICITUD, "RECHAZADA"));

        mvc.perform(patch("/api/v1/docente/solicitudes-ingreso/{id}", SOLICITUD).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accion\":\"RECHAZAR\",\"motivo\":\"Cupo reservado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.estadoResultante").value("RECHAZADA"));
    }

    @Test
    void CTD03_no_docente_es_403() throws Exception {
        when(resolver.execute(new ResolverSolicitudIngresoDTO(ACTOR, SOLICITUD, "ACEPTAR", null)))
                .thenThrow(new ForbiddenException("No fue posible resolver el docente autenticado."));

        mvc.perform(patch("/api/v1/docente/solicitudes-ingreso/{id}", SOLICITUD).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accion\":\"ACEPTAR\"}"))
                .andExpect(status().isForbidden());
    }
}
