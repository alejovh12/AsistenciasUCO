package co.edu.uco.asistenciasuco.infrastructure.config.features;

import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports.ConsultarTiposIdentificacionInputPort;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports.interactor.ConsultarTiposIdentificacionInteractor;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.usecase.ConsultarTiposIdentificacionUseCase;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.usecase.impl.ConsultarTiposIdentificacionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.TipoIdentificacionRepositoryPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.adapter.TipoIdentificacionRepositorySqlServerAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration(proxyBeanMethods = false)
public class TipoIdentificacionBeansConfig {

    @Bean
    public TipoIdentificacionRepositoryPort tipoIdentificacionRepositoryPort(final JdbcTemplate jdbcTemplate) {
        return new TipoIdentificacionRepositorySqlServerAdapter(jdbcTemplate);
    }

    @Bean
    public ConsultarTiposIdentificacionUseCase consultarTiposIdentificacionUseCase(
            final TipoIdentificacionRepositoryPort tipoIdentificacionRepositoryPort
    ) {
        return new ConsultarTiposIdentificacionUseCaseImpl(tipoIdentificacionRepositoryPort);
    }

    @Bean
    public ConsultarTiposIdentificacionInputPort consultarTiposIdentificacionInputPort(
            final ConsultarTiposIdentificacionUseCase consultarTiposIdentificacionUseCase
    ) {
        return new ConsultarTiposIdentificacionInteractor(consultarTiposIdentificacionUseCase);
    }
}
