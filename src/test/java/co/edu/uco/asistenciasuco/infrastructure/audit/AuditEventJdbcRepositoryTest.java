package co.edu.uco.asistenciasuco.infrastructure.audit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuditEventJdbcRepositoryTest {

    @Test
    void metadataFromJson_reconstruye_metadata_persistida() {
        final ObjectProvider<JdbcTemplate> jdbcTemplateProvider = mock();
        when(jdbcTemplateProvider.getIfAvailable()).thenReturn(null);
        final AuditEventJdbcRepository repository = new AuditEventJdbcRepository(jdbcTemplateProvider);

        final Map<String, String> metadata = repository.metadataFromJson("{\"handlerType\":\"HTTP\"}");

        assertEquals("HTTP", metadata.get("handlerType"));
        assertEquals(Map.of("handlerType", "HTTP"), metadata);
    }
}
