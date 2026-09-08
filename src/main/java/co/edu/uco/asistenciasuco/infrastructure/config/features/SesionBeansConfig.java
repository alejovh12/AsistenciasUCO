package co.edu.uco.asistenciasuco.infrastructure.config.features;

import co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.primaryports.CerrarSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.primaryports.interactor.CerrarSesionInteractor;
import co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.usecase.CerrarSesionUseCase;
import co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.usecase.impl.CerrarSesionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.ConsultarSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.interactor.ConsultarSesionInteractor;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.ConsultarSesionUseCase;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.impl.ConsultarSesionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.CrearSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.interactor.CrearSesionInteractor;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.usecase.CrearSesionUseCase;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.usecase.impl.CrearSesionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.adapter.SesionRepositoryMockAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.adapter.SesionRepositorySqlServerAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration(proxyBeanMethods = false)
public class SesionBeansConfig {

    @Bean
    @Profile("!mock")
    public SesionRepositoryPort sesionRepositoryPort(final JdbcTemplate jdbcTemplate) {
        return new SesionRepositorySqlServerAdapter(jdbcTemplate);
    }

    @Bean
    @Profile("mock")
    public SesionRepositoryPort sesionRepositoryMockPort() {
        return new SesionRepositoryMockAdapter();
    }

    @Bean
    public CrearSesionUseCase crearSesionUseCase(final SesionRepositoryPort sesionRepositoryPort) {
        return new CrearSesionUseCaseImpl(sesionRepositoryPort);
    }

    @Bean
    public CrearSesionInputPort crearSesionInputPort(final CrearSesionUseCase crearSesionUseCase) {
        return new CrearSesionInteractor(crearSesionUseCase);
    }

    @Bean
    public ConsultarSesionUseCase consultarSesionUseCase(final SesionRepositoryPort sesionRepositoryPort) {
        return new ConsultarSesionUseCaseImpl(sesionRepositoryPort);
    }

    @Bean
    public ConsultarSesionInputPort consultarSesionInputPort(final ConsultarSesionUseCase consultarSesionUseCase) {
        return new ConsultarSesionInteractor(consultarSesionUseCase);
    }

    @Bean
    public CerrarSesionUseCase cerrarSesionUseCase(final SesionRepositoryPort sesionRepositoryPort) {
        return new CerrarSesionUseCaseImpl(sesionRepositoryPort);
    }

    @Bean
    public CerrarSesionInputPort cerrarSesionInputPort(final CerrarSesionUseCase cerrarSesionUseCase) {
        return new CerrarSesionInteractor(cerrarSesionUseCase);
    }
}
