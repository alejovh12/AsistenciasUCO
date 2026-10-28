package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox;

import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.LogNotificacionAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.composite.CompositeNotificacionAdapter;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.OutboxNotificationProperties;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
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

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Pruebas de integración reales contra SQL Server para la capacidad Outbox (NOTIF-001).
 * Verifica los escenarios IT-01 hasta IT-09 definidos en TEST_PLAN §4.
 */
@Tag("integration")
@SpringBootTest(properties = {
        "app.adapters.notification.provider=outbox",
        "app.providers.outbox-notification.sweeper-interval=1s",
        "app.providers.outbox-notification.backoff-base=1s",
        "app.providers.outbox-notification.lease-duration=5s",
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
        public com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
            return new com.fasterxml.jackson.databind.ObjectMapper();
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
                jdbcTemplate.update("DELETE FROM dbo.NotificacionSaliente WHERE idNotificacion = ?", id);
            }
            trackedIds.clear();
        }
    }

    @Test
    @DisplayName("IT-01: registrar -> despachador entrega -> fila ENTREGADA")
    void IT01_registrar_despachador_entrega_fila_entregada() throws Exception {
        fail("RED: Failsafe IT-01..IT-09 pendiente de verificación green");
    }

    @Test
    @DisplayName("IT-02: sender que falla 2 veces y luego tiene éxito -> intentos=3, ENTREGADA")
    void IT02_sender_falla_2_veces_luego_exito() throws Exception {
        fail("RED: Failsafe IT-02 pendiente de verificación green");
    }

    @Test
    @DisplayName("IT-03: sender que siempre falla -> FALLIDA_DEFINITIVA tras max-attempts, métrica dead = 1")
    void IT03_sender_siempre_falla_marca_fallida_definitiva() throws Exception {
        fail("RED: Failsafe IT-03 pendiente de verificación green");
    }

    @Test
    @DisplayName("IT-04: NotificacionNoEntregableException -> definitiva en el primer intento")
    void IT04_notificacion_no_entregable_falla_definitiva_primer_intento() throws Exception {
        fail("RED: Failsafe IT-04 pendiente de verificación green");
    }

    @Test
    @DisplayName("IT-05: dos despachadores sobre 100 filas -> cada id se entrega exactamente una vez")
    void IT05_dos_despachadores_concurrencia_100_filas_sin_duplicados() throws Exception {
        fail("RED: Failsafe IT-05 pendiente de verificación green");
    }

    @Test
    @DisplayName("IT-06: reserva vencida -> otra instancia la recupera")
    void IT06_reserva_vencida_recuperada_por_otra_instancia() throws Exception {
        fail("RED: Failsafe IT-06 pendiente de verificación green");
    }

    @Test
    @DisplayName("IT-07: depuración elimina terminales viejas y respeta las demás")
    void IT07_depuracion_elimina_terminales_viejas_y_respeta_demas() throws Exception {
        fail("RED: Failsafe IT-07 pendiente de verificación green");
    }

    @Test
    @DisplayName("IT-08: provider=composite targets=log,outbox registra una fila y escribe log")
    void IT08_composite_targets_log_outbox_registra_y_loguea() throws Exception {
        fail("RED: Failsafe IT-08 pendiente de verificación green");
    }

    @Test
    @DisplayName("IT-09: sin filas residuales al terminar en NotificacionSaliente")
    void IT09_cero_filas_residuales_en_tabla() {
        fail("RED: Failsafe IT-09 pendiente de verificación green");
    }

    static class TestChannelSender implements NotificacionChannelSender {
        private final List<UUID> deliveredIds = Collections.synchronizedList(new ArrayList<>());
        private Function<NotificacionPersistida, Boolean> failureRule = n -> false;
        private CountDownLatch latch = new CountDownLatch(1);

        @Override
        public void enviar(NotificacionPersistida notificacion) throws NotificacionNoEntregableException {
            if (failureRule.apply(notificacion)) {
                return;
            }
            deliveredIds.add(notificacion.id());
            latch.countDown();
        }

        public void reset() {
            deliveredIds.clear();
            failureRule = n -> false;
            latch = new CountDownLatch(1);
        }

        public void setExpectedDeliveries(int count) {
            this.latch = new CountDownLatch(count);
        }

        public boolean awaitDelivery(long timeout, TimeUnit unit) throws InterruptedException {
            return latch.await(timeout, unit);
        }

        public void setFailureRule(Function<NotificacionPersistida, Boolean> rule) {
            this.failureRule = rule;
        }

        public List<UUID> getDeliveredIds() {
            return new ArrayList<>(deliveredIds);
        }
    }
}
