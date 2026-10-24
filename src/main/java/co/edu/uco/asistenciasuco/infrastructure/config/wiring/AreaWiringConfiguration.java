package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.admin.actualizararea.primaryports.ActualizarAreaInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.actualizararea.primaryports.interactor.ActualizarAreaInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.actualizararea.usecase.ActualizarAreaUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.actualizararea.usecase.impl.ActualizarAreaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadoarea.primaryports.CambiarEstadoAreaInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadoarea.primaryports.interactor.CambiarEstadoAreaInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadoarea.usecase.CambiarEstadoAreaUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadoarea.usecase.impl.CambiarEstadoAreaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.admin.creararea.primaryports.CrearAreaInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.creararea.primaryports.interactor.CrearAreaInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.creararea.usecase.CrearAreaUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.creararea.usecase.impl.CrearAreaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AreaCommandPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composition Root de los casos de uso del catalogo de areas de conocimiento (HU151 / HU152). */
@Configuration(proxyBeanMethods = false)
public class AreaWiringConfiguration {

    @Bean CrearAreaUseCase crearAreaUseCase(final AreaCommandPort port) { return new CrearAreaUseCaseImpl(port); }
    @Bean CrearAreaInputPort crearAreaInputPort(final CrearAreaUseCase useCase) { return new CrearAreaInteractor(useCase); }
    @Bean ActualizarAreaUseCase actualizarAreaUseCase(final AreaCommandPort port) { return new ActualizarAreaUseCaseImpl(port); }
    @Bean ActualizarAreaInputPort actualizarAreaInputPort(final ActualizarAreaUseCase useCase) { return new ActualizarAreaInteractor(useCase); }
    @Bean CambiarEstadoAreaUseCase cambiarEstadoAreaUseCase(final AreaCommandPort port) { return new CambiarEstadoAreaUseCaseImpl(port); }
    @Bean CambiarEstadoAreaInputPort cambiarEstadoAreaInputPort(final CambiarEstadoAreaUseCase useCase) { return new CambiarEstadoAreaInteractor(useCase); }
}
