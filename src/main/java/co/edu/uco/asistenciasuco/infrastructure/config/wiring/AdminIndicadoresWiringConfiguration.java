package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.primaryports.ConsultarComparativaFacultadesInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.primaryports.interactor.ConsultarComparativaFacultadesInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.usecase.ConsultarComparativaFacultadesUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.usecase.impl.ConsultarComparativaFacultadesUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.primaryports.ConsultarKpisInstitucionInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.primaryports.interactor.ConsultarKpisInstitucionInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.usecase.ConsultarKpisInstitucionUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.usecase.impl.ConsultarKpisInstitucionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.primaryports.ConsultarTendenciaAusentismoInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.primaryports.interactor.ConsultarTendenciaAusentismoInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.usecase.ConsultarTendenciaAusentismoUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.usecase.impl.ConsultarTendenciaAusentismoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AdminIndicadoresQueryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Cableado explicito de los casos de uso de KPIs del administrador (POJOs sin estereotipos). */
@Configuration(proxyBeanMethods = false)
public class AdminIndicadoresWiringConfiguration {

    @Bean ConsultarKpisInstitucionUseCase consultarKpisInstitucionUseCase(final AdminIndicadoresQueryPort queryPort) { return new ConsultarKpisInstitucionUseCaseImpl(queryPort); }
    @Bean ConsultarKpisInstitucionInputPort consultarKpisInstitucionInputPort(final ConsultarKpisInstitucionUseCase useCase) { return new ConsultarKpisInstitucionInteractor(useCase); }

    @Bean ConsultarComparativaFacultadesUseCase consultarComparativaFacultadesUseCase(final AdminIndicadoresQueryPort queryPort) { return new ConsultarComparativaFacultadesUseCaseImpl(queryPort); }
    @Bean ConsultarComparativaFacultadesInputPort consultarComparativaFacultadesInputPort(final ConsultarComparativaFacultadesUseCase useCase) { return new ConsultarComparativaFacultadesInteractor(useCase); }

    @Bean ConsultarTendenciaAusentismoUseCase consultarTendenciaAusentismoUseCase(final AdminIndicadoresQueryPort queryPort) { return new ConsultarTendenciaAusentismoUseCaseImpl(queryPort); }
    @Bean ConsultarTendenciaAusentismoInputPort consultarTendenciaAusentismoInputPort(final ConsultarTendenciaAusentismoUseCase useCase) { return new ConsultarTendenciaAusentismoInteractor(useCase); }
}
