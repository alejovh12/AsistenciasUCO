package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia;

import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.any;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.ParameterizedTest;
import co.edu.uco.asistenciasuco.application.features.asistencia.exception.AsistenciaErrorCode;
import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.primaryports.RegistrarAsistenciasSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.primaryports.dto.RegistrarAsistenciasSesionDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.primaryports.SolicitarRevisionAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.primaryports.dto.SolicitarRevisionAsistenciaDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.mapper.AsistenciaHttpMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.request.ConsultarAsistenciasPorGrupoRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.request.RegistrarAsistenciasSesionRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AsistenciaControllerContractTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID STUDENT = UUID.randomUUID();
    private static final UUID GROUP = UUID.randomUUID();
    private static final UUID SESSION = UUID.randomUUID();
    private static final UUID SUPPORT = UUID.randomUUID();
    private static final UUID SUPPORT_2 = UUID.randomUUID();
    private final RegistrarAsistenciasSesionInputPort registerBatch = mock(RegistrarAsistenciasSesionInputPort.class);
    private final SolicitarRevisionAsistenciaInputPort review = mock(SolicitarRevisionAsistenciaInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AsistenciaController(
            registerBatch, review, identity)).setControllerAdvice(new GlobalExceptionHandler(codigo -> java.util.Optional.empty())).build();
    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void individualRegistrationEndpointIsRetiredAndHasNoHandler() throws Exception {
        // CULM-001 D.7 (TD-009, HU027/HU033): POST /api/v1/asistencias ya no existe; el registro es por lote o por toma.
        mvc.perform(post("/api/v1/asistencias").contentType("application/json")
                        .content("""
                                {"estudiante":"%s","grupo":"%s","sesion":"%s",
                                 "presente":false,"observacion":"Excusa médica"}
                                """.formatted(STUDENT, GROUP, SESSION)))
                // Montaje standalone: sin handler no hay NoResourceFoundException (el 404 HTTP real lo fija
                // RutaRegistroIndividualRetiradaWebMvcTest); aqui se comprueba que ningun handler atiende la ruta.
                .andExpect(result -> assertNull(result.getHandler()));
        verifyNoInteractions(registerBatch, review);
    }

    @Test
    void reviewAcceptsLegacyAliasesAndUsesAuthenticatedActor() throws Exception {
        mvc.perform(post("/api/v1/asistencias/revisiones").contentType("application/json")
                        .content("""
                                {"sesion":"%s","categoria":"SALUD","motivo":"Cita médica",
                                 "soporteArchivoId":"%s",
                                 "soporteNombre":"excusa.pdf","soporteUrl":"https://example.com/excusa"}
                                """.formatted(SESSION, SUPPORT)))
                .andExpect(status().isAccepted());
        final var captor = ArgumentCaptor.forClass(SolicitarRevisionAsistenciaDTO.class);
        verify(review).execute(captor.capture());
        assertEquals(SESSION, captor.getValue().getSesion());
        assertEquals(ACTOR, captor.getValue().getUsuario());
        assertEquals("SALUD", captor.getValue().getCategoria());
        assertEquals("Cita médica", captor.getValue().getJustificacion());
        // El unico dato de soporte que viaja a Application es el fileId; soporteNombre/soporteUrl legacy se ignoran.
        assertEquals(List.of(SUPPORT), captor.getValue().getSoportes());
    }

    // ---- CULM-001 D.2 (CONTRACT D 3.3, HU014/HU015): soporteArchivoIds (0..N); soporteArchivoId (legado) = lista de uno ----

    @Test
    void reviewWithTomaIdCarriesItToApplicationAndWithoutItStaysLegacy() throws Exception {
        final java.util.UUID toma = java.util.UUID.fromString("99999999-9999-9999-9999-999999999999");
        mvc.perform(post("/api/v1/asistencias/revisiones").contentType("application/json")
                        .content("{\"sesionId\":\"" + SESSION + "\",\"tomaId\":\"" + toma + "\",\"categoria\":\"MED\",\"justificacion\":\"Cita\"}"))
                .andExpect(status().isAccepted());
        mvc.perform(post("/api/v1/asistencias/revisiones").contentType("application/json")
                        .content("{\"sesionId\":\"" + SESSION + "\",\"categoria\":\"MED\",\"justificacion\":\"Cita\"}"))
                .andExpect(status().isAccepted());
        final var captor = ArgumentCaptor.forClass(SolicitarRevisionAsistenciaDTO.class);
        verify(review, org.mockito.Mockito.times(2)).execute(captor.capture());
        assertEquals(toma, captor.getAllValues().get(0).getToma());
        assertEquals(null, captor.getAllValues().get(1).getToma());
    }

    @Test
    void reviewAcceptsSeveralSupportsInOrder() throws Exception {
        mvc.perform(post("/api/v1/asistencias/revisiones").contentType("application/json")
                        .content("""
                                {"sesionId":"%s","categoria":"MED","justificacion":"Cita médica",
                                 "soporteArchivoIds":["%s","%s"]}
                                """.formatted(SESSION, SUPPORT, SUPPORT_2)))
                .andExpect(status().isAccepted());
        final var captor = ArgumentCaptor.forClass(SolicitarRevisionAsistenciaDTO.class);
        verify(review).execute(captor.capture());
        assertEquals(List.of(SUPPORT, SUPPORT_2), captor.getValue().getSoportes());
        assertEquals("MED", captor.getValue().getCategoria());
    }

    @Test
    void reviewWithoutSupportsSendsAnEmptyList() throws Exception {
        mvc.perform(post("/api/v1/asistencias/revisiones").contentType("application/json")
                        .content("""
                                {"sesionId":"%s","categoria":"CAL","justificacion":"Calamidad"}
                                """.formatted(SESSION)))
                .andExpect(status().isAccepted());
        final var captor = ArgumentCaptor.forClass(SolicitarRevisionAsistenciaDTO.class);
        verify(review).execute(captor.capture());
        assertEquals(List.of(), captor.getValue().getSoportes());
    }

    @Test
    void reviewRejectsTheLegacyAndTheNewSupportFieldAtTheSameTime() throws Exception {
        mvc.perform(post("/api/v1/asistencias/revisiones").contentType("application/json")
                        .content("""
                                {"sesionId":"%s","categoria":"MED","justificacion":"Cita médica",
                                 "soporteArchivoId":"%s","soporteArchivoIds":["%s"]}
                                """.formatted(SESSION, SUPPORT, SUPPORT_2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("soporteArchivoIds"))
                .andExpect(jsonPath("$.details[0].code").value("FIELD_INVALID_VALUE"));
        verifyNoInteractions(review);
    }

    @Test
    void reviewRejectsANullElementInTheSupportList() throws Exception {
        mvc.perform(post("/api/v1/asistencias/revisiones").contentType("application/json")
                        .content("""
                                {"sesionId":"%s","categoria":"MED","justificacion":"Cita médica","soporteArchivoIds":["%s",null]}
                                """.formatted(SESSION, SUPPORT)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("soporteArchivoIds"));
        verifyNoInteractions(review);
    }

    @ParameterizedTest
    @EnumSource(value = AsistenciaErrorCode.class, names = {"ERR_CATEGORIA_JUSTIFICACION_INVALIDA", "ERR_SOPORTE_OBLIGATORIO_CATEGORIA",
            "ERR_MAXIMO_SOPORTES_RECLAMO", "ERR_SOPORTE_RECLAMO_DUPLICADO"})
    void reviewRejectionsOfTheCategoryAndTheSupportsAreBadRequestWithThePublicCode(final AsistenciaErrorCode code) throws Exception {
        doThrow(new ValidationException(code)).when(review).execute(any());

        mvc.perform(post("/api/v1/asistencias/revisiones").contentType("application/json")
                        .content("""
                                {"sesionId":"%s","categoria":"MED","justificacion":"Cita médica","soporteArchivoIds":["%s"]}
                                """.formatted(SESSION, SUPPORT)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(code.code()));
    }

    @Test
    void reviewAfterTheDeadlineIsConflictWithThePublicCode() throws Exception {
        doThrow(new ConflictException(AsistenciaErrorCode.ERR_PLAZO_JUSTIFICACION_VENCIDO)).when(review).execute(any());

        mvc.perform(post("/api/v1/asistencias/revisiones").contentType("application/json")
                        .content("""
                                {"sesionId":"%s","categoria":"CAL","justificacion":"Calamidad"}
                                """.formatted(SESSION)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ERR_PLAZO_JUSTIFICACION_VENCIDO"));
    }

    @Test
    void batchMapsEachStudentAndStatusAndPropagatesAuthenticatedUser() throws Exception {
        mvc.perform(post("/api/v1/asistencias/lote").contentType("application/json")
                        .content("""
                                {"sesionId":"%s","registros":[{"estudianteId":"%s","estado":"AN"}]}
                                """.formatted(SESSION, STUDENT)))
                .andExpect(status().isCreated());
        final var captor = ArgumentCaptor.forClass(RegistrarAsistenciasSesionDTO.class);
        verify(registerBatch).execute(captor.capture());
        assertEquals(SESSION, captor.getValue().getSesion());
        assertEquals(1, captor.getValue().getRegistros().size());
        assertEquals(STUDENT, captor.getValue().getRegistros().getFirst().getEstudiante());
        assertEquals("AN", captor.getValue().getRegistros().getFirst().getEstado());
        assertEquals(ACTOR, captor.getValue().getUsuarioEjecutor());
    }

    @ParameterizedTest
    @EnumSource(value = AsistenciaErrorCode.class,
            names = {"ERR_SESION_NO_INICIADA", "ERR_SESION_CERRADA", "ERR_SESION_HORARIO_INDETERMINADO"})
    void batchOutsideTheActiveSessionWindowReturnsConflictWithThePublicCode(final AsistenciaErrorCode code) throws Exception {
        doThrow(new ConflictException(code)).when(registerBatch).execute(any());

        mvc.perform(post("/api/v1/asistencias/lote").contentType("application/json")
                        .content("""
                                {"sesionId":"%s","registros":[{"estudianteId":"%s","estado":"AN"}]}
                                """.formatted(SESSION, STUDENT)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(code.code()))
                .andExpect(jsonPath("$.message").value(code.defaultMessage()));
    }

    @Test
    void invalidRequestsAreRejectedBeforePorts() throws Exception {
        mvc.perform(post("/api/v1/asistencias/revisiones").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(review);
    }

    @Test
    void mapperHandlesEmptyAndNullBatchAndQrAliases() {
        assertThrows(NullPointerException.class,
                () -> AsistenciaHttpMapper.toApplicationDTO((RegistrarAsistenciasSesionRequest) null, ACTOR));
        final RegistrarAsistenciasSesionRequest empty = new RegistrarAsistenciasSesionRequest();
        empty.setSesionId(SESSION);
        assertTrue(AsistenciaHttpMapper.toApplicationDTO(empty, ACTOR).getRegistros().isEmpty());
        assertEquals(ACTOR, AsistenciaHttpMapper.toApplicationDTO(empty, ACTOR).getUsuarioEjecutor());
        empty.setRegistros(java.util.Collections.singletonList(null));
        assertEquals(null, AsistenciaHttpMapper.toApplicationDTO(empty, ACTOR).getRegistros().getFirst().getEstudiante());

        final ConsultarAsistenciasPorGrupoRequest query = json.readValue(
                "{\"grupo\":\"" + GROUP + "\",\"sesion\":\"" + SESSION + "\"}",
                ConsultarAsistenciasPorGrupoRequest.class);
        assertEquals(GROUP, AsistenciaHttpMapper.toApplicationDTO(query, ACTOR).getGrupo());
        assertEquals(SESSION, AsistenciaHttpMapper.toApplicationDTO(query, ACTOR).getSesion());
        assertEquals(ACTOR, AsistenciaHttpMapper.toApplicationDTO(query, ACTOR).getUsuarioEjecutor());
    }
}
