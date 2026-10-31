package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.channel.smtp;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.VerificacionCorreoRepositoryPort;
import co.edu.uco.asistenciasuco.crosscutting.util.TokenHashHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionOutboxDispatcher;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.OutboxNotificacionAdapter;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IDENT-001 IT-V01..IT-V06 contra SQL Server real + GreenMail: el aviso viaja por el outbox sin token, el canal emite
 * el enlace al entregar, la BD solo guarda el SHA-256 y el token es de un solo uso. El paso de Keycloak queda cubierto
 * por pruebas unitarias del adaptador y por el E2E.
 */
@Tag("integration")
@SpringBootTest(properties = {
        "app.adapters.notification.provider=outbox",
        "app.providers.outbox-notification.sender=smtp",
        "app.providers.outbox-notification.sweeper-interval=1s",
        "app.providers.outbox-notification.backoff-base=1s",
        "app.providers.outbox-notification.lease=20s",
        "app.providers.outbox-notification.max-attempts=3",
        "app.providers.outbox-notification.retention-days=30",
        "app.providers.smtp-notification.host=127.0.0.1",
        "app.providers.smtp-notification.from-address=notificaciones@uco.edu.co",
        "app.providers.smtp-notification.from-name=Sistema de Asistencias UCO",
        "app.providers.smtp-notification.starttls=false",
        "app.providers.smtp-notification.ssl=false",
        "app.providers.smtp-notification.require-confirmed-email=true",
        "app.providers.smtp-notification.default-locale=es",
        "app.providers.smtp-notification.template-cache-ttl=1s",
        "app.providers.smtp-notification.rate-per-minute=60",
        "app.providers.smtp-notification.rate-per-day=500",
        "app.providers.smtp-notification.max-bcc=50",
        "app.providers.smtp-notification.platform-url=https://asistencias.uco.edu.co"
})
@MockitoBean(types = JwtDecoder.class)
@Import(VerificacionCorreoGreenMailIT.TestJacksonConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VerificacionCorreoGreenMailIT {

    private static final int SMTP_PORT = allocateFreePort();
    private static final Pattern ENLACE = Pattern.compile("https://asistencias\\.uco\\.edu\\.co/verificar-correo#token=([A-Za-z0-9_-]{43})");
    private static final AtomicInteger DOC_SEQ = new AtomicInteger(197500000);
    private static GreenMail greenMail;

    @DynamicPropertySource
    static void registerDynamicProperties(final DynamicPropertyRegistry registry) {
        registry.add("app.providers.smtp-notification.port", () -> SMTP_PORT);
    }

    private static int allocateFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo reservar puerto libre para GreenMail", e);
        }
    }

    @TestConfiguration
    static class TestJacksonConfig {
        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Autowired
    private OutboxNotificacionAdapter outboxNotificacionAdapter;

    @Autowired
    private NotificacionOutboxDispatcher dispatcher;

    @Autowired
    private TopeEnvios topeEnvios;

    @Autowired
    private VerificacionCorreoRepositoryPort verificacionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final Set<UUID> usuarios = ConcurrentHashMap.newKeySet();
    private final Set<UUID> estudiantes = ConcurrentHashMap.newKeySet();
    private final Set<UUID> matriculas = ConcurrentHashMap.newKeySet();

    @BeforeAll
    void startGreenMail() {
        final ServerSetup setup = new ServerSetup(SMTP_PORT, "127.0.0.1", ServerSetup.PROTOCOL_SMTP);
        setup.setServerStartupTimeout(5000L);
        greenMail = new GreenMail(setup);
        greenMail.start();
    }

    @AfterAll
    void stopGreenMail() {
        limpiar();
        greenMail.stop();
    }

    @BeforeEach
    void setUp() throws Exception {
        CorrelationIdContext.set(UUID.randomUUID().toString());
        greenMail.purgeEmailFromAllMailboxes();
        topeEnvios.reiniciarVentanas();
    }

    @AfterEach
    void tearDown() {
        CorrelationIdContext.clear();
        topeEnvios.reiniciarVentanas();
        limpiar();
    }

    private void limpiar() {
        for (final UUID id : matriculas) {
            jdbcTemplate.update("DELETE FROM dbo.EstudianteGrupo WHERE id = ?", id);
        }
        matriculas.clear();
        for (final UUID id : estudiantes) {
            jdbcTemplate.update("DELETE FROM dbo.Estudiante WHERE id = ?", id);
        }
        estudiantes.clear();
        for (final UUID id : usuarios) {
            jdbcTemplate.update("DELETE FROM dbo.NotificacionSaliente WHERE destinatarios LIKE ?", "%" + id + "%");
            jdbcTemplate.update("DELETE FROM dbo.TokenVerificacionCorreo WHERE usuario = ?", id);
            jdbcTemplate.update("DELETE FROM dbo.Usuario WHERE id = ?", id);
        }
        usuarios.clear();
    }

    private UUID crearUsuario(final String correo, final boolean confirmado) {
        final UUID id = UUID.randomUUID();
        final UUID tipo = jdbcTemplate.queryForObject("SELECT TOP 1 id FROM dbo.TipoIdentificacion", UUID.class);
        jdbcTemplate.update(
                "INSERT INTO dbo.Usuario (id, tipoIdIdentificacion, numeroIdentificacion, primerApellido, segundoApellido, "
                        + "primerNombre, segundoNombre, correo, correoConfirmado, estado, [password]) "
                        + "VALUES (?, ?, ?, N'PRUEBA', N'IDENT', N'ANA', N'MARIA', ?, ?, 1, N'HashTest')",
                id, tipo, DOC_SEQ.incrementAndGet(), correo, confirmado ? 1 : 0);
        usuarios.add(id);
        return id;
    }

    private void avisarVerificacion(final UUID usuario) {
        outboxNotificacionAdapter.notificar(new Notificacion(
                Notificacion.TIPO_VERIFICACION_CORREO,
                "Verifique su correo institucional",
                "Se registro una cuenta a su nombre.",
                List.of(new Notificacion.Destinatario("ESTUDIANTE", usuario, null, null)),
                Map.of("nombre", "Ana Maria")));
        dispatcher.signal();
    }

    private String estadoDelAviso(final UUID usuario, final int maxSegundos, final String esperado) throws InterruptedException {
        String ultimo = null;
        for (int i = 0; i < maxSegundos * 4; i++) {
            final List<String> filas = jdbcTemplate.queryForList(
                    "SELECT e.codigo FROM dbo.NotificacionSaliente n JOIN dbo.EstadoNotificacion e ON e.id = n.idEstado "
                            + "WHERE n.destinatarios LIKE ? ORDER BY n.creadoEn DESC",
                    String.class, "%" + usuario + "%");
            if (!filas.isEmpty()) {
                ultimo = filas.getFirst();
                if (esperado.equals(ultimo)) {
                    return ultimo;
                }
            }
            Thread.sleep(250);
        }
        return ultimo;
    }

    private String textoDelUltimoCorreo() throws Exception {
        assertTrue(greenMail.waitForIncomingEmail(5000, 1), "Debio llegar un correo.");
        final MimeMessage[] mensajes = greenMail.getReceivedMessages();
        return buscar(mensajes[mensajes.length - 1].getContent(), mensajes[mensajes.length - 1].getContentType(), "text/plain");
    }

    private String htmlDelUltimoCorreo() throws Exception {
        final MimeMessage[] mensajes = greenMail.getReceivedMessages();
        return buscar(mensajes[mensajes.length - 1].getContent(), mensajes[mensajes.length - 1].getContentType(), "text/html");
    }

    private String buscar(final Object contenido, final String tipo, final String prefijo) throws Exception {
        if (tipo != null && tipo.toLowerCase().startsWith(prefijo) && contenido instanceof String texto) {
            return texto;
        }
        if (contenido instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                final BodyPart parte = multipart.getBodyPart(i);
                final String hallado = buscar(parte.getContent(), parte.getContentType(), prefijo);
                if (!hallado.isEmpty()) {
                    return hallado;
                }
            }
        }
        return "";
    }

    private String extraerToken(final String texto) {
        final Matcher matcher = ENLACE.matcher(texto);
        assertTrue(matcher.find(), "El correo debe incluir el enlace con #token=. Texto: " + texto);
        return matcher.group(1);
    }

    private int tokensDelUsuario(final UUID usuario) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dbo.TokenVerificacionCorreo WHERE usuario = ?", Integer.class, usuario);
    }

    @Test
    @DisplayName("IT-V01/V02: el aviso no lleva token; el correo trae el enlace y la BD solo guarda su SHA-256")
    void ITV01_ITV02_el_token_llega_por_correo_y_en_la_bd_solo_esta_el_hash() throws Exception {
        final UUID usuario = crearUsuario("ana.ident001@uco.edu.co", false);

        avisarVerificacion(usuario);

        assertEquals("ENTREGADA", estadoDelAviso(usuario, 15, "ENTREGADA"));
        final String texto = textoDelUltimoCorreo();
        final String token = extraerToken(texto);
        assertTrue(texto.contains("Ana Maria"), texto);
        assertTrue(extraerToken(htmlDelUltimoCorreo()).equals(token), "El HTML debe llevar el mismo enlace.");
        assertTrue(htmlDelUltimoCorreo().contains("<a href=\"https://asistencias.uco.edu.co/verificar-correo#token="));

        final Map<String, Object> fila = jdbcTemplate.queryForMap(
                "SELECT n.asunto, n.mensaje, n.datos, n.destinatarios FROM dbo.NotificacionSaliente n WHERE n.destinatarios LIKE ?",
                "%" + usuario + "%");
        for (final Object valor : fila.values()) {
            assertFalse(String.valueOf(valor).contains(token), "El outbox no debe contener el token.");
        }
        assertFalse(String.valueOf(fila.get("destinatarios")).contains("ana.ident001"), "El outbox no debe contener el correo.");

        final int coincidencias = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.TokenVerificacionCorreo WHERE usuario = ? AND tokenHash = ?",
                Integer.class, usuario, TokenHashHelper.sha256(token));
        assertEquals(1, coincidencias);
        assertEquals(1, tokensDelUsuario(usuario));
    }

    @Test
    @DisplayName("IT-V03: consultar no consume; confirmar marca el correo; el token no se reutiliza")
    void ITV03_el_token_es_de_un_solo_uso_y_confirma_el_correo() throws Exception {
        final UUID usuario = crearUsuario("ana.ident001b@uco.edu.co", false);
        avisarVerificacion(usuario);
        estadoDelAviso(usuario, 15, "ENTREGADA");
        final String token = extraerToken(textoDelUltimoCorreo());

        assertEquals(usuario, verificacionRepository.consultarUsuarioPorToken(token).orElseThrow());
        assertEquals(usuario, verificacionRepository.consultarUsuarioPorToken(token).orElseThrow(), "Consultar no consume.");

        verificacionRepository.confirmar(token);

        assertEquals(1, jdbcTemplate.queryForObject("SELECT correoConfirmado FROM dbo.Usuario WHERE id = ?", Integer.class, usuario));
        assertTrue(verificacionRepository.consultarUsuarioPorToken(token).isEmpty());
        final ValidationException exception = assertThrows(ValidationException.class, () -> verificacionRepository.confirmar(token));
        assertTrue(exception.getMessage().toLowerCase().contains("enlace"), exception.getMessage());
    }

    @Test
    @DisplayName("IT-V04: un token vencido, inexistente o de otro usuario se rechaza igual")
    void ITV04_token_vencido_o_inexistente_es_indistinguible() throws Exception {
        final UUID usuario = crearUsuario("ana.ident001c@uco.edu.co", false);
        avisarVerificacion(usuario);
        estadoDelAviso(usuario, 15, "ENTREGADA");
        final String token = extraerToken(textoDelUltimoCorreo());

        jdbcTemplate.update(
                "UPDATE dbo.TokenVerificacionCorreo SET fechaEmisionUtc = DATEADD(HOUR, -30, SYSUTCDATETIME()), "
                        + "fechaExpiracionUtc = DATEADD(HOUR, -6, SYSUTCDATETIME()) WHERE usuario = ?", usuario);

        assertTrue(verificacionRepository.consultarUsuarioPorToken(token).isEmpty());
        final ValidationException vencido = assertThrows(ValidationException.class, () -> verificacionRepository.confirmar(token));
        final String inexistente = "Z".repeat(43);
        final ValidationException desconocido = assertThrows(ValidationException.class, () -> verificacionRepository.confirmar(inexistente));
        assertEquals(vencido.getMessage(), desconocido.getMessage());
        assertEquals(0, jdbcTemplate.queryForObject("SELECT correoConfirmado FROM dbo.Usuario WHERE id = ?", Integer.class, usuario));
    }

    @Test
    @DisplayName("IT-V05: un segundo aviso emite un token nuevo y revoca el anterior")
    void ITV05_un_nuevo_envio_revoca_el_token_anterior() throws Exception {
        final UUID usuario = crearUsuario("ana.ident001d@uco.edu.co", false);
        avisarVerificacion(usuario);
        estadoDelAviso(usuario, 15, "ENTREGADA");
        final String primero = extraerToken(textoDelUltimoCorreo());
        greenMail.purgeEmailFromAllMailboxes();
        jdbcTemplate.update("DELETE FROM dbo.NotificacionSaliente WHERE destinatarios LIKE ?", "%" + usuario + "%");

        avisarVerificacion(usuario);
        estadoDelAviso(usuario, 15, "ENTREGADA");
        final String segundo = extraerToken(textoDelUltimoCorreo());

        assertNotEquals(primero, segundo);
        assertTrue(verificacionRepository.consultarUsuarioPorToken(primero).isEmpty(), "El token anterior debe estar revocado.");
        assertEquals(usuario, verificacionRepository.consultarUsuarioPorToken(segundo).orElseThrow());
        assertEquals(2, tokensDelUsuario(usuario));
    }

    @Test
    @DisplayName("IT-V06: un correo ya verificado no recibe enlace (fallo permanente) aunque require-confirmed-email este activo")
    void ITV06_un_correo_ya_verificado_no_recibe_enlace() throws Exception {
        final UUID usuario = crearUsuario("ana.ident001e@uco.edu.co", true);

        avisarVerificacion(usuario);

        assertEquals("FALLIDA_DEFINITIVA", estadoDelAviso(usuario, 15, "FALLIDA_DEFINITIVA"));
        assertEquals(0, greenMail.getReceivedMessages().length);
        assertEquals(0, tokensDelUsuario(usuario));
    }

    @Test
    @DisplayName("IT-V07: un usuario sin confirmar recibe el enlace aunque require-confirmed-email este activo")
    void ITV07_el_aviso_de_verificacion_ignora_require_confirmed_email() throws Exception {
        final UUID usuario = crearUsuario("ana.ident001f@uco.edu.co", false);

        avisarVerificacion(usuario);

        assertEquals("ENTREGADA", estadoDelAviso(usuario, 15, "ENTREGADA"));
        assertTrue(greenMail.getReceivedMessages().length >= 1, "GreenMail guarda un mensaje por buzon (To y BCC).");
    }

    @Test
    @DisplayName("IT-V08: el reenvio respeta el ambito, la espera minima y el estado del correo")
    void ITV08_reenvio_ambito_espera_y_estado() {
        final Map<String, Object> grupo = jdbcTemplate.queryForMap(
                "SELECT TOP 1 g.id AS grupo, d.usuario AS titular FROM dbo.Grupo g JOIN dbo.Docente d ON d.id = g.docente");
        final UUID grupoId = UUID.fromString(grupo.get("grupo").toString());
        final UUID titular = UUID.fromString(grupo.get("titular").toString());
        final UUID otroDocente = jdbcTemplate.queryForObject(
                "SELECT TOP 1 usuario FROM dbo.Docente WHERE usuario <> ?", UUID.class, titular);

        final UUID usuario = crearUsuario("ana.ident001g@uco.edu.co", false);
        final UUID estudiante = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO dbo.Estudiante (id, usuario) VALUES (?, ?)", estudiante, usuario);
        estudiantes.add(estudiante);
        final UUID matricula = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO dbo.EstudianteGrupo (id, estado, estudiante, grupo) "
                        + "VALUES (?, (SELECT TOP 1 id FROM dbo.EstadoEstudianteGrupo WHERE codigo = 'A'), ?, ?)",
                matricula, estudiante, grupoId);
        matriculas.add(matricula);

        assertEquals(usuario, verificacionRepository.consultarReenvio(grupoId, estudiante, titular));
        assertThrows(ResourceNotFoundException.class, () -> verificacionRepository.consultarReenvio(grupoId, estudiante, otroDocente));

        final byte[] hash = TokenHashHelper.sha256("T".repeat(43));
        jdbcTemplate.update(
                "INSERT INTO dbo.TokenVerificacionCorreo (id, usuario, tokenHash, fechaEmisionUtc, fechaExpiracionUtc) "
                        + "VALUES (NEWID(), ?, ?, SYSUTCDATETIME(), DATEADD(HOUR, 24, SYSUTCDATETIME()))", usuario, hash);
        assertThrows(ConflictException.class, () -> verificacionRepository.consultarReenvio(grupoId, estudiante, titular));

        jdbcTemplate.update("UPDATE dbo.Usuario SET correoConfirmado = 1 WHERE id = ?", usuario);
        assertThrows(ConflictException.class, () -> verificacionRepository.consultarReenvio(grupoId, estudiante, titular));
    }
}
