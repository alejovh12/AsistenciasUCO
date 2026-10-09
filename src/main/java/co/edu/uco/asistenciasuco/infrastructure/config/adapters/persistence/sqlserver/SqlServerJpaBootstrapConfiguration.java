package co.edu.uco.asistenciasuco.infrastructure.config.adapters.persistence.sqlserver;

import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * LB-008 JPA-02A: UN solo bootstrap JPA, el de Spring Boot. El {@code EntityManagerFactory}, el
 * {@code EntityManager} compartido y el {@code JpaTransactionManager} los administra Boot; las entities
 * se descubren por scanning del paquete base ({@code AsistenciasUcoApplication}).
 *
 * <p>Esta clase solo fija las propiedades de Hibernate que el bootstrap estandar necesita para SQL Server:
 * dialecto explicito y {@code hibernate.boot.allow_jdbc_metadata_access=false}, de modo que arrancar la
 * aplicacion no abre conexion a la DB (la conexion ocurre solo al ejecutar un SP o una vista). El esquema
 * no lo administra el backend ({@code hbm2ddl=none}).</p>
 */
@Configuration(proxyBeanMethods = false)
public class SqlServerJpaBootstrapConfiguration {

    static final Map<String, String> HIBERNATE_SQLSERVER_PROPERTIES = Map.of(
            "hibernate.dialect", "org.hibernate.dialect.SQLServerDialect",
            "hibernate.boot.allow_jdbc_metadata_access", "false",
            "hibernate.hbm2ddl.auto", "none",
            "hibernate.show_sql", "false",
            "jakarta.persistence.validation.mode", "none",
            // Los @Column de las vistas usan nombres camelCase (idAsistencia, codigoRazonCausa). Boot aplica por
            // defecto CamelCaseToUnderscores y rompe el mapeo; se conservan los naming strategies de Hibernate
            // que usaba el piloto para no alterar el SQL generado.
            "hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl",
            "hibernate.implicit_naming_strategy", "org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl"
    );

    @Bean
    public HibernatePropertiesCustomizer sqlServerHibernatePropertiesCustomizer() {
        return hibernateProperties -> hibernateProperties.putAll(HIBERNATE_SQLSERVER_PROPERTIES);
    }
}
