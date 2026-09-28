package co.edu.uco.asistenciasuco.infrastructure.config.adapters.persistence.sqlserver;

import co.edu.uco.asistenciasuco.infrastructure.config.adapters.persistence.sqlserver.SqlServerCoreRepositoryAdapterConfiguration.AsistenciaQueryProvider;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/** Selector del provider y configuracion segura del EntityManagerFactory (sin arrancar Hibernate). */
class SqlServerJpaAsistenciaQueryConfigurationTest {

    @Test
    void selector_default_es_jdbc_y_acepta_jdbc_y_jpa_sin_distinguir_mayusculas() {
        assertEquals(AsistenciaQueryProvider.JDBC, AsistenciaQueryProvider.from(null));
        assertEquals(AsistenciaQueryProvider.JDBC, AsistenciaQueryProvider.from("jdbc"));
        assertEquals(AsistenciaQueryProvider.JPA, AsistenciaQueryProvider.from("jpa"));
        assertEquals(AsistenciaQueryProvider.JPA, AsistenciaQueryProvider.from("  JPA "));
    }

    @Test
    void selector_rechaza_valores_vacios_o_desconocidos() {
        final IllegalStateException exception =
                assertThrows(IllegalStateException.class, () -> AsistenciaQueryProvider.from(""));
        assertTrue(exception.getMessage().contains("asistencia-query-provider"));
        assertThrows(IllegalStateException.class, () -> AsistenciaQueryProvider.from("hibernate"));
    }

    @Test
    void entity_manager_factory_no_administra_schema_ni_loguea_sql() {
        final DataSource dataSource = mock(DataSource.class);

        final LocalContainerEntityManagerFactoryBean factory =
                new SqlServerJpaAsistenciaQueryAdapterConfiguration().entityManagerFactory(dataSource);

        assertEquals("none", factory.getJpaPropertyMap().get("hibernate.hbm2ddl.auto"));
        assertEquals("false", factory.getJpaPropertyMap().get("hibernate.show_sql"));
        assertSame(dataSource, factory.getDataSource());
    }

    @Test
    void con_provider_jdbc_o_sin_propiedad_no_se_construye_entity_manager_factory() {
        new ApplicationContextRunner()
                .withUserConfiguration(SqlServerJpaAsistenciaQueryAdapterConfiguration.class)
                .withBean(DataSource.class, () -> mock(DataSource.class))
                .run(context -> assertTrue(context.getBeansOfType(EntityManagerFactory.class).isEmpty()));
        new ApplicationContextRunner()
                .withPropertyValues("app.adapters.persistence.asistencia-query-provider=jdbc")
                .withUserConfiguration(SqlServerJpaAsistenciaQueryAdapterConfiguration.class)
                .withBean(DataSource.class, () -> mock(DataSource.class))
                .run(context -> assertFalse(context.containsBean("entityManagerFactory")));
    }
}
