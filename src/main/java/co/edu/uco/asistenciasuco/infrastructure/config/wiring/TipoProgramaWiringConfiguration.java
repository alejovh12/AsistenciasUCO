package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.admin.actualizartipoprograma.primaryports.ActualizarTipoProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.actualizartipoprograma.primaryports.interactor.ActualizarTipoProgramaInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.actualizartipoprograma.usecase.ActualizarTipoProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.actualizartipoprograma.usecase.impl.ActualizarTipoProgramaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadotipoprograma.primaryports.CambiarEstadoTipoProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadotipoprograma.primaryports.interactor.CambiarEstadoTipoProgramaInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadotipoprograma.usecase.CambiarEstadoTipoProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadotipoprograma.usecase.impl.CambiarEstadoTipoProgramaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.admin.consultartiposprograma.primaryports.ConsultarTiposProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultartiposprograma.primaryports.interactor.ConsultarTiposProgramaInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.consultartiposprograma.usecase.ConsultarTiposProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.consultartiposprograma.usecase.impl.ConsultarTiposProgramaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.admin.creartipoprograma.primaryports.CrearTipoProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.creartipoprograma.primaryports.interactor.CrearTipoProgramaInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.creartipoprograma.usecase.CrearTipoProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.creartipoprograma.usecase.impl.CrearTipoProgramaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.TipoProgramaCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.TipoProgramaQueryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composition Root de los casos de uso del catalogo de tipos de programa (HU023 / HU154). */
@Configuration(proxyBeanMethods = false)
public class TipoProgramaWiringConfiguration {

    @Bean ConsultarTiposProgramaUseCase consultarTiposProgramaUseCase(final TipoProgramaQueryPort port) { return new ConsultarTiposProgramaUseCaseImpl(port); }
    @Bean ConsultarTiposProgramaInputPort consultarTiposProgramaInputPort(final ConsultarTiposProgramaUseCase useCase) { return new ConsultarTiposProgramaInteractor(useCase); }
    @Bean CrearTipoProgramaUseCase crearTipoProgramaUseCase(final TipoProgramaCommandPort port) { return new CrearTipoProgramaUseCaseImpl(port); }
    @Bean CrearTipoProgramaInputPort crearTipoProgramaInputPort(final CrearTipoProgramaUseCase useCase) { return new CrearTipoProgramaInteractor(useCase); }
    @Bean ActualizarTipoProgramaUseCase actualizarTipoProgramaUseCase(final TipoProgramaCommandPort port) { return new ActualizarTipoProgramaUseCaseImpl(port); }
    @Bean ActualizarTipoProgramaInputPort actualizarTipoProgramaInputPort(final ActualizarTipoProgramaUseCase useCase) { return new ActualizarTipoProgramaInteractor(useCase); }
    @Bean CambiarEstadoTipoProgramaUseCase cambiarEstadoTipoProgramaUseCase(final TipoProgramaCommandPort port) { return new CambiarEstadoTipoProgramaUseCaseImpl(port); }
    @Bean CambiarEstadoTipoProgramaInputPort cambiarEstadoTipoProgramaInputPort(final CambiarEstadoTipoProgramaUseCase useCase) { return new CambiarEstadoTipoProgramaInteractor(useCase); }
}
