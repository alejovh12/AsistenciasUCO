package co.edu.uco.asistenciasuco.infrastructure.config.adapters.persistence.sqlserver;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvAsistenciaEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvDetalleAsistenciaEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvEstudianteGrupoEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Composition Root (LB-002.1): infraestructura JPA/Hibernate para la query piloto de asistencia.
 * Solo se activa con {@code app.adapters.persistence.asistencia-query-provider=jpa}; con el default
 * jdbc no se construye ningun {@code EntityManagerFactory} (la auto-configuracion JPA de Boot esta
 * excluida en {@code AsistenciasUcoApplication}).
 *
 * <p>Hibernate NO administra el esquema ({@code hbm2ddl=none}); solo mapea vistas de solo lectura.
 * No se registra un {@code JpaTransactionManager}: los commands JDBC conservan su estrategia
 * transaccional actual. Reutiliza el mismo {@link DataSource} que JDBC.</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "app.adapters.persistence",
        name = "asistencia-query-provider",
        havingValue = "jpa"
)
public class SqlServerJpaAsistenciaQueryAdapterConfiguration {

    static final Map<String, Object> HIBERNATE_SAFE_PROPERTIES = Map.of(
            "hibernate.hbm2ddl.auto", "none",
            "hibernate.show_sql", "false",
            "jakarta.persistence.validation.mode", "none"
    );

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(final DataSource dataSource) {
        final LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setPersistenceUnitName("asistencia-query");
        factory.setDataSource(dataSource);
        factory.setManagedTypes(PersistenceManagedTypes.of(
                UvDetalleAsistenciaEntity.class.getName(),
                UvAsistenciaEntity.class.getName(),
                UvEstudianteGrupoEntity.class.getName()
        ));
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaPropertyMap(HIBERNATE_SAFE_PROPERTIES);
        return factory;
    }
}
