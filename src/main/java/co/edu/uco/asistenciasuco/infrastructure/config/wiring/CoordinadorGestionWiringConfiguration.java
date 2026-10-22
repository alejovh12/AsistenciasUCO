package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.decano.actualizarcoordinador.primaryports.ActualizarCoordinadorInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.actualizarcoordinador.primaryports.interactor.ActualizarCoordinadorInteractor;
import co.edu.uco.asistenciasuco.application.features.decano.actualizarcoordinador.usecase.ActualizarCoordinadorUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.actualizarcoordinador.usecase.impl.ActualizarCoordinadorUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadocoordinador.primaryports.CambiarEstadoCoordinadorInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadocoordinador.primaryports.interactor.CambiarEstadoCoordinadorInteractor;
import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadocoordinador.usecase.CambiarEstadoCoordinadorUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadocoordinador.usecase.impl.CambiarEstadoCoordinadorUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.identidad.sincronizarcuenta.SincronizarCuentaInstitucionalService;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition Root de la gestion del coordinador por el decano (HU121/HU122). Separado de
 * {@link DecanoWiringConfiguration} para no chocar con otros work items sobre ese archivo (D10).
 */
@Configuration(proxyBeanMethods = false)
public class CoordinadorGestionWiringConfiguration {

    @Bean
    ActualizarCoordinadorUseCase actualizarCoordinadorUseCase(
            final CoordinadorCommandPort commandPort,
            final InstitutionalScopePort scopePort,
            final SincronizarCuentaInstitucionalService sincronizacion,
            final MessageCatalogPort messageCatalog
    ) {
        return new ActualizarCoordinadorUseCaseImpl(commandPort, scopePort, sincronizacion, messageCatalog);
    }

    @Bean
    ActualizarCoordinadorInputPort actualizarCoordinadorInputPort(final ActualizarCoordinadorUseCase useCase) {
        return new ActualizarCoordinadorInteractor(useCase);
    }

    @Bean
    CambiarEstadoCoordinadorUseCase cambiarEstadoCoordinadorUseCase(
            final CoordinadorCommandPort commandPort,
            final InstitutionalScopePort scopePort,
            final SincronizarCuentaInstitucionalService sincronizacion,
            final MessageCatalogPort messageCatalog
    ) {
        return new CambiarEstadoCoordinadorUseCaseImpl(commandPort, scopePort, sincronizacion, messageCatalog);
    }

    @Bean
    CambiarEstadoCoordinadorInputPort cambiarEstadoCoordinadorInputPort(final CambiarEstadoCoordinadorUseCase useCase) {
        return new CambiarEstadoCoordinadorInteractor(useCase);
    }
}
