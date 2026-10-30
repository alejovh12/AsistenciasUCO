package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.channel.smtp;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.contact.ContactoDestinatario;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.contact.ContactoDestinatarioResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.DestinatarioRef;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionNoEntregableException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionPersistida;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template.PlantillaNotificacion;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template.PlantillaNotificacionRepository;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template.PlantillaPorDefectoCatalogo;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template.PlantillaRenderer;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.SmtpNotificationProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SmtpNotificacionChannelSenderTest {

    private ContactoDestinatarioResolver contactoResolver;
    private PlantillaNotificacionRepository plantillaRepository;
    private SimpleMeterRegistry meterRegistry;
    private FakeCorreoTransport transport;

    @BeforeEach
    void setUp() {
        contactoResolver = mock(ContactoDestinatarioResolver.class);
        plantillaRepository = mock(PlantillaNotificacionRepository.class);
        meterRegistry = new SimpleMeterRegistry();
        transport = new FakeCorreoTransport();
    }

    private SmtpNotificationProperties crearProps(final int maxBcc, final boolean requireConfirmed) {
        return new SmtpNotificationProperties(
                "smtp.uco.edu.co",
                587,
                "user",
                "secret",
                true,
                false,
                "notificaciones@uco.edu.co",
                "Asistencia UCO",
                Duration.ofSeconds(10),
                Duration.ofSeconds(20),
                maxBcc,
                60,
                500,
                requireConfirmed,
                "https://asistencia.uco.edu.co",
                Duration.ofSeconds(60)
        );
    }

    private SmtpNotificacionChannelSender crearSender(
            final SmtpNotificationProperties props,
            final TopeEnvios tope
    ) {
        final PlantillaRenderer renderer = new PlantillaRenderer(
                plantillaRepository,
                new PlantillaPorDefectoCatalogo(),
                meterRegistry,
                props.platformUrl()
        );
        return new SmtpNotificacionChannelSender(
                contactoResolver,
                renderer,
                transport,
                tope,
                props,
                meterRegistry
        );
    }

    private static NotificacionPersistida notificacionRetiro(final List<UUID> idsUsuarios) {
        final List<DestinatarioRef> refs = idsUsuarios.stream()
                .map(id -> new DestinatarioRef("ESTUDIANTE", id))
                .toList();
        return new NotificacionPersistida(
                UUID.randomUUID(),
                "RETIRO_ESTUDIANTE_GRUPO",
                "Retiro guardado",
                "Mensaje guardado",
                refs,
                Map.of("grupo", "G01 de Software", "estudiante", "Ana Gómez", "motivo", "Motivo privado"),
                1,
                Instant.now()
        );
    }

    @Test
    @DisplayName("BE-S01: notificacion con 3 destinatarios validos -> 1 mensaje, To=remitente, 3 BCC, multipart texto+HTML, Message-ID derivado del id")
    void BE_S01_envia_un_mensaje_con_to_remitente_y_3_bcc() throws Exception {
        final UUID u1 = UUID.randomUUID();
        final UUID u2 = UUID.randomUUID();
        final UUID u3 = UUID.randomUUID();
        when(contactoResolver.consultarContactos(any())).thenReturn(List.of(
                new ContactoDestinatario(u1, "u1@uco.edu.co", true),
                new ContactoDestinatario(u2, "u2@uco.edu.co", true),
                new ContactoDestinatario(u3, "u3@uco.edu.co", true)
        ));
        when(plantillaRepository.consultarActiva("RETIRO_ESTUDIANTE_GRUPO", "es"))
                .thenReturn(Optional.of(new PlantillaNotificacion(
                        1,
                        "Retiro del grupo {grupo}",
                        "Se registró el retiro de {estudiante} del grupo {grupo}.",
                        List.of(
                                new PlantillaNotificacion.Marcador("grupo", true),
                                new PlantillaNotificacion.Marcador("estudiante", true)
                        )
                )));

        final SmtpNotificationProperties props = crearProps(50, false);
        final TopeEnvios tope = new TopeEnvios(60, 500, Clock.systemUTC(), meterRegistry);
        final SmtpNotificacionChannelSender sender = crearSender(props, tope);

        final NotificacionPersistida notif = notificacionRetiro(List.of(u1, u2, u3));
        sender.enviar(notif);

        assertEquals(1, transport.enviados.size());
        final CorreoSaliente correo = transport.enviados.getFirst();
        assertEquals("notificaciones@uco.edu.co", correo.toAddress());
        assertEquals("notificaciones@uco.edu.co", correo.fromAddress());
        assertEquals("Asistencia UCO", correo.fromName());
        assertEquals(List.of("u1@uco.edu.co", "u2@uco.edu.co", "u3@uco.edu.co"), correo.bccAddresses());
        assertEquals("Retiro del grupo G01 de Software", correo.asunto());
        assertTrue(correo.textoPlano().contains("Se registró el retiro de Ana Gómez del grupo G01 de Software."));
        assertTrue(correo.html().contains("Se registró el retiro de Ana Gómez del grupo G01 de Software."));
        assertTrue(correo.messageId().contains(notif.id().toString()));
        assertEquals(1.0, meterRegistry.counter("notification.email.send.total", "outcome", "sent").count());
        assertEquals(3.0, meterRegistry.counter("notification.email.recipients.total", "outcome", "sent").count());
    }

    @Test
    @DisplayName("BE-S02: destinatario sin correo o inhabilitado -> omitido; resto enviado; metrica omitted")
    void BE_S02_destinatario_omitido_y_metrica_omitted() throws Exception {
        final UUID uActivo = UUID.randomUUID();
        final UUID uInactivo = UUID.randomUUID();
        final UUID uCorreoVacio = UUID.randomUUID();
        // uInactivo no es devuelto por el SP; uCorreoVacio tiene correo en blanco
        when(contactoResolver.consultarContactos(any())).thenReturn(List.of(
                new ContactoDestinatario(uActivo, "activo@uco.edu.co", true),
                new ContactoDestinatario(uCorreoVacio, "   ", true)
        ));
        when(plantillaRepository.consultarActiva("RETIRO_ESTUDIANTE_GRUPO", "es")).thenReturn(Optional.empty());

        final SmtpNotificationProperties props = crearProps(50, false);
        final TopeEnvios tope = new TopeEnvios(60, 500, Clock.systemUTC(), meterRegistry);
        final SmtpNotificacionChannelSender sender = crearSender(props, tope);

        sender.enviar(notificacionRetiro(List.of(uActivo, uInactivo, uCorreoVacio)));

        assertEquals(1, transport.enviados.size());
        assertEquals(List.of("activo@uco.edu.co"), transport.enviados.getFirst().bccAddresses());
        assertEquals(1.0, meterRegistry.counter("notification.email.recipients.total", "outcome", "sent").count());
        assertEquals(2.0, meterRegistry.counter("notification.email.recipients.total", "outcome", "omitted").count());
    }

    @Test
    @DisplayName("BE-S03: ningun destinatario valido -> NotificacionNoEntregableException y metrica no_recipients")
    void BE_S03_ningun_destinatario_valido_lanza_no_entregable() {
        final UUID u1 = UUID.randomUUID();
        when(contactoResolver.consultarContactos(any())).thenReturn(List.of());

        final SmtpNotificationProperties props = crearProps(50, false);
        final TopeEnvios tope = new TopeEnvios(60, 500, Clock.systemUTC(), meterRegistry);
        final SmtpNotificacionChannelSender sender = crearSender(props, tope);

        assertThrows(NotificacionNoEntregableException.class,
                () -> sender.enviar(notificacionRetiro(List.of(u1))));
        assertEquals(1.0, meterRegistry.counter("notification.email.send.total", "outcome", "no_recipients").count());
    }

    @Test
    @DisplayName("BE-S04: require-confirmed-email=true omite no confirmados; con false los incluye")
    void BE_S04_require_confirmed_email_filtra_no_confirmados() throws Exception {
        final UUID uConfirmado = UUID.randomUUID();
        final UUID uNoConfirmado = UUID.randomUUID();
        when(contactoResolver.consultarContactos(any())).thenReturn(List.of(
                new ContactoDestinatario(uConfirmado, "confirmado@uco.edu.co", true),
                new ContactoDestinatario(uNoConfirmado, "noconf@uco.edu.co", false)
        ));
        when(plantillaRepository.consultarActiva("RETIRO_ESTUDIANTE_GRUPO", "es")).thenReturn(Optional.empty());

        // Con requireConfirmedEmail = true: solo envia a confirmado
        final SmtpNotificacionChannelSender senderStrict = crearSender(
                crearProps(50, true),
                new TopeEnvios(60, 500, Clock.systemUTC(), meterRegistry)
        );
        senderStrict.enviar(notificacionRetiro(List.of(uConfirmado, uNoConfirmado)));
        assertEquals(1, transport.enviados.size());
        assertEquals(List.of("confirmado@uco.edu.co"), transport.enviados.getFirst().bccAddresses());

        // Con requireConfirmedEmail = false: envia a ambos
        transport.enviados.clear();
        final SmtpNotificacionChannelSender senderPermissive = crearSender(
                crearProps(50, false),
                new TopeEnvios(60, 500, Clock.systemUTC(), meterRegistry)
        );
        senderPermissive.enviar(notificacionRetiro(List.of(uConfirmado, uNoConfirmado)));
        assertEquals(1, transport.enviados.size());
        assertEquals(List.of("confirmado@uco.edu.co", "noconf@uco.edu.co"), transport.enviados.getFirst().bccAddresses());
    }

    @Test
    @DisplayName("BE-S05: mas de max-bcc destinatarios -> se divide en bloques y todos son enviados")
    void BE_S05_mas_de_max_bcc_divide_en_bloques() throws Exception {
        final UUID u1 = UUID.randomUUID();
        final UUID u2 = UUID.randomUUID();
        final UUID u3 = UUID.randomUUID();
        final UUID u4 = UUID.randomUUID();
        final UUID u5 = UUID.randomUUID();
        when(contactoResolver.consultarContactos(any())).thenReturn(List.of(
                new ContactoDestinatario(u1, "u1@uco.edu.co", true),
                new ContactoDestinatario(u2, "u2@uco.edu.co", true),
                new ContactoDestinatario(u3, "u3@uco.edu.co", true),
                new ContactoDestinatario(u4, "u4@uco.edu.co", true),
                new ContactoDestinatario(u5, "u5@uco.edu.co", true)
        ));
        when(plantillaRepository.consultarActiva("RETIRO_ESTUDIANTE_GRUPO", "es")).thenReturn(Optional.empty());

        // maxBcc = 2 -> 5 destinatarios generan 3 bloques (2 + 2 + 1)
        final SmtpNotificacionChannelSender sender = crearSender(
                crearProps(2, false),
                new TopeEnvios(60, 500, Clock.systemUTC(), meterRegistry)
        );
        sender.enviar(notificacionRetiro(List.of(u1, u2, u3, u4, u5)));

        assertEquals(3, transport.enviados.size());
        assertEquals(2, transport.enviados.get(0).bccAddresses().size());
        assertEquals(2, transport.enviados.get(1).bccAddresses().size());
        assertEquals(1, transport.enviados.get(2).bccAddresses().size());
    }

    @Test
    @DisplayName("BE-S06: fallo transitorio (conexion/timeout/4xx) -> excepcion no controlada (RuntimeException) y metrica retry")
    void BE_S06_fallo_transitorio_lanza_runtime_exception() {
        final UUID u1 = UUID.randomUUID();
        when(contactoResolver.consultarContactos(any())).thenReturn(List.of(
                new ContactoDestinatario(u1, "u1@uco.edu.co", true)
        ));
        when(plantillaRepository.consultarActiva("RETIRO_ESTUDIANTE_GRUPO", "es")).thenReturn(Optional.empty());

        transport.falloSimulado = new RuntimeException("421 4.7.0 Service unavailable - try again later");

        final SmtpNotificacionChannelSender sender = crearSender(
                crearProps(50, false),
                new TopeEnvios(60, 500, Clock.systemUTC(), meterRegistry)
        );

        assertThrows(RuntimeException.class, () -> sender.enviar(notificacionRetiro(List.of(u1))));
        assertEquals(1.0, meterRegistry.counter("notification.email.send.total", "outcome", "retry").count());
    }

    @Test
    @DisplayName("BE-S07: fallo permanente (direccion rechazada/5xx) -> NotificacionNoEntregableException y metrica permanent")
    void BE_S07_fallo_permanente_lanza_no_entregable() {
        final UUID u1 = UUID.randomUUID();
        when(contactoResolver.consultarContactos(any())).thenReturn(List.of(
                new ContactoDestinatario(u1, "u1@uco.edu.co", true)
        ));
        when(plantillaRepository.consultarActiva("RETIRO_ESTUDIANTE_GRUPO", "es")).thenReturn(Optional.empty());

        transport.falloNoEntregable = new NotificacionNoEntregableException("550 5.1.1 User unknown");

        final SmtpNotificacionChannelSender sender = crearSender(
                crearProps(50, false),
                new TopeEnvios(60, 500, Clock.systemUTC(), meterRegistry)
        );

        assertThrows(NotificacionNoEntregableException.class, () -> sender.enviar(notificacionRetiro(List.of(u1))));
        assertEquals(1.0, meterRegistry.counter("notification.email.send.total", "outcome", "permanent").count());
    }

    @Test
    @DisplayName("BE-S08: inyeccion de cabeceras: direccion con \\r\\n o formato invalido se descarta y asunto con \\r\\n se sanea")
    void BE_S08_inyeccion_de_cabeceras_rechazada_y_saneada() throws Exception {
        final UUID uInyectado = UUID.randomUUID();
        final UUID uValido = UUID.randomUUID();
        when(contactoResolver.consultarContactos(any())).thenReturn(List.of(
                new ContactoDestinatario(uInyectado, "hacker@uco.edu.co\r\nBcc: spy@evil.com", true),
                new ContactoDestinatario(uValido, "limpio@uco.edu.co", true)
        ));
        when(plantillaRepository.consultarActiva("RETIRO_ESTUDIANTE_GRUPO", "es"))
                .thenReturn(Optional.of(new PlantillaNotificacion(
                        1,
                        "Asunto {grupo}\r\nBcc: otro@evil.com",
                        "Cuerpo {estudiante} en {grupo}",
                        List.of(
                                new PlantillaNotificacion.Marcador("grupo", true),
                                new PlantillaNotificacion.Marcador("estudiante", true)
                        )
                )));

        final SmtpNotificacionChannelSender sender = crearSender(
                crearProps(50, false),
                new TopeEnvios(60, 500, Clock.systemUTC(), meterRegistry)
        );

        sender.enviar(notificacionRetiro(List.of(uInyectado, uValido)));

        assertEquals(1, transport.enviados.size());
        final CorreoSaliente enviado = transport.enviados.getFirst();
        assertEquals(List.of("limpio@uco.edu.co"), enviado.bccAddresses());
        assertFalse(enviado.asunto().contains("\r"));
        assertFalse(enviado.asunto().contains("\n"));
    }

    @Test
    @DisplayName("BE-S09: el sender nunca loguea correos, asunto ni cuerpo")
    void BE_S09_sender_nunca_loguea_correos_asunto_ni_cuerpo() throws Exception {
        final Logger logger = (Logger) LoggerFactory.getLogger(SmtpNotificacionChannelSender.class);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            final UUID u1 = UUID.randomUUID();
            final String correoSecreto = "estudiante.confidencial.999@uco.edu.co";
            when(contactoResolver.consultarContactos(any())).thenReturn(List.of(
                    new ContactoDestinatario(u1, correoSecreto, true)
            ));
            when(plantillaRepository.consultarActiva("RETIRO_ESTUDIANTE_GRUPO", "es")).thenReturn(Optional.empty());

            final SmtpNotificacionChannelSender sender = crearSender(
                    crearProps(50, false),
                    new TopeEnvios(60, 500, Clock.systemUTC(), meterRegistry)
            );
            sender.enviar(notificacionRetiro(List.of(u1)));

            for (final ILoggingEvent ev : appender.list) {
                final String msg = ev.getFormattedMessage();
                assertFalse(msg.contains(correoSecreto), "El log no debe contener el correo del destinatario");
                assertFalse(msg.contains("Ana Gómez"), "El log no debe contener el cuerpo ni datos personales");
                assertFalse(msg.contains("G01 de Software"), "El log no debe contener el asunto renderizado");
            }
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    @DisplayName("BE-S10: plantilla en BD no disponible -> se envia con el respaldo por defecto y no se pierde el correo")
    void BE_S10_plantilla_bd_no_disponible_envia_con_respaldo() throws Exception {
        final UUID u1 = UUID.randomUUID();
        when(contactoResolver.consultarContactos(any())).thenReturn(List.of(
                new ContactoDestinatario(u1, "u1@uco.edu.co", true)
        ));
        when(plantillaRepository.consultarActiva("RETIRO_ESTUDIANTE_GRUPO", "es"))
                .thenThrow(new RuntimeException("DB timeout"));

        final SmtpNotificacionChannelSender sender = crearSender(
                crearProps(50, false),
                new TopeEnvios(60, 500, Clock.systemUTC(), meterRegistry)
        );

        sender.enviar(notificacionRetiro(List.of(u1)));

        assertEquals(1, transport.enviados.size());
        assertEquals("Retiro del grupo G01 de Software", transport.enviados.getFirst().asunto());
        assertEquals(1.0, meterRegistry.counter("notification.template.render.total", "origin", "default").count());
    }

    @Test
    @DisplayName("BE-S11: cobertura de ramas de sender, CorreoSaliente y JakartaMailCorreoTransport")
    void BE_S11_cobertura_ramas_sender_y_jakarta_mail_transport() throws Exception {
        // 1. CorreoSaliente ramas null en fromName y bccAddresses
        final CorreoSaliente cNulls = new CorreoSaliente(
                "from@uco.edu.co", null, "to@uco.edu.co", null, "Asunto", "Texto", "<p>HTML</p>", "<id@uco>"
        );
        assertEquals("", cNulls.fromName());
        assertTrue(cNulls.bccAddresses().isEmpty());

        // 2. SmtpNotificacionChannelSender ramas: disponible(), destinatarios vacios/nulos, tope agotado en mitad de envio,
        // contactos null/elementos null, correo con \n, sanearAsunto(null)
        final SmtpNotificationProperties props = crearProps(1, false);
        final TopeEnvios topeCorto = new TopeEnvios(1, 5, Clock.systemUTC(), meterRegistry);
        final SmtpNotificacionChannelSender sender = crearSender(props, topeCorto);
        assertTrue(sender.disponible());

        assertEquals("", SmtpNotificacionChannelSender.sanearAsunto(null));
        assertFalse(SmtpNotificacionChannelSender.esCorreoSeguro(null));
        assertFalse(SmtpNotificacionChannelSender.esCorreoSeguro("a@b.co\nBcc:x"));

        // Destinatarios vacios y con idUsuario nulo
        final NotificacionPersistida notifSinIds = new NotificacionPersistida(
                UUID.randomUUID(), "RETIRO_ESTUDIANTE_GRUPO", "A", "M",
                List.of(new DestinatarioRef("ESTUDIANTE", null)), Map.of(), 1, Instant.now()
        );
        assertThrows(NotificacionNoEntregableException.class, () -> sender.enviar(notifSinIds));
        final NotificacionPersistida notifListaNula = new NotificacionPersistida(
                UUID.randomUUID(), "RETIRO_ESTUDIANTE_GRUPO", "A", "M", null, Map.of(), 1, Instant.now()
        );
        assertThrows(NotificacionNoEntregableException.class, () -> sender.enviar(notifListaNula));

        // Contactos devueltos = null
        final UUID u1 = UUID.randomUUID();
        final UUID u2 = UUID.randomUUID();
        when(contactoResolver.consultarContactos(any())).thenReturn(null);
        assertThrows(NotificacionNoEntregableException.class, () -> sender.enviar(notificacionRetiro(List.of(u1))));

        // Contactos con elemento null y segundo bloque que agota el tope (maxBcc=1, ratePerMinute=1, 2 destinatarios)
        final List<ContactoDestinatario> listaConElementoNull = new ArrayList<>();
        listaConElementoNull.add(null);
        listaConElementoNull.add(new ContactoDestinatario(u1, "u1@uco.edu.co", true));
        listaConElementoNull.add(new ContactoDestinatario(u2, "u2@uco.edu.co", true));
        when(contactoResolver.consultarContactos(any())).thenReturn(listaConElementoNull);
        when(plantillaRepository.consultarActiva("RETIRO_ESTUDIANTE_GRUPO", "es")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> sender.enviar(notificacionRetiro(List.of(u1, u2))));
        assertFalse(sender.disponible());

        // 3. JakartaMailCorreoTransport con JavaMailSender mockeado y constructor con propiedades (con y sin auth)
        final JakartaMailCorreoTransport transportConAuth = new JakartaMailCorreoTransport(crearProps(10, false));
        assertNotNull(transportConAuth);
        final SmtpNotificationProperties propsSinAuth = new SmtpNotificationProperties(
                "smtp.uco.edu.co", 587, "", "", true, false,
                "notificaciones@uco.edu.co", "Asistencia UCO",
                Duration.ofSeconds(10), Duration.ofSeconds(20),
                10, 60, 500, false, "https://asistencia.uco.edu.co", Duration.ofSeconds(60)
        );
        final JakartaMailCorreoTransport transportSinAuth = new JakartaMailCorreoTransport(propsSinAuth);
        assertNotNull(transportSinAuth);

        final org.springframework.mail.javamail.JavaMailSender mailSenderMock =
                mock(org.springframework.mail.javamail.JavaMailSender.class);
        when(mailSenderMock.createMimeMessage()).thenAnswer(
                inv -> new jakarta.mail.internet.MimeMessage((jakarta.mail.Session) null)
        );
        final JakartaMailCorreoTransport jakartaTransport = new JakartaMailCorreoTransport(mailSenderMock);

        // Envio con fromName lleno y bcc lleno
        jakartaTransport.enviar(new CorreoSaliente(
                "from@uco.edu.co", "Nombre UCO", "to@uco.edu.co",
                List.of("bcc1@uco.edu.co"), "Asunto", "Plano", "<p>HTML</p>", "<m1@uco.edu.co>"
        ));
        // Envio con fromName vacio y bcc vacio
        jakartaTransport.enviar(new CorreoSaliente(
                "from@uco.edu.co", "", "to@uco.edu.co",
                List.of(), "Asunto", "Plano", "<p>HTML</p>", "<m2@uco.edu.co>"
        ));

        // Clasificacion de fallos permanentes vs transitorios en JakartaMailCorreoTransport
        assertTrue(JakartaMailCorreoTransport.esFalloPermanente(
                new jakarta.mail.internet.AddressException("Direccion mal formada")
        ));
        final jakarta.mail.Address[] invalidAddrs =
                new jakarta.mail.Address[]{new jakarta.mail.internet.InternetAddress("bad@uco.edu.co")};
        assertTrue(JakartaMailCorreoTransport.esFalloPermanente(
                new jakarta.mail.SendFailedException("Invalid", null, null, null, invalidAddrs)
        ));
        assertFalse(JakartaMailCorreoTransport.esFalloPermanente(
                new jakarta.mail.SendFailedException("Partial", null, null, invalidAddrs, invalidAddrs)
        ));
        assertFalse(JakartaMailCorreoTransport.esFalloPermanente(
                new jakarta.mail.SendFailedException("Empty", null, null, null, new jakarta.mail.Address[0])
        ));
        assertFalse(JakartaMailCorreoTransport.esFalloPermanente(
                new jakarta.mail.SendFailedException("NullInvalid", null, null, null, null)
        ));
        assertTrue(JakartaMailCorreoTransport.esFalloPermanente(
                new org.springframework.mail.MailSendException("550 5.1.1 Mailbox unavailable")
        ));
        assertFalse(JakartaMailCorreoTransport.esFalloPermanente(
                new org.springframework.mail.MailSendException("421 4.7.0 Try again later")
        ));
        assertFalse(JakartaMailCorreoTransport.esFalloPermanente(
                new RuntimeException((String) null)
        ));

        // Excepcion permanente y transitoria al llamar enviar()
        org.mockito.Mockito.doThrow(new org.springframework.mail.MailSendException("554 5.7.1 Rejected"))
                .when(mailSenderMock).send(any(jakarta.mail.internet.MimeMessage.class));
        assertThrows(NotificacionNoEntregableException.class, () -> jakartaTransport.enviar(cNulls));

        org.mockito.Mockito.doThrow(new org.springframework.mail.MailSendException("451 Temporary failure"))
                .when(mailSenderMock).send(any(jakarta.mail.internet.MimeMessage.class));
        assertThrows(IllegalStateException.class, () -> jakartaTransport.enviar(cNulls));
    }

    private static final class FakeCorreoTransport implements CorreoTransport {
        private final List<CorreoSaliente> enviados = new ArrayList<>();
        private RuntimeException falloSimulado;
        private NotificacionNoEntregableException falloNoEntregable;

        @Override
        public void enviar(final CorreoSaliente correo) throws NotificacionNoEntregableException {
            if (falloNoEntregable != null) {
                throw falloNoEntregable;
            }
            if (falloSimulado != null) {
                throw falloSimulado;
            }
            enviados.add(correo);
        }
    }
}
