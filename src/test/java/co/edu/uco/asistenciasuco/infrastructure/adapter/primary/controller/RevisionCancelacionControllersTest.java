package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller;

import co.edu.uco.asistenciasuco.application.features.alertainasistencia.consultaralertasestudiante.primaryports.ConsultarAlertasEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.consultarrevisionescoordinador.primaryports.ConsultarRevisionesCoordinadorInputPort;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.consultarrevisionesdecano.primaryports.ConsultarRevisionesDecanoInputPort;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.decidirrevision.primaryports.DecidirRevisionCancelacionInputPort;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.exception.RevisionCancelacionErrorCode;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.resolverrevision.primaryports.ResolverRevisionCancelacionInputPort;
import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.AlertaFila;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.PaginaAlertas;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common.PaginaRevisiones;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common.ResultadoDecision;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common.ResultadoResolucion;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common.RevisionFila;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.CoordinadorRevisionCancelacionController;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano.DecanoRevisionCancelacionController;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.DocenteRevisionCancelacionController;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante.EstudianteAlertasController;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** CULM-001 C.1 (contrato 3.2, 3.9.1): forma exacta de las respuestas, 400 por cuerpo invalido y errores de la DB como 404/409. */
class RevisionCancelacionControllersTest {

    private static final UUID USUARIO = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID REVISION = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");
    private static final UUID GRUPO = UUID.fromString("cccccccc-0000-0000-0000-000000000003");
    private final AuthenticatedUserResolver identity = () -> USUARIO;
    private final GlobalExceptionHandler advice = new GlobalExceptionHandler(codigo -> Optional.empty());

    private final ConsultarAlertasEstudianteInputPort alertas = mock(ConsultarAlertasEstudianteInputPort.class);
    private final DecidirRevisionCancelacionInputPort decidir = mock(DecidirRevisionCancelacionInputPort.class);
    private final ResolverRevisionCancelacionInputPort resolver = mock(ResolverRevisionCancelacionInputPort.class);
    private final ConsultarRevisionesCoordinadorInputPort coordinador = mock(ConsultarRevisionesCoordinadorInputPort.class);
    private final ConsultarRevisionesDecanoInputPort decano = mock(ConsultarRevisionesDecanoInputPort.class);

    private MockMvc mvc() {
        return MockMvcBuilders.standaloneSetup(new EstudianteAlertasController(alertas, identity),
                new DocenteRevisionCancelacionController(decidir, identity),
                new CoordinadorRevisionCancelacionController(coordinador, resolver, identity),
                new DecanoRevisionCancelacionController(decano, resolver, identity)).setControllerAdvice(advice).build();
    }

    private static RevisionFila fila(final boolean conPrograma) {
        return new RevisionFila(REVISION, "PROPUESTA", "Cancelación propuesta", false, true, true, List.of("APROBAR", "RECHAZAR"), "DECANO",
                Instant.parse("2026-11-08T10:00:00Z"), UUID.randomUUID(), "Est Uno", "A", GRUPO, 1, "G01", UUID.randomUUID(), "Bases", UUID.randomUUID(),
                "Doc Uno", conPrograma ? UUID.randomUUID() : null, "Ingeniería", new BigDecimal("21.43"), new BigDecimal("22.50"), new BigDecimal("20.00"),
                new BigDecimal("9"), new BigDecimal("40"), Instant.parse("2026-11-07T10:00:00Z"), "PROPONER", "motivo docente",
                Instant.parse("2026-11-07T11:00:00Z"), conPrograma ? null : "APROBAR", null, conPrograma ? null : "Decano Uno", conPrograma ? null : "DECANO", null);
    }

    @Test
    void alertas_del_estudiante_usa_el_catalogo_y_responde_la_pagina() throws Exception {
        final AlertaFila alerta = new AlertaFila(UUID.randomUUID(), "ADVERTENCIA", "Riesgo", "VIGENTE", "ALR_010", new BigDecimal("16.00"),
                new BigDecimal("16.00"), new BigDecimal("15.00"), new BigDecimal("20.00"), new BigDecimal("4"), new BigDecimal("25"),
                Instant.parse("2026-11-07T10:00:00Z"), false, null, GRUPO, 1, "G01", UUID.randomUUID(), "Bases", "A", "Llevas 16.00% de inasistencia en Bases.");
        final AlertaFila perdida = new AlertaFila(UUID.randomUUID(), "PERDIDA", "Pérdida", "VIGENTE", "ALR_011", new BigDecimal("21"), null,
                new BigDecimal("20"), null, null, null, null, true, Instant.parse("2026-11-07T10:00:00Z"), GRUPO, 1, "G01", UUID.randomUUID(), "Bases", "CI", "Alcanzaste 21%.");
        when(alertas.execute(new ConsultarAlertasEstudianteInputPort.Comando(USUARIO, 1, 20, false, false, null))).thenReturn(new PaginaAlertas(List.of(alerta, perdida), 2));

        mvc().perform(get("/api/v1/estudiante/alertas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.totalElementos").value(2))
                .andExpect(jsonPath("$.elementos[0].tipo").value("ADVERTENCIA"))
                .andExpect(jsonPath("$.elementos[0].mensaje").value("Llevas 16.00% de inasistencia en Bases."))
                .andExpect(jsonPath("$.elementos[0].grupo.codigo").value(1))
                .andExpect(jsonPath("$.elementos[0].asignatura.nombre").value("Bases"))
                .andExpect(jsonPath("$.elementos[1].leida").value(true));
    }

    @Test
    void alertas_con_filtros_usa_pagina_y_grupo() throws Exception {
        final AlertaFila alerta = new AlertaFila(UUID.randomUUID(), "ADVERTENCIA", "Riesgo", "VIGENTE", "ALR_010", new BigDecimal("16.00"), null,
                new BigDecimal("15.00"), new BigDecimal("20.00"), null, null, null, false, null, GRUPO, 1, "G01", UUID.randomUUID(), "Bases", "A", "m");
        when(alertas.execute(new ConsultarAlertasEstudianteInputPort.Comando(USUARIO, 2, 1, true, true, GRUPO))).thenReturn(new PaginaAlertas(List.of(alerta, alerta), 2));

        mvc().perform(get("/api/v1/estudiante/alertas").param("soloVigentes", "true").param("soloNoLeidas", "true")
                        .param("grupoId", GRUPO.toString()).param("pagina", "2").param("tamano", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroPagina").value(2))
                .andExpect(jsonPath("$.totalPaginas").value(2));
    }

    @Test
    void docente_decide_y_recibe_el_mensaje_del_sp() throws Exception {
        when(decidir.execute(new DecidirRevisionCancelacionInputPort.Comando(REVISION, "PROPONER", "motivo suficiente", USUARIO))).thenReturn(
                new ResultadoDecision("Se envió la propuesta de cancelación a la coordinación del programa.", REVISION, "PENDIENTE_DOCENTE", "PROPUESTA",
                        "PROPONER", false, GRUPO, UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("22"), "PROPUESTA_CANCELACION", "COORDINADOR", false));

        mvc().perform(patch("/api/v1/docente/revisiones-cancelacion/" + REVISION).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"PROPONER\",\"motivo\":\"motivo suficiente\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.estado").value("PROPUESTA"))
                .andExpect(jsonPath("$.datos.perfilResolutor").value("COORDINADOR"))
                .andExpect(jsonPath("$.datos.mensajeUsuario").value("Se envió la propuesta de cancelación a la coordinación del programa."));
    }

    @Test
    void los_cuerpos_invalidos_responden_400_sin_llegar_al_caso_de_uso() throws Exception {
        for (final String cuerpo : new String[]{"{}", "{\"decision\":\"\",\"motivo\":\"x\"}", "{\"decision\":\"MANTENER\"}"}) {
            mvc().perform(patch("/api/v1/docente/revisiones-cancelacion/" + REVISION).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isBadRequest());
        }
        mvc().perform(patch("/api/v1/coordinador/revisiones-cancelacion/" + REVISION).contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"APROBAR\"}"))
                .andExpect(status().isBadRequest());
        mvc().perform(patch("/api/v1/decano/revisiones-cancelacion/" + REVISION).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(decidir, resolver);
    }

    @Test
    void un_cuerpo_ausente_es_validation_error_400_en_los_tres_perfiles_sin_llegar_al_caso_de_uso() throws Exception {
        // java:S2259: request puede ser null (@RequestBody(required = false)); se valida antes de leer decision y motivo.
        for (final String ruta : new String[]{"docente", "coordinador", "decano"}) {
            mvc().perform(patch("/api/v1/" + ruta + "/revisiones-cancelacion/" + REVISION))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
        verifyNoInteractions(decidir, resolver);
    }

    @Test
    void coordinador_y_decano_consultan_y_resuelven_con_el_mismo_caso_de_uso() throws Exception {
        when(coordinador.execute(new ConsultarRevisionesCoordinadorInputPort.Comando(USUARIO, 1, 20, true, "PROPUESTA", null, GRUPO)))
                .thenReturn(new PaginaRevisiones(List.of(fila(true)), 1));
        when(decano.execute(new ConsultarRevisionesDecanoInputPort.Comando(USUARIO, 1, 20, false, null, null)))
                .thenReturn(new PaginaRevisiones(List.of(fila(false)), 1));
        when(resolver.execute(new ResolverRevisionCancelacionInputPort.Comando(REVISION, "APROBAR", "motivo suficiente", USUARIO))).thenReturn(
                new ResultadoResolucion("Cancelación aprobada: el estudiante quedó cancelado por inasistencia.", REVISION, "PROPUESTA", "APROBADA", "APROBAR",
                        false, true, GRUPO, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "DECANO", new BigDecimal("22"), "CANCELACION_APROBADA"));

        mvc().perform(get("/api/v1/coordinador/revisiones-cancelacion").param("soloPendientes", "true").param("estado", "PROPUESTA").param("grupoId", GRUPO.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elementos[0].accionesPermitidas[0]").value("APROBAR"))
                .andExpect(jsonPath("$.elementos[0].perfilResolutorActual").value("DECANO"))
                .andExpect(jsonPath("$.elementos[0].programa.nombre").value("Ingeniería"))
                .andExpect(jsonPath("$.elementos[0].resolutor").doesNotExist())
                .andExpect(jsonPath("$.elementos[0].docente.nombreCompleto").value("Doc Uno"));
        mvc().perform(get("/api/v1/decano/revisiones-cancelacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elementos[0].programa").doesNotExist())
                .andExpect(jsonPath("$.elementos[0].resolutor.perfil").value("DECANO"));
        for (final String ruta : new String[]{"coordinador", "decano"}) {
            mvc().perform(patch("/api/v1/" + ruta + "/revisiones-cancelacion/" + REVISION).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"decision\":\"APROBAR\",\"motivo\":\"motivo suficiente\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.datos.cambioMatricula").value(true))
                    .andExpect(jsonPath("$.datos.estado").value("APROBADA"));
        }
    }

    @Test
    void los_errores_de_la_db_llegan_como_404_y_409() throws Exception {
        when(resolver.execute(any())).thenThrow(new ResourceNotFoundException(RevisionCancelacionErrorCode.RCN_001))
                .thenThrow(new ConflictException(RevisionCancelacionErrorCode.RCN_014));
        final String cuerpo = "{\"decision\":\"APROBAR\",\"motivo\":\"motivo suficiente\"}";
        mvc().perform(patch("/api/v1/coordinador/revisiones-cancelacion/" + REVISION).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isNotFound());
        mvc().perform(patch("/api/v1/coordinador/revisiones-cancelacion/" + REVISION).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isConflict());
    }
}
