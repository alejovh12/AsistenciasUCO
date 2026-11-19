package co.edu.uco.asistenciasuco.infrastructure.config.adapters.audit;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.scheduler.PurgaAuditoriaScheduler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.AuditEventJpaRepository;
import co.edu.uco.asistenciasuco.infrastructure.audit.contract.AuditEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Duration;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditAdapterConfigurationTest {

    private final AuditAdapterConfiguration configuration = new AuditAdapterConfiguration();

    @Test
    void crea_publicador_de_eventos_de_auditoria() {
        final ObjectProvider<AuditEventJpaRepository> repositoryProvider = mock(ObjectProvider.class);

        final AuditEventPublisher publisher = configuration.auditEventPublisher(repositoryProvider);

        assertNotNull(publisher);
    }

    @Test
    void configura_y_ejecuta_el_scheduler_de_retencion_con_el_repositorio() {
        final AuditEventJpaRepository repository = mock(AuditEventJpaRepository.class);
        when(repository.purgarAnterioresA(any(OffsetDateTime.class))).thenReturn(0);

        final PurgaAuditoriaScheduler scheduler = configuration.purgaAuditoriaScheduler(
                repository,
                Duration.ofDays(4),
                Duration.ZERO,
                Duration.ofHours(24)
        );

        assertNotNull(scheduler);
        assertFalse(scheduler.isRunning());

        scheduler.ejecutarPasada();

        verify(repository).purgarAnterioresA(any(OffsetDateTime.class));
    }
}
