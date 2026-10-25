package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

import co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.primaryports.interactor.ConsultarComparativaFacultadesInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.usecase.impl.ConsultarComparativaFacultadesUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.primaryports.interactor.ConsultarKpisInstitucionInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.usecase.impl.ConsultarKpisInstitucionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.primaryports.interactor.ConsultarTendenciaAusentismoInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.usecase.impl.ConsultarTendenciaAusentismoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AdminIndicadoresQueryPort;
import org.junit.jupiter.api.Test;

/** Los casos de uso son POJOs cableados explicitamente. */
class AdminIndicadoresWiringConfigurationTest {

    private final AdminIndicadoresWiringConfiguration config = new AdminIndicadoresWiringConfiguration();
    private final AdminIndicadoresQueryPort queryPort = mock(AdminIndicadoresQueryPort.class);

    @Test
    void kpis_se_cablea_con_su_caso_de_uso_e_interactor() {
        final var useCase = config.consultarKpisInstitucionUseCase(queryPort);
        assertInstanceOf(ConsultarKpisInstitucionUseCaseImpl.class, useCase);
        assertInstanceOf(ConsultarKpisInstitucionInteractor.class, config.consultarKpisInstitucionInputPort(useCase));
    }

    @Test
    void comparativa_se_cablea_con_su_caso_de_uso_e_interactor() {
        final var useCase = config.consultarComparativaFacultadesUseCase(queryPort);
        assertInstanceOf(ConsultarComparativaFacultadesUseCaseImpl.class, useCase);
        assertInstanceOf(ConsultarComparativaFacultadesInteractor.class, config.consultarComparativaFacultadesInputPort(useCase));
    }

    @Test
    void tendencia_se_cablea_con_su_caso_de_uso_e_interactor() {
        final var useCase = config.consultarTendenciaAusentismoUseCase(queryPort);
        assertInstanceOf(ConsultarTendenciaAusentismoUseCaseImpl.class, useCase);
        assertInstanceOf(ConsultarTendenciaAusentismoInteractor.class, config.consultarTendenciaAusentismoInputPort(useCase));
    }
}
