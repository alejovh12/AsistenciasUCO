package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.primaryports.ConsultarAforoGruposInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.primaryports.interactor.ConsultarAforoGruposInteractor;
import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.usecase.ConsultarAforoGruposUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.usecase.impl.ConsultarAforoGruposUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.primaryports.ConsultarComparativaProgramasInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.primaryports.interactor.ConsultarComparativaProgramasInteractor;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.usecase.ConsultarComparativaProgramasUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.usecase.impl.ConsultarComparativaProgramasUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.primaryports.ConsultarKpisDecanoInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.primaryports.interactor.ConsultarKpisDecanoInteractor;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.usecase.ConsultarKpisDecanoUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.usecase.impl.ConsultarKpisDecanoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DecanoIndicadoresQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Cableado explicito de los casos de uso de KPIs del decano (POJOs sin estereotipos). */
@Configuration(proxyBeanMethods = false)
public class DecanoIndicadoresWiringConfiguration {

    @Bean ConsultarKpisDecanoUseCase consultarKpisDecanoUseCase(final InstitutionalScopePort scopePort, final DecanoIndicadoresQueryPort queryPort) { return new ConsultarKpisDecanoUseCaseImpl(scopePort, queryPort); }
    @Bean ConsultarKpisDecanoInputPort consultarKpisDecanoInputPort(final ConsultarKpisDecanoUseCase useCase) { return new ConsultarKpisDecanoInteractor(useCase); }

    @Bean ConsultarComparativaProgramasUseCase consultarComparativaProgramasUseCase(final InstitutionalScopePort scopePort, final DecanoIndicadoresQueryPort queryPort) { return new ConsultarComparativaProgramasUseCaseImpl(scopePort, queryPort); }
    @Bean ConsultarComparativaProgramasInputPort consultarComparativaProgramasInputPort(final ConsultarComparativaProgramasUseCase useCase) { return new ConsultarComparativaProgramasInteractor(useCase); }

    @Bean ConsultarAforoGruposUseCase consultarAforoGruposUseCase(final InstitutionalScopePort scopePort, final DecanoIndicadoresQueryPort queryPort) { return new ConsultarAforoGruposUseCaseImpl(scopePort, queryPort); }
    @Bean ConsultarAforoGruposInputPort consultarAforoGruposInputPort(final ConsultarAforoGruposUseCase useCase) { return new ConsultarAforoGruposInteractor(useCase); }
}
