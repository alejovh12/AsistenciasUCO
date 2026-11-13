package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import org.springframework.beans.factory.ObjectProvider;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.ConsultasTransversalesInputPort;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.interactor.ConsultasTransversalesInteractor;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.usecase.ConsultasTransversalesUseCase;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.usecase.impl.ConsultasTransversalesUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Consultas del decano y del administrador (HU112/HU128/HU143/HU157). Archivo propio para no tocar wirings compartidos. */
@Configuration(proxyBeanMethods = false)
public class ConsultasTransversalesWiringConfiguration {

    @Bean ConsultasTransversalesUseCase consultasTransversalesUseCase(
            final ConsultasTransversalesQueryPort queryPort,
            final ObjectProvider<ParameterCatalogPort> parameterCatalogPort
    ) {
        // PAR-06: el rango maximo de auditoria sale de AUDITORIA/RANGO_MAXIMO_DIAS (respaldo 90 dias).
        return new ConsultasTransversalesUseCaseImpl(queryPort, java.time.Clock.systemUTC(),
                parameterCatalogPort.getIfAvailable());
    }

    @Bean ConsultasTransversalesInputPort consultasTransversalesInputPort(final ConsultasTransversalesUseCase useCase) {
        return new ConsultasTransversalesInteractor(useCase);
    }
}
