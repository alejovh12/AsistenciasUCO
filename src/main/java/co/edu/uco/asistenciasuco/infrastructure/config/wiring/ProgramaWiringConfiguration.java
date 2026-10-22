package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.decano.actualizarprograma.primaryports.ActualizarProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.actualizarprograma.primaryports.interactor.ActualizarProgramaInteractor;
import co.edu.uco.asistenciasuco.application.features.decano.actualizarprograma.usecase.ActualizarProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.actualizarprograma.usecase.impl.ActualizarProgramaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadoprograma.primaryports.CambiarEstadoProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadoprograma.primaryports.interactor.CambiarEstadoProgramaInteractor;
import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadoprograma.usecase.CambiarEstadoProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadoprograma.usecase.impl.CambiarEstadoProgramaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.primaryports.ConsultarProgramasInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.primaryports.interactor.ConsultarProgramasInteractor;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.usecase.ConsultarProgramasUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.usecase.impl.ConsultarProgramasUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.decano.crearprograma.primaryports.CrearProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.crearprograma.primaryports.interactor.CrearProgramaInteractor;
import co.edu.uco.asistenciasuco.application.features.decano.crearprograma.usecase.CrearProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.crearprograma.usecase.impl.CrearProgramaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.tipoprograma.consultarcatalogosprograma.primaryports.ConsultarCatalogosProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.tipoprograma.consultarcatalogosprograma.primaryports.interactor.ConsultarCatalogosProgramaInteractor;
import co.edu.uco.asistenciasuco.application.features.tipoprograma.consultarcatalogosprograma.usecase.ConsultarCatalogosProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.tipoprograma.consultarcatalogosprograma.usecase.impl.ConsultarCatalogosProgramaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CatalogoProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ProgramaAcademicoCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ProgramaAcademicoQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Cableado explicito de los casos de uso de programa academico (POJOs sin anotaciones de stereotipo). */
@Configuration(proxyBeanMethods = false)
public class ProgramaWiringConfiguration {

    @Bean CrearProgramaUseCase crearProgramaUseCase(final InstitutionalScopePort scopePort, final ProgramaAcademicoCommandPort commandPort, final ProgramaAcademicoQueryPort queryPort) { return new CrearProgramaUseCaseImpl(scopePort, commandPort, queryPort); }
    @Bean CrearProgramaInputPort crearProgramaInputPort(final CrearProgramaUseCase useCase) { return new CrearProgramaInteractor(useCase); }

    @Bean ActualizarProgramaUseCase actualizarProgramaUseCase(final InstitutionalScopePort scopePort, final ProgramaAcademicoCommandPort commandPort, final ProgramaAcademicoQueryPort queryPort) { return new ActualizarProgramaUseCaseImpl(scopePort, commandPort, queryPort); }
    @Bean ActualizarProgramaInputPort actualizarProgramaInputPort(final ActualizarProgramaUseCase useCase) { return new ActualizarProgramaInteractor(useCase); }

    @Bean CambiarEstadoProgramaUseCase cambiarEstadoProgramaUseCase(final InstitutionalScopePort scopePort, final ProgramaAcademicoCommandPort commandPort, final ProgramaAcademicoQueryPort queryPort) { return new CambiarEstadoProgramaUseCaseImpl(scopePort, commandPort, queryPort); }
    @Bean CambiarEstadoProgramaInputPort cambiarEstadoProgramaInputPort(final CambiarEstadoProgramaUseCase useCase) { return new CambiarEstadoProgramaInteractor(useCase); }

    @Bean ConsultarProgramasUseCase consultarProgramasUseCase(final InstitutionalScopePort scopePort, final ProgramaAcademicoQueryPort queryPort) { return new ConsultarProgramasUseCaseImpl(scopePort, queryPort); }
    @Bean ConsultarProgramasInputPort consultarProgramasInputPort(final ConsultarProgramasUseCase useCase) { return new ConsultarProgramasInteractor(useCase); }

    @Bean ConsultarCatalogosProgramaUseCase consultarCatalogosProgramaUseCase(final CatalogoProgramaQueryPort queryPort) { return new ConsultarCatalogosProgramaUseCaseImpl(queryPort); }
    @Bean ConsultarCatalogosProgramaInputPort consultarCatalogosProgramaInputPort(final ConsultarCatalogosProgramaUseCase useCase) { return new ConsultarCatalogosProgramaInteractor(useCase); }
}
