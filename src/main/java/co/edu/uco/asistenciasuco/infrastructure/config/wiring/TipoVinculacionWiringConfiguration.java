package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.tipovinculacion.consultartiposvinculacion.primaryports.ConsultarTiposVinculacionInputPort;
import co.edu.uco.asistenciasuco.application.features.tipovinculacion.consultartiposvinculacion.primaryports.interactor.ConsultarTiposVinculacionInteractor;
import co.edu.uco.asistenciasuco.application.features.tipovinculacion.consultartiposvinculacion.usecase.ConsultarTiposVinculacionUseCase;
import co.edu.uco.asistenciasuco.application.features.tipovinculacion.consultartiposvinculacion.usecase.impl.ConsultarTiposVinculacionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.TipoVinculacionQueryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class TipoVinculacionWiringConfiguration {

    @Bean ConsultarTiposVinculacionUseCase consultarTiposVinculacionUseCase(final TipoVinculacionQueryPort queryPort) {
        return new ConsultarTiposVinculacionUseCaseImpl(queryPort);
    }

    @Bean ConsultarTiposVinculacionInputPort consultarTiposVinculacionInputPort(final ConsultarTiposVinculacionUseCase useCase) {
        return new ConsultarTiposVinculacionInteractor(useCase);
    }
}
