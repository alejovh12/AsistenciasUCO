package co.edu.uco.asistenciasuco.application.features.admin.procesareventoazure.usecase.impl;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.CatalogInvalidationPort;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcesarEventoAzureUseCaseImplTest {

    private RecordingInvalidationPort invalidationPort;
    private RecordingRealtimePublisherPort realtimePublisherPort;
    private ProcesarEventoAzureUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        invalidationPort = new RecordingInvalidationPort();
        realtimePublisherPort = new RecordingRealtimePublisherPort();
        useCase = new ProcesarEventoAzureUseCaseImpl(invalidationPort, realtimePublisherPort);
    }

    @Test
    void exigeAmbosPuertos() {
        assertThrows(NullPointerException.class,
                () -> new ProcesarEventoAzureUseCaseImpl(null, realtimePublisherPort));
        assertThrows(NullPointerException.class,
                () -> new ProcesarEventoAzureUseCaseImpl(invalidationPort, null));
    }

    @Test
    void ignoraEventoSinTipoODeProveedorNoSoportado() {
        useCase.procesarEvento(null, null, null);
        useCase.procesarEvento("   ", null, null);
        useCase.procesarEvento("Microsoft.Storage.BlobCreated", "subject", Map.of());

        assertTrue(invalidationPort.actions.isEmpty());
        assertTrue(realtimePublisherPort.events.isEmpty());
    }

    @Test
    void invalidaMensajeDeUsuarioYPublicaEventoConCodigoNormalizado() {
        useCase.procesarEvento(
                "Microsoft.AppConfiguration.KeyValueModified",
                "ignored",
                Map.of("key", "  messages:user:VAL-001  ")
        );

        assertEquals(List.of("message:VAL-001"), invalidationPort.actions);
        assertEquals(1, realtimePublisherPort.events.size());
        final RealtimeEvent event = realtimePublisherPort.events.getFirst();
        assertEquals("MESSAGE_UPDATED", event.type());
        assertEquals("VAL-001", event.payload().get("code"));
        assertEquals("USER", event.payload().get("type"));
    }

    @Test
    void invalidaMensajeTecnicoSinPublicarEvento() {
        useCase.procesarEvento(
                "Microsoft.AppConfiguration.KeyValueDeleted",
                "messages:technical:DB-001",
                null
        );

        assertEquals(List.of("message:DB-001"), invalidationPort.actions);
        assertTrue(realtimePublisherPort.events.isEmpty());
    }

    @Test
    void invalidaParametroAgrupadoYPublicaGrupoYClave() {
        useCase.procesarEvento(
                "Microsoft.AppConfiguration.KeyValueModified",
                null,
                Map.of("key", "asistencias:max_inasistencias")
        );

        assertEquals(List.of("parameter:asistencias:max_inasistencias"), invalidationPort.actions);
        final RealtimeEvent event = realtimePublisherPort.events.getFirst();
        assertEquals("PARAMETER_UPDATED", event.type());
        assertEquals("asistencias", event.payload().get("group"));
        assertEquals("max_inasistencias", event.payload().get("key"));
    }

    @Test
    void invalidaParametroSinGrupoUsandoElSubject() {
        final Map<String, Object> dataWithNullKey = new HashMap<>();
        dataWithNullKey.put("key", null);

        useCase.procesarEvento(
                "Microsoft.AppConfiguration.KeyValueModified",
                "  max_inasistencias  ",
                dataWithNullKey
        );

        assertEquals(List.of("parameter::max_inasistencias"), invalidationPort.actions);
        final RealtimeEvent event = realtimePublisherPort.events.getFirst();
        assertEquals("max_inasistencias", event.payload().get("key"));
    }

    @Test
    void invalidaTodosLosCatalogosCuandoAppConfigurationNoIdentificaClave() {
        useCase.procesarEvento(
                "Microsoft.AppConfiguration.KeyValueModified",
                "   ",
                Map.of("other", "value")
        );

        assertEquals(List.of("all"), invalidationPort.actions);
        assertTrue(realtimePublisherPort.events.isEmpty());
    }

    @Test
    void invalidaSecretoDesdeDataOSubjectYSinIdentidadIgnoraEvento() {
        useCase.procesarEvento(
                "Microsoft.KeyVault.SecretNewVersionCreated",
                "ignored",
                Map.of("ObjectName", "  db-password  ")
        );
        useCase.procesarEvento(
                "Microsoft.KeyVault.SecretNearExpiry",
                "  api-key  ",
                Map.of()
        );

        final Map<String, Object> dataWithNullObjectName = new HashMap<>();
        dataWithNullObjectName.put("ObjectName", null);
        useCase.procesarEvento(
                "Microsoft.KeyVault.SecretExpired",
                "   ",
                dataWithNullObjectName
        );

        assertEquals(List.of("secret:db-password", "secret:api-key"), invalidationPort.actions);
        assertTrue(realtimePublisherPort.events.isEmpty());
    }

    private static final class RecordingInvalidationPort implements CatalogInvalidationPort {

        private final List<String> actions = new ArrayList<>();

        @Override
        public void invalidateParameter(final String group, final String key) {
            actions.add("parameter:" + group + ":" + key);
        }

        @Override
        public void invalidateMessage(final String code) {
            actions.add("message:" + code);
        }

        @Override
        public void invalidateSecret(final String secretName) {
            actions.add("secret:" + secretName);
        }

        @Override
        public void invalidateAll() {
            actions.add("all");
        }
    }

    private static final class RecordingRealtimePublisherPort implements RealtimePublisherPort {

        private final List<RealtimeEvent> events = new ArrayList<>();

        @Override
        public void publish(final RealtimeEvent event) {
            events.add(event);
        }
    }
}
