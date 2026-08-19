package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente;

import co.edu.uco.asistenciasuco.application.exception.ErrorCode;
import co.edu.uco.asistenciasuco.application.exception.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.primaryports.AsignarDocenteAGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.primaryports.dto.AsignarDocenteAGrupoResultadoDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.primaryports.ConsultarAsignacionesAcademicasDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.primaryports.dto.DocenteAsignacionAcademicaDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.primaryports.ConsultarDocentesInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.primaryports.dto.DocenteIdentidadDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.primaryports.ConsultarDocentePorIdInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.primaryports.RegistrarDocenteDesdeUsuarioInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.primaryports.dto.RegistrarDocenteDesdeUsuarioResultadoDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.correlation.CorrelationIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DocenteControllerTest {

    private static final UUID DOCENTE = UUID.fromString("13641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID USUARIO = UUID.fromString("23641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID GRUPO = UUID.fromString("33641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID CORRELACION_CONTEXTO = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @AfterEach
    void clearCorrelationContext() {
        CorrelationIdContext.clear();
    }

    @Test
    void consultar_docentes_responde_catalogo_general() throws Exception {
        mockMvc().perform(get("/api/v1/docentes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(DOCENTE.toString()))
                .andExpect(jsonPath("$[0].idUsuario").value(USUARIO.toString()));
    }

    @Test
    void consultar_docente_por_id_responde_api_response_existente_con_datos() throws Exception {
        mockMvc().perform(post("/api/v1/docentes/consultas/id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "docente": "%s"
                                }
                                """.formatted(DOCENTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.id").value(DOCENTE.toString()));
    }

    @Test
    void consultar_docente_por_id_inexistente_responde_404_con_codigo_especifico() throws Exception {
        mockMvc(dto -> {
            throw new ResourceNotFoundException(ErrorCode.ERR_DOCENTE_NO_EXISTE);
        }).perform(post("/api/v1/docentes/consultas/id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "docente": "%s"
                                }
                                """.formatted(DOCENTE)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ERR_DOCENTE_NO_EXISTE"))
                .andExpect(jsonPath("$.message").value("El docente consultado no existe."));
    }

    @Test
    void consultar_asignaciones_responde_lista_y_total() throws Exception {
        mockMvc().perform(post("/api/v1/docentes/consultas/asignaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "docente": "%s"
                                }
                                """.formatted(DOCENTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.datos[0].idGrupo").value(GRUPO.toString()));
    }

    @Test
    void registrar_y_asignar_responden_sin_mensaje_tecnico() throws Exception {
        CorrelationIdContext.set(CORRELACION_CONTEXTO);

        mockMvc().perform(post("/api/v1/docentes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "usuario": "%s"
                                }
                                """.formatted(USUARIO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.mensajeUsuario").value("Docente registrado."))
                .andExpect(jsonPath("$.idCorrelacion").doesNotExist())
                .andExpect(jsonPath("$.mensajeTecnicoResultado").doesNotExist());

        mockMvc().perform(post("/api/v1/docentes/asignaciones/grupo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "docente": "%s",
                                  "grupo": "%s"
                                }
                                """.formatted(DOCENTE, GRUPO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.mensajeUsuario").value("Docente asignado."))
                .andExpect(jsonPath("$.idCorrelacion").doesNotExist())
                .andExpect(jsonPath("$.mensajeTecnicoResultado").doesNotExist());
    }

    private MockMvc mockMvc() {
        return mockMvc(dto -> identidad());
    }

    private MockMvc mockMvc(final ConsultarDocentePorIdInputPort consultarPorId) {
        final ConsultarDocentesInputPort consultarDocentes = () -> List.of(identidad());
        final ConsultarAsignacionesAcademicasDocenteInputPort consultarAsignaciones = dto -> List.of(asignacion());
        final RegistrarDocenteDesdeUsuarioInputPort registrar =
                dto -> new RegistrarDocenteDesdeUsuarioResultadoDTO(true, "Docente registrado.");
        final AsignarDocenteAGrupoInputPort asignar =
                dto -> new AsignarDocenteAGrupoResultadoDTO(true, "Docente asignado.");

        return MockMvcBuilders
                .standaloneSetup(new DocenteController(
                        consultarDocentes,
                        consultarPorId,
                        consultarAsignaciones,
                        registrar,
                        asignar
                ))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private DocenteIdentidadDTO identidad() {
        return new DocenteIdentidadDTO(DOCENTE, USUARIO, 123456789, "Ana Perez", true);
    }

    private DocenteAsignacionAcademicaDTO asignacion() {
        return new DocenteAsignacionAcademicaDTO(
                DOCENTE,
                USUARIO,
                123456789,
                "Ana Perez",
                true,
                UUID.fromString("43641bab-e3cd-485c-b275-47e7b731e18c"),
                "UCO",
                UUID.fromString("53641bab-e3cd-485c-b275-47e7b731e18c"),
                "Ingenieria",
                UUID.fromString("63641bab-e3cd-485c-b275-47e7b731e18c"),
                "Sistemas",
                UUID.fromString("73641bab-e3cd-485c-b275-47e7b731e18c"),
                "2024",
                UUID.fromString("83641bab-e3cd-485c-b275-47e7b731e18c"),
                "Backend",
                GRUPO,
                "G1",
                UUID.fromString("93641bab-e3cd-485c-b275-47e7b731e18b"),
                "DO",
                "Docente",
                true,
                "ACTIVO"
        );
    }
}
