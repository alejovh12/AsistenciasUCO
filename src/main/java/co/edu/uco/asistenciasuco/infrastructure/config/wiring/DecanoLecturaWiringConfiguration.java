package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.decano.consultargrupos.primaryports.ConsultarGruposDecanoInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.consultargrupos.primaryports.interactor.ConsultarGruposDecanoInteractor;
import co.edu.uco.asistenciasuco.application.features.decano.consultargrupos.usecase.ConsultarGruposDecanoUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.consultargrupos.usecase.impl.ConsultarGruposDecanoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.GrupoFacultadQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition Root de las lecturas del portal del decano con ambito de facultad. Archivo propio para no compartir
 * {@link DecanoWiringConfiguration} con otros work items.
 */
@Configuration(proxyBeanMethods = false)
public class DecanoLecturaWiringConfiguration {

    @Bean
    ConsultarGruposDecanoUseCase consultarGruposDecanoUseCase(final InstitutionalScopePort scopePort,
                                                              final GrupoFacultadQueryPort queryPort) {
        return new ConsultarGruposDecanoUseCaseImpl(scopePort, queryPort);
    }

    @Bean
    ConsultarGruposDecanoInputPort consultarGruposDecanoInputPort(final ConsultarGruposDecanoUseCase useCase) {
        return new ConsultarGruposDecanoInteractor(useCase);
    }
}
