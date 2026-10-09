package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.*;


import co.edu.uco.asistenciasuco.application.secondaryports.academic.DecanoCommandPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.AcademicUserParityFixture.PeriodoCierre;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.AcademicUserParityFixture.Semilla;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.CanonicalJdbcBaselineExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureResult;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.JdbcBaselineTestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LB-008 JPA-03 — paridad de EXITO REAL sobre SQL Server de los seis providers disponibles
 * ({@code usp_crear_asignatura}, {@code usp_actualizar_asignatura}, {@code usp_toggle_estado_asignatura},
 * {@code usp_ejecutar_cierre_masivo_periodo}, {@code usp_crear_coordinador}, {@code usp_crear_decano}).
 *
 * <p>Cada escenario ejecuta el BASELINE JDBC ({@link AcademicUserJdbcBaselineOracle}, solo en src/test) y el
 * CANDIDATO JPA (metodo publico del {@code *JpaRepository} sobre el EntityManager) sobre fixtures equivalentes
 * autocontenidos, y compara: contrato observable (estado y correlacion; el JPA publica {@code void}, por lo que
 * un exito es ausencia de excepcion y la correlacion la valida internamente {@code ProcedureResultValidator}) y
 * efectos DB normalizados (filas, estados, contadores, auditoria y delta de conteos por tabla). Los IDs generados
 * no se comparan literalmente.</p>
 *
 * <p>No envuelve los calls en {@code @Transactional}: la frontera transaccional de produccion se ejercita tal cual
 * (TARGET_OUTER_TX = NO). La limpieza ocurre en {@code @AfterEach}, despues de las assertions.</p>
 *
 * <p>Los dos providers ausentes (TD-043) NO forman parte de este IT; su caracterizacion vive en
 * {@link AcademicUserCommandsSpParityIT}.</p>
 */
@Import(JdbcBaselineTestConfiguration.class)
@Tag("integration")
@SpringBootTest
@MockitoBean(types = JwtDecoder.class)
class AcademicUserCommandsSuccessParityIT {

    private static final String ACTOR_CIERRE = "ACTOR-IT-LB008";
    private static final String PREFIJO_LOG = "PARITY_SUCCESS";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CanonicalJdbcBaselineExecutor procedureExecutor;

    @Autowired
    private EntityManager entityManager;

    private AcademicUserParityFixture fixture;
    private AcademicUserJdbcBaselineOracle oracle;
    private long residualesAntes;
    private String programaCoordinadorAntes;
    private String facultadDecanoAntes;

    @BeforeEach
    void prepararFixture() {
        CorrelationIdContext.set(UUID.randomUUID());
        fixture = new AcademicUserParityFixture(jdbcTemplate);
        oracle = new AcademicUserJdbcBaselineOracle(procedureExecutor);
        residualesAntes = fixture.residuos();
        final Semilla s = fixture.semilla();
        programaCoordinadorAntes = String.valueOf(s.coordinadorOriginal());
        facultadDecanoAntes = String.valueOf(s.decanoOriginal());
    }

    @AfterEach
    void limpiarFixture() {
        try {
            fixture.limpiar();
            assertEquals(residualesAntes, fixture.residuos(),
                    "El cleanup debe dejar 0 residuos con prefijo " + AcademicUserParityFixture.IT_PREFIX);
            final Semilla s = fixture.semilla();
            assertEquals(programaCoordinadorAntes, String.valueOf(coordinadorDePrograma(s)),
                    "Programa.coordinador de la semilla debe quedar intacto");
            assertEquals(facultadDecanoAntes, String.valueOf(decanoDeFacultad(s)),
                    "Facultad.decano de la semilla debe quedar intacto");
        } finally {
            CorrelationIdContext.clear();
        }
    }

    // ------------------------------------------------------------------ ASG_SUCCESS_01 — crear

    @Test
    void asg_success_01_crear_jdbc_y_jpa_producen_resultado_y_efectos_equivalentes() {
        final Semilla s = fixture.semilla();
        final String tokenJdbc = fixture.nuevoToken();
        final String tokenJpa = fixture.nuevoToken();
        final UUID idJdbc = UUID.randomUUID();
        final UUID idJpa = UUID.randomUUID();

        final UUID corrJdbc = nuevaCorrelacion();
        final List<Long> antesJdbc = fixture.conteos();
        final ProcedureResult jdbc = oracle.crearAsignatura(idJdbc, tokenJdbc, "Asignatura fixture", 4,
                s.planEstudio(), s.semestreNumero(), "", "", s.ejecutorCoordinador());
        final String efectosJdbc = fixture.asignaturaPorId(idJdbc, tokenJdbc) + "#"
                + fixture.deltaConteos(antesJdbc, fixture.conteos());
        final String resultadoJdbc = canonico(jdbc, corrJdbc);

        nuevaCorrelacion();
        final List<Long> antesJpa = fixture.conteos();
        final String resultadoJpa = exitoJpa(() -> new AsignaturaJpaRepository(entityManager, new JpaProcedureExecutor(entityManager)).crearAsignatura(idJpa,
                tokenJpa, "Asignatura fixture", 4, s.planEstudio(), s.semestreNumero(), "", "", s.ejecutorCoordinador()));
        final String efectosJpa = fixture.asignaturaPorId(idJpa, tokenJpa) + "#"
                + fixture.deltaConteos(antesJpa, fixture.conteos());

        assertTrue(jdbc.getEstadoResultado(), "El camino de exito JDBC debe producir estadoResultado = 1");
        assertEquals(resultadoJdbc, resultadoJpa, "Resultado observable JDBC y JPA deben ser equivalentes");
        assertEquals(efectosJdbc, efectosJpa, "Efectos DB JDBC y JPA deben ser equivalentes");
        assertTrue(efectosJpa.contains("|OK|OK|SEED_SPE|"), "La fila creada debe tener area, componente y SPE de la semilla");
        registrar("ASG_SUCCESS_01", resultadoJdbc, resultadoJpa, true);
    }

    // ------------------------------------------------------------------ ASG_SUCCESS_02 — actualizar

    @Test
    void asg_success_02_actualizar_jdbc_y_jpa_persisten_los_mismos_cambios() {
        final Semilla s = fixture.semilla();
        final String tokenJdbc = fixture.nuevoToken();
        final String tokenJpa = fixture.nuevoToken();
        final UUID idJdbc = fixture.insertarAsignatura(tokenJdbc + "-ORI", true);
        final UUID idJpa = fixture.insertarAsignatura(tokenJpa + "-ORI", true);

        final UUID corrJdbc = nuevaCorrelacion();
        final ProcedureResult jdbc = oracle.actualizarAsignatura(idJdbc, tokenJdbc + "-NEW", "Nombre actualizado",
                5, s.planEstudio(), s.semestreNumero(), "", "");
        final String efectosJdbc = fixture.asignaturaPorId(idJdbc, tokenJdbc);
        final String resultadoJdbc = canonico(jdbc, corrJdbc);

        nuevaCorrelacion();
        final String resultadoJpa = exitoJpa(() -> new AsignaturaJpaRepository(entityManager, new JpaProcedureExecutor(entityManager)).actualizarAsignatura(idJpa,
                tokenJpa + "-NEW", "Nombre actualizado", 5, s.planEstudio(), s.semestreNumero(), "", ""));
        final String efectosJpa = fixture.asignaturaPorId(idJpa, tokenJpa);

        assertTrue(jdbc.getEstadoResultado(), "El camino de exito JDBC debe producir estadoResultado = 1");
        assertEquals(resultadoJdbc, resultadoJpa, "Resultado observable JDBC y JPA deben ser equivalentes");
        assertEquals(efectosJdbc, efectosJpa, "Cambios persistidos JDBC y JPA deben ser equivalentes");
        assertTrue(efectosJpa.startsWith("<TOKEN>-NEW|Nombre actualizado|5|"),
                "Solo codigo, nombre y creditos cambian (el SP no altera area, componente ni SPE)");
        registrar("ASG_SUCCESS_02", resultadoJdbc, resultadoJpa, true);
    }

    // ------------------------------------------------------------------ ASG_SUCCESS_03 — toggle

    @Test
    void asg_success_03_toggle_jdbc_y_jpa_producen_la_misma_transicion() {
        final String tokenJdbc = fixture.nuevoToken();
        final String tokenJpa = fixture.nuevoToken();
        final UUID idJdbc = fixture.insertarAsignatura(tokenJdbc, true);
        final UUID idJpa = fixture.insertarAsignatura(tokenJpa, true);
        final String antesJdbc = fixture.estadoAsignatura(idJdbc);
        final String antesJpa = fixture.estadoAsignatura(idJpa);

        final UUID corrJdbc = nuevaCorrelacion();
        final ProcedureResult jdbc = oracle.toggleEstadoAsignatura(idJdbc);
        final String despuesJdbc = fixture.estadoAsignatura(idJdbc);
        final String resultadoJdbc = canonico(jdbc, corrJdbc);

        nuevaCorrelacion();
        final String resultadoJpa = exitoJpa(() -> new AsignaturaJpaRepository(entityManager, new JpaProcedureExecutor(entityManager)).toggleEstadoAsignatura(idJpa));
        final String despuesJpa = fixture.estadoAsignatura(idJpa);

        assertEquals("true", antesJdbc, "Estado inicial JDBC debe ser activo");
        assertEquals("true", antesJpa, "Estado inicial JPA debe ser activo");
        assertEquals("false", despuesJdbc, "JDBC debe alternar de activo a inactivo");
        assertEquals(despuesJdbc, despuesJpa, "La transicion JPA debe coincidir con JDBC");
        assertEquals(resultadoJdbc, resultadoJpa, "Resultado observable JDBC y JPA deben ser equivalentes");
        registrar("ASG_SUCCESS_03", resultadoJdbc, resultadoJpa, true);
    }

    // ------------------------------------------------------------------ CIE_SUCCESS — cierre masivo

    @Test
    void cie_success_cierre_masivo_jdbc_y_jpa_afectan_solo_su_fixture_y_coinciden() {
        final Semilla s = fixture.semilla();
        final String tokenJdbc = fixture.nuevoToken();
        final String tokenJpa = fixture.nuevoToken();
        final PeriodoCierre periodoJdbc = fixture.insertarPeriodoCierre(tokenJdbc);
        final PeriodoCierre periodoJpa = fixture.insertarPeriodoCierre(tokenJpa);

        // Guarda de seguridad: el codigo debe resolver SOLO al fixture, nunca al periodo compartido de la semilla.
        assertEquals(1L, periodosConNombre(tokenJdbc), "El codigo de cierre JDBC debe identificar un unico periodo");
        assertEquals(1L, periodosConNombre(tokenJpa), "El codigo de cierre JPA debe identificar un unico periodo");
        final String estadosSemillaAntes = fixture.estadosCierre(
                new PeriodoCierre(s.periodoSemilla(), s.grupoSemilla(), null, null, null));

        final UUID corrJdbc = nuevaCorrelacion();
        final List<Long> antesJdbc = fixture.conteos();
        final ProcedureResult jdbc = oracle.ejecutarCierreMasivoPeriodo(tokenJdbc, ACTOR_CIERRE, s.ejecutorAdmin());
        final String efectosJdbc = fixture.estadosCierre(periodoJdbc) + "#"
                + fixture.auditoriaCierre(periodoJdbc, corrJdbc.toString()) + "#"
                + fixture.deltaConteos(antesJdbc, fixture.conteos());
        final String resultadoJdbc = canonico(jdbc, corrJdbc);

        final UUID corrJpa = nuevaCorrelacion();
        final List<Long> antesJpa = fixture.conteos();
        final String resultadoJpa = exitoJpa(() -> new CierrePeriodoJpaRepository(new JpaProcedureExecutor(entityManager))
                .ejecutarCierreMasivoPeriodo(tokenJpa, ACTOR_CIERRE, s.ejecutorAdmin()));
        final String efectosJpa = fixture.estadosCierre(periodoJpa) + "#"
                + fixture.auditoriaCierre(periodoJpa, corrJpa.toString()) + "#"
                + fixture.deltaConteos(antesJpa, fixture.conteos());

        assertTrue(jdbc.getEstadoResultado(), "El camino de exito JDBC debe producir estadoResultado = 1");
        assertTrue(efectosJdbc.contains("=CI") && efectosJdbc.contains("=F"),
                "El fixture debe ejercitar ambas transiciones: inasistencia (CI) y finalizado (F)");
        assertEquals(resultadoJdbc, resultadoJpa, "Resultado observable JDBC y JPA deben ser equivalentes");
        assertEquals(efectosJdbc.replace(periodoJdbc.periodo().toString().toUpperCase(), "<PERIODO>"),
                efectosJpa.replace(periodoJpa.periodo().toString().toUpperCase(), "<PERIODO>"),
                "Estados finales, contadores, auditoria y delta de conteos deben coincidir");
        assertEquals(estadosSemillaAntes, fixture.estadosCierre(
                        new PeriodoCierre(s.periodoSemilla(), s.grupoSemilla(), null, null, null)),
                "El periodo compartido de la semilla no debe cambiar");
        registrar("CIE_SUCCESS", resultadoJdbc, resultadoJpa, true);
    }

    // ------------------------------------------------------------------ COO_SUCCESS — crear coordinador

    @Test
    void coo_success_crear_coordinador_jdbc_y_jpa_persisten_la_misma_estructura() {
        final Semilla s = fixture.semilla();
        final String tokenJdbc = fixture.nuevoToken();
        final String tokenJpa = fixture.nuevoToken();
        final String correoJdbc = fixture.emailDe(tokenJdbc);
        final String correoJpa = fixture.emailDe(tokenJpa);
        fixture.registrarUsuario(correoJdbc);
        fixture.registrarUsuario(correoJpa);

        final UUID corrJdbc = nuevaCorrelacion();
        final List<Long> antesJdbc = fixture.conteos();
        final ProcedureResult jdbc = oracle.crearCoordinador(UUID.randomUUID(),
                String.valueOf(fixture.numeroIdentificacionUnico()), "Nombre", "", "Apellido", "", correoJdbc,
                s.programa(), s.facultad(), AcademicUserParityFixture.PASSWORD_IT, s.ejecutorDecano());
        final String efectosJdbc = fixture.usuarioPorCorreo(correoJdbc) + "#"
                + fixture.vinculoCoordinador(correoJdbc, s.programa()) + "#"
                + fixture.deltaConteos(antesJdbc, fixture.conteos());
        final String resultadoJdbc = canonico(jdbc, corrJdbc);
        fixture.restaurarReferencias();

        nuevaCorrelacion();
        final List<Long> antesJpa = fixture.conteos();
        final String resultadoJpa = exitoJpa(() -> new CoordinadorJpaRepository(entityManager, new JpaProcedureExecutor(entityManager)).crearCoordinador(UUID.randomUUID(),
                String.valueOf(fixture.numeroIdentificacionUnico()), "Nombre", "", "Apellido", "", correoJpa,
                s.programa(), s.facultad(), AcademicUserParityFixture.PASSWORD_IT, s.ejecutorDecano()));
        final String efectosJpa = fixture.usuarioPorCorreo(correoJpa) + "#"
                + fixture.vinculoCoordinador(correoJpa, s.programa()) + "#"
                + fixture.deltaConteos(antesJpa, fixture.conteos());
        fixture.restaurarReferencias();

        assertTrue(jdbc.getEstadoResultado(), "El camino de exito JDBC debe producir estadoResultado = 1");
        assertEquals(resultadoJdbc, resultadoJpa, "Resultado observable JDBC y JPA deben ser equivalentes");
        assertEquals(efectosJdbc.replace(correoJdbc, "<CORREO>"), efectosJpa.replace(correoJpa, "<CORREO>"),
                "Usuario, coordinador, relacion con el programa y delta de conteos deben coincidir");
        assertTrue(efectosJpa.contains("COORD_OK|PROGRAMA_APUNTA_NUEVO"),
                "El coordinador debe quedar vinculado y el programa debe apuntar a el");
        registrar("COO_SUCCESS", resultadoJdbc, resultadoJpa, true);
    }

    // ------------------------------------------------------------------ DEC_SUCCESS — crear decano

    @Test
    void dec_success_crear_decano_jdbc_y_jpa_persisten_la_misma_estructura() {
        final Semilla s = fixture.semilla();
        final String tokenJdbc = fixture.nuevoToken();
        final String tokenJpa = fixture.nuevoToken();
        final String correoJdbc = fixture.emailDe(tokenJdbc);
        final String correoJpa = fixture.emailDe(tokenJpa);
        fixture.registrarUsuario(correoJdbc);
        fixture.registrarUsuario(correoJpa);

        final UUID corrJdbc = nuevaCorrelacion();
        final List<Long> antesJdbc = fixture.conteos();
        final ProcedureResult jdbc = oracle.crearDecano(UUID.randomUUID(), fixture.numeroIdentificacionUnico(),
                "Nombre", "", "Apellido", "", correoJdbc, s.facultad(), "", AcademicUserParityFixture.PASSWORD_IT,
                s.ejecutorAdmin());
        final String efectosJdbc = fixture.usuarioPorCorreo(correoJdbc) + "#"
                + fixture.vinculoDecano(correoJdbc, s.facultad()) + "#"
                + fixture.deltaConteos(antesJdbc, fixture.conteos());
        final String resultadoJdbc = canonico(jdbc, corrJdbc);
        fixture.restaurarReferencias();

        nuevaCorrelacion();
        final List<Long> antesJpa = fixture.conteos();
        final String resultadoJpa = exitoJpa(() -> new DecanoJpaRepository(entityManager, new JpaProcedureExecutor(entityManager)).crearDecano(
                new DecanoCommandPort.CrearDecanoCommand(UUID.randomUUID(), null, fixture.numeroIdentificacionUnico(),
                        "Nombre", "", "Apellido", "", correoJpa, s.facultad(), "",
                        AcademicUserParityFixture.PASSWORD_IT, s.ejecutorAdmin())));
        final String efectosJpa = fixture.usuarioPorCorreo(correoJpa) + "#"
                + fixture.vinculoDecano(correoJpa, s.facultad()) + "#"
                + fixture.deltaConteos(antesJpa, fixture.conteos());
        fixture.restaurarReferencias();

        assertTrue(jdbc.getEstadoResultado(), "El camino de exito JDBC debe producir estadoResultado = 1");
        assertEquals(resultadoJdbc, resultadoJpa, "Resultado observable JDBC y JPA deben ser equivalentes");
        assertEquals(efectosJdbc.replace(correoJdbc, "<CORREO>"), efectosJpa.replace(correoJpa, "<CORREO>"),
                "Usuario, decano, relacion con la facultad y delta de conteos deben coincidir");
        assertTrue(efectosJpa.contains("DECANO_OK|FACULTAD_APUNTA_NUEVO"),
                "El decano debe quedar vinculado y la facultad debe apuntar a el");
        registrar("DEC_SUCCESS", resultadoJdbc, resultadoJpa, true);
    }

    // ------------------------------------------------------------------ utilidades

    private UUID nuevaCorrelacion() {
        final UUID correlacion = UUID.randomUUID();
        CorrelationIdContext.set(correlacion);
        return correlacion;
    }

    /** Contrato observable del baseline JDBC: estado y correlacion verificada contra el contexto. */
    private static String canonico(final ProcedureResult resultado, final UUID correlacionEsperada) {
        return "estado=" + resultado.getEstadoResultado()
                + ";correlacion=" + (correlacionEsperada.equals(resultado.getIdCorrelacion()) ? "OK" : "MISMATCH");
    }

    /**
     * Contrato observable del candidato JPA (metodo publico {@code void}): exito es ausencia de excepcion. Un fallo
     * se registra con su clasificacion para que la comparacion lo muestre, en lugar de ocultarse.
     */
    private static String exitoJpa(final Runnable operacion) {
        try {
            operacion.run();
            return "estado=true;correlacion=OK";
        } catch (final RuntimeException excepcion) {
            return "estado=false;excepcion=" + excepcion.getClass().getSimpleName();
        }
    }

    private void registrar(final String escenario, final String jdbc, final String jpa, final boolean iguales) {
        System.out.println(PREFIJO_LOG + "|" + escenario + "|JDBC_SUCCESS_OUTCOME=" + jdbc + "|JPA_SUCCESS_OUTCOME="
                + jpa + "|DB_EFFECTS_EQUAL=" + iguales);
    }

    private long periodosConNombre(final String nombre) {
        return jdbcTemplate.queryForObject("SELECT COUNT_BIG(*) FROM dbo.PeriodoAcademico WHERE nombre = ?", Long.class, nombre);
    }

    private UUID coordinadorDePrograma(final Semilla s) {
        return UUID.fromString(jdbcTemplate.queryForObject(
                "SELECT CAST(coordinador AS NVARCHAR(36)) FROM dbo.Programa WHERE id = ?", String.class,
                s.programa().toString()));
    }

    private UUID decanoDeFacultad(final Semilla s) {
        return UUID.fromString(jdbcTemplate.queryForObject(
                "SELECT CAST(decano AS NVARCHAR(36)) FROM dbo.Facultad WHERE id = ?", String.class,
                s.facultad().toString()));
    }
}
