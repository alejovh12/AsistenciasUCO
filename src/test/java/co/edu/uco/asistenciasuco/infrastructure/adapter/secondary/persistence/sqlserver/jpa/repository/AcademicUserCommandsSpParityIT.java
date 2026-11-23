package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.ApplicationException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.usuario.exception.UsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsignaturaCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CierrePeriodoCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DecanoCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.PlanEstudioCommandPort;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.CommonErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.SecurityErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * LB-008 JPA-03 — rechazos funcionales REALES sobre SQL Server de los commands academicos y de usuario, ejercitados
 * SOLO a traves de los puertos de Application (mismo mecanismo que {@code SesionGrupoCommandsSpParityIT}).
 *
 * <p>Cada escenario afirma tres cosas: la clase de excepcion de Application que recibe el caso de uso, su codigo de
 * error (el que llega al cliente) y que el SP no escribio nada (firma de conteos identica antes y despues). Ningun
 * escenario puede aprobar solo por imprimir: si el SP acepta la entrada, cambia la clasificacion o deja filas, falla.
 * El DBCODE exacto que emite cada SP no viaja en la excepcion traducida; lo certifica la suite de BD.</p>
 *
 * <p>Tres familias, en el orden de validacion de {@code usp_validar_permiso_rbac_usuario_interno}:
 * <ul>
 *   <li>{@code EJE_*}: ejecutor inexistente. El SP rechaza con USU_001 (usuario inexistente) antes de mirar el perfil
 *       o el recurso: {@link ResourceNotFoundException} con {@code ERR_USUARIO_NO_EXISTE}.</li>
 *   <li>{@code PER_*}: ejecutor real de la semilla con un perfil distinto del exigido. El SP rechaza con SEC_001:
 *       {@link ForbiddenException} con {@code FORBIDDEN}.</li>
 *   <li>{@code REC_*}: ejecutor real con el perfil exigido y un recurso inexistente. El SP llega a su validacion de
 *       existencia: PLA_002 y PROG_001 (no encontrado), VAL_002 (asignatura inexistente, validacion).</li>
 * </ul>
 * El cierre masivo y el alta de decano no tienen escenario {@code REC_*}: con un ejecutor valido,
 * {@code usp_ejecutar_cierre_masivo_periodo} cierra el periodo mas reciente cuando el codigo pedido no existe, y
 * {@code usp_crear_decano} busca la facultad por nombre con {@code LIKE} y admite decano sin facultad; ejecutar esos
 * caminos escribiria datos reales. Ese comportamiento queda como hallazgo pendiente de decision de producto.</p>
 *
 * <p>Historia: hasta 2026-11-15 esta clase solo imprimia {@code PARITY_OUTCOME} para comparar a mano el log JDBC
 * (BEFORE) con el JPA (AFTER) y no tenia ninguna asercion. Con ejecutores aleatorios todos sus escenarios terminaban en
 * USU_001 sin alcanzar la validacion que su nombre anunciaba, y el alta de coordinador fallaba por un numero de
 * identificacion no numerico sin que nadie lo notara. El camino de exito se certifica en
 * {@code AcademicUserCommandsSuccessParityIT}.</p>
 */
@Tag("integration")
@SpringBootTest
@MockitoBean(types = JwtDecoder.class)
class AcademicUserCommandsSpParityIT {

    private static final String CLAVE = "Clave-IT-LB008";

    @Autowired
    private AsignaturaCommandPort asignaturaCommandPort;

    @Autowired
    private CierrePeriodoCommandPort cierrePeriodoCommandPort;

    @Autowired
    private CoordinadorCommandPort coordinadorCommandPort;

    @Autowired
    private DecanoCommandPort decanoCommandPort;

    @Autowired
    private PlanEstudioCommandPort planEstudioCommandPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private AcademicUserParityFixture fixture;
    private long residualesAntes;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(UUID.randomUUID());
        fixture = new AcademicUserParityFixture(jdbcTemplate);
        residualesAntes = fixture.residuos();
    }

    @AfterEach
    void limpiar() {
        try {
            fixture.limpiar();
            assertEquals(residualesAntes, fixture.residuos(), "El cleanup debe dejar 0 residuos con el prefijo de la IT.");
        } finally {
            CorrelationIdContext.clear();
        }
    }

    // ------------------------------------------------------------------ EJE: ejecutor inexistente -> USU_001

    @Test
    void EJE_01_crear_asignatura_con_ejecutor_inexistente_es_USU_001_sin_efectos() {
        afirmarUsuarioInexistente("EJE_01", () -> crearAsignatura(UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    void EJE_02_actualizar_asignatura_con_ejecutor_inexistente_es_USU_001_sin_efectos() {
        afirmarUsuarioInexistente("EJE_02", () -> actualizarAsignatura(UUID.randomUUID()));
    }

    @Test
    void EJE_03_cambiar_estado_asignatura_con_ejecutor_inexistente_es_USU_001_sin_efectos() {
        afirmarUsuarioInexistente("EJE_03", () -> asignaturaCommandPort.toggleEstadoAsignatura(UUID.randomUUID(), null, UUID.randomUUID()));
    }

    @Test
    void EJE_04_cierre_masivo_con_ejecutor_inexistente_es_USU_001_sin_cerrar_ningun_periodo() {
        afirmarUsuarioInexistente("EJE_04", () -> cierreMasivo(UUID.randomUUID()));
    }

    @Test
    void EJE_05_crear_coordinador_con_ejecutor_inexistente_es_USU_001_sin_efectos() {
        afirmarUsuarioInexistente("EJE_05", () -> crearCoordinador(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    void EJE_06_crear_decano_con_ejecutor_inexistente_es_USU_001_sin_efectos() {
        afirmarUsuarioInexistente("EJE_06", () -> crearDecano(UUID.randomUUID()));
    }

    @Test
    void EJE_07_registrar_plan_con_ejecutor_inexistente_es_USU_001_sin_efectos() {
        afirmarUsuarioInexistente("EJE_07", () -> registrarPlan(UUID.randomUUID(), UUID.randomUUID()));
    }

    // ------------------------------------------------------------------ PER: perfil distinto del exigido -> SEC_001

    @Test
    void PER_01_crear_asignatura_exige_coordinador_y_un_administrador_recibe_SEC_001_sin_efectos() {
        final UUID admin = fixture.semilla().ejecutorAdmin();
        afirmarPerfilDenegado("PER_01", () -> crearAsignatura(fixture.semilla().planEstudio(), admin));
    }

    @Test
    void PER_02_actualizar_asignatura_exige_coordinador_y_un_administrador_recibe_SEC_001_sin_efectos() {
        final UUID admin = fixture.semilla().ejecutorAdmin();
        afirmarPerfilDenegado("PER_02", () -> actualizarAsignatura(admin));
    }

    @Test
    void PER_03_cambiar_estado_asignatura_exige_coordinador_y_un_administrador_recibe_SEC_001_sin_efectos() {
        final UUID admin = fixture.semilla().ejecutorAdmin();
        afirmarPerfilDenegado("PER_03", () -> asignaturaCommandPort.toggleEstadoAsignatura(UUID.randomUUID(), null, admin));
    }

    @Test
    void PER_04_cierre_masivo_exige_administrador_y_un_coordinador_recibe_SEC_001_sin_cerrar_ningun_periodo() {
        final UUID coordinador = fixture.semilla().ejecutorCoordinador();
        afirmarPerfilDenegado("PER_04", () -> cierreMasivo(coordinador));
    }

    @Test
    void PER_05_crear_coordinador_exige_decano_y_un_administrador_recibe_SEC_001_sin_efectos() {
        final AcademicUserParityFixture.Semilla s = fixture.semilla();
        afirmarPerfilDenegado("PER_05", () -> crearCoordinador(s.programa(), s.facultad(), s.ejecutorAdmin()));
    }

    @Test
    void PER_06_crear_decano_exige_administrador_y_un_coordinador_recibe_SEC_001_sin_efectos() {
        final UUID coordinador = fixture.semilla().ejecutorCoordinador();
        afirmarPerfilDenegado("PER_06", () -> crearDecano(coordinador));
    }

    @Test
    void PER_07_registrar_plan_exige_coordinador_y_un_administrador_recibe_SEC_001_sin_efectos() {
        final AcademicUserParityFixture.Semilla s = fixture.semilla();
        afirmarPerfilDenegado("PER_07", () -> registrarPlan(s.programa(), s.ejecutorAdmin()));
    }

    // ------------------------------------------------------------------ REC: perfil correcto, recurso inexistente

    @Test
    void REC_01_crear_asignatura_en_plan_inexistente_es_PLA_002_no_encontrado_sin_efectos() {
        final UUID coordinador = fixture.semilla().ejecutorCoordinador();
        afirmarRechazo("REC_01", ResourceNotFoundException.class, CommonErrorCode.RESOURCE_NOT_FOUND,
                () -> crearAsignatura(UUID.randomUUID(), coordinador));
    }

    @Test
    void REC_02_actualizar_asignatura_inexistente_es_VAL_002_validacion_sin_efectos() {
        final UUID coordinador = fixture.semilla().ejecutorCoordinador();
        afirmarRechazo("REC_02", ValidationException.class, CommonErrorCode.VALIDATION_ERROR,
                () -> actualizarAsignatura(coordinador));
    }

    @Test
    void REC_03_cambiar_estado_de_asignatura_inexistente_es_VAL_002_validacion_sin_efectos() {
        final UUID coordinador = fixture.semilla().ejecutorCoordinador();
        afirmarRechazo("REC_03", ValidationException.class, CommonErrorCode.VALIDATION_ERROR,
                () -> asignaturaCommandPort.toggleEstadoAsignatura(UUID.randomUUID(), null, coordinador));
    }

    @Test
    void REC_04_crear_coordinador_de_programa_inexistente_es_PROG_001_no_encontrado_sin_efectos() {
        final AcademicUserParityFixture.Semilla s = fixture.semilla();
        afirmarRechazo("REC_04", ResourceNotFoundException.class, CommonErrorCode.RESOURCE_NOT_FOUND,
                () -> crearCoordinador(UUID.randomUUID(), s.facultad(), s.ejecutorDecano()));
    }

    @Test
    void REC_05_registrar_plan_de_programa_inexistente_es_PROG_001_no_encontrado_sin_efectos() {
        final UUID coordinador = fixture.semilla().ejecutorCoordinador();
        afirmarRechazo("REC_05", ResourceNotFoundException.class, CommonErrorCode.RESOURCE_NOT_FOUND,
                () -> registrarPlan(UUID.randomUUID(), coordinador));
    }

    // ------------------------------------------------------------------ invocaciones de los puertos

    private void crearAsignatura(final UUID idPlanEstudio, final UUID ejecutor) {
        asignaturaCommandPort.crearAsignatura(UUID.randomUUID(), fixture.nuevoToken(), "Asignatura prueba",
                3, idPlanEstudio, 1, "AREA-NO-EXISTE", "COMPONENTE-NO-EXISTE", null, null, ejecutor);
    }

    private void actualizarAsignatura(final UUID ejecutor) {
        final AcademicUserParityFixture.Semilla s = fixture.semilla();
        asignaturaCommandPort.actualizarAsignatura(UUID.randomUUID(), fixture.nuevoToken(), "Asignatura prueba",
                3, s.planEstudio(), s.semestreNumero(), "AREA-NO-EXISTE", "COMPONENTE-NO-EXISTE", null, null, ejecutor);
    }

    private void cierreMasivo(final UUID ejecutor) {
        cierrePeriodoCommandPort.ejecutarCierreMasivoPeriodo(fixture.nuevoToken(), "ACTOR-IT", ejecutor);
    }

    private void crearCoordinador(final UUID idPrograma, final UUID idFacultad, final UUID ejecutor) {
        coordinadorCommandPort.crearCoordinador(UUID.randomUUID(), String.valueOf(fixture.numeroIdentificacionUnico()),
                "Nombre", null, "Apellido", null, fixture.emailDe(fixture.nuevoToken()), idPrograma, idFacultad, ejecutor);
    }

    private void crearDecano(final UUID ejecutor) {
        decanoCommandPort.crearDecano(new DecanoCommandPort.CrearDecanoCommand(
                UUID.randomUUID(), null, fixture.numeroIdentificacionUnico(), "Nombre", null, "Apellido", null,
                fixture.emailDe(fixture.nuevoToken()), UUID.randomUUID(), "FACULTAD-NO-EXISTE", ejecutor));
    }

    private void registrarPlan(final UUID idPrograma, final UUID ejecutor) {
        planEstudioCommandPort.registrarOActualizarPlanEstudio(UUID.randomUUID(), idPrograma, 2026, ejecutor);
    }

    // ------------------------------------------------------------------ infraestructura del IT

    private void afirmarUsuarioInexistente(final String escenario, final Runnable accion) {
        afirmarRechazo(escenario, ResourceNotFoundException.class, UsuarioErrorCode.ERR_USUARIO_NO_EXISTE, accion);
    }

    private void afirmarPerfilDenegado(final String escenario, final Runnable accion) {
        afirmarRechazo(escenario, ForbiddenException.class, SecurityErrorCode.FORBIDDEN, accion);
    }

    /**
     * Ejecuta la accion y exige que falle con la excepcion y el codigo esperados, sin cambiar la firma de conteos de las
     * tablas que estos providers escriben (y sin registrar un cierre masivo).
     */
    private void afirmarRechazo(final String escenario, final Class<? extends ApplicationException> excepcionEsperada,
                                final ErrorDefinition codigoEsperado, final Runnable accion) {
        final String firmaAntes = firmaDeEscrituras();
        RuntimeException obtenida = null;
        try {
            accion.run();
        } catch (final RuntimeException exception) {
            obtenida = exception;
        }
        final String firmaDespues = firmaDeEscrituras();

        assertNotNull(obtenida, escenario + ": el SP acepto una entrada que debia rechazar.");
        final ApplicationException aplicacion = assertInstanceOf(excepcionEsperada, obtenida,
                escenario + ": excepcion inesperada " + describir(obtenida));
        assertEquals(codigoEsperado.code(), aplicacion.getCode(), escenario + ": codigo inesperado " + describir(obtenida));
        assertEquals(firmaAntes, firmaDespues, escenario + ": el rechazo dejo escrituras (firma de conteos distinta).");
    }

    /** Conteos de las tablas que escriben estos providers mas los cierres masivos auditados. */
    private String firmaDeEscrituras() {
        return jdbcTemplate.queryForObject("""
                SELECT CONCAT(
                    (SELECT COUNT_BIG(*) FROM dbo.Asignatura), '|',
                    (SELECT COUNT_BIG(*) FROM dbo.AsignaturaPlanEstudio), '|',
                    (SELECT COUNT_BIG(*) FROM dbo.Usuario), '|',
                    (SELECT COUNT_BIG(*) FROM dbo.Coordinador), '|',
                    (SELECT COUNT_BIG(*) FROM dbo.Decano), '|',
                    (SELECT COUNT_BIG(*) FROM dbo.PlanEstudio), '|',
                    (SELECT COUNT_BIG(*) FROM dbo.AuditoriaEvento WHERE action = 'CIERRE_MASIVO_PERIODO'))
                """, String.class);
    }

    private static String describir(final Throwable exception) {
        final StringBuilder texto = new StringBuilder();
        for (Throwable actual = exception; actual != null && actual.getCause() != actual; actual = actual.getCause()) {
            texto.append(" <- ").append(actual.getClass().getSimpleName());
            if (actual instanceof ApplicationException application) {
                texto.append('[').append(application.getCode()).append(']');
            }
            texto.append(": ").append(actual.getMessage());
        }
        return texto.toString();
    }
}
