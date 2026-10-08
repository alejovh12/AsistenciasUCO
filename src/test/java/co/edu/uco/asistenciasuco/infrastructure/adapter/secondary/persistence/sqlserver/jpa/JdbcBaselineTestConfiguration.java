package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.CanonicalJdbcBaselineExecutor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

/**
 * Oraculo JDBC de SOLO test (LB-008 JPA-07): expone el ejecutor baseline a los ITs de paridad. No es un bean
 * productivo; produccion no tiene ningun ejecutor JDBC.
 */
@TestConfiguration(proxyBeanMethods = false)
public class JdbcBaselineTestConfiguration {

    @Bean
    public CanonicalJdbcBaselineExecutor canonicalJdbcBaselineExecutor(final NamedParameterJdbcOperations operations) {
        return new CanonicalJdbcBaselineExecutor(operations);
    }
}
