package co.edu.uco.asistenciasuco.infrastructure.config.features;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.primaryports.ConsultarEstudiantePorIdInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.primaryports.interactor.ConsultarEstudiantePorIdInteractor;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.usecase.ConsultarEstudiantePorIdUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.usecase.impl.ConsultarEstudiantePorIdUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.primaryports.ConsultarEstudiantesInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.primaryports.interactor.ConsultarEstudiantesInteractor;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.usecase.ConsultarEstudiantesUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.usecase.impl.ConsultarEstudiantesUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.EstudianteRepositoryPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.EstudianteRepositorySqlServerAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration(proxyBeanMethods = false)
public class EstudianteBeansConfig {

    @Bean
    public EstudianteRepositoryPort estudianteRepositoryPort(final JdbcTemplate jdbcTemplate) {
        return new EstudianteRepositorySqlServerAdapter(jdbcTemplate);
    }

    @Bean
    public ConsultarEstudiantesUseCase consultarEstudiantesUseCase(
            final EstudianteRepositoryPort estudianteRepositoryPort
    ) {
        return new ConsultarEstudiantesUseCaseImpl(estudianteRepositoryPort);
    }

    @Bean
    public ConsultarEstudiantesInputPort consultarEstudiantesInputPort(
            final ConsultarEstudiantesUseCase consultarEstudiantesUseCase
    ) {
        return new ConsultarEstudiantesInteractor(consultarEstudiantesUseCase);
    }

    @Bean
    public ConsultarEstudiantePorIdUseCase consultarEstudiantePorIdUseCase(
            final EstudianteRepositoryPort estudianteRepositoryPort
    ) {
        return new ConsultarEstudiantePorIdUseCaseImpl(estudianteRepositoryPort);
    }

    @Bean
    public ConsultarEstudiantePorIdInputPort consultarEstudiantePorIdInputPort(
            final ConsultarEstudiantePorIdUseCase consultarEstudiantePorIdUseCase
    ) {
        return new ConsultarEstudiantePorIdInteractor(consultarEstudiantePorIdUseCase);
    }
}
