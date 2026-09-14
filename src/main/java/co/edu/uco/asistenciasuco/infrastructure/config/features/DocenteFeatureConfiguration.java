package co.edu.uco.asistenciasuco.infrastructure.config.features;

import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.primaryports.ConsultarAsignaturasDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.primaryports.interactor.ConsultarAsignaturasDocenteInteractor;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.usecase.ConsultarAsignaturasDocenteUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.usecase.impl.ConsultarAsignaturasDocenteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.primaryports.ConsultarHorariosDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.primaryports.interactor.ConsultarHorariosDocenteInteractor;
import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.usecase.ConsultarHorariosDocenteUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.usecase.impl.ConsultarHorariosDocenteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsignaturaDocenteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.HorarioDocenteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class DocenteFeatureConfiguration {

    @Bean ConsultarHorariosDocenteUseCase consultarHorariosDocenteUseCase(final InstitutionalScopePort scopePort, final HorarioDocenteQueryPort queryPort) { return new ConsultarHorariosDocenteUseCaseImpl(scopePort, queryPort); }
    @Bean ConsultarHorariosDocenteInputPort consultarHorariosDocenteInputPort(final ConsultarHorariosDocenteUseCase useCase) { return new ConsultarHorariosDocenteInteractor(useCase); }
    @Bean ConsultarAsignaturasDocenteUseCase consultarAsignaturasDocenteUseCase(final InstitutionalScopePort scopePort, final AsignaturaDocenteQueryPort queryPort) { return new ConsultarAsignaturasDocenteUseCaseImpl(scopePort, queryPort); }
    @Bean ConsultarAsignaturasDocenteInputPort consultarAsignaturasDocenteInputPort(final ConsultarAsignaturasDocenteUseCase useCase) { return new ConsultarAsignaturasDocenteInteractor(useCase); }
}
