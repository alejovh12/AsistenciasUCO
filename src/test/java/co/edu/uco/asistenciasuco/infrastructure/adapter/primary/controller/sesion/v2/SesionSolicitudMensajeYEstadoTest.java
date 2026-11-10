package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.v2;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.primaryports.dto.ConsultasAsistenciaProgramaDTO.SesionGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.common.dto.SesionV2ConsultadaDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.common.entity.SesionProcedenciaEntity;
import co.edu.uco.asistenciasuco.application.features.sesion.common.mapper.SesionProcedenciaMapper;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.dto.SesionConsultadaDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.mapper.ConsultarSesionMapper;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.entity.SesionConsultadaEntity;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.mapper.ConsultarSesionRepositoryMapper;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.CrearSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.dto.CrearSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.primaryports.ActualizarSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.primaryports.CerrarSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.ConsultarSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupo.primaryports.ConsultarSesionesPorGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.primaryports.GenerarSesionesGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.common.entity.EstadoSesion;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionProcedenciaRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionRepositoryProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.SesionGrupoCoordinadorResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.SesionController;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DEC-065 (2): la creacion devuelve tal cual el {@code mensajeUsuario} del SP (SES_022 cuando la sesion queda como solicitud) y el
 * estado de la solicitud ({@code estadoSolicitud}, {@code pendienteAprobacion}) viaja desde la proyeccion hasta las respuestas v1, v2
 * y del coordinador.
 */
class SesionSolicitudMensajeYEstadoTest {

    private static final String SES_022 = "La sesión extraordinaria quedó registrada como solicitud pendiente de aprobación del coordinador del programa.";
    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID SESION = UUID.randomUUID();
    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 11, 10, 13, 0);

    private final CrearSesionInputPort crear = mock(CrearSesionInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;

    @Test
    void v2_devuelve_el_mensaje_del_sp_y_cae_al_texto_historico_si_no_llega() throws Exception {
        final MockMvc mvc = MockMvcBuilders.standaloneSetup(new SesionV2Controller(crear, mock(ActualizarSesionInputPort.class), identity))
                .setControllerAdvice(new GlobalExceptionHandler(codigo -> java.util.Optional.empty())).build();
        final String cuerpo = "{\"grupo\":\"" + GRUPO + "\",\"nombre\":\"Repaso\",\"fechaHoraInicio\":\"2026-11-10T13:00:00Z\","
                + "\"fechaHoraFin\":\"2026-11-10T15:00:00Z\"}";
        when(crear.execute(any(CrearSesionDTO.class))).thenReturn(SES_022, (String) null, "  ");

        mvc.perform(post("/api/v2/sesiones").contentType("application/json").content(cuerpo))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.mensaje").value(SES_022));
        mvc.perform(post("/api/v2/sesiones").contentType("application/json").content(cuerpo))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.mensaje").value("Sesion creada correctamente."));
        mvc.perform(post("/api/v2/sesiones").contentType("application/json").content(cuerpo))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.mensaje").value("Sesion creada correctamente."));
    }

    @Test
    void v1_devuelve_el_mensaje_del_sp_y_cae_al_texto_historico_si_no_llega() throws Exception {
        final MockMvc mvc = MockMvcBuilders.standaloneSetup(new SesionController(crear, mock(ConsultarSesionInputPort.class),
                        mock(ConsultarSesionesPorGrupoInputPort.class), mock(CerrarSesionInputPort.class),
                        mock(ActualizarSesionInputPort.class), mock(GenerarSesionesGrupoInputPort.class), identity))
                .setControllerAdvice(new GlobalExceptionHandler(codigo -> java.util.Optional.empty())).build();
        final String cuerpo = "{\"grupo\":\"" + GRUPO + "\",\"nombre\":\"Repaso\",\"fechaHoraInicio\":\"2026-11-10T08:00\","
                + "\"fechaHoraFin\":\"2026-11-10T10:00\"}";
        when(crear.execute(any(CrearSesionDTO.class))).thenReturn(SES_022, (String) null);

        mvc.perform(post("/api/v1/sesiones").contentType("application/json").content(cuerpo))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.mensaje").value(SES_022));
        mvc.perform(post("/api/v1/sesiones").contentType("application/json").content(cuerpo))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.mensaje").value("Sesion creada correctamente."));
    }

    @Test
    void v2_la_proyeccion_con_solicitud_llega_a_la_respuesta() {
        final SesionProcedenciaRepositoryProjection fila = new SesionProcedenciaRepositoryProjection(SESION, GRUPO, "Repaso", 3, "S-3", 1,
                "01", "G01", INICIO, INICIO.plusHours(2), "UTC_V2", null, "PENDIENTE", true);

        final SesionV2Response respuesta = SesionV2ConsultaController.toResponse(
                SesionProcedenciaMapper.toDTO(SesionProcedenciaMapper.toEntity(fila)));

        assertEquals("PENDIENTE", respuesta.estadoSolicitud());
        assertTrue(respuesta.pendienteAprobacion());
        assertEquals("2026-11-10T13:00:00Z", respuesta.fechaHoraInicio());
    }

    @Test
    void v2_una_sesion_sin_solicitud_expone_nulo_y_falso() {
        final SesionProcedenciaRepositoryProjection fila = new SesionProcedenciaRepositoryProjection(SESION, GRUPO, "Clase", 1, "S-1", 1,
                "01", "G01", INICIO, INICIO.plusHours(2), "UTC_V2");

        final SesionV2Response respuesta = SesionV2ConsultaController.toResponse(
                SesionProcedenciaMapper.toDTO(SesionProcedenciaMapper.toEntity(fila)));

        assertNull(respuesta.estadoSolicitud());
        assertFalse(respuesta.pendienteAprobacion());
        final SesionV2ConsultadaDTO sinSolicitud = new SesionV2ConsultadaDTO(SESION, GRUPO, "Clase", 1, "S-1", 1, "01", "G01", INICIO,
                INICIO.plusHours(1), "CONFIRMADA", "UTC_V2");
        assertNull(sinSolicitud.estadoSolicitud());
        final SesionProcedenciaEntity historica = new SesionProcedenciaEntity(SESION, GRUPO, "Clase", 1, "S-1", 1, "01", "G01", INICIO,
                INICIO.plusHours(1), co.edu.uco.asistenciasuco.application.features.sesion.common.EstadoHorarioSesion.CONFIRMADA, "UTC_V2");
        assertNull(historica.estadoSolicitud());
        assertEquals(EstadoSesion.PROGRAMADA, historica.estado());
    }

    @Test
    void v1_la_proyeccion_con_solicitud_llega_al_dto_de_la_sesion() {
        final SesionRepositoryProjection fila = new SesionRepositoryProjection(SESION, GRUPO, "Repaso", 3, "S-3", 1, "01", "G01", INICIO,
                INICIO.plusHours(2), null, "RECHAZADA", false);

        final SesionConsultadaEntity entidad = ConsultarSesionRepositoryMapper.toUseCaseEntity(fila);
        final SesionConsultadaDTO dto = ConsultarSesionMapper.toDTO(entidad);

        assertEquals("RECHAZADA", dto.getEstadoSolicitud());
        assertFalse(dto.isPendienteAprobacion());
        assertEquals("RECHAZADA", entidad.getEstadoSolicitud());
        assertFalse(entidad.isPendienteAprobacion());
    }

    @Test
    void v1_sin_solicitud_conserva_los_valores_por_defecto() {
        final SesionRepositoryProjection fila = new SesionRepositoryProjection(SESION, GRUPO, "Clase", 1, "S-1", 1, "01", "G01", INICIO,
                INICIO.plusHours(2));
        final SesionConsultadaDTO dto = ConsultarSesionMapper.toDTO(ConsultarSesionRepositoryMapper.toUseCaseEntity(fila));

        assertNull(dto.getEstadoSolicitud());
        assertFalse(dto.isPendienteAprobacion());
        dto.setEstadoSolicitud("PENDIENTE");
        dto.setPendienteAprobacion(true);
        assertTrue(dto.isPendienteAprobacion());
        assertEquals("PENDIENTE", fila.isPendienteAprobacion() ? "x" : dto.getEstadoSolicitud());
        assertNull(new SesionConsultadaDTO(SESION, GRUPO, "Clase", 1, "S-1", 1, "01", "G01", INICIO, INICIO.plusHours(1), null)
                .getEstadoSolicitud());
        assertNull(new SesionConsultadaEntity(SESION, GRUPO, "Clase", 1, "S-1", 1, "01", "G01", INICIO, INICIO.plusHours(1))
                .getEstadoSolicitud());
    }

    @Test
    void coordinador_la_sesion_del_grupo_expone_el_estado_de_la_solicitud() {
        final SesionGrupoDTO dto = new SesionGrupoDTO(SESION, GRUPO, 1, "G01", "Repaso", 3, "S-3", 1, INICIO, INICIO.plusHours(2), INICIO,
                INICIO.plusHours(2), "CONFIRMADA", "PROGRAMADA", null, "PENDIENTE", true);

        final SesionGrupoCoordinadorResponse respuesta = SesionGrupoCoordinadorResponse.from(dto);

        assertEquals("PENDIENTE", respuesta.estadoSolicitud());
        assertTrue(respuesta.pendienteAprobacion());

        final SesionGrupoDTO sinSolicitud = new SesionGrupoDTO(SESION, GRUPO, 1, "G01", "Clase", 1, "S-1", 1, INICIO, INICIO.plusHours(2),
                INICIO, INICIO.plusHours(2), "CONFIRMADA");
        assertNull(sinSolicitud.estadoSolicitud());
        assertFalse(sinSolicitud.pendienteAprobacion());
        final SesionGrupoDTO conCancelacion = new SesionGrupoDTO(SESION, GRUPO, 1, "G01", "Clase", 1, "S-1", 1, INICIO, INICIO.plusHours(2),
                INICIO, INICIO.plusHours(2), "CONFIRMADA", "CANCELADA", null);
        assertNull(conCancelacion.estadoSolicitud());
    }
}
