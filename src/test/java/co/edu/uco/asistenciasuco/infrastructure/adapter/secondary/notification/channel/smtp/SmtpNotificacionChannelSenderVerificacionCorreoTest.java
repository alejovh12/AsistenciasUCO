package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.channel.smtp;

import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.contact.ContactoDestinatario;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.contact.ContactoDestinatarioResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.DestinatarioRef;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionNoEntregableException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionPersistida;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template.MarcadorDinamicoProvider;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template.PlantillaNotificacionRepository;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template.PlantillaPorDefectoCatalogo;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template.PlantillaRenderer;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.SmtpNotificationProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * IDENT-001 BE-S01..BE-S02 y BE-T01: el aviso de verificacion de correo emite el enlace al entregar, ignora el filtro de
 * correo confirmado (el destinatario aun no lo esta) y no emite token si no hay a quien enviarlo.
 */
class SmtpNotificacionChannelSenderVerificacionCorreoTest {

    private static final UUID ESTUDIANTE = UUID.fromString("93641bab-e3cd-485c-b275-47e7b731e18c");
    private static final String ENLACE = "https://asistencia.uco.edu.co/verificar-correo#token=" + "A".repeat(43);

    private ContactoDestinatarioResolver contactoResolver;
    private PlantillaNotificacionRepository plantillaRepository;
    private SimpleMeterRegistry meterRegistry;
    private final List<CorreoSaliente> enviados = new ArrayList<>();
    private final List<String> llamadasProveedor = new ArrayList<>();

    @BeforeEach
    void setUp() {
        contactoResolver = mock(ContactoDestinatarioResolver.class);
        plantillaRepository = mock(PlantillaNotificacionRepository.class);
        when(plantillaRepository.consultarActiva(any(), any())).thenReturn(Optional.empty());
        meterRegistry = new SimpleMeterRegistry();
    }

    private SmtpNotificationProperties props(final boolean requireConfirmed) {
        return new SmtpNotificationProperties(
                "smtp.uco.edu.co", 587, "user", "secret", true, false,
                "notificaciones@uco.edu.co", "Asistencia UCO",
                Duration.ofSeconds(10), Duration.ofSeconds(20),
                50, 60, 500, requireConfirmed, "https://asistencia.uco.edu.co", Duration.ofSeconds(60));
    }

    private SmtpNotificacionChannelSender sender(final SmtpNotificationProperties props, final MarcadorDinamicoProvider proveedor) {
        final PlantillaRenderer renderer = new PlantillaRenderer(
                plantillaRepository, new PlantillaPorDefectoCatalogo(), meterRegistry, props.platformUrl());
        final CorreoTransport transporte = enviados::add;
        return new SmtpNotificacionChannelSender(
                contactoResolver, renderer, transporte,
                new TopeEnvios(60, 500, Clock.systemUTC(), meterRegistry), props, meterRegistry, proveedor);
    }

    private static NotificacionPersistida verificacion(final List<UUID> ids) {
        return new NotificacionPersistida(
                UUID.randomUUID(), Notificacion.TIPO_VERIFICACION_CORREO,
                "Verifique su correo institucional", "Se registro una cuenta a su nombre.",
                ids.stream().map(id -> new DestinatarioRef("ESTUDIANTE", id)).toList(),
                Map.of("nombre", "Ana"), 1, Instant.now());
    }

    private MarcadorDinamicoProvider proveedorEnlace() {
        return (tipo, ids) -> {
            llamadasProveedor.add(tipo);
            return Notificacion.TIPO_VERIFICACION_CORREO.equals(tipo) ? Map.of("enlace", ENLACE) : Map.of();
        };
    }

    @Test
    void BE_T01_el_correo_incluye_el_nombre_el_enlace_en_texto_y_como_vinculo_html() throws Exception {
        when(contactoResolver.consultarContactos(any()))
                .thenReturn(List.of(new ContactoDestinatario(ESTUDIANTE, "ana.perez@uco.edu.co", false)));

        sender(props(false), proveedorEnlace()).enviar(verificacion(List.of(ESTUDIANTE)));

        assertEquals(1, enviados.size());
        final CorreoSaliente correo = enviados.getFirst();
        assertEquals("Verifique su correo institucional", correo.asunto());
        assertEquals(List.of("ana.perez@uco.edu.co"), correo.bccAddresses());
        assertTrue(correo.textoPlano().contains("Ana"), correo.textoPlano());
        assertTrue(correo.textoPlano().contains(ENLACE), correo.textoPlano());
        assertTrue(correo.html().contains("<a href=\"" + ENLACE + "\""), correo.html());
    }

    @Test
    void BE_S01_el_aviso_de_verificacion_ignora_require_confirmed_email() throws Exception {
        when(contactoResolver.consultarContactos(any()))
                .thenReturn(List.of(new ContactoDestinatario(ESTUDIANTE, "ana.perez@uco.edu.co", false)));

        sender(props(true), proveedorEnlace()).enviar(verificacion(List.of(ESTUDIANTE)));

        assertEquals(1, enviados.size());
    }

    @Test
    void BE_S02_otros_tipos_siguen_respetando_require_confirmed_email() {
        when(contactoResolver.consultarContactos(any()))
                .thenReturn(List.of(new ContactoDestinatario(ESTUDIANTE, "ana.perez@uco.edu.co", false)));
        final NotificacionPersistida retiro = new NotificacionPersistida(
                UUID.randomUUID(), Notificacion.TIPO_RETIRO_ESTUDIANTE_GRUPO, "Retiro", "Mensaje",
                List.of(new DestinatarioRef("ESTUDIANTE", ESTUDIANTE)),
                Map.of("grupo", "G01", "estudiante", "Ana"), 1, Instant.now());

        assertThrows(NotificacionNoEntregableException.class, () -> sender(props(true), proveedorEnlace()).enviar(retiro));

        assertTrue(enviados.isEmpty());
        assertTrue(llamadasProveedor.isEmpty(), "No debe emitirse un token si no hay destinatario valido.");
    }

    @Test
    void no_emite_token_cuando_el_destinatario_no_tiene_correo_valido() {
        when(contactoResolver.consultarContactos(any())).thenReturn(List.of());

        assertThrows(NotificacionNoEntregableException.class,
                () -> sender(props(false), proveedorEnlace()).enviar(verificacion(List.of(ESTUDIANTE))));

        assertTrue(llamadasProveedor.isEmpty());
        assertTrue(enviados.isEmpty());
    }

    @Test
    void un_fallo_permanente_del_proveedor_no_envia_nada() {
        when(contactoResolver.consultarContactos(any()))
                .thenReturn(List.of(new ContactoDestinatario(ESTUDIANTE, "ana.perez@uco.edu.co", false)));
        final MarcadorDinamicoProvider yaVerificado = (tipo, ids) -> {
            throw new NotificacionNoEntregableException("Correo ya verificado.");
        };

        assertThrows(NotificacionNoEntregableException.class,
                () -> sender(props(false), yaVerificado).enviar(verificacion(List.of(ESTUDIANTE))));

        assertTrue(enviados.isEmpty());
    }

    @Test
    void un_fallo_transitorio_del_proveedor_se_propaga_para_reintentar() {
        when(contactoResolver.consultarContactos(any()))
                .thenReturn(List.of(new ContactoDestinatario(ESTUDIANTE, "ana.perez@uco.edu.co", false)));
        final MarcadorDinamicoProvider caido = (tipo, ids) -> {
            throw new IllegalStateException("SQL Server no disponible.");
        };

        assertThrows(IllegalStateException.class,
                () -> sender(props(false), caido).enviar(verificacion(List.of(ESTUDIANTE))));

        assertTrue(enviados.isEmpty());
    }

    @Test
    void el_constructor_sin_proveedor_conserva_el_comportamiento_anterior() throws Exception {
        when(contactoResolver.consultarContactos(any()))
                .thenReturn(List.of(new ContactoDestinatario(ESTUDIANTE, "ana.perez@uco.edu.co", true)));
        final SmtpNotificationProperties propiedades = props(false);
        final PlantillaRenderer renderer = new PlantillaRenderer(
                plantillaRepository, new PlantillaPorDefectoCatalogo(), meterRegistry, propiedades.platformUrl());
        final CorreoTransport transporte = enviados::add;
        final SmtpNotificacionChannelSender sinProveedor = new SmtpNotificacionChannelSender(
                contactoResolver, renderer, transporte,
                new TopeEnvios(60, 500, Clock.systemUTC(), meterRegistry), propiedades, meterRegistry);
        final NotificacionPersistida retiro = new NotificacionPersistida(
                UUID.randomUUID(), Notificacion.TIPO_RETIRO_ESTUDIANTE_GRUPO, "Retiro", "Mensaje",
                List.of(new DestinatarioRef("ESTUDIANTE", ESTUDIANTE)),
                Map.of("grupo", "G01", "estudiante", "Ana"), 1, Instant.now());

        sinProveedor.enviar(retiro);

        assertEquals(1, enviados.size());
        assertFalse(enviados.getFirst().textoPlano().contains("token"));
    }
}
