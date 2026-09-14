package co.edu.uco.asistenciasuco.infrastructure.config.adapters.persistence.sqlserver;

import co.edu.uco.asistenciasuco.application.secondaryports.report.ReporteAsistenciaQueryPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.report.ReporteAsistenciaSqlServerAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "app.adapters.persistence",
        name = "provider",
        havingValue = "sqlserver",
        matchIfMissing = true
)
public class SqlServerReportAdapterConfiguration {

    @Bean
    public ReporteAsistenciaQueryPort reporteAsistenciaQueryPort(final NamedParameterJdbcOperations jdbcOperations) {
        return new ReporteAsistenciaSqlServerAdapter(jdbcOperations);
    }
}
