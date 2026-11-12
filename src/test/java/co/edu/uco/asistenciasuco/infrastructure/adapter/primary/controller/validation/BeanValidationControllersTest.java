package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation;

import co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.primaryports.AdmitirEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarprogramasinstitucion.primaryports.ConsultarProgramasInstitucionInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.asignaturaplan.primaryports.AsignaturaPlanInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.asignaturaplan.primaryports.dto.AsociarAsignaturaPlanDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.primaryports.ConsultarEstudiantesProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarperiodosacademicos.primaryports.ConsultarPeriodosAcademicosInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.primaryports.GestionarAsignaturaInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarplanestudio.primaryports.GestionarPlanEstudioInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.retirarestudiantegrupo.primaryports.RetirarEstudianteGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.abrirtoma.primaryports.AbrirTomaAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.cerrartoma.primaryports.CerrarTomaAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.consultarasistenciastoma.primaryports.ConsultarAsistenciasTomaInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.consultarfaltantestoma.primaryports.ConsultarFaltantesTomaInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.consultarresumensesion.primaryports.ConsultarResumenTomasSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.consultartomassesion.primaryports.ConsultarTomasSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.registrarasistenciastoma.primaryports.RegistrarAsistenciasTomaInputPort;
import co.edu.uco.asistenciasuco.application.features.usuario.verificarcorreo.primaryports.VerificarCorreoInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.AdminAdmisionEstudianteController;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.AsignaturaPlanController;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.CoordinadorPortalController;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.DocenteGrupoEstudianteController;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.tomaasistencia.SesionTomaController;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.tomaasistencia.TomaAsistenciaController;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.verificacioncorreo.VerificacionCorreoController;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ROL-06 (decision G): Jakarta Bean Validation es la primera barrera de los cuerpos HTTP. Cubre los controladores que no
 * tenian validator propio (toma de asistencia, asignaturas del plan, portal del coordinador, retiro de estudiante,
 * verificacion de correo) y el contrato de error: 400 {@code VALIDATION_ERROR} con {@code details[]} del mismo formato que
 * los validators propios, sin mensajes por defecto del proveedor y sin llegar al caso de uso.
 */
class BeanValidationControllersTest {

    private static final UUID ACTOR = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private AbrirTomaAsistenciaInputPort abrirToma;
    private RegistrarAsistenciasTomaInputPort registrarToma;
    private AsignaturaPlanInputPort asignaturaPlan;
    private GestionarPlanEstudioInputPort planEstudio;
    private GestionarAsignaturaInputPort asignatura;
    private RetirarEstudianteGrupoInputPort retirar;
    private VerificarCorreoInputPort verificarCorreo;
    private AdmitirEstudianteInputPort admitir;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        abrirToma = mock(AbrirTomaAsistenciaInputPort.class);
        registrarToma = mock(RegistrarAsistenciasTomaInputPort.class);
        asignaturaPlan = mock(AsignaturaPlanInputPort.class);
        planEstudio = mock(GestionarPlanEstudioInputPort.class);
        asignatura = mock(GestionarAsignaturaInputPort.class);
        retirar = mock(RetirarEstudianteGrupoInputPort.class);
        verificarCorreo = mock(VerificarCorreoInputPort.class);
        admitir = mock(AdmitirEstudianteInputPort.class);
        final AuthenticatedUserResolver usuario = () -> ACTOR;
        mvc = MockMvcBuilders.standaloneSetup(
                        new SesionTomaController(abrirToma, mock(ConsultarTomasSesionInputPort.class),
                                mock(ConsultarResumenTomasSesionInputPort.class), usuario),
                        new TomaAsistenciaController(mock(CerrarTomaAsistenciaInputPort.class), registrarToma,
                                mock(ConsultarAsistenciasTomaInputPort.class), mock(ConsultarFaltantesTomaInputPort.class), usuario),
                        new AsignaturaPlanController(asignaturaPlan, usuario),
                        new CoordinadorPortalController(mock(ConsultarPeriodosAcademicosInputPort.class),
                                mock(ConsultarEstudiantesProgramaInputPort.class), planEstudio, asignatura, usuario),
                        new DocenteGrupoEstudianteController(retirar, usuario),
                        new VerificacionCorreoController(verificarCorreo),
                        new AdminAdmisionEstudianteController(admitir, mock(ConsultarProgramasInstitucionInputPort.class), usuario))
                .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty()))
                .build();
    }

    @Test
    void toma_lote_vacio_es_400_validation_error_sin_llegar_al_caso_de_uso() throws Exception {
        mvc.perform(post("/api/v1/tomas/{tomaId}/asistencias", ID).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"registros\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("registros"))
                .andExpect(jsonPath("$.details[0].code").value("FIELD_INVALID_LENGTH"))
                .andExpect(jsonPath("$.details[0].message").value("El lote debe tener entre 1 y 200 registros."));
        verifyNoInteractions(registrarToma);
    }

    @Test
    void toma_lote_valida_cada_registro_en_cascada() throws Exception {
        mvc.perform(post("/api/v1/tomas/{tomaId}/asistencias", ID).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"registros\":[{\"estudianteId\":\"" + ID + "\",\"estado\":\" \"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("registros[0].estado"))
                .andExpect(jsonPath("$.details[0].code").value("FIELD_REQUIRED"));
        verifyNoInteractions(registrarToma);
    }

    @Test
    void toma_abrir_sin_cuerpo_sigue_delegando_el_modo_al_caso_de_uso() throws Exception {
        // El modo vacio conserva su codigo de negocio TOM_002 (Application); Bean Validation no lo intercepta.
        mvc.perform(post("/api/v1/sesiones/{sesionId}/tomas", ID).contentType(MediaType.APPLICATION_JSON).content("{}"));
        verify(abrirToma).execute(any());
    }

    @Test
    void asignatura_plan_sin_asignatura_ni_semestre_es_400_con_ambos_campos_en_orden() throws Exception {
        mvc.perform(post("/api/v1/coordinador/planes-estudio/{planId}/asignaturas-asociadas", ID)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"codigoCaracter\":\"OBL\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("idAsignatura"))
                .andExpect(jsonPath("$.details[0].code").value("FIELD_REQUIRED"))
                .andExpect(jsonPath("$.details[0].message").value("La asignatura es obligatoria."))
                .andExpect(jsonPath("$.details[1].field").value("semestreNumero"));
        verifyNoInteractions(asignaturaPlan);
    }

    @Test
    void asignatura_plan_valida_mapea_el_request_de_infrastructure_al_dto_de_application() throws Exception {
        mvc.perform(post("/api/v1/coordinador/planes-estudio/{planId}/asignaturas-asociadas", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idAsignatura\":\"" + ID + "\",\"semestreNumero\":2,\"codigoCaracter\":\"OBL\"}"))
                .andExpect(status().isCreated());
        verify(asignaturaPlan).asociar(ACTOR, ID, new AsociarAsignaturaPlanDTO(ID, 2, "OBL"));
    }

    @Test
    void asignatura_plan_semestre_no_positivo_es_fuera_de_rango() throws Exception {
        mvc.perform(post("/api/v1/coordinador/planes-estudio/{planId}/asignaturas-asociadas", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idAsignatura\":\"" + ID + "\",\"semestreNumero\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("semestreNumero"))
                .andExpect(jsonPath("$.details[0].code").value("FIELD_OUT_OF_RANGE"));
        verifyNoInteractions(asignaturaPlan);
    }

    @Test
    void coordinador_plan_estudio_sin_inp_es_400() throws Exception {
        mvc.perform(post("/api/v1/coordinador/planes-estudio").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("inp"))
                .andExpect(jsonPath("$.details[0].code").value("FIELD_REQUIRED"));
        verifyNoInteractions(planEstudio);
    }

    @Test
    void coordinador_asignatura_con_codigo_mayor_a_la_columna_es_400() throws Exception {
        mvc.perform(put("/api/v1/coordinador/planes-estudio/{planId}/asignaturas/{asigId}", ID, ID)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"" + "A".repeat(51) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("codigo"))
                .andExpect(jsonPath("$.details[0].code").value("FIELD_INVALID_LENGTH"));
        verify(asignatura, never()).actualizar(any());
    }

    @Test
    void retiro_con_motivo_de_mas_de_500_caracteres_es_400() throws Exception {
        mvc.perform(post("/api/v1/docente/grupos/{g}/estudiantes/{e}/retiro", ID, ID).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"" + "m".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("motivo"))
                .andExpect(jsonPath("$.details[0].code").value("FIELD_INVALID_LENGTH"));
        verifyNoInteractions(retirar);
    }

    @Test
    void verificacion_de_correo_con_token_desmedido_es_400_sin_consultar_la_bd() throws Exception {
        mvc.perform(post("/api/v1/public/verificar-correo").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + "t".repeat(300) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("token"))
                .andExpect(jsonPath("$.details[0].message").value("El enlace de verificacion no es valido."));
        verifyNoInteractions(verificarCorreo);
    }

    @Test
    void payload_fija_el_codigo_del_validator_propio_y_nunca_expone_mensajes_del_proveedor() throws Exception {
        mvc.perform(post("/api/v1/admin/estudiantes/admision").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipoIdentificacionId\":\"" + ID + "\",\"numeroIdentificacion\":0,\"primerNombre\":\"Ana\","
                                + "\"primerApellido\":\"Perez\",\"correo\":\"ana@uco.edu.co\",\"idPrograma\":\"" + ID + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("numeroIdentificacion"))
                .andExpect(jsonPath("$.details[0].code").value("FIELD_INVALID_FORMAT"))
                .andExpect(jsonPath("$.details[0].message").value("El numero de identificacion es obligatorio y debe ser positivo."))
                .andExpect(jsonPath("$.details[?(@.message =~ /.*must.*/)]").isEmpty());
        verifyNoInteractions(admitir);
    }
}
