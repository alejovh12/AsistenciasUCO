package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.ApplicationException;
import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.abrirtoma.primaryports.AbrirTomaAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.cerrartoma.primaryports.CerrarTomaAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomaAsistenciaResumen;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.registrarasistenciastoma.primaryports.RegistrarAsistenciasTomaInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.TomaQrCargaFixture;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * VAL-oleada8 (SQL Server real, por los puertos de entrada reales: caso de uso + adaptador JPA + SP):
 * <ul>
 *   <li>PRU-11: tope {@code TOMA_ASISTENCIA/MAXIMO_TOMAS_POR_SESION} (20). La toma 20 abre; la 21 responde
 *   {@code TOM_004} (409) y no crea fila.</li>
 *   <li>PRU-12: carreras con dos hilos sobre la misma toma. Dos cierres a la vez: uno cierra y el otro ve la toma ya
 *   cerrada, sin excepcion cruda. Un lote y un cierre a la vez: ninguno falla con una excepcion tecnica y no quedan
 *   detalles duplicados por (asistencia, toma).</li>
 * </ul>
 */
@Tag("integration")
@SpringBootTest
@MockitoBean(types = JwtDecoder.class)
class TomaAsistenciaBordesSqlServerIT {

    private static final int MAXIMO_TOMAS_POR_SESION = 20;

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private AbrirTomaAsistenciaInputPort abrirToma;
    @Autowired private CerrarTomaAsistenciaInputPort cerrarToma;
    @Autowired private RegistrarAsistenciasTomaInputPort registrarLote;

    private TomaQrCargaFixture fixture;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(UUID.randomUUID());
        fixture = new TomaQrCargaFixture(jdbcTemplate).crear(1, 3);
    }

    @AfterEach
    void limpiar() {
        try {
            fixture.limpiar();
        } finally {
            CorrelationIdContext.clear();
        }
    }

    @Test
    void pru_11_la_toma_20_abre_y_la_21_responde_tom_004_sin_crear_fila() {
        assertEquals(MAXIMO_TOMAS_POR_SESION, jdbcTemplate.queryForObject(
                "SELECT dbo.ufn_obtener_parametro_int(NULL, 'TOMA_ASISTENCIA', 'MAXIMO_TOMAS_POR_SESION')", Integer.class),
                "el borde de la prueba es el parametro vigente");
        final TomaQrCargaFixture.GrupoCarga grupo = fixture.grupos().getFirst();
        final UUID docente = fixture.docenteUsuario();

        TomaAsistenciaResumen ultima = null;
        for (int numero = 1; numero <= MAXIMO_TOMAS_POR_SESION; numero++) {
            ultima = abrirToma.execute(new AbrirTomaAsistenciaInputPort.Comando(grupo.sesion(), "MANUAL", docente));
            assertEquals(numero, ultima.numero());
            cerrarToma.execute(new CerrarTomaAsistenciaInputPort.Comando(ultima.idToma(), null, docente));
        }
        assertNotNull(ultima);
        assertEquals(MAXIMO_TOMAS_POR_SESION, tomasDeLaSesion(grupo.sesion()));

        final var comandoAbrir = new AbrirTomaAsistenciaInputPort.Comando(grupo.sesion(), "MANUAL", docente);
        final ConflictException rechazo = assertThrows(ConflictException.class,
                () -> abrirToma.execute(comandoAbrir));

        assertEquals("TOM_004", rechazo.getCode());
        assertEquals(MAXIMO_TOMAS_POR_SESION, tomasDeLaSesion(grupo.sesion()), "la toma 21 no debe quedar registrada");
    }

    @Test
    void pru_12_dos_cierres_simultaneos_de_la_misma_toma_cierran_una_sola_vez() throws Exception {
        final TomaQrCargaFixture.GrupoCarga grupo = fixture.grupos().getFirst();
        final UUID docente = fixture.docenteUsuario();
        final TomaAsistenciaResumen toma = abrirToma.execute(
                new AbrirTomaAsistenciaInputPort.Comando(grupo.sesion(), "MANUAL", docente));
        final CerrarTomaAsistenciaInputPort.Comando cierre = new CerrarTomaAsistenciaInputPort.Comando(toma.idToma(), null, docente);

        final List<CerrarTomaAsistenciaInputPort.Resultado> resultados = aLaVez(
                () -> cerrarToma.execute(cierre), () -> cerrarToma.execute(cierre));

        final long cierresEfectivos = resultados.stream().filter(r -> !r.yaEstabaCerrada()).count();
        assertEquals(1, cierresEfectivos, "solo uno de los dos cierres debe cerrar la toma");
        resultados.forEach(r -> assertFalse(r.toma().estaAbierta()));
        // El cierre idempotente (TOM_011) no escribe y devuelve el total guardado por el cierre efectivo.
        resultados.forEach(r -> assertEquals(grupo.estudiantes().size(), r.totalAusentesMarcados()));
        assertEquals(grupo.estudiantes().size(), detallesDeLaToma(toma.idToma()), "los ausentes se marcan una sola vez");
        assertEquals(0, fixture.duplicados(grupo.sesion()));
    }

    @Test
    void pru_12_un_lote_y_un_cierre_simultaneos_no_fallan_con_excepcion_tecnica_ni_duplican() throws Exception {
        final TomaQrCargaFixture.GrupoCarga grupo = fixture.grupos().getFirst();
        final UUID docente = fixture.docenteUsuario();
        final TomaAsistenciaResumen toma = abrirToma.execute(
                new AbrirTomaAsistenciaInputPort.Comando(grupo.sesion(), "MANUAL", docente));
        final List<RegistrarAsistenciasTomaInputPort.Registro> presentes = grupo.estudiantes().stream()
                .map(e -> new RegistrarAsistenciasTomaInputPort.Registro(e.estudiante(), "AN"))
                .toList();

        final List<Object> resultados = aLaVez(
                () -> registrarLote.execute(new RegistrarAsistenciasTomaInputPort.Comando(toma.idToma(), presentes, docente)),
                () -> cerrarToma.execute(new CerrarTomaAsistenciaInputPort.Comando(toma.idToma(), null, docente)));

        // El cierre siempre termina; el lote termina o se rechaza con un codigo de negocio (nunca una excepcion cruda).
        assertTrue(resultados.get(1) instanceof CerrarTomaAsistenciaInputPort.Resultado, "el cierre no debe fallar");
        final Object lote = resultados.get(0);
        assertTrue(lote instanceof RegistrarAsistenciasTomaInputPort.Resultado || lote instanceof ApplicationException,
                () -> "el lote fallo con una excepcion tecnica: " + lote);
        assertEquals(0, fixture.duplicados(grupo.sesion()), "no puede haber dos detalles por (asistencia, toma)");
        assertEquals(grupo.estudiantes().size(), detallesDeLaToma(toma.idToma()),
                "cada estudiante queda con un solo estado en la toma (presente por el lote o ausente por el cierre)");
    }

    /**
     * Ejecuta las tareas a la vez (barrera) en hilos propios. Devuelve el resultado de cada una, o la
     * {@link ApplicationException} con la que termino; cualquier otra excepcion hace fallar la prueba.
     */
    @SafeVarargs
    private <T> List<T> aLaVez(final Callable<? extends T>... tareas) throws Exception {
        final CyclicBarrier barrera = new CyclicBarrier(tareas.length);
        final ExecutorService hilos = Executors.newFixedThreadPool(tareas.length);
        try {
            final List<Future<Object>> futuros = new ArrayList<>();
            for (final Callable<? extends T> tarea : tareas) {
                futuros.add(hilos.submit(() -> {
                    CorrelationIdContext.set(UUID.randomUUID());
                    try {
                        barrera.await(10, TimeUnit.SECONDS);
                        return tarea.call();
                    } catch (final ApplicationException rechazoDeNegocio) {
                        return rechazoDeNegocio;
                    } finally {
                        CorrelationIdContext.clear();
                    }
                }));
            }
            final List<T> resultados = new ArrayList<>();
            for (final Future<Object> futuro : futuros) {
                @SuppressWarnings("unchecked")
                final T resultado = (T) futuro.get(60, TimeUnit.SECONDS);
                resultados.add(resultado);
            }
            return resultados;
        } finally {
            hilos.shutdownNow();
        }
    }

    private int tomasDeLaSesion(final UUID sesion) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dbo.TomaAsistencia WHERE sesion = ?", Integer.class,
                sesion.toString());
    }

    private int detallesDeLaToma(final UUID toma) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dbo.DetalleAsistencia WHERE toma = ?",
                Integer.class, toma.toString());
    }
}
