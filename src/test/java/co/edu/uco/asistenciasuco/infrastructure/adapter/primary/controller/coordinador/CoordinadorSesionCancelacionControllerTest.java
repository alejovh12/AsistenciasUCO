package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.sesion.common.dto.CancelacionSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.common.entity.EstadoSesion;
import co.edu.uco.asistenciasuco.application.features.sesion.exception.SesionErrorCode;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.cancelarsesioncoordinador.primaryports.CancelarSesionCoordinadorInputPort;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.consultarmotivos.primaryports.ConsultarMotivosCancelacionInputPort;
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
 * CULM-001 / DEC-064 (HU047): {@code POST /api/v1/coordinador/sesiones/{id}/cancelacion} (200 con la sesion cancelada, 400 de forma,
 * 403 fuera de ambito, 409 no manual o ya cancelada, 404 inexistente) y {@code GET /api/v1/coordinador/sesiones/motivos-cancelacion}.
 */
class CoordinadorSesionCancelacionControllerTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final String URL = "/api/v1/coordinador/sesiones/{id}/cancelacion";

    private final CancelarSesionCoordinadorInputPort cancelar = mock(CancelarSesionCoordinadorInputPort.class);
    private final ConsultarMotivosCancelacionInputPort motivos = mock(ConsultarMotivosCancelacionInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final CoordinadorSesionCancelacionController controller =
            new CoordinadorSesionCancelacionController(cancelar, motivos, identity);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty())).build();

    @Test
    void cancelacion_delega_con_el_actor_y_responde_200_con_la_sesion_cancelada() throws Exception {
        final UUID sesion = UUID.randomUUID();
        final UUID grupo = UUID.randomUUID();
        when(cancelar.execute(new CancelarSesionCoordinadorInputPort.Comando(sesion, "DUPLICADA", "Ya existe la del lunes", ACTOR)))
                .thenReturn(new CancelarSesionCoordinadorInputPort.Resultado(sesion, grupo, EstadoSesion.CANCELADA,
                        new CancelacionSesionDTO("COORDINADOR", "Sesión duplicada: Ya existe la del lunes", null,
                                Instant.parse("2026-11-09T14:00:00Z")), 38));

        mvc.perform(post(URL, sesion).contentType("application/json")
                        .content("{\"codigoMotivo\":\"DUPLICADA\",\"observacion\":\"Ya existe la del lunes\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.idSesion").value(sesion.toString()))
                .andExpect(jsonPath("$.datos.idGrupo").value(grupo.toString()))
                .andExpect(jsonPath("$.datos.estado").value("CANCELADA"))
                .andExpect(jsonPath("$.datos.cancelacion.tipo").value("COORDINADOR"))
                .andExpect(jsonPath("$.datos.cancelacion.motivo").value("Sesión duplicada: Ya existe la del lunes"))
                .andExpect(jsonPath("$.datos.cancelacion.nombreFestivo").value(nullValue()))
                .andExpect(jsonPath("$.datos.cancelacion.fechaCancelacionUtc").value("2026-11-09T14:00:00Z"))
                .andExpect(jsonPath("$.datos.estudiantesActivos").value(38));
    }

    @Test
    void la_observacion_es_opcional() throws Exception {
        final UUID sesion = UUID.randomUUID();
        when(cancelar.execute(new CancelarSesionCoordinadorInputPort.Comando(sesion, "OTRO", null, ACTOR)))
                .thenReturn(new CancelarSesionCoordinadorInputPort.Resultado(sesion, UUID.randomUUID(), EstadoSesion.CANCELADA,
                        new CancelacionSesionDTO("COORDINADOR", "Otro motivo", null, null), 0));

        mvc.perform(post(URL, sesion).contentType("application/json").content("{\"codigoMotivo\":\"OTRO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.cancelacion.fechaCancelacionUtc").value(nullValue()));
    }

    @Test
    void sin_cuerpo_sin_motivo_o_con_campos_demasiado_largos_responde_400_sin_invocar_el_caso_de_uso() throws Exception {
        final UUID sesion = UUID.randomUUID();
        mvc.perform(post(URL, sesion).contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(post(URL, sesion).contentType("application/json").content("{\"codigoMotivo\":\"   \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(URL, sesion).contentType("application/json").content("{\"codigoMotivo\":\"" + "X".repeat(51) + "\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(URL, sesion).contentType("application/json")
                        .content("{\"codigoMotivo\":\"OTRO\",\"observacion\":\"" + "x".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cancelar);
    }

    @Test
    void fuera_de_ambito_responde_403() throws Exception {
        when(cancelar.execute(any())).thenThrow(new ForbiddenException("SEC_002", "Sin acceso al recurso solicitado."));

        mvc.perform(post(URL, UUID.randomUUID()).contentType("application/json").content("{\"codigoMotivo\":\"OTRO\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void una_sesion_no_manual_o_ya_cancelada_responde_409_con_su_codigo() throws Exception {
        when(cancelar.execute(any())).thenThrow(new ConflictException(SesionErrorCode.SES_018))
                .thenThrow(new ConflictException(SesionErrorCode.SES_011));

        mvc.perform(post(URL, UUID.randomUUID()).contentType("application/json").content("{\"codigoMotivo\":\"OTRO\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SES_018"));
        mvc.perform(post(URL, UUID.randomUUID()).contentType("application/json").content("{\"codigoMotivo\":\"OTRO\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SES_011"));
    }

    @Test
    void motivos_devuelve_el_catalogo_con_el_total() throws Exception {
        when(motivos.execute(new ConsultarMotivosCancelacionInputPort.Comando(ACTOR))).thenReturn(List.of(
                new ConsultarMotivosCancelacionInputPort.Motivo("DUPLICADA", "Sesión duplicada"),
                new ConsultarMotivosCancelacionInputPort.Motivo("OTRO", "Otro motivo")));

        mvc.perform(get("/api/v1/coordinador/sesiones/motivos-cancelacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.datos[0].codigo").value("DUPLICADA"))
                .andExpect(jsonPath("$.datos[1].nombre").value("Otro motivo"));
    }

    @Test
    void el_constructor_exige_sus_dependencias() {
        assertThrows(NullPointerException.class, () -> new CoordinadorSesionCancelacionController(null, motivos, identity));
        assertThrows(NullPointerException.class, () -> new CoordinadorSesionCancelacionController(cancelar, null, identity));
        assertThrows(NullPointerException.class, () -> new CoordinadorSesionCancelacionController(cancelar, motivos, null));
    }
}
