package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.FeatureUnavailableException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.primaryports.dto.ConsultarEstudiantesProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.primaryports.dto.EstudianteProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.primaryports.dto.PaginaEstudiantesProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarperiodosacademicos.primaryports.dto.PeriodoAcademicoDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.primaryports.ConsultarEstudiantesProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarperiodosacademicos.primaryports.ConsultarPeriodosAcademicosInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.primaryports.GestionarAsignaturaInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.primaryports.dto.EstadoAsignaturaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.primaryports.dto.GuardarAsignaturaDTO;
import co.edu.uco.asistenciasuco.application.features.admin.exception.CatalogoErrorCode;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarplanestudio.primaryports.GestionarPlanEstudioInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarplanestudio.primaryports.dto.GuardarPlanEstudioDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.times;

class CoordinadorPortalControllerTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID PLAN = UUID.randomUUID();
    private static final UUID SUBJECT = UUID.randomUUID();

    private final ConsultarPeriodosAcademicosInputPort periods = mock(ConsultarPeriodosAcademicosInputPort.class);
    private final ConsultarEstudiantesProgramaInputPort students = mock(ConsultarEstudiantesProgramaInputPort.class);
    private final GestionarPlanEstudioInputPort managePlans = mock(GestionarPlanEstudioInputPort.class);
    private final GestionarAsignaturaInputPort manageSubjects = mock(GestionarAsignaturaInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final CoordinadorPortalController controller = new CoordinadorPortalController(
            periods, students, managePlans, manageSubjects, identity);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty())).build();

    @Test
    void periodosAcademicosTrasladanElFiltroDeEstadoYElTotalEsElDeLaListaFiltrada() throws Exception {
        when(periods.execute("ACTIVO")).thenReturn(List.of(new PeriodoAcademicoDTO(UUID.randomUUID(), UUID.randomUUID(),
                "UCO", "2026-2", "2026-2", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 1), 2026, "ACTIVO")));

        mvc.perform(get("/api/v1/coordinador/periodos-academicos").param("estado", "ACTIVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.datos[0].estado").value("ACTIVO"));
        verify(periods).execute("ACTIVO");
    }

    @Test
    void queryEndpointsPassActorAndSerializePortResults() throws Exception {
        when(periods.execute((String) null)).thenReturn(List.of(new PeriodoAcademicoDTO(UUID.randomUUID(), UUID.randomUUID(),
                "UCO", "2026-1", "2026-1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 1), 2026, "CERRADO")));
        final UUID programa = UUID.randomUUID();
        when(students.execute(any())).thenReturn(new PaginaEstudiantesProgramaDTO(List.of(new EstudianteProgramaDTO(
                UUID.randomUUID(), UUID.randomUUID(), "123456", "Ana", "ana@example.com", programa, "Ingeniería")), 41));

        mvc.perform(get("/api/v1/coordinador/periodos-academicos"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.datos[0].nombre").value("2026-1"))
                .andExpect(jsonPath("$.datos[0].estado").value("CERRADO"));
        // CULM-001 B2NM (S4, O2-B2): estudiantes paginado por el SP de directorio; programaId opcional
        mvc.perform(get("/api/v1/coordinador/estudiantes").param("programaId", programa.toString())
                        .param("busqueda", "ana").param("estaActivo", "true").param("pagina", "2").param("tamano", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.totalElementos").value(41))
                .andExpect(jsonPath("$.numeroPagina").value(2))
                .andExpect(jsonPath("$.tamanoPagina").value(10))
                .andExpect(jsonPath("$.elementos[0].nombreCompleto").value("Ana"))
                .andExpect(jsonPath("$.elementos[0].idPrograma").value(programa.toString()));
        final var captor = ArgumentCaptor.forClass(ConsultarEstudiantesProgramaDTO.class);
        verify(students).execute(captor.capture());
        assertEquals(ACTOR, captor.getValue().actorUsuarioId());
        assertEquals(programa, captor.getValue().programaId());
        assertEquals("ana", captor.getValue().busqueda());
        assertEquals(Boolean.TRUE, captor.getValue().estaActivo());
        assertEquals(2, captor.getValue().pagina());
        assertEquals(10, captor.getValue().tamano());
    }

    @Test
    void estudiantesSinFiltrosUsaPaginaUnoYTamanoVeinte() throws Exception {
        when(students.execute(any())).thenReturn(new PaginaEstudiantesProgramaDTO(List.of(), 0));

        mvc.perform(get("/api/v1/coordinador/estudiantes")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(0)).andExpect(jsonPath("$.elementos").isEmpty());
        final var captor = ArgumentCaptor.forClass(ConsultarEstudiantesProgramaDTO.class);
        verify(students).execute(captor.capture());
        assertNull(captor.getValue().programaId());
        assertEquals(1, captor.getValue().pagina());
        assertEquals(20, captor.getValue().tamano());
    }

    @Test
    void estudiantesDeProgramaAjenoEs403() throws Exception {
        when(students.execute(any())).thenThrow(new co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException("COO_003"));

        mvc.perform(get("/api/v1/coordinador/estudiantes").param("programaId", UUID.randomUUID().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void commandsMapBodyAndPathToInputPorts() throws Exception {
        mvc.perform(post("/api/v1/coordinador/planes-estudio").contentType("application/json")
                        .content("{\"inp\":2026}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.exitoso").value(true));
        mvc.perform(put("/api/v1/coordinador/planes-estudio/{id}", PLAN).contentType("application/json")
                        .content("{\"inp\":2027}"))
                .andExpect(status().isOk());
        final var planCaptor = ArgumentCaptor.forClass(GuardarPlanEstudioDTO.class);
        verify(managePlans, times(2)).guardar(planCaptor.capture());
        assertNull(planCaptor.getAllValues().get(0).idPlanEstudio());
        assertEquals(2026, planCaptor.getAllValues().get(0).inp());
        assertEquals(ACTOR, planCaptor.getAllValues().get(0).usuario());
        assertEquals(PLAN, planCaptor.getAllValues().get(1).idPlanEstudio());
        assertEquals(2027, planCaptor.getAllValues().get(1).inp());

        final String body = "{\"codigo\":\"A01\",\"nombre\":\"Álgebra\",\"creditos\":3,"
                + "\"semestreNumero\":2,\"nombreArea\":\"Matemáticas\",\"nombreComponente\":\"Básico\"}";
        mvc.perform(post("/api/v1/coordinador/planes-estudio/{id}/asignaturas", PLAN)
                        .contentType("application/json").content(body)).andExpect(status().isOk());
        mvc.perform(put("/api/v1/coordinador/planes-estudio/{planId}/asignaturas/{asigId}", PLAN, SUBJECT)
                        .contentType("application/json").content(body)).andExpect(status().isOk());
        final var subjectCaptor = ArgumentCaptor.forClass(GuardarAsignaturaDTO.class);
        verify(manageSubjects).crear(subjectCaptor.capture());
        assertNull(subjectCaptor.getValue().idAsignatura());
        assertEquals(PLAN, subjectCaptor.getValue().idPlanEstudio());
        assertEquals("A01", subjectCaptor.getValue().codigo());
        assertEquals(3, subjectCaptor.getValue().creditos());
        assertEquals("Matemáticas", subjectCaptor.getValue().nombreArea());
        verify(manageSubjects).actualizar(subjectCaptor.capture());
        assertEquals(SUBJECT, subjectCaptor.getValue().idAsignatura());
        assertEquals(ACTOR, subjectCaptor.getValue().usuarioEjecutor());

        when(manageSubjects.toggleEstado(SUBJECT, null, ACTOR)).thenReturn(new EstadoAsignaturaDTO(SUBJECT, false, "La asignatura quedó inactiva."));
        mvc.perform(patch("/api/v1/coordinador/asignaturas/{id}/estado", SUBJECT))
                .andExpect(status().isOk());
        verify(manageSubjects).toggleEstado(SUBJECT, null, ACTOR);
    }

    // HU074: el PATCH de estado responde el estado en que quedo la asignatura y el mensaje del catalogo (ASI_011 / ASI_012).
    @Test
    void hu074_toggle_estado_responde_200_con_el_estado_nuevo_y_el_mensaje_de_la_bd() throws Exception {
        when(manageSubjects.toggleEstado(SUBJECT, null, ACTOR)).thenReturn(new EstadoAsignaturaDTO(SUBJECT, true, "La asignatura quedó activa."));

        mvc.perform(patch("/api/v1/coordinador/asignaturas/{id}/estado", SUBJECT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.idAsignatura").value(SUBJECT.toString()))
                .andExpect(jsonPath("$.datos.estaActivaAsignatura").value(true))
                .andExpect(jsonPath("$.mensajeUsuario").value("La asignatura quedó activa."));
    }

    // HU074: cuerpo opcional {"estaActiva": true|false}; ausente, vacio o null alterna (contrato anterior).
    @ParameterizedTest
    @CsvSource(delimiter = ';', nullValues = "NULL", value = {
            "{\"estaActiva\": true};true",
            "{\"estaActiva\": false};false",
            "{\"estaActiva\": null};NULL",
            "{};NULL"
    })
    void hu074_toggle_estado_pasa_el_estado_pedido_en_el_cuerpo(final String cuerpo, final Boolean deseada) throws Exception {
        final boolean quedaActiva = !Boolean.FALSE.equals(deseada);
        when(manageSubjects.toggleEstado(SUBJECT, deseada, ACTOR))
                .thenReturn(new EstadoAsignaturaDTO(SUBJECT, quedaActiva, "Mensaje del SP."));

        mvc.perform(patch("/api/v1/coordinador/asignaturas/{id}/estado", SUBJECT).contentType("application/json").content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.estaActivaAsignatura").value(quedaActiva))
                .andExpect(jsonPath("$.mensajeUsuario").value("Mensaje del SP."));
        verify(manageSubjects).toggleEstado(SUBJECT, deseada, ACTOR);
    }

    @Test
    void hu074_toggle_estado_con_estaActiva_no_booleano_responde_400_sin_llamar_al_puerto() throws Exception {
        mvc.perform(patch("/api/v1/coordinador/asignaturas/{id}/estado", SUBJECT).contentType("application/json")
                        .content("{\"estaActiva\": \"quizas\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(manageSubjects);
    }

    // HU074: ASI_009 (grupos en el periodo vigente o futuros) y ASI_006 (compartida con programas ajenos) son 409 con su codigo y
    // el texto del catalogo de mensajes.
    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
            "ASI_009;No puedes inactivar la asignatura: tiene grupos en el período vigente o en períodos futuros.",
            "ASI_006;La asignatura es compartida con programas que no coordinas; solo puedes cambiar su semestre y su carácter en tu plan."
    })
    void hu074_toggle_estado_rechazado_responde_409_con_el_mensaje_del_catalogo(final String codigo, final String texto) throws Exception {
        final MockMvc conCatalogo = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(mensaje -> codigo.equals(mensaje) ? Optional.of(texto) : Optional.empty()))
                .build();
        doThrow(new ConflictException(CatalogoErrorCode.valueOf(codigo))).when(manageSubjects).toggleEstado(SUBJECT, null, ACTOR);

        conCatalogo.perform(patch("/api/v1/coordinador/asignaturas/{id}/estado", SUBJECT))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(codigo))
                .andExpect(jsonPath("$.message").value(texto));
    }

    @Test
    void missingBodyAndPortFailurePreserveHttpErrorContract() throws Exception {
        mvc.perform(post("/api/v1/coordinador/planes-estudio").contentType("application/json"))
                .andExpect(status().isBadRequest());
        doThrow(new FeatureUnavailableException("Sin capacidad pública"))
                .when(manageSubjects).toggleEstado(SUBJECT, null, ACTOR);
        mvc.perform(patch("/api/v1/coordinador/asignaturas/{id}/estado", SUBJECT))
                .andExpect(status().isNotImplemented());
    }

    @Test
    void planStateAndSemesterCommandsDelegateToInputPortWithAuthenticatedActor() throws Exception {
        mvc.perform(patch("/api/v1/coordinador/planes-estudio/{id}/toggle-estado", PLAN))
                .andExpect(status().isOk()).andExpect(jsonPath("$.exitoso").value(true));
        mvc.perform(post("/api/v1/coordinador/planes-estudio/{id}/semestres", PLAN))
                .andExpect(status().isOk()).andExpect(jsonPath("$.exitoso").value(true));
        mvc.perform(delete("/api/v1/coordinador/planes-estudio/{id}/semestres/{numero}", PLAN, 8))
                .andExpect(status().isOk()).andExpect(jsonPath("$.exitoso").value(true));

        verify(managePlans).cambiarEstado(PLAN, ACTOR);
        verify(managePlans).agregarSemestre(PLAN, ACTOR);
        verify(managePlans).quitarSemestre(PLAN, 8, ACTOR);
    }

    @Test
    void planSemesterBusinessRejectionsKeepHttpErrorContract() throws Exception {
        doThrow(new ConflictException("El semestre tiene asignaturas asociadas."))
                .when(managePlans).quitarSemestre(PLAN, 3, ACTOR);
        doThrow(new ResourceNotFoundException("El plan de estudio indicado no existe."))
                .when(managePlans).cambiarEstado(PLAN, ACTOR);

        mvc.perform(delete("/api/v1/coordinador/planes-estudio/{id}/semestres/{numero}", PLAN, 3))
                .andExpect(status().isConflict());
        mvc.perform(patch("/api/v1/coordinador/planes-estudio/{id}/toggle-estado", PLAN))
                .andExpect(status().isNotFound());
    }

    // CULM-001 A.2-14 (A2-B-16): los stubs 501 DELETE /planes-estudio/{planId}/asignaturas/{asigId} y
    // PATCH /periodos-academicos/{id}/estado se retiran; la lista de 501 queda vacia y su caso se sustituye por este.
    @Test
    void stubs_501_de_asignatura_del_plan_y_estado_del_periodo_retirados_no_tienen_ruta() {
        final List<String> rutas = Arrays.stream(CoordinadorPortalController.class.getDeclaredMethods())
                .filter(metodo -> metodo.isAnnotationPresent(DeleteMapping.class) || metodo.isAnnotationPresent(PatchMapping.class))
                .flatMap(CoordinadorPortalControllerTest::rutasDe)
                .toList();

        assertTrue(rutas.stream().noneMatch(ruta -> ruta.equals("/planes-estudio/{planId}/asignaturas/{asigId}")
                        || ruta.equals("/periodos-academicos/{id}/estado")),
                () -> "Stubs retirados aun mapeados: " + rutas);
    }

    // CULM-001 A.1 (B-RED-03): HU053/HU054 descartadas y F-COO-004 (el coordinador no matricula). Las rutas de
    // solicitudes de matricula y de matricula/retiro del coordinador se retiran (404 sin handler).
    @Test
    void solicitudes_y_matricula_del_coordinador_retiradas_no_tienen_ruta() {
        final List<String> rutas = Arrays.stream(CoordinadorPortalController.class.getDeclaredMethods())
                .flatMap(CoordinadorPortalControllerTest::rutasDe)
                .toList();

        assertFalse(rutas.isEmpty());
        assertTrue(rutas.stream().noneMatch(ruta -> ruta.startsWith("/solicitudes-matricula")
                        || ruta.startsWith("/grupos/{grupoId}/estudiantes")),
                () -> "Rutas retiradas aun mapeadas: " + rutas);
    }

    static Stream<String> rutasDe(final Method metodo) {
        return Stream.of(
                        Optional.ofNullable(metodo.getAnnotation(GetMapping.class)).map(GetMapping::value),
                        Optional.ofNullable(metodo.getAnnotation(PostMapping.class)).map(PostMapping::value),
                        Optional.ofNullable(metodo.getAnnotation(PutMapping.class)).map(PutMapping::value),
                        Optional.ofNullable(metodo.getAnnotation(PatchMapping.class)).map(PatchMapping::value),
                        Optional.ofNullable(metodo.getAnnotation(DeleteMapping.class)).map(DeleteMapping::value))
                .flatMap(Optional::stream)
                .flatMap(Arrays::stream);
    }
}
