package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox;

import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.LogNotificacionAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.composite.CompositeNotificacionAdapter;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.OutboxNotificationProperties;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.awaitility.Awaitility.await;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de integracion reales contra SQL Server para la capacidad Outbox (NOTIF-001).
 * Verifica los escenarios IT-01 hasta IT-09 definidos en TEST_PLAN §4.
 */
@Tag("integration")
@SpringBootTest(properties = {
        "app.adapters.notification.provider=outbox",
        "app.providers.outbox-notification.sweeper-interval=1s",
        "app.providers.outbox-notification.backoff-base=1s",
        "app.providers.outbox-notification.lease=5s",
        "app.providers.outbox-notification.max-attempts=3",
        "app.providers.outbox-notification.retention-days=30"
})
@MockitoBean(types = JwtDecoder.class)
@Import(NotificacionOutboxIT.TestSenderConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NotificacionOutboxIT {

    @Autowired
    private OutboxNotificacionAdapter outboxNotificacionAdapter;

    @Autowired
    private NotificacionSalienteJpaRepository jpaRepository;

    @Autowired
    private NotificacionOutboxDispatcher dispatcher;

    @Autowired
    private OutboxNotificationProperties properties;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestChannelSender testSender;

    private final Set<UUID> trackedIds = ConcurrentHashMap.newKeySet();

    @TestConfiguration
    static class TestSenderConfig {
        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        @Primary
        public TestChannelSender testChannelSender() {
            return new TestChannelSender();
        }
    }

    @BeforeEach
    void setUp() {
        testSender.reset();
        CorrelationIdContext.set(UUID.randomUUID().toString());
    }

    @AfterEach
    void tearDown() {
        CorrelationIdContext.clear();
        limpiarFilasPropias();
    }

    @AfterAll
    void tearDownAll() {
        limpiarFilasPropias();
    }

    private void limpiarFilasPropias() {
        if (!trackedIds.isEmpty()) {
            for (UUID id : trackedIds) {
                jdbcTemplate.update("DELETE FROM dbo.NotificacionSaliente WHERE id = ?", id);
            }
            trackedIds.clear();
        }
        jdbcTemplate.update("DELETE FROM dbo.NotificacionSaliente WHERE tipo LIKE 'TEST_IT%'");
    }

    private Map<String, Object> esperarEstado(UUID id, String estadoEsperado, int maxSegundos) {
        final AtomicReference<Map<String, Object>> rowRef = new AtomicReference<>();
        await().atMost(Duration.ofSeconds(maxSegundos))
                .pollInterval(Duration.ofMillis(250))
                .until(() -> {
                    Map<String, Object> row = jdbcTemplate.queryForMap(
                            "SELECT n.intentos, n.entregadoEn, n.reservadoPor, e.codigo as estado " +
                                    "FROM dbo.NotificacionSaliente n " +
                                    "JOIN dbo.EstadoNotificacion e ON n.idEstado = e.id " +
                                    "WHERE n.id = ?", id);
                    rowRef.set(row);
                    return estadoEsperado.equals(row.get("estado"));
                });
        return rowRef.get();
    }

    @Test
    @DisplayName("IT-01: registrar -> despachador entrega -> fila ENTREGADA")
    void IT01_registrar_despachador_entrega_fila_entregada() throws Exception {
        UUID destId = UUID.randomUUID();
        Notificacion notificacion = new Notificacion(
                "TEST_IT01",
                "Asunto IT01",
                "Mensaje IT01",
                List.of(new Notificacion.Destinatario("ESTUDIANTE", destId, null, null)),
                Map.of("grupoId", UUID.randomUUID().toString())
        );

        testSender.reset();
        testSender.setExpectedDeliveries(1);

        outboxNotificacionAdapter.notificar(notificacion);

        boolean delivered = testSender.awaitDelivery(10, TimeUnit.SECONDS);
        assertTrue(delivered, "El despachador debio entregar la notificacion");

        List<UUID> deliveredIds = testSender.getDeliveredIds();
        assertEquals(1, deliveredIds.size());
        UUID id = deliveredIds.get(0);
        trackedIds.add(id);

        Map<String, Object> row = esperarEstado(id, "ENTREGADA", 10);
        assertNotNull(row);
        assertEquals("ENTREGADA", row.get("estado"));
        assertEquals(1, ((Number) row.get("intentos")).intValue());
        assertNotNull(row.get("entregadoEn"));
    }

    @Test
    @DisplayName("IT-02: sender que falla 2 veces y luego tiene éxito -> intentos=3, ENTREGADA")
    void IT02_sender_falla_2_veces_luego_exito() throws Exception {
        UUID destId = UUID.randomUUID();
        Notificacion notificacion = new Notificacion(
                "TEST_IT02",
                "Asunto IT02",
                "Mensaje IT02",
                List.of(new Notificacion.Destinatario("ESTUDIANTE", destId, null, null)),
                Map.of("grupoId", UUID.randomUUID().toString())
        );

        AtomicInteger attempts = new AtomicInteger(0);
        testSender.reset();
        testSender.setSenderAction(n -> {
            if (attempts.incrementAndGet() <= 2) {
                throw new RuntimeException("Fallo transitorio simulado intento " + attempts.get());
            }
        });
        testSender.setExpectedDeliveries(1);

        outboxNotificacionAdapter.notificar(notificacion);

        boolean delivered = testSender.awaitDelivery(15, TimeUnit.SECONDS);
        assertTrue(delivered, "Debio entregarse tras 3 intentos");

        List<UUID> deliveredIds = testSender.getDeliveredIds();
        assertEquals(1, deliveredIds.size());
        UUID id = deliveredIds.get(0);
        trackedIds.add(id);

        Map<String, Object> row = esperarEstado(id, "ENTREGADA", 10);
        assertNotNull(row);
        assertEquals("ENTREGADA", row.get("estado"));
        assertEquals(3, ((Number) row.get("intentos")).intValue());
        assertNotNull(row.get("entregadoEn"));
    }

    @Test
    @DisplayName("IT-03: sender que siempre falla -> FALLIDA_DEFINITIVA tras max-attempts, métrica dead = 1")
    void IT03_sender_siempre_falla_marca_fallida_definitiva() throws Exception {
        UUID destId = UUID.randomUUID();
        String tipo = "TEST_IT03_" + UUID.randomUUID().toString().substring(0, 8);
        Notificacion notificacion = new Notificacion(
                tipo,
                "Asunto IT03",
                "Mensaje IT03",
                List.of(new Notificacion.Destinatario("ESTUDIANTE", destId, null, null)),
                Map.of("grupoId", UUID.randomUUID().toString())
        );

        double initialDead = meterRegistry.find("notification.dispatch.total")
                .tag("outcome", "dead")
                .counter() != null
                ? meterRegistry.find("notification.dispatch.total").tag("outcome", "dead").counter().count()
                : 0.0;

        testSender.reset();
        testSender.setSenderAction(n -> {
            throw new RuntimeException("Fallo transitorio permanente");
        });

        outboxNotificacionAdapter.notificar(notificacion);

        final AtomicReference<UUID> idRef = new AtomicReference<>();
        await().atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(500))
                .until(() -> {
                    List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                            "SELECT n.id, n.intentos, e.codigo as estado " +
                                    "FROM dbo.NotificacionSaliente n " +
                                    "JOIN dbo.EstadoNotificacion e ON n.idEstado = e.id " +
                                    "WHERE n.tipo = ?", tipo);
                    if (!rows.isEmpty()) {
                        Object idObj = rows.get(0).get("id");
                        UUID currentId = idObj instanceof UUID u ? u : UUID.fromString(idObj.toString());
                        idRef.set(currentId);
                        trackedIds.add(currentId);
                        return "FALLIDA_DEFINITIVA".equals(rows.get(0).get("estado"));
                    }
                    return false;
                });
        UUID id = idRef.get();

        assertNotNull(id, "La fila debio persistirse en dbo.NotificacionSaliente");
        Map<String, Object> finalRow = jdbcTemplate.queryForMap(
                "SELECT n.intentos, e.codigo as estado " +
                        "FROM dbo.NotificacionSaliente n " +
                        "JOIN dbo.EstadoNotificacion e ON n.idEstado = e.id " +
                        "WHERE n.id = ?", id);
        assertEquals("FALLIDA_DEFINITIVA", finalRow.get("estado"));
        assertEquals(3, ((Number) finalRow.get("intentos")).intValue());

        double finalDead = meterRegistry.find("notification.dispatch.total")
                .tag("outcome", "dead")
                .counter().count();
        assertTrue(finalDead >= initialDead + 1.0, "La metrica dead debio incrementarse");
    }

    @Test
    @DisplayName("IT-04: NotificacionNoEntregableException -> definitiva en el primer intento")
    void IT04_notificacion_no_entregable_falla_definitiva_primer_intento() throws Exception {
        UUID destId = UUID.randomUUID();
        String tipo = "TEST_IT04_" + UUID.randomUUID().toString().substring(0, 8);
        Notificacion notificacion = new Notificacion(
                tipo,
                "Asunto IT04",
                "Mensaje IT04",
                List.of(new Notificacion.Destinatario("ESTUDIANTE", destId, null, null)),
                Map.of("grupoId", UUID.randomUUID().toString())
        );

        testSender.reset();
        testSender.setSenderAction(n -> {
            throw new NotificacionNoEntregableException("Buzon inexistente");
        });

        outboxNotificacionAdapter.notificar(notificacion);

        final AtomicReference<UUID> idRef = new AtomicReference<>();
        await().atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .until(() -> {
                    List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                            "SELECT n.id, n.intentos, n.codigoError, n.detalleError, e.codigo as estado " +
                                    "FROM dbo.NotificacionSaliente n " +
                                    "JOIN dbo.EstadoNotificacion e ON n.idEstado = e.id " +
                                    "WHERE n.tipo = ?", tipo);
                    if (!rows.isEmpty()) {
                        Object idObj = rows.get(0).get("id");
                        UUID currentId = idObj instanceof UUID u ? u : UUID.fromString(idObj.toString());
                        idRef.set(currentId);
                        trackedIds.add(currentId);
                        return "FALLIDA_DEFINITIVA".equals(rows.get(0).get("estado"));
                    }
                    return false;
                });
        UUID id = idRef.get();

        assertNotNull(id, "La fila debio persistirse");
        Map<String, Object> finalRow = jdbcTemplate.queryForMap(
                "SELECT n.intentos, n.codigoError, n.detalleError, e.codigo as estado " +
                        "FROM dbo.NotificacionSaliente n " +
                        "JOIN dbo.EstadoNotificacion e ON n.idEstado = e.id " +
                        "WHERE n.id = ?", id);
        assertEquals("FALLIDA_DEFINITIVA", finalRow.get("estado"));
        assertEquals(1, ((Number) finalRow.get("intentos")).intValue());
        assertEquals("FALLO_DEFINITIVO", finalRow.get("codigoError"));
        assertEquals("Buzon inexistente", finalRow.get("detalleError"));
    }

    @Test
    @DisplayName("IT-05: dos despachadores sobre 100 filas -> cada id se entrega exactamente una vez")
    void IT05_dos_despachadores_concurrencia_100_filas_sin_duplicados() throws Exception {
        OutboxNotificationProperties props2 = new OutboxNotificationProperties(
                OutboxNotificationProperties.Sender.LOG,
                true,
                50,
                Duration.ofSeconds(5),
                Duration.ofSeconds(1),
                3,
                Duration.ofSeconds(1),
                Duration.ofSeconds(10),
                30,
                Duration.ofHours(6),
                "dispatcher-instancia-2"
        );

        NotificacionOutboxDispatcher secondDispatcher = new NotificacionOutboxDispatcher(
                jpaRepository,
                testSender,
                props2,
                meterRegistry,
                Clock.systemUTC()
        );

        int totalCount = 100;
        testSender.reset();
        testSender.setExpectedDeliveries(totalCount);

        secondDispatcher.start();
        try {
            for (int i = 0; i < totalCount; i++) {
                Notificacion n = new Notificacion(
                        "TEST_IT05",
                        "Asunto " + i,
                        "Mensaje " + i,
                        List.of(new Notificacion.Destinatario("DOCENTE", UUID.randomUUID(), null, null)),
                        Map.of("index", String.valueOf(i))
                );
                outboxNotificacionAdapter.notificar(n);
            }

            dispatcher.signal();
            secondDispatcher.signal();

            boolean delivered = testSender.awaitDelivery(60, TimeUnit.SECONDS);
            assertTrue(delivered, "Debieron entregarse las 100 notificaciones");

            List<UUID> deliveredIds = testSender.getDeliveredIds();
            trackedIds.addAll(deliveredIds);
            assertEquals(totalCount, deliveredIds.size());
            Set<UUID> uniqueIds = new HashSet<>(deliveredIds);
            assertEquals(totalCount, uniqueIds.size(), "No debe haber identificadores duplicados");
        } finally {
            secondDispatcher.stop();
        }
    }

    @Test
    @DisplayName("IT-06: reserva vencida -> otra instancia la recupera")
    void IT06_reserva_vencida_recuperada_por_otra_instancia() throws Exception {
        UUID id = UUID.randomUUID();
        UUID destId = UUID.randomUUID();
        UUID corrId = UUID.randomUUID();

        jdbcTemplate.update(
                "INSERT INTO dbo.NotificacionSaliente (" +
                        "  id, tipo, asunto, mensaje, destinatarios, datos, idEstado, " +
                        "  intentos, creadoEn, proximoIntentoEn, reservadoHasta, reservadoPor, idCorrelacion" +
                        ") VALUES (" +
                        "  ?, 'TEST_IT06', 'Asunto IT06', 'Mensaje IT06', " +
                        "  ?, '{}', (SELECT id FROM dbo.EstadoNotificacion WHERE codigo = 'RESERVADA'), " +
                        "  1, DATEADD(MINUTE, -5, SYSUTCDATETIME()), DATEADD(MINUTE, -5, SYSUTCDATETIME()), " +
                        "  DATEADD(SECOND, -10, SYSUTCDATETIME()), 'instancia-fantasma-previa', ?" +
                        ")",
                id,
                "[{\"rol\":\"DOCENTE\",\"idUsuario\":\"" + destId + "\"}]",
                corrId
        );
        trackedIds.add(id);

        testSender.reset();
        testSender.setExpectedDeliveries(1);

        dispatcher.signal();

        boolean delivered = testSender.awaitDelivery(10, TimeUnit.SECONDS);
        assertTrue(delivered, "La reserva vencida debio ser recuperada y entregada");

        List<UUID> deliveredIds = testSender.getDeliveredIds();
        assertTrue(deliveredIds.contains(id), "El id entregado debe ser el recuperado");

        Map<String, Object> row = esperarEstado(id, "ENTREGADA", 10);
        assertNotNull(row);
        assertEquals("ENTREGADA", row.get("estado"));
        assertEquals(2, ((Number) row.get("intentos")).intValue(), "Debe tener 2 intentos (reserva previa + recuperacion)");
    }

    @Test
    @DisplayName("IT-07: depuración elimina terminales viejas y respeta las demás")
    void IT07_depuracion_elimina_terminales_viejas_y_respeta_demas() {
        UUID idTermVieja1 = UUID.randomUUID();
        UUID idTermVieja2 = UUID.randomUUID();
        UUID idTermReciente = UUID.randomUUID();
        UUID idNoTermVieja = UUID.randomUUID();
        UUID destId = UUID.randomUUID();
        UUID corrId = UUID.randomUUID();
        String destJson = "[{\"rol\":\"ESTUDIANTE\",\"idUsuario\":\"" + destId + "\"}]";

        // 1. Terminal vieja ENTREGADA (>30 dias)
        jdbcTemplate.update(
                "INSERT INTO dbo.NotificacionSaliente (" +
                        "  id, tipo, asunto, mensaje, destinatarios, datos, idEstado, " +
                        "  intentos, creadoEn, proximoIntentoEn, entregadoEn, idCorrelacion" +
                        ") VALUES (" +
                        "  ?, 'TEST_IT07_TERM_VIEJA1', 'A', 'M', ?, '{}', " +
                        "  (SELECT id FROM dbo.EstadoNotificacion WHERE codigo = 'ENTREGADA'), " +
                        "  1, DATEADD(DAY, -35, SYSUTCDATETIME()), DATEADD(DAY, -35, SYSUTCDATETIME()), " +
                        "  DATEADD(DAY, -35, SYSUTCDATETIME()), ?" +
                        ")",
                idTermVieja1, destJson, corrId
        );

        // 2. Terminal vieja FALLIDA_DEFINITIVA (>30 dias)
        jdbcTemplate.update(
                "INSERT INTO dbo.NotificacionSaliente (" +
                        "  id, tipo, asunto, mensaje, destinatarios, datos, idEstado, " +
                        "  intentos, creadoEn, proximoIntentoEn, idCorrelacion" +
                        ") VALUES (" +
                        "  ?, 'TEST_IT07_TERM_VIEJA2', 'A', 'M', ?, '{}', " +
                        "  (SELECT id FROM dbo.EstadoNotificacion WHERE codigo = 'FALLIDA_DEFINITIVA'), " +
                        "  3, DATEADD(DAY, -35, SYSUTCDATETIME()), DATEADD(DAY, -35, SYSUTCDATETIME()), ?" +
                        ")",
                idTermVieja2, destJson, corrId
        );

        // 3. Terminal reciente ENTREGADA (<30 dias)
        jdbcTemplate.update(
                "INSERT INTO dbo.NotificacionSaliente (" +
                        "  id, tipo, asunto, mensaje, destinatarios, datos, idEstado, " +
                        "  intentos, creadoEn, proximoIntentoEn, entregadoEn, idCorrelacion" +
                        ") VALUES (" +
                        "  ?, 'TEST_IT07_TERM_RECIENTE', 'A', 'M', ?, '{}', " +
                        "  (SELECT id FROM dbo.EstadoNotificacion WHERE codigo = 'ENTREGADA'), " +
                        "  1, DATEADD(DAY, -2, SYSUTCDATETIME()), DATEADD(DAY, -2, SYSUTCDATETIME()), " +
                        "  DATEADD(DAY, -2, SYSUTCDATETIME()), ?" +
                        ")",
                idTermReciente, destJson, corrId
        );

        // 4. No terminal vieja PENDIENTE (>30 dias)
        jdbcTemplate.update(
                "INSERT INTO dbo.NotificacionSaliente (" +
                        "  id, tipo, asunto, mensaje, destinatarios, datos, idEstado, " +
                        "  intentos, creadoEn, proximoIntentoEn, idCorrelacion" +
                        ") VALUES (" +
                        "  ?, 'TEST_IT07_NO_TERM_VIEJA', 'A', 'M', ?, '{}', " +
                        "  (SELECT id FROM dbo.EstadoNotificacion WHERE codigo = 'PENDIENTE'), " +
                        "  0, DATEADD(DAY, -35, SYSUTCDATETIME()), DATEADD(DAY, -35, SYSUTCDATETIME()), ?" +
                        ")",
                idNoTermVieja, destJson, corrId
        );

        trackedIds.add(idTermVieja1);
        trackedIds.add(idTermVieja2);
        trackedIds.add(idTermReciente);
        trackedIds.add(idNoTermVieja);

        jpaRepository.depurar(30, 100);

        Integer c1 = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dbo.NotificacionSaliente WHERE id = ?", Integer.class, idTermVieja1);
        Integer c2 = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dbo.NotificacionSaliente WHERE id = ?", Integer.class, idTermVieja2);
        Integer cReciente = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dbo.NotificacionSaliente WHERE id = ?", Integer.class, idTermReciente);
        Integer cNoTerm = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dbo.NotificacionSaliente WHERE id = ?", Integer.class, idNoTermVieja);

        assertEquals(0, c1, "La terminal vieja 1 debio ser depurada");
        assertEquals(0, c2, "La terminal vieja 2 debio ser depurada");
        assertEquals(1, cReciente, "La terminal reciente debe conservarse");
        assertEquals(1, cNoTerm, "La no-terminal vieja debe conservarse");
    }

    @Test
    @DisplayName("IT-08: provider=composite targets=log,outbox registra una fila y escribe log")
    void IT08_composite_targets_log_outbox_registra_y_loguea() throws Exception {
        CompositeNotificacionAdapter composite = new CompositeNotificacionAdapter(
                List.of(
                        new CompositeNotificacionAdapter.NamedTarget("log", new LogNotificacionAdapter()),
                        new CompositeNotificacionAdapter.NamedTarget("outbox", outboxNotificacionAdapter)
                ),
                meterRegistry
        );

        UUID destId = UUID.randomUUID();
        Notificacion notificacion = new Notificacion(
                "TEST_IT08",
                "Asunto IT08",
                "Mensaje IT08",
                List.of(new Notificacion.Destinatario("ESTUDIANTE", destId, null, null)),
                Map.of("grupoId", UUID.randomUUID().toString())
        );

        testSender.reset();
        testSender.setExpectedDeliveries(1);

        composite.notificar(notificacion);

        boolean delivered = testSender.awaitDelivery(10, TimeUnit.SECONDS);
        assertTrue(delivered, "El composite debio despachar a traves de outbox");

        List<UUID> deliveredIds = testSender.getDeliveredIds();
        assertEquals(1, deliveredIds.size());
        UUID id = deliveredIds.get(0);
        trackedIds.add(id);

        Map<String, Object> row = esperarEstado(id, "ENTREGADA", 10);
        assertNotNull(row);
        assertEquals("ENTREGADA", row.get("estado"));
    }

    @Test
    @DisplayName("IT-09: sin filas residuales al terminar en NotificacionSaliente")
    void IT09_cero_filas_residuales_en_tabla() {
        limpiarFilasPropias();
        Integer testResidualRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.NotificacionSaliente WHERE tipo LIKE 'TEST_%'",
                Integer.class
        );
        assertEquals(0, testResidualRows, "No deben quedar filas residuales de pruebas en dbo.NotificacionSaliente");
        assertEquals(0, trackedIds.size());
    }

    @FunctionalInterface
    interface SenderAction {
        void execute(NotificacionPersistida notificacion) throws NotificacionNoEntregableException;
    }

    static class TestChannelSender implements NotificacionChannelSender {
        private final List<UUID> deliveredIds = Collections.synchronizedList(new ArrayList<>());
        private SenderAction action = n -> {};
        private CountDownLatch latch = new CountDownLatch(1);

        @Override
        public void enviar(NotificacionPersistida notificacion) throws NotificacionNoEntregableException {
            if (action != null) {
                action.execute(notificacion);
            }
            deliveredIds.add(notificacion.id());
            latch.countDown();
        }

        public void reset() {
            deliveredIds.clear();
            action = n -> {};
            latch = new CountDownLatch(1);
        }

        public void setExpectedDeliveries(int count) {
            this.latch = new CountDownLatch(count);
        }

        public boolean awaitDelivery(long timeout, TimeUnit unit) throws InterruptedException {
            return latch.await(timeout, unit);
        }

        public void setSenderAction(SenderAction action) {
            this.action = action;
        }

        public List<UUID> getDeliveredIds() {
            return new ArrayList<>(deliveredIds);
        }
    }
}
