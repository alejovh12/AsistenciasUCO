package co.edu.uco.asistenciasuco.infrastructure.config.features;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.primaryports.ConsultarHorariosEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.primaryports.interactor.ConsultarHorariosEstudianteInteractor;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.usecase.ConsultarHorariosEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.usecase.impl.ConsultarHorariosEstudianteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.primaryports.ConsultarMateriasEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.primaryports.interactor.ConsultarMateriasEstudianteInteractor;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.usecase.ConsultarMateriasEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.usecase.impl.ConsultarMateriasEstudianteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.primaryports.ConsultarSesionesMateriaEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.primaryports.interactor.ConsultarSesionesMateriaEstudianteInteractor;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.ConsultarSesionesMateriaEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.impl.ConsultarSesionesMateriaEstudianteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.HorarioEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.MateriaEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.SesionMateriaEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class EstudianteFeatureConfiguration {

    @Bean ConsultarMateriasEstudianteUseCase consultarMateriasEstudianteUseCase(final InstitutionalScopePort scopePort, final MateriaEstudianteQueryPort queryPort) { return new ConsultarMateriasEstudianteUseCaseImpl(scopePort, queryPort); }
    @Bean ConsultarMateriasEstudianteInputPort consultarMateriasEstudianteInputPort(final ConsultarMateriasEstudianteUseCase useCase) { return new ConsultarMateriasEstudianteInteractor(useCase); }
    @Bean ConsultarHorariosEstudianteUseCase consultarHorariosEstudianteUseCase(final InstitutionalScopePort scopePort, final HorarioEstudianteQueryPort queryPort) { return new ConsultarHorariosEstudianteUseCaseImpl(scopePort, queryPort); }
    @Bean ConsultarHorariosEstudianteInputPort consultarHorariosEstudianteInputPort(final ConsultarHorariosEstudianteUseCase useCase) { return new ConsultarHorariosEstudianteInteractor(useCase); }
    @Bean ConsultarSesionesMateriaEstudianteUseCase consultarSesionesMateriaEstudianteUseCase(final InstitutionalScopePort scopePort, final SesionMateriaEstudianteQueryPort queryPort) { return new ConsultarSesionesMateriaEstudianteUseCaseImpl(scopePort, queryPort); }
    @Bean ConsultarSesionesMateriaEstudianteInputPort consultarSesionesMateriaEstudianteInputPort(final ConsultarSesionesMateriaEstudianteUseCase useCase) { return new ConsultarSesionesMateriaEstudianteInteractor(useCase); }
}
