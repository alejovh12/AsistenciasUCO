package co.edu.uco.asistenciasuco.infrastructure.config.adapters.persistence.sqlserver;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvAsistenciaEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvDetalleAsistenciaEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvEstudianteGrupoEntity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.EntityType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * LB-008 JPA-02A: bootstrap JPA estandar de Spring Boot (un unico EntityManagerFactory administrado por
 * Boot). Hibernate debe fijar el dialecto SQL Server y no consultar metadatos JDBC al arrancar; el
 * scanning debe descubrir las entities de Asistencia sin registro manual.
 */
class SqlServerJpaBootstrapConfigurationTest {

    @Test
    void customizer_fija_dialecto_sqlserver_y_no_consulta_metadatos_jdbc_al_arrancar() {
        final Map<String, Object> properties = new HashMap<>();

        new SqlServerJpaBootstrapConfiguration().sqlServerHibernatePropertiesCustomizer().customize(properties);

        assertEquals("org.hibernate.dialect.SQLServerDialect", properties.get("hibernate.dialect"));
        assertEquals("false", properties.get("hibernate.boot.allow_jdbc_metadata_access"));
        assertEquals("none", properties.get("hibernate.hbm2ddl.auto"));
        assertEquals("false", properties.get("hibernate.show_sql"));
    }

    @Test
    void customizer_conserva_los_naming_strategies_de_hibernate_del_piloto_y_no_el_camel_case_de_boot() {
        final Map<String, Object> properties = new HashMap<>();

        new SqlServerJpaBootstrapConfiguration().sqlServerHibernatePropertiesCustomizer().customize(properties);

        assertEquals("org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl",
                properties.get("hibernate.physical_naming_strategy"));
        assertEquals("org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl",
                properties.get("hibernate.implicit_naming_strategy"));
    }

    @Test
    void bootstrap_estandar_crea_un_unico_emf_sin_conectar_y_descubre_las_entities_de_asistencia() throws SQLException {
        final DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("el arranque no debe conectar a la DB"));

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(HibernateJpaAutoConfiguration.class))
                .withUserConfiguration(SqlServerJpaBootstrapConfiguration.class, AutoConfigurationScanBase.class)
                .withBean(DataSource.class, () -> dataSource)
                .withPropertyValues("spring.jpa.hibernate.ddl-auto=none", "spring.jpa.open-in-view=false")
                .run(context -> {
                    assertEquals(1, context.getBeansOfType(EntityManagerFactory.class).size());

                    final Set<Class<?>> entidades = context.getBean(EntityManagerFactory.class)
                            .getMetamodel()
                            .getEntities()
                            .stream()
                            .map(EntityType::getJavaType)
                            .collect(Collectors.toSet());

                    assertTrue(entidades.containsAll(List.of(
                            UvAsistenciaEntity.class,
                            UvDetalleAsistenciaEntity.class,
                            UvEstudianteGrupoEntity.class
                    )));
                    verify(dataSource, never()).getConnection();
                    verify(dataSource, never()).getConnection(anyString(), anyString());
                });
    }

    @Configuration(proxyBeanMethods = false)
    @AutoConfigurationPackage(basePackages = "co.edu.uco.asistenciasuco")
    static class AutoConfigurationScanBase {
    }
}


