package co.edu.uco.asistenciasuco.infrastructure.config;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.testdouble.AsistenciaRepositoryMockAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.testdouble.SesionRepositoryMockAdapter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Reemplaza los repositorios SQL Server por los mocks en tests que necesiten
 * levantar el contexto de Spring sin base de datos.
 *
 * <p>Sustituye la antigua selección tecnológica vía {@code spring.profiles.active=mock},
 * eliminada porque mezclaba PROFILE (entorno) con PROVIDER (tecnología). Los mocks ya
 * no se activan mediante perfiles; deben importarse explícitamente en el test que los
 * necesite, por ejemplo con {@code @Import(MockRepositoryTestConfiguration.class)}.</p>
 */
@TestConfiguration
public class MockRepositoryTestConfiguration {

    @Bean
    public SesionRepositoryPort sesionRepositoryPort() {
        return new SesionRepositoryMockAdapter();
    }

    @Bean
    public AsistenciaRepositoryPort asistenciaRepositoryPort() {
        return new AsistenciaRepositoryMockAdapter();
    }
}
