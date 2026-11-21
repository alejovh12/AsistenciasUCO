package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.consultarsolicitudes.primaryports.ConsultarSolicitudesSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.consultarsolicitudesdocente.primaryports.ConsultarSolicitudesSesionDocenteInputPort;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** ATA D6: {@code GET /api/v1/docente/sesiones/solicitudes} entrega la pagina con el contrato de fila del coordinador. */
class DocenteSesionSolicitudControllerTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final String RUTA = "/api/v1/docente/sesiones/solicitudes";

    private final ConsultarSolicitudesSesionDocenteInputPort consultar = mock(ConsultarSolicitudesSesionDocenteInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new DocenteSesionSolicitudController(consultar, identity))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty())).build();

    @Test
    void consulta_devuelve_la_pagina_con_las_horas_en_utc_y_los_filtros_recibidos() throws Exception {
        final UUID sesion = UUID.randomUUID();
        final UUID grupo = UUID.randomUUID();
        final UUID programa = UUID.randomUUID();
        final UUID docente = UUID.randomUUID();
        when(consultar.execute(new ConsultarSolicitudesSesionDocenteInputPort.Comando(ACTOR, false, grupo, 2, 5)))
                .thenReturn(new ConsultarSolicitudesSesionInputPort.Resultado(List.of(
                        new ConsultarSolicitudesSesionInputPort.Solicitud(sesion, "Repaso", "S-9", grupo, "G01 de Calculo", "Calculo",
                                programa, "Ingenieria", docente, "Maria Docente", Instant.parse("2026-11-10T13:00:00Z"),
                                Instant.parse("2026-11-10T15:00:00Z"), "RECHAZADA", "Rechazada",
                                Instant.parse("2026-11-09T14:00:00Z"), Instant.parse("2026-11-09T16:00:00Z"), "Ya existe", "Ingenieria", "Carlos Coordinador")), 11, 2, 5));

        mvc.perform(get(RUTA).param("soloPendientes", "false").param("grupoId", grupo.toString()).param("pagina", "2").param("tamano", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.numeroPagina").value(2))
                .andExpect(jsonPath("$.tamanoPagina").value(5))
                .andExpect(jsonPath("$.totalElementos").value(11))
                .andExpect(jsonPath("$.totalPaginas").value(3))
                .andExpect(jsonPath("$.elementos[0].idSesion").value(sesion.toString()))
                .andExpect(jsonPath("$.elementos[0].nombreSesion").value("Repaso"))
                .andExpect(jsonPath("$.elementos[0].idGrupo").value(grupo.toString()))
                .andExpect(jsonPath("$.elementos[0].nombreAsignatura").value("Calculo"))
                .andExpect(jsonPath("$.elementos[0].nombrePrograma").value("Ingenieria"))
                .andExpect(jsonPath("$.elementos[0].nombreDocente").value("Maria Docente"))
                .andExpect(jsonPath("$.elementos[0].fechaHoraInicio").value("2026-11-10T13:00:00Z"))
                .andExpect(jsonPath("$.elementos[0].fechaHoraFin").value("2026-11-10T15:00:00Z"))
                .andExpect(jsonPath("$.elementos[0].estadoSolicitud").value("RECHAZADA"))
                .andExpect(jsonPath("$.elementos[0].nombreEstadoSolicitud").value("Rechazada"))
                .andExpect(jsonPath("$.elementos[0].fechaSolicitudUtc").value("2026-11-09T14:00:00Z"))
                .andExpect(jsonPath("$.elementos[0].fechaResolucionUtc").value("2026-11-09T16:00:00Z"))
                .andExpect(jsonPath("$.elementos[0].observacion").value("Ya existe"))
                .andExpect(jsonPath("$.elementos[0].nombreProgramaResponsable").value("Ingenieria"))
                .andExpect(jsonPath("$.elementos[0].nombreCoordinadorResolutor").value("Carlos Coordinador"));
    }

    @Test
    void consulta_toma_por_defecto_solo_pendientes_pagina_1_y_tamano_20() throws Exception {
        when(consultar.execute(new ConsultarSolicitudesSesionDocenteInputPort.Comando(ACTOR, true, null, 1, 20)))
                .thenReturn(new ConsultarSolicitudesSesionInputPort.Resultado(List.of(), 0, 1, 20));

        mvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(0))
                .andExpect(jsonPath("$.totalPaginas").value(0))
                .andExpect(jsonPath("$.elementos").isEmpty());
    }

    @Test
    void campos_sin_valor_salen_como_nulos_explicitos() throws Exception {
        final UUID sesion = UUID.randomUUID();
        when(consultar.execute(any())).thenReturn(new ConsultarSolicitudesSesionInputPort.Resultado(List.of(
                new ConsultarSolicitudesSesionInputPort.Solicitud(sesion, "Repaso", "S-9", UUID.randomUUID(), "G01", "Calculo",
                        UUID.randomUUID(), "Ingenieria", UUID.randomUUID(), "Maria", null, null, "PENDIENTE", "Pendiente", null, null, null, null, null)),
                1, 1, 20));

        mvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elementos[0].fechaHoraInicio").value(nullValue()))
                .andExpect(jsonPath("$.elementos[0].fechaResolucionUtc").value(nullValue()))
                .andExpect(jsonPath("$.elementos[0].observacion").value(nullValue()));
    }

    @Test
    void fuera_de_rol_responde_403() throws Exception {
        when(consultar.execute(any())).thenThrow(new ForbiddenException("SEC_001", "Sin acceso al recurso solicitado."));

        mvc.perform(get(RUTA)).andExpect(status().isForbidden());
    }

    @Test
    void el_constructor_exige_sus_dependencias() {
        assertThrows(NullPointerException.class, () -> new DocenteSesionSolicitudController(null, identity));
        assertThrows(NullPointerException.class, () -> new DocenteSesionSolicitudController(consultar, null));
    }
}
