package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsolicitudesingreso.primaryports.ConsultarSolicitudesIngresoEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsolicitudesingreso.primaryports.dto.ConsultarSolicitudesIngresoEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsolicitudesingreso.primaryports.dto.SolicitudIngresoEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsolicitudesingreso.primaryports.dto.SolicitudesIngresoEstudiantePaginaDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.retirarsolicitudingreso.primaryports.RetirarSolicitudIngresoInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.retirarsolicitudingreso.primaryports.dto.RetirarSolicitudIngresoDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.solicitaringresogrupo.primaryports.SolicitarIngresoGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.solicitaringresogrupo.primaryports.dto.SolicitarIngresoGrupoDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU053/HU055: el estudiante solicita, consulta y retira sus solicitudes de ingreso (CTE-01..CTE-04). */
class EstudianteSolicitudIngresoControllerTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID SOLICITUD = UUID.randomUUID();
    private static final String RUTA = "/api/v1/estudiante/solicitudes-matricula";

    private final SolicitarIngresoGrupoInputPort solicitar = mock(SolicitarIngresoGrupoInputPort.class);
    private final ConsultarSolicitudesIngresoEstudianteInputPort consultar = mock(ConsultarSolicitudesIngresoEstudianteInputPort.class);
    private final RetirarSolicitudIngresoInputPort retirar = mock(RetirarSolicitudIngresoInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new EstudianteSolicitudIngresoController(solicitar, consultar, retirar, identity))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> java.util.Optional.empty())).build();

    @Test
    void CTE01_solicitar_devuelve_el_id() throws Exception {
        when(solicitar.execute(new SolicitarIngresoGrupoDTO(ACTOR, GRUPO))).thenReturn(SOLICITUD);

        mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("{\"grupoId\":\"" + GRUPO + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.id").value(SOLICITUD.toString()));
    }

    @Test
    void CTE02_duplicada_es_409() throws Exception {
        when(solicitar.execute(new SolicitarIngresoGrupoDTO(ACTOR, GRUPO)))
                .thenThrow(new ConflictException("Ya existe una solicitud abierta para este grupo."));

        mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("{\"grupoId\":\"" + GRUPO + "\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void CTE03_mis_solicitudes_con_fechas_utc_y_motivo() throws Exception {
        when(consultar.execute(new ConsultarSolicitudesIngresoEstudianteDTO(ACTOR, null, null, null))).thenReturn(
                new SolicitudesIngresoEstudiantePaginaDTO(List.of(new SolicitudIngresoEstudianteDTO(SOLICITUD, GRUPO, 1, "Grupo 1",
                        UUID.randomUUID(), "Calculo", "Doc Uno", "RECHAZADA", Instant.parse("2026-10-27T13:00:00Z"),
                        Instant.parse("2026-10-27T14:00:00Z"), "Cupo reservado")), 1));

        mvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.datos[0].estado").value("RECHAZADA"))
                .andExpect(jsonPath("$.datos[0].motivoRechazo").value("Cupo reservado"))
                .andExpect(jsonPath("$.datos[0].fechaResolucionUtc").value("2026-10-27T14:00:00Z"));
    }

    @Test
    void CTE04_retirar_delega_y_ajena_es_404() throws Exception {
        mvc.perform(post(RUTA + "/{id}/retiro", SOLICITUD)).andExpect(status().isOk());
        verify(retirar).execute(new RetirarSolicitudIngresoDTO(ACTOR, SOLICITUD));

        final UUID ajena = UUID.randomUUID();
        doThrow(new co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException("No existe."))
                .when(retirar).execute(new RetirarSolicitudIngresoDTO(ACTOR, ajena));
        mvc.perform(post(RUTA + "/{id}/retiro", ajena)).andExpect(status().isNotFound());
    }
}
