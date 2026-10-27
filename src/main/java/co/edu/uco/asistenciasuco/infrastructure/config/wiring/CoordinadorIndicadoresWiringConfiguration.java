package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.primaryports.ConsultarAlertaTempranaInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.primaryports.interactor.ConsultarAlertaTempranaInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.usecase.ConsultarAlertaTempranaUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.usecase.impl.ConsultarAlertaTempranaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.primaryports.ConsultarBitacoraEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.primaryports.interactor.ConsultarBitacoraEstudianteInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.usecase.ConsultarBitacoraEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.usecase.impl.ConsultarBitacoraEstudianteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.primaryports.ConsultarFichaEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.primaryports.interactor.ConsultarFichaEstudianteInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.usecase.ConsultarFichaEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.usecase.impl.ConsultarFichaEstudianteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.primaryports.ConsultarKpisCoordinadorInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.primaryports.interactor.ConsultarKpisCoordinadorInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.usecase.ConsultarKpisCoordinadorUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.usecase.impl.ConsultarKpisCoordinadorUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorIndicadoresQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Cableado explicito de los casos de uso de KPIs del coordinador (POJOs sin estereotipos). */
@Configuration(proxyBeanMethods = false)
public class CoordinadorIndicadoresWiringConfiguration {

    @Bean ConsultarKpisCoordinadorUseCase consultarKpisCoordinadorUseCase(final InstitutionalScopePort scopePort, final CoordinadorIndicadoresQueryPort queryPort) { return new ConsultarKpisCoordinadorUseCaseImpl(scopePort, queryPort); }
    @Bean ConsultarKpisCoordinadorInputPort consultarKpisCoordinadorInputPort(final ConsultarKpisCoordinadorUseCase useCase) { return new ConsultarKpisCoordinadorInteractor(useCase); }

    @Bean ConsultarAlertaTempranaUseCase consultarAlertaTempranaUseCase(final InstitutionalScopePort scopePort, final CoordinadorIndicadoresQueryPort queryPort) { return new ConsultarAlertaTempranaUseCaseImpl(scopePort, queryPort); }
    @Bean ConsultarAlertaTempranaInputPort consultarAlertaTempranaInputPort(final ConsultarAlertaTempranaUseCase useCase) { return new ConsultarAlertaTempranaInteractor(useCase); }

    @Bean ConsultarBitacoraEstudianteUseCase consultarBitacoraEstudianteUseCase(final InstitutionalScopePort scopePort, final CoordinadorIndicadoresQueryPort queryPort) { return new ConsultarBitacoraEstudianteUseCaseImpl(scopePort, queryPort); }
    @Bean ConsultarBitacoraEstudianteInputPort consultarBitacoraEstudianteInputPort(final ConsultarBitacoraEstudianteUseCase useCase) { return new ConsultarBitacoraEstudianteInteractor(useCase); }

    @Bean ConsultarFichaEstudianteUseCase consultarFichaEstudianteUseCase(final InstitutionalScopePort scopePort, final CoordinadorIndicadoresQueryPort queryPort) { return new ConsultarFichaEstudianteUseCaseImpl(scopePort, queryPort); }
    @Bean ConsultarFichaEstudianteInputPort consultarFichaEstudianteInputPort(final ConsultarFichaEstudianteUseCase useCase) { return new ConsultarFichaEstudianteInteractor(useCase); }
}
