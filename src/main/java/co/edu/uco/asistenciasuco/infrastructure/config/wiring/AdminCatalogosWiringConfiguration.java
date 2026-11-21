package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.admin.gestionarcategoriajustificacion.primaryports.GestionarCategoriaJustificacionInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarcategoriajustificacion.primaryports.interactor.GestionarCategoriaJustificacionInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarcategoriajustificacion.usecase.GestionarCategoriaJustificacionUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarcategoriajustificacion.usecase.impl.GestionarCategoriaJustificacionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarmotivocancelacion.primaryports.GestionarMotivoCancelacionInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarmotivocancelacion.primaryports.interactor.GestionarMotivoCancelacionInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarmotivocancelacion.usecase.GestionarMotivoCancelacionUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarmotivocancelacion.usecase.impl.GestionarMotivoCancelacionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CategoriaJustificacionAdminPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.MotivoCancelacionAdminPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composition Root de la administracion de categorias de justificacion y motivos de cancelacion de sesion (migracion 65 de la BD). */
@Configuration
public class AdminCatalogosWiringConfiguration {

    @Bean
    GestionarCategoriaJustificacionUseCase gestionarCategoriaJustificacionUseCase(final CategoriaJustificacionAdminPort port) {
        return new GestionarCategoriaJustificacionUseCaseImpl(port);
    }

    @Bean
    GestionarCategoriaJustificacionInputPort gestionarCategoriaJustificacionInputPort(final GestionarCategoriaJustificacionUseCase useCase) {
        return new GestionarCategoriaJustificacionInteractor(useCase);
    }

    @Bean
    GestionarMotivoCancelacionUseCase gestionarMotivoCancelacionUseCase(final MotivoCancelacionAdminPort port) {
        return new GestionarMotivoCancelacionUseCaseImpl(port);
    }

    @Bean
    GestionarMotivoCancelacionInputPort gestionarMotivoCancelacionInputPort(final GestionarMotivoCancelacionUseCase useCase) {
        return new GestionarMotivoCancelacionInteractor(useCase);
    }
}
