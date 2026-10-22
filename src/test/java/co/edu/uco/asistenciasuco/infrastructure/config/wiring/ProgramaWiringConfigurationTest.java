package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.decano.actualizarprograma.primaryports.interactor.ActualizarProgramaInteractor;
import co.edu.uco.asistenciasuco.application.features.decano.actualizarprograma.usecase.impl.ActualizarProgramaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadoprograma.primaryports.interactor.CambiarEstadoProgramaInteractor;
import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadoprograma.usecase.impl.CambiarEstadoProgramaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.primaryports.interactor.ConsultarProgramasInteractor;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.usecase.impl.ConsultarProgramasUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.decano.crearprograma.primaryports.interactor.CrearProgramaInteractor;
import co.edu.uco.asistenciasuco.application.features.decano.crearprograma.usecase.impl.CrearProgramaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.tipoprograma.consultarcatalogosprograma.primaryports.interactor.ConsultarCatalogosProgramaInteractor;
import co.edu.uco.asistenciasuco.application.features.tipoprograma.consultarcatalogosprograma.usecase.impl.ConsultarCatalogosProgramaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CatalogoProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ProgramaAcademicoCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ProgramaAcademicoQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

/** T-WIR-01: los casos de uso son POJOs cableados explicitamente (sin @Service). */
class ProgramaWiringConfigurationTest {

    private final ProgramaWiringConfiguration config = new ProgramaWiringConfiguration();
    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final ProgramaAcademicoCommandPort commandPort = mock(ProgramaAcademicoCommandPort.class);
    private final ProgramaAcademicoQueryPort queryPort = mock(ProgramaAcademicoQueryPort.class);

    @Test
    void crear_programa_se_cablea_con_su_caso_de_uso_e_interactor() {
        final var useCase = config.crearProgramaUseCase(scopePort, commandPort, queryPort);
        assertInstanceOf(CrearProgramaUseCaseImpl.class, useCase);
        assertInstanceOf(CrearProgramaInteractor.class, config.crearProgramaInputPort(useCase));
    }

    @Test
    void actualizar_programa_se_cablea_con_su_caso_de_uso_e_interactor() {
        final var useCase = config.actualizarProgramaUseCase(scopePort, commandPort, queryPort);
        assertInstanceOf(ActualizarProgramaUseCaseImpl.class, useCase);
        assertInstanceOf(ActualizarProgramaInteractor.class, config.actualizarProgramaInputPort(useCase));
    }

    @Test
    void cambiar_estado_programa_se_cablea_con_su_caso_de_uso_e_interactor() {
        final var useCase = config.cambiarEstadoProgramaUseCase(scopePort, commandPort, queryPort);
        assertInstanceOf(CambiarEstadoProgramaUseCaseImpl.class, useCase);
        assertInstanceOf(CambiarEstadoProgramaInteractor.class, config.cambiarEstadoProgramaInputPort(useCase));
    }

    @Test
    void consultar_programas_se_cablea_con_su_caso_de_uso_e_interactor() {
        final var useCase = config.consultarProgramasUseCase(scopePort, queryPort);
        assertInstanceOf(ConsultarProgramasUseCaseImpl.class, useCase);
        assertInstanceOf(ConsultarProgramasInteractor.class, config.consultarProgramasInputPort(useCase));
    }

    @Test
    void catalogos_de_programa_se_cablean_con_su_caso_de_uso_e_interactor() {
        final var useCase = config.consultarCatalogosProgramaUseCase(mock(CatalogoProgramaQueryPort.class));
        assertInstanceOf(ConsultarCatalogosProgramaUseCaseImpl.class, useCase);
        assertInstanceOf(ConsultarCatalogosProgramaInteractor.class, config.consultarCatalogosProgramaInputPort(useCase));
    }
}
