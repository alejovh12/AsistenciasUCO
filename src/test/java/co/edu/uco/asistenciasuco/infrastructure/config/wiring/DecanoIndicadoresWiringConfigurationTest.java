package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.primaryports.interactor.ConsultarAforoGruposInteractor;
import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.usecase.impl.ConsultarAforoGruposUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.primaryports.interactor.ConsultarComparativaProgramasInteractor;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.usecase.impl.ConsultarComparativaProgramasUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.primaryports.interactor.ConsultarKpisDecanoInteractor;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.usecase.impl.ConsultarKpisDecanoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DecanoIndicadoresQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.Test;

/** Los casos de uso son POJOs cableados explicitamente. */
class DecanoIndicadoresWiringConfigurationTest {

    private final DecanoIndicadoresWiringConfiguration config = new DecanoIndicadoresWiringConfiguration();
    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final DecanoIndicadoresQueryPort queryPort = mock(DecanoIndicadoresQueryPort.class);

    @Test
    void kpis_se_cablea_con_su_caso_de_uso_e_interactor() {
        final var useCase = config.consultarKpisDecanoUseCase(scopePort, queryPort);
        assertInstanceOf(ConsultarKpisDecanoUseCaseImpl.class, useCase);
        assertInstanceOf(ConsultarKpisDecanoInteractor.class, config.consultarKpisDecanoInputPort(useCase));
    }

    @Test
    void comparativa_se_cablea_con_su_caso_de_uso_e_interactor() {
        final var useCase = config.consultarComparativaProgramasUseCase(scopePort, queryPort);
        assertInstanceOf(ConsultarComparativaProgramasUseCaseImpl.class, useCase);
        assertInstanceOf(ConsultarComparativaProgramasInteractor.class, config.consultarComparativaProgramasInputPort(useCase));
    }

    @Test
    void aforo_se_cablea_con_su_caso_de_uso_e_interactor() {
        final var useCase = config.consultarAforoGruposUseCase(scopePort, queryPort);
        assertInstanceOf(ConsultarAforoGruposUseCaseImpl.class, useCase);
        assertInstanceOf(ConsultarAforoGruposInteractor.class, config.consultarAforoGruposInputPort(useCase));
    }
}
