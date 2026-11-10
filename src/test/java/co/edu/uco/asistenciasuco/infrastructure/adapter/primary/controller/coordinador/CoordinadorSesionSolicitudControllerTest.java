package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.sesion.exception.SesionErrorCode;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.aprobarsolicitud.primaryports.AprobarSolicitudSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.consultarsolicitudes.primaryports.ConsultarSolicitudesSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.rechazarsolicitud.primaryports.RechazarSolicitudSesionInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DEC-065 (2): {@code GET /api/v1/coordinador/sesiones/solicitudes} (pagina con el contrato exacto), {@code POST .../aprobacion} y
 * {@code POST .../rechazo} (200 con el {@code mensajeUsuario} del SP, 400 de forma, 403 fuera de ambito, 409 sin solicitud o ya resuelta).
 */
class CoordinadorSesionSolicitudControllerTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final String APROBACION = "/api/v1/coordinador/sesiones/{id}/aprobacion";
    private static final String RECHAZO = "/api/v1/coordinador/sesiones/{id}/rechazo";

    private final ConsultarSolicitudesSesionInputPort consultar = mock(ConsultarSolicitudesSesionInputPort.class);
    private final AprobarSolicitudSesionInputPort aprobar = mock(AprobarSolicitudSesionInputPort.class);
    private final RechazarSolicitudSesionInputPort rechazar = mock(RechazarSolicitudSesionInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final CoordinadorSesionSolicitudController controller =
            new CoordinadorSesionSolicitudController(consultar, aprobar, rechazar, identity);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty())).build();

    @Test
    void consulta_devuelve_la_pagina_con_las_horas_en_utc_y_los_filtros_recibidos() throws Exception {
        final UUID sesion = UUID.randomUUID();
        final UUID grupo = UUID.randomUUID();
        final UUID programa = UUID.randomUUID();
        final UUID docente = UUID.randomUUID();
        when(consultar.execute(new ConsultarSolicitudesSesionInputPort.Comando(ACTOR, false, programa, grupo, 2, 5)))
                .thenReturn(new ConsultarSolicitudesSesionInputPort.Resultado(List.of(
                        new ConsultarSolicitudesSesionInputPort.Solicitud(sesion, "Repaso", "S-9", grupo, "G01 de Calculo", "Calculo",
                                programa, "Ingenieria", docente, "Maria Docente", Instant.parse("2026-11-10T13:00:00Z"),
                                Instant.parse("2026-11-10T15:00:00Z"), "PENDIENTE", "Pendiente de aprobación",
                                Instant.parse("2026-11-09T14:00:00Z"), null, null)), 11, 2, 5));

        mvc.perform(get("/api/v1/coordinador/sesiones/solicitudes")
                        .param("soloPendientes", "false").param("programaId", programa.toString())
                        .param("grupoId", grupo.toString()).param("pagina", "2").param("tamano", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.numeroPagina").value(2))
                .andExpect(jsonPath("$.tamanoPagina").value(5))
                .andExpect(jsonPath("$.totalElementos").value(11))
                .andExpect(jsonPath("$.totalPaginas").value(3))
                .andExpect(jsonPath("$.elementos[0].idSesion").value(sesion.toString()))
                .andExpect(jsonPath("$.elementos[0].nombreSesion").value("Repaso"))
                .andExpect(jsonPath("$.elementos[0].codigoSesion").value("S-9"))
                .andExpect(jsonPath("$.elementos[0].idGrupo").value(grupo.toString()))
                .andExpect(jsonPath("$.elementos[0].nombreGrupo").value("G01 de Calculo"))
                .andExpect(jsonPath("$.elementos[0].nombreAsignatura").value("Calculo"))
                .andExpect(jsonPath("$.elementos[0].idPrograma").value(programa.toString()))
                .andExpect(jsonPath("$.elementos[0].nombrePrograma").value("Ingenieria"))
                .andExpect(jsonPath("$.elementos[0].idDocente").value(docente.toString()))
                .andExpect(jsonPath("$.elementos[0].nombreDocente").value("Maria Docente"))
                .andExpect(jsonPath("$.elementos[0].fechaHoraInicio").value("2026-11-10T13:00:00Z"))
                .andExpect(jsonPath("$.elementos[0].fechaHoraFin").value("2026-11-10T15:00:00Z"))
                .andExpect(jsonPath("$.elementos[0].estadoSolicitud").value("PENDIENTE"))
                .andExpect(jsonPath("$.elementos[0].nombreEstadoSolicitud").value("Pendiente de aprobación"))
                .andExpect(jsonPath("$.elementos[0].fechaSolicitudUtc").value("2026-11-09T14:00:00Z"))
                .andExpect(jsonPath("$.elementos[0].fechaResolucionUtc").value(nullValue()))
                .andExpect(jsonPath("$.elementos[0].observacion").value(nullValue()));
    }

    @Test
    void consulta_toma_por_defecto_solo_pendientes_pagina_1_y_tamano_20() throws Exception {
        when(consultar.execute(new ConsultarSolicitudesSesionInputPort.Comando(ACTOR, true, null, null, 1, 20)))
                .thenReturn(new ConsultarSolicitudesSesionInputPort.Resultado(List.of(), 0, 1, 20));

        mvc.perform(get("/api/v1/coordinador/sesiones/solicitudes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(0))
                .andExpect(jsonPath("$.totalPaginas").value(0))
                .andExpect(jsonPath("$.elementos").isEmpty());
    }

    @Test
    void consulta_fuera_de_rol_o_ambito_responde_403() throws Exception {
        when(consultar.execute(any())).thenThrow(new ForbiddenException("SEC_001", "Sin acceso al recurso solicitado."));

        mvc.perform(get("/api/v1/coordinador/sesiones/solicitudes")).andExpect(status().isForbidden());
    }

    @Test
    void aprobacion_delega_con_el_actor_y_devuelve_el_mensaje_del_sp() throws Exception {
        final UUID sesion = UUID.randomUUID();
        final UUID grupo = UUID.randomUUID();
        when(aprobar.execute(new AprobarSolicitudSesionInputPort.Comando(sesion, "Adelante", ACTOR)))
                .thenReturn(new AprobarSolicitudSesionInputPort.Resultado(sesion, grupo, "La solicitud de sesión fue aprobada.", 30, 30,
                        true, true));

        mvc.perform(post(APROBACION, sesion).contentType("application/json").content("{\"observacion\":\"Adelante\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.mensajeUsuario").value("La solicitud de sesión fue aprobada."))
                .andExpect(jsonPath("$.datos.idSesion").value(sesion.toString()))
                .andExpect(jsonPath("$.datos.idGrupo").value(grupo.toString()))
                .andExpect(jsonPath("$.datos.estado").value("APROBADA"))
                .andExpect(jsonPath("$.datos.estudiantesActivos").value(30));
    }

    @Test
    void aprobacion_acepta_cuerpo_vacio_o_ausente() throws Exception {
        final UUID sesion = UUID.randomUUID();
        when(aprobar.execute(new AprobarSolicitudSesionInputPort.Comando(sesion, null, ACTOR)))
                .thenReturn(new AprobarSolicitudSesionInputPort.Resultado(sesion, UUID.randomUUID(), "ok", 0, 0, false, false));

        mvc.perform(post(APROBACION, sesion).contentType("application/json").content("{}")).andExpect(status().isOk());
        mvc.perform(post(APROBACION, sesion)).andExpect(status().isOk());
    }

    @Test
    void aprobacion_con_observacion_demasiado_larga_responde_400_sin_invocar_el_caso_de_uso() throws Exception {
        mvc.perform(post(APROBACION, UUID.randomUUID()).contentType("application/json")
                        .content("{\"observacion\":\"" + "x".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(aprobar);
    }

    @Test
    void aprobacion_sin_solicitud_o_ya_resuelta_responde_409_y_fuera_de_ambito_403() throws Exception {
        when(aprobar.execute(any())).thenThrow(new ConflictException(SesionErrorCode.SES_025))
                .thenThrow(new ConflictException(SesionErrorCode.SES_024))
                .thenThrow(new ForbiddenException("SEC_002", "Sin acceso al recurso solicitado."));

        mvc.perform(post(APROBACION, UUID.randomUUID())).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SES_025"));
        mvc.perform(post(APROBACION, UUID.randomUUID())).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SES_024"));
        mvc.perform(post(APROBACION, UUID.randomUUID())).andExpect(status().isForbidden());
    }

    @Test
    void rechazo_delega_con_motivo_y_observacion_y_devuelve_el_mensaje_del_sp() throws Exception {
        final UUID sesion = UUID.randomUUID();
        final UUID grupo = UUID.randomUUID();
        when(rechazar.execute(new RechazarSolicitudSesionInputPort.Comando(sesion, "DUPLICADA", "Ya existe", ACTOR)))
                .thenReturn(new RechazarSolicitudSesionInputPort.Resultado(sesion, grupo, "La solicitud de sesión fue rechazada.", 12,
                        true, true));

        mvc.perform(post(RECHAZO, sesion).contentType("application/json")
                        .content("{\"codigoMotivo\":\" DUPLICADA \",\"observacion\":\"Ya existe\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.mensajeUsuario").value("La solicitud de sesión fue rechazada."))
                .andExpect(jsonPath("$.datos.estado").value("RECHAZADA"))
                .andExpect(jsonPath("$.datos.idGrupo").value(grupo.toString()))
                .andExpect(jsonPath("$.datos.estudiantesActivos").value(12));
    }

    @Test
    void rechazo_sin_motivo_o_con_campos_demasiado_largos_responde_400_sin_invocar_el_caso_de_uso() throws Exception {
        final UUID sesion = UUID.randomUUID();
        mvc.perform(post(RECHAZO, sesion).contentType("application/json").content("{}")).andExpect(status().isBadRequest());
        mvc.perform(post(RECHAZO, sesion).contentType("application/json").content("{\"codigoMotivo\":\"X\",\"observacion\":\""
                + "x".repeat(501) + "\"}")).andExpect(status().isBadRequest());
        verifyNoInteractions(rechazar);
    }

    @Test
    void rechazo_con_motivo_invalido_responde_400_y_ya_resuelta_409() throws Exception {
        when(rechazar.execute(any())).thenThrow(new co.edu.uco.asistenciasuco.application.exception.validation.ValidationException(
                SesionErrorCode.SES_019)).thenThrow(new ConflictException(SesionErrorCode.SES_024));

        mvc.perform(post(RECHAZO, UUID.randomUUID()).contentType("application/json").content("{\"codigoMotivo\":\"NOPE\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("SES_019"));
        mvc.perform(post(RECHAZO, UUID.randomUUID()).contentType("application/json").content("{\"codigoMotivo\":\"OTRO\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void el_constructor_exige_sus_dependencias() {
        assertThrows(NullPointerException.class, () -> new CoordinadorSesionSolicitudController(null, aprobar, rechazar, identity));
        assertThrows(NullPointerException.class, () -> new CoordinadorSesionSolicitudController(consultar, null, rechazar, identity));
        assertThrows(NullPointerException.class, () -> new CoordinadorSesionSolicitudController(consultar, aprobar, null, identity));
        assertThrows(NullPointerException.class, () -> new CoordinadorSesionSolicitudController(consultar, aprobar, rechazar, null));
    }
}
