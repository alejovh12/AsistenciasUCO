package co.edu.uco.asistenciasuco.infrastructure.config.adapters.persistence.sqlserver;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.DocenteRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.EstudianteRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.GrupoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.TipoIdentificacionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core.AsistenciaRepositorySqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core.DocenteRepositorySqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core.EstudianteRepositorySqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core.GrupoRepositorySqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core.SesionRepositorySqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core.TipoIdentificacionRepositorySqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core.UsuarioRepositorySqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.CanonicalStoredProcedureExecutor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Composition Root: selecciona SQL Server como tecnología de persistencia para los
 * repositorios "core" (Grupo, Usuario, TipoIdentificacion, Estudiante, Docente, Sesion, Asistencia).
 *
 * <p>Las Feature Configs (p.ej. {@code GrupoWiringConfiguration}) solo dependen de los
 * {@code *RepositoryPort} declarados aquí; no conocen SQL Server.</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "app.adapters.persistence",
        name = "provider",
        havingValue = "sqlserver",
        matchIfMissing = true
)
public class SqlServerCoreRepositoryAdapterConfiguration {

    @Bean
    public GrupoRepositoryPort grupoRepositoryPort(
            final CanonicalStoredProcedureExecutor canonicalStoredProcedureExecutor,
            final NamedParameterJdbcOperations namedParameterJdbcOperations,
            final TransactionOperations transactionOperations
    ) {
        return new GrupoRepositorySqlServerAdapter(
                canonicalStoredProcedureExecutor,
                namedParameterJdbcOperations,
                transactionOperations
        );
    }

    @Bean
    public UsuarioRepositoryPort usuarioRepositoryPort(
            final CanonicalStoredProcedureExecutor canonicalStoredProcedureExecutor,
            final NamedParameterJdbcOperations namedParameterJdbcOperations
    ) {
        return new UsuarioRepositorySqlServerAdapter(canonicalStoredProcedureExecutor, namedParameterJdbcOperations);
    }

    @Bean
    public TipoIdentificacionRepositoryPort tipoIdentificacionRepositoryPort(final JdbcTemplate jdbcTemplate) {
        return new TipoIdentificacionRepositorySqlServerAdapter(jdbcTemplate);
    }

    @Bean
    public EstudianteRepositoryPort estudianteRepositoryPort(final JdbcTemplate jdbcTemplate) {
        return new EstudianteRepositorySqlServerAdapter(jdbcTemplate);
    }

    @Bean
    public DocenteRepositoryPort docenteRepositoryPort(final JdbcTemplate jdbcTemplate) {
        return new DocenteRepositorySqlServerAdapter(jdbcTemplate);
    }

    @Bean
    public SesionRepositoryPort sesionRepositoryPort(
            final CanonicalStoredProcedureExecutor procedureExecutor,
            final NamedParameterJdbcOperations namedParameterJdbcOperations
    ) {
        return new SesionRepositorySqlServerAdapter(procedureExecutor, namedParameterJdbcOperations);
    }

    @Bean
    public AsistenciaRepositoryPort asistenciaRepositoryPort(
            final NamedParameterJdbcOperations namedParameterJdbcOperations,
            final CanonicalStoredProcedureExecutor procedureExecutor
    ) {
        return new AsistenciaRepositorySqlServerAdapter(namedParameterJdbcOperations, procedureExecutor);
    }
}
