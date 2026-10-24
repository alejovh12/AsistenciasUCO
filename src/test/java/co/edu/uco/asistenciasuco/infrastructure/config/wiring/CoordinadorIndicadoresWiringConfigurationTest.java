package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.primaryports.interactor.ConsultarAlertaTempranaInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.usecase.impl.ConsultarAlertaTempranaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.primaryports.interactor.ConsultarBitacoraEstudianteInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.usecase.impl.ConsultarBitacoraEstudianteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.primaryports.interactor.ConsultarFichaEstudianteInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.usecase.impl.ConsultarFichaEstudianteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.primaryports.interactor.ConsultarKpisCoordinadorInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.usecase.impl.ConsultarKpisCoordinadorUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorIndicadoresQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

/** T-BE-08: los casos de uso son POJOs cableados explicitamente. */
class CoordinadorIndicadoresWiringConfigurationTest {

    private final CoordinadorIndicadoresWiringConfiguration config = new CoordinadorIndicadoresWiringConfiguration();
    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final CoordinadorIndicadoresQueryPort queryPort = mock(CoordinadorIndicadoresQueryPort.class);

    @Test
    void kpis_se_cablea_con_su_caso_de_uso_e_interactor() {
        final var useCase = config.consultarKpisCoordinadorUseCase(scopePort, queryPort);
        assertInstanceOf(ConsultarKpisCoordinadorUseCaseImpl.class, useCase);
        assertInstanceOf(ConsultarKpisCoordinadorInteractor.class, config.consultarKpisCoordinadorInputPort(useCase));
    }

    @Test
    void alerta_temprana_se_cablea_con_su_caso_de_uso_e_interactor() {
        final var useCase = config.consultarAlertaTempranaUseCase(scopePort, queryPort);
        assertInstanceOf(ConsultarAlertaTempranaUseCaseImpl.class, useCase);
        assertInstanceOf(ConsultarAlertaTempranaInteractor.class, config.consultarAlertaTempranaInputPort(useCase));
    }

    @Test
    void bitacora_se_cablea_con_su_caso_de_uso_e_interactor() {
        final var useCase = config.consultarBitacoraEstudianteUseCase(scopePort, queryPort);
        assertInstanceOf(ConsultarBitacoraEstudianteUseCaseImpl.class, useCase);
        assertInstanceOf(ConsultarBitacoraEstudianteInteractor.class, config.consultarBitacoraEstudianteInputPort(useCase));
    }

    @Test
    void ficha_se_cablea_con_su_caso_de_uso_e_interactor() {
        final var useCase = config.consultarFichaEstudianteUseCase(scopePort, queryPort);
        assertInstanceOf(ConsultarFichaEstudianteUseCaseImpl.class, useCase);
        assertInstanceOf(ConsultarFichaEstudianteInteractor.class, config.consultarFichaEstudianteInputPort(useCase));
    }
}
