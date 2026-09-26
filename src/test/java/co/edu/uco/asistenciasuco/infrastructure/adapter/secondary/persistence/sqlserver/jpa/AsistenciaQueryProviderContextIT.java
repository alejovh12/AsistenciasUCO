package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core.AsistenciaRepositoryHybridSqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core.AsistenciaRepositorySqlServerAdapter;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JPA-Q-009/010 con contexto Spring REAL (SQL Server): el default sigue en JDBC sin construir
 * Hibernate, y con {@code jpa} la estrategia transaccional JDBC no cambia (no hay
 * JpaTransactionManager) ni hay dos puertos de asistencia.
 */
@Tag("integration")
class AsistenciaQueryProviderContextIT {

    @Nested
    @SpringBootTest
    @MockitoBean(types = JwtDecoder.class)
    class Default {

        @Autowired
        private ApplicationContext context;

        @Test
        void sin_propiedad_el_puerto_es_jdbc_y_no_existe_entity_manager_factory() {
            final Map<String, AsistenciaRepositoryPort> ports = context.getBeansOfType(AsistenciaRepositoryPort.class);

            assertEquals(1, ports.size());
            assertInstanceOf(AsistenciaRepositorySqlServerAdapter.class, ports.values().iterator().next());
            assertTrue(context.getBeansOfType(EntityManagerFactory.class).isEmpty());
            assertTransactionManagerIsJdbcOnly(context);
        }
    }

    @Nested
    @SpringBootTest(properties = "app.adapters.persistence.asistencia-query-provider=jpa")
    @MockitoBean(types = JwtDecoder.class)
    class Jpa {

        @Autowired
        private ApplicationContext context;

        @Test
        void con_jpa_hay_un_unico_puerto_hibrido_y_la_transaccion_jdbc_no_cambia() {
            final Map<String, AsistenciaRepositoryPort> ports = context.getBeansOfType(AsistenciaRepositoryPort.class);

            assertEquals(1, ports.size());
            assertInstanceOf(AsistenciaRepositoryHybridSqlServerAdapter.class, ports.values().iterator().next());
            assertEquals(1, context.getBeansOfType(EntityManagerFactory.class).size());
            assertTransactionManagerIsJdbcOnly(context);
        }

        @Test
        void hibernate_no_administra_el_schema() {
            final EntityManagerFactory factory = context.getBean(EntityManagerFactory.class);

            assertEquals("none", String.valueOf(factory.getProperties().get("hibernate.hbm2ddl.auto")));
        }
    }

    private static void assertTransactionManagerIsJdbcOnly(final ApplicationContext context) {
        final Map<String, PlatformTransactionManager> managers = context.getBeansOfType(PlatformTransactionManager.class);
        assertFalse(managers.isEmpty());
        managers.values().forEach(manager -> assertInstanceOf(JdbcTransactionManager.class, manager));
    }
}
