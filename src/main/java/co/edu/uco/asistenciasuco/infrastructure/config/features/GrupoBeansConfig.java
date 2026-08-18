package co.edu.uco.asistenciasuco.infrastructure.config.features;

import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.primaryports.ConsultarGruposInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.primaryports.interactor.ConsultarGruposInteractor;
import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.usecase.ConsultarGruposUseCase;
import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.usecase.impl.ConsultarGruposUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.RegistrarEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.interactor.RegistrarEstudianteInteractor;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.RegistrarEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.impl.RegistrarEstudianteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.GrupoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.GrupoRepositorySqlServerAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods = false)
public class GrupoBeansConfig {

    @Bean
    public GrupoRepositoryPort grupoRepositoryPort(
            final JdbcTemplate jdbcTemplate,
            final TransactionTemplate transactionTemplate
    ) {
        return new GrupoRepositorySqlServerAdapter(jdbcTemplate, transactionTemplate);
    }

    @Bean
    public RegistrarEstudianteUseCase registrarEstudianteUseCase(
            final GrupoRepositoryPort grupoRepositoryPort,
            final UsuarioRepositoryPort usuarioRepositoryPort,
            final PasswordEncoderPort passwordEncoderPort
    ) {
        return new RegistrarEstudianteUseCaseImpl(grupoRepositoryPort, usuarioRepositoryPort, passwordEncoderPort);
    }

    @Bean
    public RegistrarEstudianteInputPort registrarEstudianteInputPort(
            final RegistrarEstudianteUseCase registrarEstudianteUseCase
    ) {
        return new RegistrarEstudianteInteractor(registrarEstudianteUseCase);
    }

    @Bean
    public ConsultarGruposUseCase consultarGruposUseCase(final GrupoRepositoryPort grupoRepositoryPort) {
        return new ConsultarGruposUseCaseImpl(grupoRepositoryPort);
    }

    @Bean
    public ConsultarGruposInputPort consultarGruposInputPort(final ConsultarGruposUseCase consultarGruposUseCase) {
        return new ConsultarGruposInteractor(consultarGruposUseCase);
    }
}
