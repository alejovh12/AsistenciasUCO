package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.admin.consultarcatalogossistema.primaryports.ConsultarCatalogosSistemaInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcatalogossistema.primaryports.interactor.ConsultarCatalogosSistemaInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcatalogossistema.usecase.ConsultarCatalogosSistemaUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcatalogossistema.usecase.impl.ConsultarCatalogosSistemaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CatalogosSistemaConsultaPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composition Root de la consulta de catalogos del sistema para el ADMINISTRADOR. */
@Configuration
public class AdminCatalogosWiringConfiguration {

    @Bean
    ConsultarCatalogosSistemaUseCase consultarCatalogosSistemaUseCase(final CatalogosSistemaConsultaPort port) {
        return new ConsultarCatalogosSistemaUseCaseImpl(port);
    }

    @Bean
    ConsultarCatalogosSistemaInputPort consultarCatalogosSistemaInputPort(final ConsultarCatalogosSistemaUseCase useCase) {
        return new ConsultarCatalogosSistemaInteractor(useCase);
    }
}
