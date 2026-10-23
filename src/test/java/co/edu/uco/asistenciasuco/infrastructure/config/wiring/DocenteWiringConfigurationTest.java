package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.primaryports.interactor.ConsultarAsignacionesAcademicasDocenteInteractor;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.usecase.impl.ConsultarAsignacionesAcademicasDocenteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.primaryports.interactor.ConsultarDocentePorIdInteractor;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.usecase.impl.ConsultarDocentePorIdUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.primaryports.interactor.ConsultarDocentesInteractor;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.usecase.impl.ConsultarDocentesUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.DocenteRepositoryPort;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

class DocenteWiringConfigurationTest {

    private final DocenteWiringConfiguration config = new DocenteWiringConfiguration();
    private final DocenteRepositoryPort docenteRepositoryPort = mock(DocenteRepositoryPort.class);

    @Test
    void todos_los_beans_se_construyen_con_el_adapter_esperado() {
        final var consultarDocentesUseCase = config.consultarDocentesUseCase(docenteRepositoryPort);
        assertInstanceOf(ConsultarDocentesUseCaseImpl.class, consultarDocentesUseCase);
        assertInstanceOf(ConsultarDocentesInteractor.class, config.consultarDocentesInputPort(consultarDocentesUseCase));

        final var consultarDocentePorIdUseCase = config.consultarDocentePorIdUseCase(docenteRepositoryPort);
        assertInstanceOf(ConsultarDocentePorIdUseCaseImpl.class, consultarDocentePorIdUseCase);
        assertInstanceOf(ConsultarDocentePorIdInteractor.class,
                config.consultarDocentePorIdInputPort(consultarDocentePorIdUseCase));

        final var consultarAsignacionesUseCase = config.consultarAsignacionesAcademicasDocenteUseCase(docenteRepositoryPort);
        assertInstanceOf(ConsultarAsignacionesAcademicasDocenteUseCaseImpl.class, consultarAsignacionesUseCase);
        assertInstanceOf(ConsultarAsignacionesAcademicasDocenteInteractor.class,
                config.consultarAsignacionesAcademicasDocenteInputPort(consultarAsignacionesUseCase));


    }
}
