package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion;

import co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.primaryports.ActualizarSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.primaryports.dto.ActualizarSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.primaryports.CerrarSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.ConsultarSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.dto.ConsultarSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.dto.SesionConsultadaDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupo.primaryports.ConsultarSesionesPorGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupo.primaryports.dto.ConsultarSesionesPorGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.CrearSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.dto.CrearSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.primaryports.GenerarSesionesGrupoInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import co.edu.uco.asistenciasuco.infrastructure.config.jackson.JacksonInputConfig;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
    private final ConsultarSesionesPorGrupoInputPort queryByGroup = mock(ConsultarSesionesPorGrupoInputPort.class);
    private final CerrarSesionInputPort close = mock(CerrarSesionInputPort.class);
    private final ActualizarSesionInputPort update = mock(ActualizarSesionInputPort.class);
    private final GenerarSesionesGrupoInputPort generate = mock(GenerarSesionesGrupoInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final SesionController controller =
            new SesionController(create, query, queryByGroup, close, update, generate, identity);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> java.util.Optional.empty()))
            .setMessageConverters(new JacksonJsonHttpMessageConverter(buildProductionJsonMapper()))
            .build();

    /**
     * Arnes de prueba (setup), no aserciones: MockMvcBuilders.standaloneSetup() arma su propio
     * JsonMapper por defecto y nunca aplica los beans gestionados por Spring, por lo que
     * FAIL_ON_UNKNOWN_PROPERTIES (JacksonInputConfig, unico bean JsonMapperBuilderCustomizer
     * registrado en produccion) no se activaba en este test. Se carga aqui el @Configuration
     * JacksonInputConfig real (sin tocarlo ni cambiar su visibilidad) en un ApplicationContext
     * minimo, se obtiene su bean JsonMapperBuilderCustomizer tal como lo hace Spring Boot
     * autoconfiguration en produccion, y se aplica al builder del JsonMapper standalone, para que
     * el MockMvc de este test valide el comportamiento real de la aplicacion (RED_SNAPSHOT
     * resolucion LB-001B.1, ver TEST_PLAN.md).
     */
    private static JsonMapper buildProductionJsonMapper() {
        final JsonMapper.Builder builder = JsonMapper.builder();
        try (var context = new AnnotationConfigApplicationContext(JacksonInputConfig.class)) {
            context.getBean(JsonMapperBuilderCustomizer.class).customize(builder);
        }
        return builder.build();
    }

    /**
     * Contrato TARGET (LB-001B.1, CONTRACT_FREEZE.md secc. 3.1/4): CrearSesionRequest/CrearSesionDTO
     * retiran descripcion/aula/tipo/room del contrato de creacion. Este test ya no envia esos campos
     * y no debe asertar CrearSesionDTO.getAula()/getTipo() (retirados). Sigue GREEN hoy porque el
     * subconjunto de campos superviviente (grupo/tema/fechas/actor) ya se mapea igual en AS-IS.
     */
    @Test
    void createAcceptsMinimalContractAndPassesParsedDatesAndActor() throws Exception {
        mvc.perform(post("/api/v1/sesiones").contentType("application/json")
                        .content("""
                                {"grupo":"%s","nombre":"Tema válido",
                                 "fechaHoraInicio":"2026-09-14T08:00","fechaHoraFin":"2026-09-14T09:00"}
                                """.formatted(GROUP)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exitoso").value(true));
        final var captor = ArgumentCaptor.forClass(CrearSesionDTO.class);
        verify(create).execute(captor.capture());
        assertEquals(GROUP, captor.getValue().getGrupo());
        assertEquals("Tema válido", captor.getValue().getNombre());
        assertEquals(START, captor.getValue().getFechaHoraInicio());
        assertEquals(START.plusHours(1), captor.getValue().getFechaHoraFin());
        assertEquals(ACTOR, captor.getValue().getUsuarioEjecutor());
    }

    /**
     * Contrato TARGET (LB-001B.1, CONTRACT_FREEZE.md secc. 2/3.1/7#17): una vez retirados
     * descripcion/aula/tipo/room de CrearSesionRequest, FAIL_ON_UNKNOWN_PROPERTIES (JacksonInputConfig)
     * debe rechazar con 400 (UnrecognizedPropertyException -> FIELD_UNKNOWN) cualquier cliente que
     * los siga enviando. RED esperado: hoy estos 4 campos todavia son reconocidos por
     * CrearSesionRequest (incluido el alias setRoom -> aula), por lo que la peticion HOY devuelve
     * 201 (no 400) y el input port SI es invocado.
     */
    @Test
    void createRejectsRetiredFieldsWithBadRequest() throws Exception {
        assertCreateRejectsUnknownField("descripcion", "\"descripcion\":\"Descripción suficiente\"");
        assertCreateRejectsUnknownField("aula", "\"aula\":\"A101\"");
        assertCreateRejectsUnknownField("tipo", "\"tipo\":\"PRESENCIAL\"");
        assertCreateRejectsUnknownField("room", "\"room\":\"A101\"");
        assertCreateRejectsUnknownField("tema", "\"tema\":\"Tema legado\"");
        assertCreateRejectsUnknownField("topic", "\"topic\":\"Tema legado\"");
        assertCreateRejectsUnknownField("status", "\"status\":\"ABIERTA\"");
        assertCreateRejectsUnknownField("docente", "\"docente\":\"%s\"".formatted(ACTOR));
        org.mockito.Mockito.verifyNoInteractions(create);
    }

    /**
     * LB-001B.4A: el contrato exacto usa {@code nombre}; {@code tema} ya no es alias, por lo que
     * un cliente que envie SOLO {@code tema} (sin nombre) recibe FIELD_UNKNOWN, no un 201.
     */
    @Test
    void createWithTemaInsteadOfNombreIsRejectedAsUnknownField() throws Exception {
        mvc.perform(post("/api/v1/sesiones").contentType("application/json")
                        .content("""
                                {"grupo":"%s","tema":"Tema legado",
                                 "fechaHoraInicio":"2026-09-14T08:00","fechaHoraFin":"2026-09-14T09:00"}
                                """.formatted(GROUP)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("tema"))
                .andExpect(jsonPath("$.details[0].code").value("FIELD_UNKNOWN"));
        org.mockito.Mockito.verifyNoInteractions(create);
    }

    @Test
    void createMissingOrTooLongNombreReportsFieldNombreNeverTema() throws Exception {
        mvc.perform(post("/api/v1/sesiones").contentType("application/json")
                        .content("""
                                {"grupo":"%s","fechaHoraInicio":"2026-09-14T08:00","fechaHoraFin":"2026-09-14T09:00"}
                                """.formatted(GROUP)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("nombre"))
                .andExpect(jsonPath("$.details[?(@.field=='tema')]").isEmpty());
        mvc.perform(post("/api/v1/sesiones").contentType("application/json")
                        .content("""
                                {"grupo":"%s","nombre":"%s",
                                 "fechaHoraInicio":"2026-09-14T08:00","fechaHoraFin":"2026-09-14T09:00"}
                                """.formatted(GROUP, "X".repeat(51))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("nombre"))
                .andExpect(jsonPath("$.details[0].code").value("FIELD_INVALID_LENGTH"))
                .andExpect(jsonPath("$.details[?(@.field=='tema')]").isEmpty());
        org.mockito.Mockito.verifyNoInteractions(create);
    }

    private void assertCreateRejectsUnknownField(final String expectedField, final String extraJsonProperty) throws Exception {
        mvc.perform(post("/api/v1/sesiones").contentType("application/json")
                        .content("""
                                {"grupo":"%s","nombre":"Tema válido",
                                 "fechaHoraInicio":"2026-09-14T08:00","fechaHoraFin":"2026-09-14T09:00",%s}
                                """.formatted(GROUP, extraJsonProperty)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value(expectedField))
                .andExpect(jsonPath("$.details[0].code").value("FIELD_UNKNOWN"));
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

    /**
     * Contrato TARGET (LB-001B.1, CONTRACT_FREEZE.md secc. 3.7/4): ActualizarSesionRequest/DTO
     * retiran aula/descripcion/room del contrato de actualizacion. Ya no envia esos campos y no
     * debe asertar ActualizarSesionDTO.getAula() (retirado). Sigue GREEN hoy porque el subconjunto
     * superviviente (nombre/fechas/actor) ya se mapea igual en AS-IS.
     */
    @Test
    void updateCloseAndGeneratePassPathBodyAndActor() throws Exception {
        mvc.perform(put("/api/v1/sesiones/{id}", SESSION).contentType("application/json")
                        .content("""
                                {"nombre":"Tema actualizado","fechaHoraInicio":"2026-09-14T08:00",
                                 "fechaHoraFin":"2026-09-14T10:00"}
                                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.exitoso").value(true));
        final var updateCaptor = ArgumentCaptor.forClass(ActualizarSesionDTO.class);
        verify(update).execute(updateCaptor.capture());
        assertEquals(SESSION, updateCaptor.getValue().getSesion());
        assertEquals("Tema actualizado", updateCaptor.getValue().getNombre());
        assertEquals(START.plusHours(2), updateCaptor.getValue().getFechaHoraFin());
        assertEquals(ACTOR, updateCaptor.getValue().getUsuarioEjecutor());

        mvc.perform(post("/api/v1/sesiones/cierres").contentType("application/json")
                        .content("{\"sesion\":\"" + SESSION + "\",\"observacionCierre\":\"Finalizada\"}"))
                .andExpect(status().isOk());
        final var closeCaptor = ArgumentCaptor.forClass(co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.primaryports.dto.CerrarSesionDTO.class);
        verify(close).execute(closeCaptor.capture());
        assertEquals(SESSION, closeCaptor.getValue().getSesion());
        assertEquals(ACTOR, closeCaptor.getValue().getDocente());
        assertEquals(ACTOR, closeCaptor.getValue().getUsuarioEjecutor());

        mvc.perform(post("/api/v1/sesiones/grupo/{id}/generacion", GROUP))
                .andExpect(status().isCreated());
        final var generateCaptor = ArgumentCaptor.forClass(co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.primaryports.dto.GenerarSesionesGrupoDTO.class);
        verify(generate).execute(generateCaptor.capture());
        assertEquals(GROUP, generateCaptor.getValue().getGrupo());
        assertEquals(ACTOR, generateCaptor.getValue().getUsuarioEjecutor());
    }

    /**
     * Contrato TARGET (LB-001B.1, CONTRACT_FREEZE.md secc. 2/3.7/7#17): una vez retirados
     * aula/descripcion/room de ActualizarSesionRequest, FAIL_ON_UNKNOWN_PROPERTIES debe rechazar
     * con 400 (FIELD_UNKNOWN) cualquier cliente que los siga enviando. RED esperado: hoy estos 3
     * campos todavia son reconocidos por ActualizarSesionRequest (incluido el alias setRoom -> aula),
     * por lo que la peticion HOY devuelve 200 (no 400) y el input port SI es invocado.
     */
    @Test
    void updateRejectsRetiredFieldsWithBadRequest() throws Exception {
        assertUpdateRejectsUnknownField("descripcion", "\"descripcion\":\"Cambio de aula\"");
        assertUpdateRejectsUnknownField("aula", "\"aula\":\"B202\"");
        assertUpdateRejectsUnknownField("room", "\"room\":\"B202\"");
        assertUpdateRejectsUnknownField("tipo", "\"tipo\":\"PRESENCIAL\"");
        assertUpdateRejectsUnknownField("tema", "\"tema\":\"Tema legado\"");
        assertUpdateRejectsUnknownField("topic", "\"topic\":\"Tema legado\"");
        assertUpdateRejectsUnknownField("status", "\"status\":\"ABIERTA\"");
        org.mockito.Mockito.verifyNoInteractions(update);
    }

    /** LB-001B.4A: PUT exacto {nombre, fechaHoraInicio, fechaHoraFin}; {@code tema} ya no es alias de nombre. */
    @Test
    void updateWithTemaInsteadOfNombreIsRejectedAsUnknownField() throws Exception {
        mvc.perform(put("/api/v1/sesiones/{id}", SESSION).contentType("application/json")
                        .content("""
                                {"tema":"Tema legado","fechaHoraInicio":"2026-09-14T08:00",
                                 "fechaHoraFin":"2026-09-14T10:00"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("tema"))
                .andExpect(jsonPath("$.details[0].code").value("FIELD_UNKNOWN"));
        org.mockito.Mockito.verifyNoInteractions(update);
    }

    private void assertUpdateRejectsUnknownField(final String expectedField, final String extraJsonProperty) throws Exception {
        mvc.perform(put("/api/v1/sesiones/{id}", SESSION).contentType("application/json")
                        .content("""
                                {"nombre":"Tema actualizado","fechaHoraInicio":"2026-09-14T08:00",
                                 "fechaHoraFin":"2026-09-14T10:00",%s}
                                """.formatted(extraJsonProperty)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value(expectedField))
                .andExpect(jsonPath("$.details[0].code").value("FIELD_UNKNOWN"));
    }

    private static final String VALID_UPDATE_BODY = """
            {"nombre":"Tema actualizado","fechaHoraInicio":"2026-09-14T08:00",
             "fechaHoraFin":"2026-09-14T10:00"}
            """;

    /** LB-001C.2A: PATCH delega al mismo input port con path, body y actor JWT; success 200 como el PUT. */
    @Test
    void patchUpdatesSessionThroughSameInputPortWithPathBodyAndActor() throws Exception {
        mvc.perform(patch("/api/v1/sesiones/{id}", SESSION).contentType("application/json").content(VALID_UPDATE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos").doesNotExist());
        final var captor = ArgumentCaptor.forClass(ActualizarSesionDTO.class);
        verify(update).execute(captor.capture());
        assertEquals(SESSION, captor.getValue().getSesion());
        assertEquals("Tema actualizado", captor.getValue().getNombre());
        assertEquals(START, captor.getValue().getFechaHoraInicio());
        assertEquals(START.plusHours(2), captor.getValue().getFechaHoraFin());
        assertEquals(ACTOR, captor.getValue().getUsuarioEjecutor());
    }

    /** LB-001C.2A: PUT legacy sigue operativo; PUT y PATCH producen el mismo comando y la misma respuesta. */
    @Test
    void putLegacyAndPatchProduceIdenticalCommandAndResponse() throws Exception {
        final String putBody = mvc.perform(put("/api/v1/sesiones/{id}", SESSION).contentType("application/json").content(VALID_UPDATE_BODY))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        final String patchBody = mvc.perform(patch("/api/v1/sesiones/{id}", SESSION).contentType("application/json").content(VALID_UPDATE_BODY))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals(putBody, patchBody);
        final var captor = ArgumentCaptor.forClass(ActualizarSesionDTO.class);
        verify(update, org.mockito.Mockito.times(2)).execute(captor.capture());
        final var commands = captor.getAllValues();
        assertEquals(commands.get(0).getSesion(), commands.get(1).getSesion());
        assertEquals(commands.get(0).getNombre(), commands.get(1).getNombre());
        assertEquals(commands.get(0).getFechaHoraInicio(), commands.get(1).getFechaHoraInicio());
        assertEquals(commands.get(0).getFechaHoraFin(), commands.get(1).getFechaHoraFin());
        assertEquals(commands.get(0).getUsuarioEjecutor(), commands.get(1).getUsuarioEjecutor());
    }

    @Test
    void patchPreservesValidationBehaviorOfPut() throws Exception {
        mvc.perform(patch("/api/v1/sesiones/{id}", SESSION).contentType("application/json")
                        .content("{\"fechaHoraInicio\":\"no-es-iso\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/v1/sesiones/{id}", SESSION).contentType("application/json")
                        .content("""
                                {"tema":"Tema legado","fechaHoraInicio":"2026-09-14T08:00",
                                 "fechaHoraFin":"2026-09-14T10:00"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("tema"))
                .andExpect(jsonPath("$.details[0].code").value("FIELD_UNKNOWN"));
        mvc.perform(patch("/api/v1/sesiones/{id}", SESSION).contentType("application/json")
                        .content("{\"nombre\":\"x\",\"fechaHoraInicio\":\"2026-09-14T08:00\",\"fechaHoraFin\":\"2026-09-14T10:00\",\"aula\":\"B202\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].code").value("FIELD_UNKNOWN"));
        org.mockito.Mockito.verifyNoInteractions(update);
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
    void groupListingDelegatesToInputPortWithAuthenticatedActor() throws Exception {
        when(queryByGroup.execute(any(ConsultarSesionesPorGrupoDTO.class))).thenReturn(List.of(
                new SesionConsultadaDTO(SESSION, GROUP, "Sesión 1", 1, "S01", 2, "G01", "Grupo 1", START, START.plusHours(1))
        ));

        mvc.perform(get("/api/v1/sesiones/grupo/{grupoId}", GROUP))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos[0].sesion").value(SESSION.toString()))
                .andExpect(jsonPath("$.total").value(1));

        final var captor = ArgumentCaptor.forClass(ConsultarSesionesPorGrupoDTO.class);
        verify(queryByGroup).execute(captor.capture());
        assertEquals(GROUP, captor.getValue().getGrupo());
        assertEquals(ACTOR, captor.getValue().getUsuarioEjecutor());
    }
}
